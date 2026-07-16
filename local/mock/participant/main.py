#
# Copyright (c) 2026 Volkswagen AG
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#
"""
Mock participant — single-container mock for n-tier supply-chain simulation.

Implements the provider side of:
  - DSP v0.8 catalog / negotiation / transfer endpoints
  - A data plane (HttpData-PULL) serving DTR lookups and PURIS submodels
"""

import asyncio
import base64
import json
import logging
import os

import httpx
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

import data as mock_data
from dsp import catalog, negotiations, outbound, transfers

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("mock-participant")

# --------------------------------------------------------------------------
# Config (from environment)
# --------------------------------------------------------------------------
BPNL = os.getenv("TIER2_BPNL", "BPNL3333333333T3")
BASE_URL = os.getenv("MOCK_PARTICIPANT_BASE_URL", "http://puris-mock-participant:8083")
SUPPLIER_BPNL = os.getenv("SUPPLIER_BPNL", "BPNL1234567890ZZ")
WALLET_URL = os.getenv("WALLET_URL", "http://wallet:80")
WALLET_OAUTH_SECRET = os.getenv("WALLET_OAUTH_SECRET", "miw_private_client")
SUPPLIER_DSP_URL = os.getenv("SUPPLIER_DSP_URL", "http://supplier-control-plane:9184/api/v1/dsp")

# Common kwargs for wallet/IATP-token calls shared across the negotiation, transfer, and
# outbound-notification flows.
WALLET_KWARGS = {
    "bpnl": BPNL,
    "supplier_bpnl": SUPPLIER_BPNL,
    "wallet_url": WALLET_URL,
    "wallet_secret": WALLET_OAUTH_SECRET,
}

app = FastAPI(title="PURIS Mock Participant", version="1.0.0")

# Tasks fired from request handlers without being awaited. asyncio only holds a weak
# reference to a Task, so without this the task can be garbage-collected mid-execution;
# keeping a strong reference here (discarded via the done-callback) prevents that.
_background_tasks: set[asyncio.Task] = set()


def _spawn(coro) -> asyncio.Task:
    task = asyncio.create_task(coro)
    _background_tasks.add(task)
    task.add_done_callback(_background_tasks.discard)
    return task


# --------------------------------------------------------------------------
# DSP: Version discovery (required by Tractus-X EDC before accepting negotiations)
# --------------------------------------------------------------------------

@app.get("/api/v1/dsp/.well-known/dspace-version")
async def dsp_version():
    return {
        "protocolVersions": [
            {"version": "v0.8", "path": "/", "binding": "HTTPS"}
        ]
    }


# --------------------------------------------------------------------------
# DSP: Catalog
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/catalog/request")
async def handle_catalog(request: Request):
    logger.info("Catalog request from %s", request.client.host if request.client else "unknown")
    return catalog.build(BPNL, BASE_URL)


# --------------------------------------------------------------------------
# DSP: Contract Negotiations
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/negotiations/request")
async def start_negotiation(request: Request):
    body = await request.json()
    response, neg_id, callback = negotiations.create(body)
    callback = callback or SUPPLIER_DSP_URL
    logger.info("Negotiation created: %s, callback: %s", neg_id, callback)
    _spawn(
        negotiations.push_agreement_message(
            neg_id=neg_id,
            callback_address=callback,
            **WALLET_KWARGS,
        )
    )
    return response


@app.get("/api/v1/dsp/negotiations/{neg_id}")
async def get_negotiation(neg_id: str):
    state = negotiations.get_state(neg_id)
    if state is None:
        return JSONResponse(status_code=404, content={"error": f"Negotiation {neg_id} not found"})
    return state


@app.post("/api/v1/dsp/negotiations/{neg_id}/agreement/verification")
async def verify_agreement(neg_id: str):
    logger.info("Agreement verification for negotiation %s", neg_id)
    callback = negotiations.get_callback_address(neg_id) or SUPPLIER_DSP_URL
    _spawn(
        negotiations.push_finalized_message(
            neg_id=neg_id,
            callback_address=callback,
            **WALLET_KWARGS,
        )
    )
    return {
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
        "@type": "dspace:ContractNegotiation",
        "dspace:providerPid": neg_id,
        "dspace:state": "dspace:FINALIZED",
    }


# --------------------------------------------------------------------------
# DSP: Transfers
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/transfers/request")
async def start_transfer(request: Request):
    body = await request.json()
    response, provider_pid, callback = transfers.create(body, BASE_URL)
    logger.info("Transfer created: %s, callback: %s", provider_pid, callback)

    # Send TransferStartMessage asynchronously to the consumer's callback address
    _spawn(
        transfers.push_start_message(
            provider_pid=provider_pid,
            callback_address=callback,
            **WALLET_KWARGS,
        )
    )
    return response


@app.get("/api/v1/dsp/transfers/{transfer_id}")
async def get_transfer(transfer_id: str):
    state = transfers.get_state(transfer_id)
    if state is None:
        return JSONResponse(status_code=404, content={"error": f"Transfer {transfer_id} not found"})
    return state


@app.post("/api/v1/dsp/transfers/{transfer_id}/completion")
async def complete_transfer(transfer_id: str):
    logger.info("Transfer completion for %s", transfer_id)
    return JSONResponse(status_code=200, content={})


@app.post("/api/v1/dsp/transfers/{transfer_id}/termination")
async def terminate_transfer(transfer_id: str):
    logger.info("Transfer termination for %s", transfer_id)
    return JSONResponse(status_code=200, content={})


