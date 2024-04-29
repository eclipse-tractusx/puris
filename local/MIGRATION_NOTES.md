# Policy Definition -> still in alignment

Old

```json
{
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    },
    "@type": "PolicyDefinitionRequestDto",
    "@id": "{{POLICY_ID}}",
    "policy": {
        "@type": "Policy",
        "odrl:permission": [
            {
                "odrl:action": "USE",
                "odrl:constraint": {
                    "@type": "LogicalConstraint",
                    "odrl:or": [
                        {
                            "@type": "Constraint",
                            "odrl:leftOperand": "BusinessPartnerNumber",
                            "odrl:operator": {
                                "@id": "odrl:eq"
                            },
                            "odrl:rightOperand": "{{SUPPLIER_BPNL}}"
                        }
                    ]
                }
            }
        ]
    }
}
```

error

```json
[
    {
        "message": "https://w3id.org/edc/v0.0.1/ns/policy/@type was expected to be http://www.w3.org/ns/odrl/2/Set but it was not",
        "type": "ValidationFailure",
        "path": "https://w3id.org/edc/v0.0.1/ns/policy/@type",
        "invalidValue": [
            "https://w3id.org/edc/v0.0.1/ns/Policy"
        ]
    }
]
```

policy.@type = "odrl:Set"

Catalog Request needs `counterPartyId`

# bdrs

Calls needed:

- management -> create bpn directory
- BPN-Directory -> map of bpn and did reachable

The EDC needs to self-IATP to get a `MembershipCredential` to use the BDRS

DIDs are build following JsonWebKey2020
DID-ID like did:web:name-to-use

Credential Service
mock: https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/test/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImplComponentTest.java

Update Cache -> why does it need a bearer token with the membershipCredToken sent to /bpn-directory?
https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/main/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImpl.java#L92

