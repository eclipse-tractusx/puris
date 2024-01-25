#!/bin/bash

# retrieve access token for miw from keycloak
response=$(curl -X POST -d 'client_id=miw_private_client&grant_type=client_credentials&client_secret=miw_private_client&scope=openid' http://localhost:8080/realms/miw_test/protocol/openid-connect/token)

token=$(echo "$response" | jq -r '.access_token')

echo $response

# register customer wallet at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "customer wallet", "bpn": "BPNL4444444444XX" }' http://localhost:8000/api/wallets
echo ""
# register supplier wallet at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "supplier wallet", "bpn": "BPNL1234567890ZZ" }' http://localhost:8000/api/wallets
echo ""
# register customer for framework agreement at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "holderIdentifier": "BPNL4444444444XX", "type": "TraceabilityCredential", "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf", "contract-version": "1.0.0" }' http://localhost:8000/api/credentials/issuer/framework
echo ""
# register supplier for framework agreement at miw
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "holderIdentifier": "BPNL1234567890ZZ", "type": "TraceabilityCredential", "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf", "contract-version": "1.0.0" }' http://localhost:8000/api/credentials/issuer/framework
echo ""