# --------------------------------------------------------------------------
# DSP: Consumer callbacks (this participant acting as consumer, e.g. for sending notifications)
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/negotiations/{consumer_pid}/agreement")
async def consumer_negotiation_agreement(consumer_pid: str, request: Request):
    """Receive ContractAgreementMessage from supplier EDC and fire verification."""
    body = await request.json()
    logger.info("Consumer: received ContractAgreementMessage for consumer_pid=%s", consumer_pid)
    provider_pid = outbound.handle_agreement(consumer_pid, body)
    if provider_pid is not None:
        _spawn(
            outbound.send_verification(
                consumer_pid=consumer_pid,
                provider_pid=provider_pid,
                supplier_dsp_url=SUPPLIER_DSP_URL,
                **WALLET_KWARGS,
            )
        )
    return JSONResponse(status_code=200, content={})


@app.post("/api/v1/dsp/negotiations/{consumer_pid}/events")
async def consumer_negotiation_events(consumer_pid: str, request: Request):
    """Receive ContractNegotiationEventMessage (FINALIZED) from supplier EDC."""
    body = await request.json()
    logger.info("Consumer: received negotiation event for consumer_pid=%s", consumer_pid)
    outbound.handle_negotiation_event(consumer_pid, body)
    return JSONResponse(status_code=200, content={})


@app.post("/api/v1/dsp/transfers/{consumer_pid}/start")
async def consumer_transfer_start(consumer_pid: str, request: Request):
    """Receive TransferStartMessage (with EDR) from supplier EDC."""
    body = await request.json()
    logger.info("Consumer: received TransferStartMessage for consumer_pid=%s", consumer_pid)
    outbound.handle_transfer_start(consumer_pid, body)
    return JSONResponse(status_code=200, content={})


# --------------------------------------------------------------------------
# Mock trigger: the participant sends a notification to the supplier
# --------------------------------------------------------------------------

@app.post("/api/mock/send-notification")
async def mock_send_notification():
    """Trigger the mock participant to send a DemandAndCapacityNotification to the supplier via full DSP flow."""
    logger.info("Mock trigger: participant initiating outbound notification to supplier")
    ok, step = await outbound.negotiate_and_send_notification(
        supplier_dsp_url=SUPPLIER_DSP_URL,
        base_url=BASE_URL,
        **WALLET_KWARGS,
    )
    if ok:
        return {"status": "sent", "notificationId": outbound.FIXED_NOTIFICATION_ID}
    return JSONResponse(
        status_code=500,
        content={"error": f"Failed at step: {step} — check mock participant logs"},
    )


@app.get("/api/mock/debug/supplier-catalog")
async def debug_supplier_catalog():
    """Fetch and return the supplier's DSP catalog as seen by the mock participant (for debugging)."""
    asset_id, offer = await outbound.fetch_notification_asset(SUPPLIER_DSP_URL, **WALLET_KWARGS)
    return {
        "supplier_dsp_url": SUPPLIER_DSP_URL,
        "notification_asset_id": asset_id,
        "notification_offer": offer,
    }


@app.get("/api/mock/debug/iatp-token")
async def debug_iatp_token():
    """Obtain and decode the IATP token the mock participant would use for negotiations (for debugging)."""
    token = await outbound.get_iatp_token(**WALLET_KWARGS)
    if not token:
        return JSONResponse(status_code=500, content={"error": "Failed to obtain IATP token — check logs"})

    try:
        parts = token.split(".")
        payload_b64 = parts[1] + "==" * (4 - len(parts[1]) % 4)
        payload = json.loads(base64.urlsafe_b64decode(payload_b64))
    except Exception as exc:
        payload = {"decode_error": str(exc)}

    # Also probe whether the supplier catalog is accessible without any auth
    catalog_url = f"{SUPPLIER_DSP_URL.rstrip('/')}/catalog/request"
    catalog_body = {"@context": {"dspace": "https://w3id.org/dspace/v0.8/"}, "@type": "dspace:CatalogRequestMessage"}
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            r = await client.post(catalog_url, json=catalog_body)
            catalog_no_auth = r.status_code
    except Exception as exc:
        catalog_no_auth = f"error: {exc}"

    return {
        "token_first_80": token[:80],
        "jwt_payload": payload,
        "catalog_status_no_auth": catalog_no_auth,
    }


# --------------------------------------------------------------------------
# Data Plane  (HttpData-PULL)
# --------------------------------------------------------------------------

@app.get("/api/public/lookup/shells")
async def dtr_lookup(request: Request):
    asset_ids_b64 = request.query_params.get("assetIds", "")
    logger.info("DTR lookup, assetIds=%s", asset_ids_b64)
    result = mock_data.lookup_shells(asset_ids_b64)
    return result


@app.get("/api/public/shell-descriptors/{aas_id_b64}")
async def dtr_shell(aas_id_b64: str):
    logger.info("DTR shell-descriptors: %s", aas_id_b64)
    shell = mock_data.get_shell(aas_id_b64, BPNL, BASE_URL)
    if shell is None:
        return JSONResponse(status_code=404, content={"error": "Shell not found"})
    return shell


@app.get("/api/public/{asset_id:path}")
async def data_plane(asset_id: str):
    # Strip trailing $value representation suffix if present
    clean_id = asset_id.split("/$value")[0].split("/$metadata")[0]
    logger.info("Data plane request: asset=%s", clean_id)
    result = mock_data.get_submodel(clean_id, BPNL)
    if result is None:
        logger.warning("Unknown asset requested: %s", clean_id)
        return JSONResponse(status_code=404, content={"error": f"Unknown asset: {clean_id}"})
    return result


# --------------------------------------------------------------------------
# Health
# --------------------------------------------------------------------------

@app.get("/health")
async def health():
    return {"status": "UP", "bpnl": BPNL}
