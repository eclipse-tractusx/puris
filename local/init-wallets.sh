#!/bin/bash

response=$(curl -X POST -d 'client_id=miw_private_client&grant_type=client_credentials&client_secret=miw_private_client&scope=openid' http://localhost:8080/realms/miw_test/protocol/openid-connect/token)

token=$(echo "$response" | jq -r '.access_token')

echo $result

curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "customer wallet", "bpn": "BPNL4444444444XX" }' http://localhost:8000/api/wallets

curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{ "name": "supplier wallet", "bpn": "BPNL1234567890ZZ" }' http://localhost:8000/api/wallets