Dids seem to
be [dependent on the hosting companies' url](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/tx.dataspace.topology.md)

Seems like:

- MIW / DIM are credential services
- Portal + DIM are issuer services

A client uses a token during a request, to grant access to specific resources

- verifier uses it to request the vp
- the CS endpoint is resolved using bdrs
- **What's the bearer access scope**

access scopes

- org.eclipse.tractusx.vc.type:Member:read
- org.eclipse.tractusx.vc.id:uuid:read -> give access to verifieable credential by id

Endoints:

- POST presentations/query
    - uses OAuth2 scopes that need to be mapped to presentation definition
- storage api credentials

https://github.com/eclipse-edc/Connector/blob/4fd16b8e34d685239ea40fc3d8e9b02cc8ccf323/core/common/token-core/src/main/java/org/eclipse/edc/token/TokenValidationServiceImpl.java#L54

- a key is somehow resolved. This may be from the did.json

Following
this [test](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-extensions/bdrs-client/src/test/java/org/eclipse/tractusx/edc/identity/mapper/BdrsClientImplComponentTest.java),

- the VC is signed by the issuer
- the VP is signed by the holder

Following Tractus-X Connector Setup

- DIM = your wallet that already contains VCs -> encapsulated STS and CS
- Credential Service = get your own VP to hand over. (something like the miw)
- SecureTokenService = get auth for something and then request presentation

# Updates EDR

edr callback payload

```json
{
    "id": "3099e0f1-e255-4a00-8a8b-8ec5c16e8758",
    "at": 1714325393313,
    "payload": {
        "transferProcessId": "07231854-112b-45bb-957b-4fb01dc2718f",
        "callbackAddresses": [
            {
                "uri": "http://mock-util-service:80/edr-log",
                "events": [
                    "transfer.process.started"
                ],
                "transactional": false,
                "authKey": "None",
                "authCodeId": "None"
            }
        ],
        "assetId": "ASSET_1",
        "type": "CONSUMER",
        "contractId": "54dd6fe4-7a4e-4de6-b7b8-2f131fc99f79",
        "dataAddress": {
            "properties": {
                "process_id": "6570b7a5-7df9-42be-9fd7-80f200427fc3",
                "participant_id": "BPNL1234567890ZZ",
                "asset_id": "ASSET_1",
                "https://w3id.org/edc/v0.0.1/ns/endpointType": "https://w3id.org/idsa/v4.1/HTTP",
                "https://w3id.org/tractusx/auth/refreshEndpoint": "http://customer-data-plane:8285/api/public",
                "https://w3id.org/tractusx/auth/audience": "did:web:mock-util-service/supplier",
                "agreement_id": "54dd6fe4-7a4e-4de6-b7b8-2f131fc99f79",
                "flow_type": "PULL",
                "https://w3id.org/edc/v0.0.1/ns/type": "https://w3id.org/idsa/v4.1/HTTP",
                "https://w3id.org/edc/v0.0.1/ns/endpoint": "http://customer-data-plane:8285/api/public",
                "https://w3id.org/tractusx/auth/refreshToken": "eyJraWQiOiJjdXN0b21lci1jZXJ0IiwiYWxnIjoiUlMyNTYifQ.eyJleHAiOjE3MTQzMjU2OTMsImlhdCI6MTcxNDMyNTM5MywianRpIjoiMGY2YzM4NjItOGYxZS00YzU1LWIwMzEtNGMzM2NhZWIxMzY5In0.L_r5a_hZY3aFYw4SYOoV_Ct5yWuDJBRwPeujAPKv8aPVB_buRZHDPwwnrlYAIWa4j4QIiKjmMMFQN7NUi56tIYr3An3KGwfycekCAS5CSMMAx7x6In5JTRPyyBEi897gjXYGHDlfFa_j7G5bG4__InwDt5HF_2_BKTrPMGEEGL62pAm2cm9qfZJCNJx2R6tnkSymlR0E6Dju2FsCWiOIbYlPP6JHjDkU9aKRIv6l_n0HodRUELBLKBGi565O5zwkec9sNxYdv4mTwskU4IMOvGJPNgHE3QKpzyPCIl7CzVJICCaMszl698rAp9BYP0tokUNj8yNAKbR5ZutYFnAwSA",
                "https://w3id.org/tractusx/auth/expiresIn": "300",
                "https://w3id.org/edc/v0.0.1/ns/authorization": "eyJraWQiOiJjdXN0b21lci1jZXJ0IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJCUE5MNDQ0NDQ0NDQ0NFhYIiwiYXVkIjoiQlBOTDEyMzQ1Njc4OTBaWiIsInN1YiI6IkJQTkw0NDQ0NDQ0NDQ0WFgiLCJleHAiOjE3MTQzMjU2OTMsImlhdCI6MTcxNDMyNTM5MywianRpIjoiMzMwMjhjZDEtMTVlZC00Njk1LWE0NjMtNDc2MTJlNmZhNDk5In0.AP8BY0gjnKFxeswCPRaalKPD-nyLtXqe8hpEQH_CcWoN48KLXLJzgyQXo04WtcCPe7QBU0dyOd9UBi71tmxPNNACLRg_HZVmAFfRZWSkCY9pr-sreChP0EJcTT7AXgHnBIT0mKZbcQ_8b8g9BI-nS43eAd52I_WAg6oTK5hvyMOha7H-HvPeyNDGPA5QQ2RKuf3JKEw-26RALZdgkLz0VDjHd9CMDJJC0nvkbzP928LvzmLs8r-e1YFJwFtZ-ipVlxb7OiFrg7UeAwwb46spi2epMj3Px1QLXrd-Fd9skV2Iw8PugPIUFm5ehyK2d5mQYB4waAm5kEmgVVLLvwVX8A",
                "https://w3id.org/tractusx/auth/refreshAudience": "did:web:mock-util-service/supplier"
            }
        }
    },
    "type": "TransferProcessStarted"
}
```

Get against EDR API after Transfer Process:
`{{SUPPLIER_EDC}}/{{MANAGEMENT_PATH}}/v2/transferprocesses/{{TRANSFER_PROCESS_ID}}`
Will be loaded lazily

```json
{
    "@type": "DataAddress",
    "endpointType": "https://w3id.org/idsa/v4.1/HTTP",
    "tx-auth:refreshEndpoint": "http://customer-data-plane:8285/api/public",
    "tx-auth:audience": "did:web:mock-util-service/supplier",
    "type": "https://w3id.org/idsa/v4.1/HTTP",
    "endpoint": "http://customer-data-plane:8285/api/public",
    "tx-auth:refreshToken": "eyJraWQiOiJjdXN0b21lci1jZXJ0IiwiYWxnIjoiUlMyNTYifQ.eyJleHAiOjE3MTQzMjU2OTMsImlhdCI6MTcxNDMyNTM5MywianRpIjoiMGY2YzM4NjItOGYxZS00YzU1LWIwMzEtNGMzM2NhZWIxMzY5In0.L_r5a_hZY3aFYw4SYOoV_Ct5yWuDJBRwPeujAPKv8aPVB_buRZHDPwwnrlYAIWa4j4QIiKjmMMFQN7NUi56tIYr3An3KGwfycekCAS5CSMMAx7x6In5JTRPyyBEi897gjXYGHDlfFa_j7G5bG4__InwDt5HF_2_BKTrPMGEEGL62pAm2cm9qfZJCNJx2R6tnkSymlR0E6Dju2FsCWiOIbYlPP6JHjDkU9aKRIv6l_n0HodRUELBLKBGi565O5zwkec9sNxYdv4mTwskU4IMOvGJPNgHE3QKpzyPCIl7CzVJICCaMszl698rAp9BYP0tokUNj8yNAKbR5ZutYFnAwSA",
    "tx-auth:expiresIn": "300",
    // use Header Authorization <authorization token>
    "authorization": "eyJraWQiOiJjdXN0b21lci1jZXJ0IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJCUE5MNDQ0NDQ0NDQ0NFhYIiwiYXVkIjoiQlBOTDEyMzQ1Njc4OTBaWiIsInN1YiI6IkJQTkw0NDQ0NDQ0NDQ0WFgiLCJleHAiOjE3MTQzMjU2OTMsImlhdCI6MTcxNDMyNTM5MywianRpIjoiMzMwMjhjZDEtMTVlZC00Njk1LWE0NjMtNDc2MTJlNmZhNDk5In0.AP8BY0gjnKFxeswCPRaalKPD-nyLtXqe8hpEQH_CcWoN48KLXLJzgyQXo04WtcCPe7QBU0dyOd9UBi71tmxPNNACLRg_HZVmAFfRZWSkCY9pr-sreChP0EJcTT7AXgHnBIT0mKZbcQ_8b8g9BI-nS43eAd52I_WAg6oTK5hvyMOha7H-HvPeyNDGPA5QQ2RKuf3JKEw-26RALZdgkLz0VDjHd9CMDJJC0nvkbzP928LvzmLs8r-e1YFJwFtZ-ipVlxb7OiFrg7UeAwwb46spi2epMj3Px1QLXrd-Fd9skV2Iw8PugPIUFm5ehyK2d5mQYB4waAm5kEmgVVLLvwVX8A",
    "tx-auth:refreshAudience": "did:web:mock-util-service/supplier",
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
        "tx-auth": "https://w3id.org/tractusx/auth/",
        "cx-policy": "https://w3id.org/catenax/policy/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    }
}
```
