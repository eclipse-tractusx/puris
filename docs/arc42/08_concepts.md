# Cross-cutting Concepts

## Multi-Partner Information

Within the supply network there may be materials / parts that are produced for multiple partners or customers that
share the same material number.
A partner asking for information may not receive any information that are not meant for him. Therefor the PURIS concept
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

## Data Sovereignty

The application only provides assets (API usage) based on a Business Partner Number Legal Entity (BPNL) based access
policy via the [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc). All traffic to exchange
information is routed through the EDC data plane using the proxy pull mechanism.

Whenever a partner is created, all exchanged information APIs that comply to a Catena-X standard are registered as
contract offer for the partner's BPNL.

Additionally, the administrator may configure the application to use a framework agreement policy for data offers (see
[Administration Guide](../adminGuide/Admin_Guide.md)). When activated,

- the application requires the respective credential to be active for the partner.
- the application requires the framework agreement credential as contract policy when contracting a partner's offer as a
  consumer.

## Security

Backend APIs are secured by an API Key. The Frontend may be configured to be accessed based on keycloak authentication.
Refer to the [Admin Guide](../adminGuide/Admin_Guide.md) for further information.
