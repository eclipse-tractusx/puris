#!/bin/bash
# generate .key .cert (asymmetric encryption) and .keys (data encryption edc) for customer and supplier

# create folders, if not existing
mkdir -p ./vault/secrets

# generate .env
echo "Creating .env"
cat << EOF > .env
VAULT_DEV_ROOT_TOKEN_ID=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
EDC_API_PW=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
PG_USER=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
PG_PW=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
VAULT_SECRETS_DIR=/vault/secrets/
KC_MIW_ENC=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
CUSTOMER_OAUTH_SECRET_ALIAS=customer.miw.secret
CUSTOMER_OAUTH_CLIENT_ID=customer_private_client
CUSTOMER_PRIVATE_KEY_ALIAS=customer-key
CUSTOMER_PUBLIC_KEY_ALIAS=customer-cert
CUSTOMER_ENCRYPTION_KEYS_ALIAS=customer-encryption-keys
CUSTOMER_BACKEND_API_KEY=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
SUPPLIER_OAUTH_SECRET_ALIAS=supplier.miw.secret
SUPPLIER_OAUTH_CLIENT_ID=supplier_private_client
SUPPLIER_PRIVATE_KEY_ALIAS=supplier-key
SUPPLIER_PUBLIC_KEY_ALIAS=supplier-cert
SUPPLIER_ENCRYPTION_KEYS_ALIAS=supplier-encryption-keys
SUPPLIER_BACKEND_API_KEY=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
KEYCLOAK_MIW_PUBLIC_CLIENT=miw_public
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
KEYCLOAK_CLIENT_ID=miw_private_client
EOF

echo "Creating customer key, cert, keys and SHA...   "
CUSTOMER_CERT="./vault/secrets/customer.cert"
CUSTOMER_KEY="./vault/secrets/customer.key"
CUSTOMER_ENCRYPTION_KEYS="./vault/secrets/customer-encryption.keys"
CUSTOMER_MIW_CLIENT_SECRET="./vault/secrets/customer.miw.secret"
openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $CUSTOMER_KEY -out $CUSTOMER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key=`openssl rand -base64 32`
printf "${key}" > $CUSTOMER_ENCRYPTION_KEYS

# Generate new random password for customer in miw
miw_secret=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
printf "${miw_secret}" > $CUSTOMER_MIW_CLIENT_SECRET
jq ".clients[5].secret = \"$miw_secret\"" ./miw/keycloak-setup.json > ./miw/keycloak-setup-temp.json

CUSTOMER_CERT_SHA="$(openssl x509 -in "$CUSTOMER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

echo "Creating supplier key, cert, keys and SHA..."
SUPPLIER_CERT="./vault/secrets/supplier.cert"
SUPPLIER_KEY="./vault/secrets/supplier.key"
SUPPLIER_ENCRYPTION_KEYS="./vault/secrets/supplier-encryption.keys"
SUPPLIER_MIW_CLIENT_SECRET="./vault/secrets/supplier.miw.secret"
openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $SUPPLIER_KEY -out $SUPPLIER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key=`openssl rand -base64 32`
printf "${key}" > $SUPPLIER_ENCRYPTION_KEYS

# Generate new random password for supplier in miw
miw_secret=`openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32`
printf "${miw_secret}" > $SUPPLIER_MIW_CLIENT_SECRET
jq ".clients[6].secret = \"$miw_secret\"" ./miw/keycloak-setup-temp.json > ./miw/keycloak-setup.json

# remove temp file
rm ./miw/keycloak-setup-temp.json

SUPPLIER_CERT_SHA="$(openssl x509 -in "$SUPPLIER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

# let everyone access the files so that the non-root user in vault container can put them
chmod -R 755 ./vault/secrets
