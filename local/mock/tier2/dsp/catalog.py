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
"""Builds the DSP catalog (DCAT dataset list) that tier2 offers as a provider."""

import uuid

from data import PART_TYPE_INFO_ASSET, PART_TYPE_INFO_SEMANTIC_ID, SUBMODELS
from dsp.common import CX_POLICY_NS, DSPACE_NS, ODRL_NS, build_permission

CONTEXT = {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "cx-policy": CX_POLICY_NS,
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "odrl": ODRL_NS,
    "dspace": DSPACE_NS,
    "cx-common": "https://w3id.org/catenax/ontology/common#",
    "cx-taxo": "https://w3id.org/catenax/taxonomy#",
    "aas-semantics": "https://admin-shell.io/aas/3/0/HasSemantics/",
}

DTR_ASSET_PREFIX = "DigitalTwinRegistryId"


def _offer_policy(offer_id: str) -> dict:
    return {
        "@id": offer_id,
        "@type": "odrl:Offer",
        "odrl:permission": build_permission(),
        "odrl:prohibition": [],
        "odrl:obligation": [],
    }


def _distribution(service_id: str) -> dict:
    return {
        "@type": "dcat:Distribution",
        "dct:format": {"@id": "HttpData-PULL"},
        "dcat:accessService": {"@id": service_id},
    }


def build(bpnl: str, base_url: str) -> dict:
    service_id = f"urn:uuid:{uuid.uuid4()}"
    datasets = []

    # DTR asset
    dtr_asset_id = f"{DTR_ASSET_PREFIX}@{bpnl}"
    datasets.append({
        "@id": dtr_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:DigitalTwinRegistry"},
        "cx-common:version": "3.0",
        "odrl:hasPolicy": _offer_policy(f"offer-dtr-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service_id)],
    })

    # PartTypeInformation asset
    part_type_asset_id = f"{PART_TYPE_INFO_ASSET}@{bpnl}"
    datasets.append({
        "@id": part_type_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:Submodel"},
        "cx-common:version": "3.0",
        "aas-semantics:semanticId": {"@id": PART_TYPE_INFO_SEMANTIC_ID},
        "odrl:hasPolicy": _offer_policy(f"offer-pti-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service_id)],
    })

    # Submodel assets
    for prefix, semantic_id, version in SUBMODELS:
        asset_id = f"{prefix}@{bpnl}"
        datasets.append({
            "@id": asset_id,
            "@type": "dcat:Dataset",
            "dct:type": {"@id": "cx-taxo:Submodel"},
            "cx-common:version": version,
            "aas-semantics:semanticId": {"@id": semantic_id},
            "odrl:hasPolicy": _offer_policy(f"offer-{prefix}-{uuid.uuid4()}"),
            "dcat:distribution": [_distribution(service_id)],
        })

    return {
        "@context": CONTEXT,
        "@type": "dcat:Catalog",
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "dspace:participantId": bpnl,
        "dcat:dataset": datasets,
        "dcat:service": {
            "@id": service_id,
            "@type": "dcat:DataService",
            "dct:terms": "connector",
            "dcat:endpointURL": f"{base_url}/api/v1/dsp",
        },
    }
