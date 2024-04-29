#
# Copyright (c) 2024 Volkswagen AG
# Copyright (c) 2024 Contributors to the Eclipse Foundation
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

from pathlib import Path

"""
Read key information
"""
def read_file(path: Path):
    with path.open() as file:
        file_content = file.read()
    return file_content


# openssl ecparam -name prime256v1 -genkey -out private_key.pem
# openssl ec -in private_key.pem -pubout -out public_key.pem
ES256_PRIVATE_KEY = read_file(Path("keys/private_key.pem"))
ES256_PUBLIC_KEY = read_file(Path("keys/public_key.pem"))

DID_CUSTOMER = "did:web:mock-util-service/customer"
DID_SUPPLIER = "did:web:mock-util-service/supplier"
DID_TRUSTED_ISSUER = "did:web:mock-util-service/trusted-issuer"

DID_DICT = {
    DID_TRUSTED_ISSUER: {
        "bpnl": "NONE",
        "did_resolve_name": "trusted-issuer",
    },
    DID_SUPPLIER: {
        "bpnl": "BPNL1234567890ZZ",
        "did_resolve_name": "supplier",
    },
    DID_CUSTOMER: {
        "bpnl": "BPNL4444444444XX",
        "did_resolve_name": "customer",
    }
}

"""
lookup bpnl by did
"""
def get_did_for_bpnl(did_resolve_name: str):
    for key, value in DID_DICT.items():
        if value["did_resolve_name"] == did_resolve_name:
            return key
    return None
