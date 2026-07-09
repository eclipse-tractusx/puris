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
"""Provider-side contract negotiation state machine (tier2 acting as DSP provider)."""

import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional

from dsp.common import CONTEXT_DSPACE, CONTEXT_DSPACE_ODRL_POLICY, build_permission, push_dsp_message

logger = logging.getLogger("tier2-mock")

_store: dict[str, dict] = {}


def create(body: dict) -> tuple[dict, str, str]:
    """Returns (response_body, neg_id, callback_address)."""
    neg_id = f"urn:uuid:{uuid.uuid4()}"
    agreement_id = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("dspace:consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("dspace:callbackAddress", "")
    asset_id = _extract_asset_id(body)

    entry = {
        "agreementId": agreement_id,
        "assetId": asset_id,
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
    }
    _store[neg_id] = entry
    return _requested(neg_id, consumer_pid), neg_id, callback


def get_state(neg_id: str) -> Optional[dict]:
    entry = _store.get(neg_id)
    if not entry:
        return None
    return _finalized(neg_id, entry["agreementId"], entry["consumerPid"])


def get_callback_address(neg_id: str) -> str:
    return _store.get(neg_id, {}).get("callbackAddress", "")


def _extract_asset_id(body: dict) -> str:
    offer = body.get("dspace:offer") or body.get("offer") or {}
    target = offer.get("odrl:target", offer.get("target", {}))
    if isinstance(target, dict):
        return target.get("@id", "")
    return str(target)


def _requested(neg_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractNegotiation",
        "@id": neg_id,
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:REQUESTED",
    }


def _finalized(neg_id: str, agreement_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractNegotiation",
        "@id": neg_id,
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:FINALIZED",
        "dspace:contractAgreementId": agreement_id,
    }


async def push_agreement_message(
    neg_id: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send ContractAgreementMessage to consumer's DSP callback."""
    entry = _store.get(neg_id)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.5)

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    message = {
        "@context": CONTEXT_DSPACE_ODRL_POLICY,
        "@type": "dspace:ContractAgreementMessage",
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:agreement": {
            "@id": entry["agreementId"],
            "@type": "odrl:Agreement",
            "odrl:target": {"@id": entry["assetId"]},
            "dspace:timestamp": now,
            "odrl:assigner": bpnl,
            "odrl:assignee": supplier_bpnl,
            "odrl:permission": build_permission(),
            "odrl:prohibition": [],
            "odrl:obligation": [],
        },
    }

    callback_url = f"{callback_address.rstrip('/')}/negotiations/{entry['consumerPid']}/agreement"
    await push_dsp_message("ContractAgreementMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)


async def push_finalized_message(
    neg_id: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send ContractNegotiationEventMessage (FINALIZED) to consumer's DSP callback."""
    entry = _store.get(neg_id)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.3)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractNegotiationEventMessage",
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:eventType": {"@id": "dspace:FINALIZED"},
    }

    callback_url = f"{callback_address.rstrip('/')}/negotiations/{entry['consumerPid']}/events"
    await push_dsp_message("ContractNegotiationEventMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)
