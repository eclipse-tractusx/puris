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
"""Mock data for DTR and PURIS submodels served by the mock participant's data plane."""

import base64
import json
import uuid
from datetime import date, timedelta
from typing import Optional


# Must match what the Supplier PURIS registers as partnerMaterialNumber for tier2.
TIER2_MATERIAL_NUMBER = "MNR-8101-ID175283.001"
TIER2_AAS_ID = "urn:uuid:tier2-aas-01"

# Shared between the DTR shell descriptors (_build_shell) and the data-plane dispatch table (get_submodel).
ITEM_STOCK_ASSET = "itemstocksubmodel-api-asset"
PLANNED_PRODUCTION_ASSET = "productionsubmodel-api-asset"
DELIVERY_ASSET = "deliverysubmodel-api-asset"
DAYS_OF_SUPPLY_ASSET = "daysofsupplysubmodel-api-asset"
NOTIFICATION_ASSET = "notification-api-asset"
PART_TYPE_INFO_ASSET = "PartTypeInformationSubmodelApi"

# Single source of truth for catalog offers and DTR shell descriptors.
# GET-able via the data plane and advertised as SUBMODEL-3.0.
SUBMODELS = [
    (ITEM_STOCK_ASSET, "urn:samm:io.catenax.item_stock:2.0.0#ItemStock"),
    (PLANNED_PRODUCTION_ASSET, "urn:samm:io.catenax.planned_production_output:2.0.0#PlannedProductionOutput"),
    (DELIVERY_ASSET, "urn:samm:io.catenax.delivery_information:2.0.0#DeliveryInformation"),
    (DAYS_OF_SUPPLY_ASSET, "urn:samm:io.catenax.days_of_supply:2.0.0#DaysOfSupply"),
    (PART_TYPE_INFO_ASSET, "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation"),
]


# PURIS backend expects a real UUID here, so use a deterministic uuid5 for stability.
def _material_global_asset_id(material_number: str) -> str:
    return f"urn:uuid:{uuid.uuid5(uuid.NAMESPACE_URL, f'tier2-mock-material-{material_number}')}"

# -------------------------------------------------------------------
# DTR
# -------------------------------------------------------------------


def lookup_shells(asset_ids_b64: str) -> dict:
    """Handle GET /lookup/shells?assetIds=... — return matching AAS global IDs."""
    try:
        decoded = base64.b64decode(asset_ids_b64 + "==").decode("utf-8")
        params = json.loads(f"[{decoded}]")
    except Exception:
        params = []

    manufacturer_part_id = None
    for p in params:
        if p.get("name") == "manufacturerPartId":
            manufacturer_part_id = p.get("value")

    if manufacturer_part_id == TIER2_MATERIAL_NUMBER:
        return {"result": [TIER2_AAS_ID]}
    return {"result": []}


def get_shell(aas_id_b64: str, bpnl: str, base_url: str) -> Optional[dict]:
    """Handle GET /shell-descriptors/{base64(aasId)} — return AAS shell descriptor."""
    try:
        aas_id = base64.b64decode(aas_id_b64 + "==").decode("utf-8")
    except Exception:
        return None
    if aas_id != TIER2_AAS_ID:
        return None
    return _build_shell(bpnl, base_url)


def _build_shell(bpnl: str, base_url: str) -> dict:
    dsp_url = f"{base_url}/api/v1/dsp"
    public_url = f"{base_url}/api/public"
    material_global_asset_id = _material_global_asset_id(TIER2_MATERIAL_NUMBER)

    def _submodel_descriptor(semantic_id: str, asset_id: str) -> dict:
        return {
            "id": f"urn:uuid:sm-{asset_id}",
            "semanticId": {
                "type": "ExternalReference",
                "keys": [{"type": "GlobalReference", "value": semantic_id}],
            },
            "endpoints": [
                {
                    "interface": "SUBMODEL-3.0",
                    "protocolInformation": {
                        "href": f"{public_url}/{material_global_asset_id}/{asset_id}",
                        "endpointProtocol": "HTTP",
                        "endpointProtocolVersion": ["1.1"],
                        "subprotocol": "DSP",
                        "subprotocolBody": f"id={asset_id};dspEndpoint={dsp_url}",
                        "subprotocolBodyEncoding": "plain",
                    }, 
                }
            ],
        }

    submodel_descriptors = [
        _submodel_descriptor(semantic_id, f"{prefix}@{bpnl}")
        for prefix, semantic_id in SUBMODELS
    ]
    return {
        "id": TIER2_AAS_ID,
        "globalAssetId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "specificAssetIds": [
            {"name": "manufacturerPartId", "value": TIER2_MATERIAL_NUMBER},
            {"name": "manufacturerId", "value": bpnl},
            {"name": "digitalTwinType", "value": "PartType"},
        ],
        "submodelDescriptors": submodel_descriptors,
    }


