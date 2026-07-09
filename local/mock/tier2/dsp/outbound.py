#
# Copyright (c) 2026 Contributors to the Eclipse Foundation
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
"""Consumer-side (outbound) DSP flows — tier2 initiating negotiation and sending a notification."""

import asyncio
import logging
import time
import uuid
from datetime import date
from typing import Optional

import httpx

from dsp.common import CONTEXT_DSPACE, CONTEXT_DSPACE_ODRL_POLICY, auth_headers
from dsp.common import get_iatp_token as _common_get_iatp_token

logger = logging.getLogger("tier2-mock")

# Fixed notification ID so tests can assert on it consistently.
FIXED_NOTIFICATION_ID = "a1b2c3d4-e5f6-7890-abcd-000000000002"
FIXED_SOURCE_DISRUPTION_ID = "a1b2c3d4-e5f6-7890-abcd-000000000001"

# In-memory state for in-flight consumer negotiations and transfers.
_consumer_negotiations: dict[str, dict] = {}
_consumer_transfers: dict[str, dict] = {}


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

async def negotiate_and_send_notification(
    supplier_dsp_url: str,
    base_url: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
) -> tuple[bool, str]:
    """Full consumer-side DSP flow: negotiate → transfer → POST notification to supplier.

    Returns (success, step_or_error) where step_or_error names the failed step on failure.
    """

    # Step 1: Fetch supplier catalog and find the notification asset.
    asset_id, offer = await fetch_notification_asset(
        supplier_dsp_url, bpnl, supplier_bpnl, wallet_url, wallet_secret
    )
    if not asset_id:
        logger.error("outbound: notification asset not found in supplier catalog")
        return False, "catalog_fetch"

    logger.info("outbound: found notification asset %s — starting negotiation", asset_id)

    # Step 2: Start negotiation.
    consumer_pid = f"urn:uuid:{uuid.uuid4()}"
    finalized_event = asyncio.Event()
    _consumer_negotiations[consumer_pid] = {
        "state": "REQUESTED",
        "providerPid": None,
        "agreementId": None,
        "finalized_event": finalized_event,
        "supplier_dsp_url": supplier_dsp_url,
        "bpnl": bpnl,
        "supplier_bpnl": supplier_bpnl,
        "wallet_url": wallet_url,
        "wallet_secret": wallet_secret,
    }

    started = await _start_negotiation(
        supplier_dsp_url, base_url, bpnl, supplier_bpnl, wallet_url, wallet_secret,
        consumer_pid, asset_id, offer,
    )
    if not started:
        del _consumer_negotiations[consumer_pid]
        return False, "negotiation_request"

    logger.info("outbound: negotiation started (consumer_pid=%s) — waiting for FINALIZED", consumer_pid)

    # Wait until supplier EDC sends FINALIZED event (via callback).
    try:
        await asyncio.wait_for(finalized_event.wait(), timeout=60.0)
    except asyncio.TimeoutError:
        logger.error("outbound: timeout waiting for negotiation FINALIZED (consumer_pid=%s)", consumer_pid)
        _consumer_negotiations.pop(consumer_pid, None)
        return False, "negotiation_timeout"

    entry = _consumer_negotiations.pop(consumer_pid)
    agreement_id = entry.get("agreementId")
    if not agreement_id:
        logger.error("outbound: no agreementId after FINALIZED")
        return False, "no_agreement_id"

    logger.info("outbound: negotiation FINALIZED (agreementId=%s) — starting transfer", agreement_id)

    # Step 3: Start transfer.
    transfer_consumer_pid = f"urn:uuid:{uuid.uuid4()}"
    started_event = asyncio.Event()
    _consumer_transfers[transfer_consumer_pid] = {
        "state": "REQUESTED",
        "data_address": None,
        "started_event": started_event,
    }

    ok = await _start_transfer(
        supplier_dsp_url, base_url, bpnl, supplier_bpnl, wallet_url, wallet_secret,
        transfer_consumer_pid, asset_id, agreement_id,
    )
    if not ok:
        _consumer_transfers.pop(transfer_consumer_pid, None)
        return False, "transfer_request"

    logger.info("outbound: transfer requested (consumer_pid=%s) — waiting for EDR", transfer_consumer_pid)

    # Wait until supplier EDC sends TransferStartMessage with EDR.
    try:
        await asyncio.wait_for(started_event.wait(), timeout=60.0)
    except asyncio.TimeoutError:
        logger.error("outbound: timeout waiting for TransferStart (consumer_pid=%s)", transfer_consumer_pid)
        _consumer_transfers.pop(transfer_consumer_pid, None)
        return False, "transfer_timeout"

    transfer_entry = _consumer_transfers.pop(transfer_consumer_pid)
    data_address = transfer_entry.get("data_address")
    if not data_address:
        logger.error("outbound: no data_address in TransferStartMessage")
        return False, "no_data_address"

    # Step 4: POST notification to supplier via EDR.
    logger.info("outbound: got EDR — posting notification")
    ok = await _post_notification(data_address, bpnl, supplier_bpnl, wallet_url, wallet_secret)
    return (True, "ok") if ok else (False, "notification_post")


