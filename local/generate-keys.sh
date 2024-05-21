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

# generate EDC PW (used for both EDC and BDRS)
EDC_API_PW=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`

# create folders, if not existing
mkdir -p ./vault/secrets
mkdir -p ./iam-mock/keys

CUSTOMER_KC_DTR_EDC_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
SUPPLIER_KC_DTR_EDC_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`

CUSTOMER_KC_MIW_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
SUPPLIER_KC_MIW_CLIENT_SECRET=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`

# generate .env
echo "Creating .env"
cat << EOF > .env
VAULT_DEV_ROOT_TOKEN_ID=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
EDC_API_PW=$EDC_API_PW
PG_USER=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
PG_PW=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
VAULT_SECRETS_DIR=/vault/secrets/
KC_MIW_ENC=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`

CUSTOMER_BPNL=BPNL4444444444XX
CUSTOMER_OAUTH_SECRET_ALIAS=customer.miw.secret
CUSTOMER_OAUTH_CLIENT_ID=customer_private_client
CUSTOMER_PRIVATE_KEY_ALIAS=customer-key
CUSTOMER_PUBLIC_KEY_ALIAS=customer-cert
CUSTOMER_ENCRYPTION_KEYS_ALIAS=customer-encryption-keys
CUSTOMER_BACKEND_API_KEY=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
CUSTOMER_KC_DTR_EDC_CLIENT_ALIAS=customer.dtr.edc-client.secret
CUSTOMER_KC_DTR_PURIS_CLIENT_ALIAS=customer.dtr.puris-client.secret

SUPPLIER_BPNL=BPNL1234567890ZZ
SUPPLIER_OAUTH_SECRET_ALIAS=supplier.miw.secret
SUPPLIER_OAUTH_CLIENT_ID=supplier_private_client
SUPPLIER_PRIVATE_KEY_ALIAS=supplier-key
SUPPLIER_PUBLIC_KEY_ALIAS=supplier-cert
SUPPLIER_ENCRYPTION_KEYS_ALIAS=supplier-encryption-keys
SUPPLIER_BACKEND_API_KEY=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
KEYCLOAK_MIW_PUBLIC_CLIENT=miw_public
SUPPLIER_KC_DTR_EDC_CLIENT_ALIAS=supplier.dtr.edc-client.secret
SUPPLIER_KC_DTR_PURIS_CLIENT_ALIAS=supplier.dtr.puris-client.secret

KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
KEYCLOAK_CLIENT_ID=miw_private_client
SUPPLIER_KC_MIW_CLIENT_SECRET=$SUPPLIER_KC_MIW_CLIENT_SECRET
CUSTOMER_KC_MIW_CLIENT_SECRET=$CUSTOMER_KC_MIW_CLIENT_SECRET

KC_READ_CLIENT_ID=FOSS-EDC_CLIENT
CUSTOMER_KC_DTR_EDC_CLIENT_SECRET=$CUSTOMER_KC_DTR_EDC_CLIENT_SECRET
SUPPLIER_KC_DTR_EDC_CLIENT_SECRET=$SUPPLIER_KC_DTR_EDC_CLIENT_SECRET
KC_MANAGE_CLIENT_ID=FOSS-DTR-CLIENT
CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET=$CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET
SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET=$SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET
EOF

echo "Creating customer key, cert, keys and SHA...   "
CUSTOMER_CERT="./vault/secrets/customer.cert"
CUSTOMER_KEY="./vault/secrets/customer.key"
CUSTOMER_ENCRYPTION_KEYS="./vault/secrets/customer-encryption.keys"
CUSTOMER_MIW_CLIENT_SECRET="./vault/secrets/customer.miw.secret"

CUSTOMER_KC_DTR_EDC_CLIENT_SECRET_FILE_PATH="./vault/secrets/customer.dtr.edc-client.secret"
echo -n $CUSTOMER_KC_DTR_EDC_CLIENT_SECRET >> $CUSTOMER_KC_DTR_EDC_CLIENT_SECRET_FILE_PATH
CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET_FILE_PATH="./vault/secrets/customer.dtr.puris-client.secret"
echo -n $CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET>> $CUSTOMER_KC_DTR_PURIS_CLIENT_SECRET_FILE_PATH

openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $CUSTOMER_KEY -out $CUSTOMER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key=`openssl rand -base64 32`
printf "${key}" > $CUSTOMER_ENCRYPTION_KEYS

# Save customer secret (miw) to file for vault put
printf "${CUSTOMER_KC_MIW_CLIENT_SECRET}" > $CUSTOMER_MIW_CLIENT_SECRET

CUSTOMER_CERT_SHA="$(openssl x509 -in "$CUSTOMER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

echo "Creating supplier key, cert, keys and SHA..."
SUPPLIER_CERT="./vault/secrets/supplier.cert"
SUPPLIER_KEY="./vault/secrets/supplier.key"
SUPPLIER_ENCRYPTION_KEYS="./vault/secrets/supplier-encryption.keys"
SUPPLIER_MIW_CLIENT_SECRET="./vault/secrets/supplier.miw.secret"

SUPPLIER_KC_DTR_EDC_CLIENT_SECRET_FILE_PATH="./vault/secrets/supplier.dtr.edc-client.secret"
echo -n $SUPPLIER_KC_DTR_EDC_CLIENT_SECRET >> $SUPPLIER_KC_DTR_EDC_CLIENT_SECRET_FILE_PATH
SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET_FILE_PATH="./vault/secrets/supplier.dtr.puris-client.secret"
echo -n $SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET >> $SUPPLIER_KC_DTR_PURIS_CLIENT_SECRET_FILE_PATH

openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $SUPPLIER_KEY -out $SUPPLIER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key=`openssl rand -base64 32`
printf "${key}" > $SUPPLIER_ENCRYPTION_KEYS

# Save customer secret (miw) to file for vault put
printf "${SUPPLIER_KC_MIW_CLIENT_SECRET}" > $SUPPLIER_MIW_CLIENT_SECRET

SUPPLIER_CERT_SHA="$(openssl x509 -in "$SUPPLIER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

echo "Creating key pair for mock iam"
openssl ecparam -name prime256v1 -genkey -out ./iam-mock/keys/private_key.pem
openssl ec -in ./iam-mock/keys/private_key.pem -pubout -out ./iam-mock/keys/public_key.pem

echo "Copy private keys for supplier and customer edr refresh signing in mock iam"
cp $SUPPLIER_KEY ./iam-mock/keys/supplier.key
cp $CUSTOMER_KEY ./iam-mock/keys/customer.key

echo "Generate seed-bdrs.sh"
cat << EOF > seed-bdrs.sh
#!/bin/bash

KEY=$EDC_API_PW

if [ -z "\$KEY" ]; then
  echo "KEY is not set. Please specify the key (see bdrs docker compose definition). Exiting..."
  exit 1
fi

curl -X POST -H "x-api-key: \$KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL4444444444XX", "did": "did:web:mock-util-service/customer" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""

curl -X POST -H "x-api-key: \$KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL1234567890ZZ", "did": "did:web:mock-util-service/supplier" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""

curl -X POST -H "x-api-key: \$KEY" -H "Content-Type: application/json" -d '{ "bpn": "BPNL000000000000", "did": "did:web:mock-util-service/trusted-issuer" }' http://localhost:8581/api/management/bpn-directory | jq
echo ""
EOF

# let everyone access the files so that the non-root user in vault container can put them
chmod -R 755 ./vault/secrets
chmod -R 755 ./iam-mock/keys
