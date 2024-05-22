# Cross-cutting Concepts

## Multi-Partner Information

Within the supply network there may be materials / parts that are produced for multiple partners or customers that
share the same material number.
A partner asking for information may not receive any information that are not meant for him. Therefore the PURIS concept
is, that exchanged information MUST be allocated to a partner to not leak any partner relationships in the vertical
direction.

## Material Numbers

In the backend materials are commonly handled by the own material number. The partner material relationship then brings
the respective material number of a partner leading to the following constellations:

- puris user acts as customer:
    - own material number = material number customer
    - material number partner = material number supplier
- puris user acts as supplier:
    - own material number = material number supplier
    - material number partner = material number customer

Furthermore, as of ItemStockSAMM v2.0.0 the supplier CX ID is the sole identifier for a material of a respective
partner.
This leads to two scenarios, within the shared asset approach:

- when acting as a customer, the supplier's global asset id (Catena-X ID) is leading and taken from the
  PartTypeInformation of the supplier's twin.
- when acting as a supplier, the initially generated global asset id (Catena-X ID) is handed over to the customers.

Within PURIS this results in the following steps:

- when creating a material (independent of the role) a global asset id (Catena-X ID) is defined on the material, which
  is used for the supplier twin.
- when creating a mpr, the global asset id (Catena-X ID), initially left null
    - is usually not set (acting as supplier)
    - is set to the supplier's material's catena-X id (acting as customer)

## Data Sovereignty

Following the standard, the following measures have been taken:

- Access Policies and Access Restrictions
- Contract Policies
- Consumer Side Validation
- Views in Frontend (admin access, see [User Guide](../user/User_Guide.md), not handled in this chapter) to
  overview negotiations and transfers.

### Access Policies and Access Restrictions

Each Contract Offer has the same access policies. When creating a partner, for all potential assets a contract offer
is created using the following constraints:

- Business Partner Number Legal Entity (BPNL)
- Membership Credential

Example Access Policy creation request used:

```json 
{
    "@context": [
        "http://www.w3.org/ns/odrl.jsonld",
        {
            "edc": "https://w3id.org/edc/v0.0.1/ns/",
            "cx-policy": "https://w3id.org/catenax/policy/"
        }
    ],
    "@type": "PolicyDefinitionRequestDto",
    "@id": "BPNL1234567890ZZ_policy",
    "edc:policy": {
        "@type": "Set",
        "permission": [
            {
                "action": "use",
                "constraint": {
                    "@type": "LogicalConstraint",
                    "and": [
                        {
                            "@type": "LogicalConstraint",
                            "leftOperand": "BusinessPartnerNumber",
                            "operator": "eq",
                            "rightOperand": "BPNL1234567890ZZ"
                        },
                        {
                            "@type": "LogicalConstraint",
                            "leftOperand": "Membership",
                            "operator": "eq",
                            "rightOperand": "active"
                        }
                    ]
                }
            }
        ]
    }
}
```

**Digital Twin Registry Configuration**

