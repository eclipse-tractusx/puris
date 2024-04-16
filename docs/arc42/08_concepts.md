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

Access Policies in charge:

- Business Partner Number Legal Entity (BPNL)
- Membership Credential

Usage Policies in charge:

- Framework Agreement credential (partner signed the Framework Agreement)
- Purpose credential (partner is member of Catena-X)

TODO: Example policies

_Note: see configuration of usage policies in [AdminGuide](../adminGuide/Admin_Guide.md)._

All traffic to exchange information is routed through the EDC data plane using the proxy pull mechanism.

Whenever a partner is created, a contract offer for the DTR is registered for the partner's BPNL.
Whenever a material partner relation (partner buys material, partner suppliers material),

- the contract offers for the submodels are created for the partner
- the submodel descriptors are added to the twin

Additionally, the administrator may configure the application to use a framework agreement policy for data offers (see
[Administration Guide](../adminGuide/Admin_Guide.md)). When activated,

- the application requires the respective credential to be active for the partner.
- the application requires the framework agreement credential as contract policy when contracting a partner's offer as a
  consumer.

## Reusing Contracts & Caching DTR information

Following the Digital Twin KIT, assets in the EDC can be cut as follows:

1. one asset per material and submodel
1. one asset per submodel (implicitly emphasized by R24.05 standards)
1. one asset per material (excluded by R24.05 standards)

**NOTE: the PURIS FOSS assumes the second and can only optimize and cache the information for the second case.**

There are two kind of assets, the PURIS FOSS application works with:

- Application Programming Interfaces (currently only the DTR)
- Submodel Endpoints (value-only serialization)

When contracting, the following information (`ContractMapping`) is stored:

- asset id
- contract id
- protocol url of edc
- href url per material (submodel only)

When a new transfer is needed, the system checks if a `ContractMapping` for the resource in question exists.

- If yes,
    - use this information to get the data directly via the EDC by initializing a transfer.
    - if anything fails, go the whole way to get the data (contract DTR, contract submodel, save ContractMapping)
- if no,
    - go the whole way to get the data (contract DTR, contract submodel, save ContractMapping)

Whenever a partner is created, all exchanged information APIs that comply to a Catena-X standard are registered as
contract offer for the partner's BPNL.

## Security

Backend APIs are secured by an API Key. The Frontend may be configured to be accessed based on keycloak authentication.
Refer to the [Admin Guide](../adminGuide/Admin_Guide.md) for further information.

Access to respective resources is always granted based on the actual relationships as stated in section 'Multi-Partner
Information'. Means there are two levels of security:

1. The partner is maintained and access has been granted via the EDC.
2. The partner is routed through the EDC.

To do the second, the Connector in use needs the extension [additional headers](TODO).
