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
"""Constants and helpers shared by the catalog/negotiations/transfers/outbound DSP flows."""

import logging
from typing import Optional, Sequence

import httpx

logger = logging.getLogger("mock-participant")


CONTEXT_DSPACE = "https://w3id.org/dspace/2025/1/context.jsonld"
CONTEXT_DSPACE_ODRL_POLICY = [
    "https://w3id.org/dspace/2025/1/context.jsonld",
    {"cx-policy": "https://w3id.org/catenax/2025/9/policy/"},
]

DSP_VERSION_PATH = "/2025-1"


def versioned_callback_base(callback_address: str) -> str:
    """Append DSP_VERSION_PATH unless already present (some callers already hand us a versioned base)."""
    base = callback_address.rstrip("/")
    if base.endswith(DSP_VERSION_PATH):
        return base
    return f"{base}{DSP_VERSION_PATH}"

FRAMEWORK_AGREEMENT = "DataExchangeGovernance:1.0"
USAGE_PURPOSE = "cx.puris.base:1"


def auth_headers(token: Optional[str]) -> dict:
    """JSON headers, with a Bearer Authorization header added if a token is present."""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def build_permission() -> dict:
    """The permission block granting use under the PURIS framework agreement/usage purpose."""
    return {
        "action": "use",
        "constraint": {
            "and": [
                {
                    "leftOperand": "cx-policy:FrameworkAgreement",
                    "operator": "eq",
                    "rightOperand": FRAMEWORK_AGREEMENT,
                },
                {
                    "leftOperand": "cx-policy:UsagePurpose",
                    "operator": "isAnyOf",
                    "rightOperand": USAGE_PURPOSE,
                },
            ]
        },
    }


async def get_iatp_token(
    own_bpnl: str,
    counterparty_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
    credential_types: Sequence[str] = ("MembershipCredential",),
    log_prefix: str = "",
) -> Optional[str]:
    """Obtain a scoped IATP token from the wallet.
    """
    prefix = f"{log_prefix}: " if log_prefix else ""
    consumer_bpnl, provider_bpnl = own_bpnl, counterparty_bpnl
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                f"{wallet_url}/oauth/token",
                data={
                    "grant_type": "client_credentials",
                    "client_id": own_bpnl,
                    "client_secret": wallet_secret,
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
            if not resp.is_success:
                logger.warning("%sWallet OAuth failed: %s", prefix, resp.status_code)
                return None
            bearer = resp.json().get("access_token")
            if not bearer:
                logger.warning("%sWallet OAuth returned no access_token", prefix)
                return None

            resp2 = await client.post(
                f"{wallet_url}/api/sts",
                json={
                    "grantAccess": {
                        "scope": "read",
                        "credentialTypes": list(credential_types),
                        "consumerDid": f"did:web:wallet:{consumer_bpnl}",
                        "providerDid": f"did:web:wallet:{provider_bpnl}",
                    }
                },
                headers={"Authorization": f"Bearer {bearer}", "Content-Type": "application/json"},
            )
            if not resp2.is_success:
                logger.warning("%sSTS grantAccess failed: %s %s", prefix, resp2.status_code, resp2.text[:400])
                return None
            body2 = resp2.json()
            jwt = body2.get("jwt")
            if not jwt:
                logger.warning("%sSTS grantAccess returned no 'jwt' field; body keys: %s", prefix, list(body2.keys()))
                return None
            return jwt
    except Exception as exc:
        logger.warning("%sIATP token acquisition failed: %s", prefix, exc)
        return None


async def push_dsp_message(
    message_type: str,
    callback_url: str,
    message: dict,
    provider_bpnl: str,
    consumer_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
) -> None:
    """POST a DSP callback message (agreement/event/transfer-start) with a bearer IATP token."""
    iatp_token = await get_iatp_token(
        provider_bpnl,
        consumer_bpnl,
        wallet_url,
        wallet_secret,
        credential_types=("DataExchangeGovernanceCredential", "MembershipCredential", "BpnCredential"),
    )
    headers = auth_headers(iatp_token)

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(callback_url, json=message, headers=headers)
            if resp.is_success:
                logger.info("%s sent to %s — %s", message_type, callback_url, resp.status_code)
            else:
                logger.warning("%s to %s returned %s: %s", message_type, callback_url, resp.status_code, resp.text[:500])
    except Exception as exc:
        logger.warning("Could not push %s to %s: %s", message_type, callback_url, exc)