# -------------------------------------------------------------------
# PURIS Submodels  (minimal valid payloads)
# -------------------------------------------------------------------

def get_submodel(asset_id: str, bpnl: str) -> Optional[dict]:
    prefix = asset_id.rsplit("/", 1)[-1].split("@")[0]
    fn = _SUBMODEL_DISPATCH.get(prefix)
    if fn is None:
        return None
    return fn(bpnl)


def _today() -> str:
    return date.today().isoformat()


def _future(days: int) -> str:
    return (date.today() + timedelta(days=days)).isoformat()


def _bpns(bpnl: str) -> str:
    return bpnl.replace("BPNL", "BPNS", 1)


def _bpna(bpnl: str) -> str:
    return bpnl.replace("BPNL", "BPNA", 1)


def _item_stock(bpnl: str) -> dict:
    return {
        "materialGlobalAssetId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "positions": [
            {
                "orderPositionReference": {
                    "supplierOrderId": "M-Nbr-4711",
                    "customerOrderId": "C-Nbr-4711",
                    "customerOrderPositionId": "PositionId-01",
                },
                "allocatedStocks": [
                    {
                        "quantityOnAllocatedStock": {"value": 200.0, "unit": "unit:piece"},
                        "stockLocationBPNS": _bpns(bpnl),
                        "stockLocationBPNA": _bpna(bpnl),
                        "isBlocked": False,
                        "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                    }
                ],
            }
        ],
        "direction": "OUTBOUND",
    }


def _planned_production_output(bpnl: str) -> dict:
    return {
        "materialGlobalAssetId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "positions": [
            {
                "orderPositionReference": {
                    "supplierOrderId": "M-Nbr-4711",
                    "customerOrderId": "C-Nbr-4711",
                    "customerOrderPositionId": "PositionId-01",
                },
                "allocatedPlannedProductionOutputs": [
                    {
                        "plannedProductionQuantity": {"value": 500.0, "unit": "unit:piece"},
                        "productionSiteBpns": _bpns(bpnl),
                        "estimatedTimeOfCompletion": f"{_future(7)}T00:00:00Z",
                        "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                    }
                ],
            }
        ],
    }


def _delivery(bpnl: str) -> dict:
    return {
        "materialGlobalAssetId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "positions": [
            {
                "orderPositionReference": {
                    "supplierOrderId": "M-Nbr-4711",
                    "customerOrderId": "C-Nbr-4711",
                    "customerOrderPositionId": "PositionId-01",
                },
                "deliveries": [
                    {
                        "deliveryQuantity": {"value": 100.0, "unit": "unit:piece"},
                        "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                        "transitEvents": [
                            {"dateTimeOfEvent": f"{_future(3)}T00:00:00Z",
                             "eventType": "estimated-departure"},
                            {"dateTimeOfEvent": f"{_future(5)}T00:00:00Z",
                             "eventType": "estimated-arrival"},
                        ],
                        "transitLocations": {
                            "origin": {"bpnsProperty": _bpns(bpnl), "bpnaProperty": _bpna(bpnl)},
                            "destination": {"bpnsProperty": "BPNS1234567890ZZ", "bpnaProperty": "BPNA1234567890AA"},
                        },
                        "trackingNumber": "1Z9829WDE02128",
                        "incoterm": "DAP",
                    }
                ],
            }
        ],
    }


def _days_of_supply(bpnl: str) -> dict:
    return {
        "materialGlobalAssetId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "allocatedDaysOfSupply": [
            {
                "stockLocationBPNS": _bpns(bpnl),
                "stockLocationBPNA": _bpna(bpnl),
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                "amountOfAllocatedDaysOfSupply": [
                    {"date": _today(), "daysOfSupply": 5.0},
                    {"date": _future(1), "daysOfSupply": 4.5},
                ],
            }
        ],
        "direction": "OUTBOUND",
    }


def _part_type_info(bpnl: str) -> dict:
    return {
        "catenaXId": _material_global_asset_id(TIER2_MATERIAL_NUMBER),
        "partTypeInformation": {
            "manufacturerPartId": TIER2_MATERIAL_NUMBER,
            "nameAtManufacturer": "Tier2 Semiconductor",
            "partClassification": [
                {
                    "classificationStandard": "IEC",
                    "classificationID": "IC",
                    "classificationDescription": "Integrated Circuit",
                }
            ],
        },
    }


_SUBMODEL_DISPATCH = {
    ITEM_STOCK_ASSET: _item_stock,
    PLANNED_PRODUCTION_ASSET: _planned_production_output,
    DELIVERY_ASSET: _delivery,
    DAYS_OF_SUPPLY_ASSET: _days_of_supply,
    PART_TYPE_INFO_ASSET: _part_type_info,
}
