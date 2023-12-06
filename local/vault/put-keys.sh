#!/bin/bash

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