# ---------------------------------------------------------------------------
# Callback handlers (called from main.py route handlers)
# ---------------------------------------------------------------------------

def handle_agreement(consumer_pid: str, body: dict) -> Optional[str]:
    """Record ContractAgreementMessage fields. Returns providerPid (possibly ""), or None if
    consumer_pid is unknown — callers must use `is not None` to check for that case, since a
    known consumer_pid with a missing dspace:providerPid still returns a (falsy) empty string.

    This must run synchronously (not as a background task) before the request handler
    returns 200 — the supplier EDC may send the FINALIZED event immediately afterwards,
    and negotiate_and_send_notification's finalized_event.wait() would otherwise be able
    to observe FINALIZED before agreementId is recorded.
    """
    entry = _consumer_negotiations.get(consumer_pid)
    if not entry:
        logger.warning("outbound: agreement callback for unknown consumer_pid=%s", consumer_pid)
        return None

    agreement = body.get("dspace:agreement", {})
    agreement_id = agreement.get("@id") or body.get("dspace:agreementId")
    provider_pid = body.get("dspace:providerPid") or ""

    entry["agreementId"] = agreement_id
    entry["providerPid"] = provider_pid
    entry["state"] = "AGREED"
    logger.info("outbound: received agreement consumer_pid=%s agreementId=%s", consumer_pid, agreement_id)
    return provider_pid


async def send_verification(
    consumer_pid: str,
    provider_pid: str,
    supplier_dsp_url: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
) -> bool:
    """Send verification back to supplier DSP. Safe to run as a background task."""
    return await _send_verification(supplier_dsp_url, provider_pid, consumer_pid, bpnl, supplier_bpnl, wallet_url, wallet_secret)


def handle_negotiation_event(consumer_pid: str, body: dict) -> bool:
    """Handle ContractNegotiationEventMessage (FINALIZED) from supplier EDC."""
    entry = _consumer_negotiations.get(consumer_pid)
    if not entry:
        logger.warning("outbound: negotiation event for unknown consumer_pid=%s", consumer_pid)
        return False

    event_type = body.get("dspace:eventType", {})
    if isinstance(event_type, dict):
        event_type = event_type.get("@id", "")

    if "FINALIZED" in str(event_type):
        entry["state"] = "FINALIZED"
        entry["finalized_event"].set()
        logger.info("outbound: negotiation FINALIZED for consumer_pid=%s", consumer_pid)
    return True


def handle_transfer_start(consumer_pid: str, body: dict) -> bool:
    """Handle TransferStartMessage from supplier EDC; stores EDR data address."""
    entry = _consumer_transfers.get(consumer_pid)
    if not entry:
        logger.warning("outbound: transfer start for unknown consumer_pid=%s", consumer_pid)
        return False

    data_address = body.get("dspace:dataAddress", {})
    logger.info("outbound: TransferStart dataAddress: %s", data_address)
    entry["data_address"] = data_address
    entry["state"] = "STARTED"
    entry["started_event"].set()
    logger.info("outbound: transfer STARTED for consumer_pid=%s", consumer_pid)
    return True


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

async def fetch_notification_asset(
    supplier_dsp_url: str, bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str
) -> tuple[Optional[str], Optional[dict]]:
    """Fetch supplier catalog; return (asset_id, offer) for the notification asset."""
    token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    headers = auth_headers(token)

    catalog_url = f"{supplier_dsp_url.rstrip('/')}/catalog/request"
    body = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:CatalogRequestMessage",
    }
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(catalog_url, json=body, headers=headers)
            if not resp.is_success:
                logger.error("outbound: catalog request to %s failed: %s %s", catalog_url, resp.status_code, resp.text[:200])
                return None, None
            catalog = resp.json()
            datasets = catalog.get("dcat:dataset", [])
            if isinstance(datasets, dict):
                datasets = [datasets]
            for ds in datasets:
                asset_id = ds.get("@id", "")
                if "notification-api-asset" in asset_id:
                    offer = ds.get("odrl:hasPolicy")
                    logger.info("outbound: found notification asset %s", asset_id)
                    return asset_id, offer
            logger.error("outbound: notification-api-asset not found in supplier catalog")
            return None, None
    except Exception as exc:
        logger.error("outbound: error fetching catalog: %s", exc)
        return None, None


