#!/bin/bash

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

# enable job control (bg, fg)
set -m

# start vault
docker-entrypoint.sh server -dev &

echo "Environment Variable VAULT_ADDR=$VAULT_ADDR"

echo "Trying to login..."
until vault login $VAULT_DEV_ROOT_TOKEN_ID
do
    echo "Waiting for vault startup..."
    sleep 1
done

echo "Adding customer certificates"
cat $VAULT_PUT_SECRETS_DIR/customer.key | vault kv put secret/customer-key content=-
cat $VAULT_PUT_SECRETS_DIR/customer.cert | vault kv put secret/customer-cert content=-
cat $VAULT_PUT_SECRETS_DIR/customer-encryption.keys | vault kv put secret/customer-encryption-keys content=-
cat $VAULT_PUT_SECRETS_DIR/customer.miw.secret | vault kv put secret/customer.miw.secret content=-

echo "Adding supplier certificates"
cat $VAULT_PUT_SECRETS_DIR/supplier.key | vault kv put secret/supplier-key content=-
cat $VAULT_PUT_SECRETS_DIR/supplier.cert | vault kv put secret/supplier-cert content=-
cat $VAULT_PUT_SECRETS_DIR/supplier-encryption.keys | vault kv put secret/supplier-encryption-keys content=-
cat $VAULT_PUT_SECRETS_DIR/supplier.miw.secret | vault kv put secret/supplier.miw.secret content=-

# and get the actual server process back to the foreground
fg %1
