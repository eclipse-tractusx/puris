#
# Copyright (c) 2022,2024 Volkswagen AG
# Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

#!/bin/bash

# retrieve access token for miw from keycloak
response=$(curl -X POST -d 'client_id=miw_private_client&grant_type=client_credentials&client_secret=miw_private_client&scope=openid' http://localhost:8080/realms/miw_test/protocol/openid-connect/token)

token=$(echo "$response" | jq -r '.access_token')

echo "$response" | jq

# register customer wallet at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "customer wallet", "bpn": "BPNL4444444444XX" }' http://localhost:8000/api/wallets | jq
echo ""
# register supplier wallet at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "supplier wallet", "bpn": "BPNL1234567890ZZ" }' http://localhost:8000/api/wallets | jq
echo ""
# register customer for framework agreement at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "holderIdentifier": "BPNL4444444444XX", "type": "TraceabilityCredential", "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf", "contract-version": "1.0.0" }' http://localhost:8000/api/credentials/issuer/framework | jq
echo ""
# register supplier for framework agreement at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "holderIdentifier": "BPNL1234567890ZZ", "type": "TraceabilityCredential", "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf", "contract-version": "1.0.0" }' http://localhost:8000/api/credentials/issuer/framework | jq
echo ""
