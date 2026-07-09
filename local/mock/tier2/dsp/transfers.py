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
"""Provider-side transfer process state machine (tier2 acting as DSP provider, HttpData-PULL)."""

import asyncio
import uuid
from typing import Optional

from dsp.common import CONTEXT_DSPACE, DSPACE_NS, push_dsp_message

_store: dict[str, dict] = {}

FIXED_TOKEN = "tier2-mock-token"


def create(body: dict, base_url: str) -> tuple[dict, str, str]:
    """Returns (response_body, provider_pid, callback_address)."""
    provider_pid = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("dspace:consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("dspace:callbackAddress", "")

    _store[provider_pid] = {
        "state": "STARTED",
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
        "endpoint": f"{base_url}/api/public",
        "token": FIXED_TOKEN,
    }

    response = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferProcess",
        "@id": provider_pid,
        "dspace:providerPid": provider_pid,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:REQUESTED",
    }
    return response, provider_pid, callback


def get_state(transfer_id: str) -> Optional[dict]:
    entry = _store.get(transfer_id)
    if not entry:
        return None
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferProcess",
        "@id": transfer_id,
        "dspace:providerPid": transfer_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:state": "dspace:STARTED",
        "dspace:dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }


def _data_address(endpoint: str, token: str) -> dict:
    bearer = f"Bearer {token}"
    return {
        "@context": {
            "dspace": DSPACE_NS,
            "edc": "https://w3id.org/edc/v0.0.1/ns/",
            "tx-auth": "https://w3id.org/tractusx/auth/",
        },
        "@type": "dspace:DataAddress",
        "dspace:endpointType": "https://w3id.org/idsa/v4.1/HTTP",
        "dspace:endpoint": endpoint,
        "dspace:endpointProperties": [
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "https://w3id.org/edc/v0.0.1/ns/endpoint",
                "dspace:value": endpoint,
            },
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "https://w3id.org/edc/v0.0.1/ns/authorization",
                "dspace:value": bearer,
            },
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "authType",
                "dspace:value": "bearer",
            },
        ],
    }


async def push_start_message(
    provider_pid: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send TransferStartMessage to consumer's callback address."""
    entry = _store.get(provider_pid)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.5)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferStartMessage",
        "dspace:providerPid": provider_pid,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }

    callback_url = f"{callback_address.rstrip('/')}/transfers/{entry['consumerPid']}/start"
    await push_dsp_message("TransferStartMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)