Additionally, when creating the Digital Twins in the Digital Twin Registry, the `ShellDescriptors` are restricted in
access using
the [classic Access Control](https://github.com/eclipse-tractusx/sldt-digital-twin-registry/tree/main/docs#classic-implementation)
which restricts the `specificAssetIds` based on BPNLs.

### Contract Policies

Two different policies are used depending on the Asset Type:

- Digital Twin Registry: Use Access Policy also as Contract Policy
- Submodels: Use Contract Policy as defined below

_Note: The submodel `PartTypeInformation` is also registered with the same Contract Policy as the other Submodels._

The Constraints used for the Submodel Contract Policies are the following:

- Framework Agreement credential (partner signed the Framework Agreement)
- Usage Purpose (for what can the data be used)

Example for Submodels based on following configurations:

| Property (helm)                             | Value         |
|---------------------------------------------|---------------|
| backend.puris.frameworkagreement.credential | Puris         |
| backend.puris.frameworkagreement.version    | 1.0           |
| backend.puris.purpose.name                  | cx.puris.base |
| backend.puris.purpose.version               | 1             |

```json
{
    "@context": [
        "http://www.w3.org/ns/odrl.jsonld",
        {
            "edc": "https://w3id.org/edc/v0.0.1/ns/",
            "cx-policy": "https://w3id.org/catenax/policy/"
        }
    ],
    "@type": "PolicyDefinitionRequestDto",
    "@id": "Contract_Policy",
    "edc:policy": {
        "@type": "Set",
        "profile": "cx-policy:profile2405",
        "permission": [
            {
                "action": "use",
                "constraint": {
                    "@type": "LogicalConstraint",
                    "and": [
                        {
                            "@type": "LogicalConstraint",
                            "leftOperand": "https://w3id.org/catenax/policy/FrameworkAgreement",
                            "operator": "eq",
                            "rightOperand": "Puris:1.0"
                        },
                        {
                            "@type": "LogicalConstraint",
                            "leftOperand": "https://w3id.org/catenax/policy/UsagePurpose",
                            "operator": "eq",
                            "rightOperand": "cx.puris.base:1"
                        }
                    ]
                }
            }
        ]
    }
}
```

_Note: see configuration of usage policies in [AdminGuide](../admin/Admin_Guide.md)._

### Consumer Side Validation

Following
the [Digital Twin KIT R24.05](https://eclipse-tractusx.github.io/docs-kits/kits/Digital%20Twin%20Kit/Software%20Development%20View/dt-kit-software-development-view#usage-policies)
an empty contract policy shall be used for the DRR. This application does **NOT** check the contract policy of the
Digital Twin Registry, and uses an Access Policy on its own.

For Submodel assets a validation is performed: Whenever the catalog is queried for a Submodel Asset, the returned
catalog is filtered to the same policy that has been configured for the PURIS FOSS.

- if none matches, no negotiation is started
- if at least one matching definition is found, the first is taken to start the negotiation.

The Contract Policy definition can be found above.

## Reusing Contracts & Caching DTR information

Following the Digital Twin KIT, assets in the EDC can be cut as follows:

1. one asset per material and submodel
1. one asset per submodel (implicitly emphasized by R24.05 standards)
1. one asset per material (excluded by R24.05 standards)

**NOTE: the PURIS FOSS assumes the second and can only optimize and cache the information for the second case.**

There are two kind of assets, the PURIS FOSS application works with:

- Application Programming Interfaces (currently only the DTR)
- Submodel Endpoints (value-only serialization)

When contracting, the following information (`ContractMapping`) is stored per partner (bpnl):

- asset id
- contract id
- protocol url of edc

When a new transfer is needed, the system checks if a `ContractMapping` for the resource in question exists.

- If yes,
    - use this information to get the data directly via the EDC by initializing a transfer.
    - if anything fails, go the whole way to get the data (contract DTR, contract submodel, save ContractMapping)
- if no,
    - go the whole way to get the data (contract DTR, contract submodel, save ContractMapping)

Sidenote: Edc information and hrefs for submodels are always taken from the `ShellDescriptor`. That means that whenever
a submodel is requested, the DTR of a partner is queried.

Whenever a partner is created, all exchanged information APIs that comply to a Catena-X standard are registered as
contract offer for the partner's BPNL.

## Security

Backend APIs are secured by an API Key. The Frontend may be configured to be accessed based on keycloak authentication.
Refer to the [Admin Guide](../admin/Admin_Guide.md) for further information.

Access to respective resources is always granted based on the actual relationships as stated in section 'Multi-Partner
Information'. Means there are two levels of security:

1. The partner is maintained and access has been granted via the EDC.
2. The partner is routed through the EDC.

To do the second, the Connector in use needs the
extension [provision additional headers](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/provision-additional-headers).

Beside that the Backend can be configured to use the DTR with an IDP. The PURIS application could differentiate two
users. For configuration, please refer to the [Admin Guide](../admin/Admin_Guide.md).

- read access client for the EDC: added to the `dataAddress` of the DTR Asset
- manage access client for the PURIS application: used to create and manage the ShellDescriptors

_Note: The reference
implementation [Digital Twin Registry](https://github.com/eclipse-tractusx/sldt-digital-twin-registry)
currently only works with one client._

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