async def _start_negotiation(
    supplier_dsp_url: str, base_url: str,
    bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str,
    consumer_pid: str, asset_id: str, offer: Optional[dict],
) -> bool:
    """Send ContractRequestMessage to supplier DSP."""
    token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    headers = auth_headers(token)

    offer = offer or {}
    offer_id = offer.get("@id", f"offer-{uuid.uuid4()}")

    # Build the offer body including the policy constraints from the catalog entry.
    offer_body: dict = {
        "@type": "odrl:Offer",
        "@id": offer_id,
        "odrl:target": {"@id": asset_id},
    }
    for key in ("odrl:permission", "odrl:prohibition", "odrl:obligation"):
        if key in offer:
            offer_body[key] = offer[key]

    body = {
        "@context": CONTEXT_DSPACE_ODRL_POLICY,
        "@type": "dspace:ContractRequestMessage",
        "dspace:consumerPid": consumer_pid,
        "dspace:callbackAddress": f"{base_url.rstrip('/')}/api/v1/dsp",
        "dspace:offer": offer_body,
    }

    neg_url = f"{supplier_dsp_url.rstrip('/')}/negotiations/request"
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(neg_url, json=body, headers=headers)
            if resp.is_success:
                data = resp.json()
                provider_pid = data.get("dspace:providerPid") or data.get("@id")
                logger.info("outbound: negotiation requested consumer_pid=%s provider_pid=%s", consumer_pid, provider_pid)
                if provider_pid:
                    _consumer_negotiations[consumer_pid]["providerPid"] = provider_pid
                return True
            else:
                logger.error("outbound: negotiation request failed: %s %s", resp.status_code, resp.text[:300])
                return False
    except Exception as exc:
        logger.error("outbound: error starting negotiation: %s", exc)
        return False


async def _send_verification(
    supplier_dsp_url: str, provider_pid: str, consumer_pid: str,
    bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str,
) -> None:
    """Send ContractAgreementVerificationMessage to supplier DSP."""
    await asyncio.sleep(0.2)

    token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    headers = auth_headers(token)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractAgreementVerificationMessage",
        "dspace:providerPid": provider_pid,
        "dspace:consumerPid": consumer_pid,
    }

    url = f"{supplier_dsp_url.rstrip('/')}/negotiations/{provider_pid}/agreement/verification"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=message, headers=headers)
            if resp.is_success:
                logger.info("outbound: ContractAgreementVerification sent to %s — %s", url, resp.status_code)
            else:
                logger.warning("outbound: verification to %s returned %s: %s", url, resp.status_code, resp.text[:200])
    except Exception as exc:
        logger.warning("outbound: could not send verification to %s: %s", url, exc)


async def _start_transfer(
    supplier_dsp_url: str, base_url: str,
    bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str,
    consumer_pid: str, asset_id: str, agreement_id: str,
) -> bool:
    """Send TransferRequestMessage to supplier DSP (HttpData-PULL)."""
    token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    headers = auth_headers(token)

    body = {
        "@context": {
            "dspace": "https://w3id.org/dspace/v0.8/",
            "dct": "http://purl.org/dc/terms/",
        },
        "@type": "dspace:TransferRequestMessage",
        "dspace:consumerPid": consumer_pid,
        "dspace:callbackAddress": f"{base_url.rstrip('/')}/api/v1/dsp",
        "dspace:agreementId": agreement_id,
        "dct:format": "HttpData-PULL",
    }

    transfer_url = f"{supplier_dsp_url.rstrip('/')}/transfers/request"
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(transfer_url, json=body, headers=headers)
            if resp.is_success:
                logger.info("outbound: transfer requested consumer_pid=%s", consumer_pid)
                return True
            else:
                logger.error("outbound: transfer request failed: %s %s", resp.status_code, resp.text[:300])
                return False
    except Exception as exc:
        logger.error("outbound: error starting transfer: %s", exc)
        return False


async def _post_notification(data_address: dict, bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str) -> bool:
    """POST notification payload to supplier's data plane endpoint using EDR credentials."""
    edr = _extract_edr(data_address)
    endpoint = edr["endpoint"]
    if not endpoint:
        logger.error("outbound: no endpoint in data address: %s", data_address)
        return False

    # The supplier EDC may reuse a cached transfer process with a stale/expired token.
    # Always refresh to guarantee a valid token.
    auth_token = ""
    if edr["refresh_endpoint"] and edr["refresh_token"]:
        auth_token = await _get_fresh_access_token(
            edr["refresh_endpoint"], edr["refresh_token"], bpnl, supplier_bpnl, wallet_url, wallet_secret
        )
    if not auth_token:
        auth_token = edr["auth_token"]
        logger.info("outbound: using EDR authorization token directly (no refresh)")

    if not auth_token:
        logger.error("outbound: no usable auth token in EDR")
        return False

    payload = _build_notification_payload(bpnl, supplier_bpnl)
    headers = {
        "Content-Type": "application/json",
        "Authorization": auth_token,
        "edc-bpn": bpnl,
    }

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(endpoint, json=payload, headers=headers)
            if resp.is_success:
                logger.info("outbound: notification posted to %s — %s", endpoint, resp.status_code)
                return True
            else:
                logger.warning("outbound: notification POST to %s returned %s: %s", endpoint, resp.status_code, resp.text[:300])
                return False
    except Exception as exc:
        logger.error("outbound: error posting notification to %s: %s", endpoint, exc)
        return False


