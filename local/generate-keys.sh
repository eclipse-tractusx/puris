#!/bin/bash
# generate .key .cert (asymmetric encryption) and .keys (data encryption edc) for customer and supplier
# generate .key .cert (asymmetric encryption) and .keys (data encryption edc) for daps

# create folders, if not existing
mkdir -p ./vault/secrets
mkdir -p ./daps/keys

echo "Creating customer key, cert, keys and SHA..."
CUSTOMER_CERT="./vault/secrets/customer.cert"
CUSTOMER_KEY="./vault/secrets/customer.key"
CUSTOMER_ENCRYPTION_KEYS="./vault/secrets/customer-encryption.keys"
openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $CUSTOMER_KEY -out $CUSTOMER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key1=`openssl rand -base64 16`
key2=`openssl rand -base64 24`
key3=`openssl rand -base64 32`
echo "${key1},${key2},${key3}" > $CUSTOMER_ENCRYPTION_KEYS

CUSTOMER_CERT_SHA="$(openssl x509 -in "$CUSTOMER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

echo "Creating supplier key, cert, keys and SHA..."
SUPPLIER_CERT="./vault/secrets/supplier.cert"
SUPPLIER_KEY="./vault/secrets/supplier.key"
SUPPLIER_ENCRYPTION_KEYS="./vault/secrets/supplier-encryption.keys"
openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $SUPPLIER_KEY -out $SUPPLIER_CERT
# EDC token encryption keys for edc-extensions/data-encryption
key1=`openssl rand -base64 16`
key2=`openssl rand -base64 24`
key3=`openssl rand -base64 32`
echo "${key1},${key2},${key3}" > $SUPPLIER_ENCRYPTION_KEYS


SUPPLIER_CERT_SHA="$(openssl x509 -in "$SUPPLIER_CERT" -noout -sha256 -fingerprint | tr '[:upper:]' '[:lower:]' | tr -d : | sed 's/.*=//')"

echo "Make sure to update the ./daps/config/clients.yml:"
echo "Customer.transportCertsSha256: $CUSTOMER_CERT_SHA"
echo "Supplier.transportCertsSha256: $SUPPLIER_CERT_SHA"

# DAPS
echo "Creating daps key and cert..."
DAPS_CERT="./daps/keys/omejdn.cert"
DAPS_KEY="./daps/keys/omejdn.key"
openssl req -newkey rsa:2048 -new -batch -nodes -x509 -days 3650 -text -keyout $DAPS_KEY -out $DAPS_CERT

# let everyone access the files so that the non-root user in vault container can put them
chmod -R 755 ./vault/secrets
