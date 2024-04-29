#!/bin/bash
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

KEY=

if [ -z "$KEY" ]; then
  echo "KEY is not set. Please specify the key (see bdrs docker compose definition). Exiting..."
  exit 1
fi

curl -X POST -H "x-api-key: $KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL4444444444XX", "did": "did:web:mock-util-service/customer" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""

curl -X POST -H "x-api-key: $KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL1234567890ZZ", "did": "did:web:mock-util-service/supplier" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""

curl -X POST -H "x-api-key: $KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL000000000000", "did": "did:web:mock-util-service/trusted-issuer" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""