def _extract_edr(data_address: dict) -> dict:
    """Extract EDR fields from a Tractus-X EDC DataAddress.

    Returns a dict with keys: endpoint, auth_token, refresh_endpoint, refresh_token.
    The auth_token may be expired; use refresh_token + refresh_endpoint to get a fresh one.
    """
    result = {"endpoint": "", "auth_token": "", "refresh_endpoint": "", "refresh_token": ""}
    for prop in data_address.get("dspace:endpointProperties", []):
        name = prop.get("dspace:name", "")
        value = prop.get("dspace:value", "")
        if name == "https://w3id.org/edc/v0.0.1/ns/endpoint":
            result["endpoint"] = value
        elif name == "https://w3id.org/edc/v0.0.1/ns/authorization":
            result["auth_token"] = value
        elif name == "https://w3id.org/tractusx/auth/refreshEndpoint":
            result["refresh_endpoint"] = value
        elif name == "https://w3id.org/tractusx/auth/refreshToken":
            result["refresh_token"] = value
    return result


async def _get_fresh_access_token(
    refresh_endpoint: str, refresh_token: str,
    bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str,
) -> Optional[str]:
    """Refresh the EDR access token via the Tractus-X data plane token endpoint.

    The Tractus-X EDC data plane's /api/public/token endpoint:
    - Requires an IATP SI token (containing a 'token' VP claim) in the Authorization header
    - Requires grant_type and refresh_token as QUERY PARAMS (body is consumed by a Jetty filter
      before JAX-RS reads @FormParam, so body params are always null)
    """
    url = f"{refresh_endpoint.rstrip('/')}/token"
    si_token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    if not si_token:
        logger.warning("outbound: could not get IATP SI token for refresh")
        return None
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                url,
                params={"grant_type": "refresh_token", "refresh_token": refresh_token},
                headers={"Authorization": f"Bearer {si_token}"},
            )
            if resp.is_success:
                data = resp.json()
                token = data.get("token") or data.get("access_token")
                if token:
                    logger.info("outbound: token refresh succeeded via %s", url)
                    return token
                logger.warning("outbound: token refresh response missing token field; keys: %s body: %s",
                               list(data.keys()), str(data)[:200])
            else:
                logger.warning("outbound: token refresh failed %s %s: %s", url, resp.status_code, resp.text[:300])
    except Exception as exc:
        logger.warning("outbound: token refresh error: %s", exc)
    return None


def _build_notification_payload(bpnl: str, supplier_bpnl: str) -> dict:
    """Build the DemandAndCapacityNotification request body."""
    now_ms = int(time.time() * 1000)
    future_ms = int((time.time() + 14 * 86400) * 1000)
    today_iso = date.today().isoformat()
    return {
        "header": {
            "senderBpn": bpnl,
            "receiverBpn": supplier_bpnl,
            "context": "CX-DemandAndCapacityNotificationAPI-Receive:2.0.0",
            "messageId": str(uuid.uuid4()),
            "sentDateTime": f"{today_iso}T00:00:00Z",
            "version": "3.0.0",
        },
        "content": {
            "demandAndCapacityNotification": {
                "notificationId": FIXED_NOTIFICATION_ID,
                "sourceDisruptionId": FIXED_SOURCE_DISRUPTION_ID,
                "relatedNotificationIds": [],
                "leadingRootCause": "strike",
                "effect": "capacity-reduction",
                "text": "Mock capacity disruption notification from Tier2.",
                "startDateOfEffect": now_ms,
                "expectedEndDateOfEffect": future_ms,
                "contentChangedAt": now_ms,
                "affectedSitesSender": [],
                "affectedSitesRecipient": [],
                "materialsAffected": [],
                "status": "open",
            }
        },
    }


async def get_iatp_token(bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str) -> Optional[str]:
    """Obtain a scoped IATP token from the wallet (tier2 as consumer, supplier as provider).

    Uses the full DSP:2025-1 VC scope set so the supplier's EDC accepts the token for
    negotiation requests (catalog-only tokens need just MembershipCredential, but negotiations
    are validated against the full scope).
    """
    return await _common_get_iatp_token(
        bpnl, supplier_bpnl, wallet_url, wallet_secret,
        credential_types=("DataExchangeGovernanceCredential", "MembershipCredential", "BpnCredential"),
        log_prefix="outbound",
    )
