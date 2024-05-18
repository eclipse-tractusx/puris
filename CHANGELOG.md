# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v2.0.0](https://github.com/eclipse-tractusx/puris/releases/tag/v2.0.0)

The following Changelog lists the changes. Please refer to the [documentation](docs/README.md) for configuration needs
and understanding the concept changes.

The **need for configuration updates** is **marked bold**.

### Added

- Implementation of the following standards relying on Digital Twins and Industry Core
    - Delivery Information Exchange CX-0118, version 2.0.0
    - Short-Term Material Demand Exchange CX-0120, version 2.0.0
    - Planned Production Output Exchange CX-0121, version 2.0.0
    - Item Stock Exchange CX-0122, version 2.0.0
- Added submodel PartTypeInformation + pull flow of the Catena-X ID for shared assets pattern of the Digital Twin /
  Industry Core KIT.
- Implementation of Digital Twins. **Added Digital Twin Registry (DTR) as dependency**. Used to
    - create and update digital twins for partners
    - query partner materials and determine submodels
- Added Identity Provider (IDP) support for DTR
    - One user for EDC (read)
    - One user for PURIS (manage)
- Frontend updates
    - Reworked whole frontend to use React instead of Vue -> better Styleguide conformity as cx components are used.
    - Updated Dashboard
        - Customer view shows own demand, stock and incoming deliveries. You can select customer partner sites to see
          planned production, stocks and outgoing deliveries.
        - Supplier view shows own planned production, stock and outgoing deliveries. You can select supplier partner
          sites
          to see demand, stocks and incoming deliveries.
        - Added manual create / delete for production, deliveries and demand (stock still separate ui).
        - Added projection of stock from latest stock.
        - Added possibility to pull data from partner for demand, planned production and deliveries.
    - Catalog now uses partners and their EDC url (no freehand check possible anymore)
- **Data Sovereignty**
    - Added Membership Credential to all access policies.
    - **Added Usage Purpose as mandatory for submodel.**
    - **Added version to usage purpose and framework agreement.**
    - Added consumer side verification of contracts.
- Infrastructure
    - **Digital Twin Registry version 0.4.3 is mandatory**
    - **Update to Tractus-x EDC version 0.7.3**
    - Mock version of IATP compliant MIW for local deployment

### Changed

- Data Sovereignty
    - Updated to be compliant to cx odrl:profile. (No Schema-Validation)
    - **Framework Credential now is mandatory** (removed `backend.frameworkagreement.use`)
- Bump supported EDC version from 0.5.x to 0.7.x
    - Update of EDR flow: Don't use the Http Dynamic Receiver extension anymore as it was removed from EDC 0.7.x. Now
      uses
      Tractus-X EDC EDR v2 version getting a fresh token synchronously. EDR Controller to be removed.
    - Terminate transfers after opening.
    - Enhanced reusage of contracts - but still not always possible.
- Local deployment updated.
    - Version bumps for EDC + switched from in-memory to postgres version.
    - Use one postgres for PURIS, EDC and DTR.
    - Updated keycloak to have a Customer and Supplier realm for DTR IDP configuration.
    - Updated MIW to 0.4.0. Then outcommented MIW as it doesn't support Identity and Trust Protocol (IATP, needed for TX
      EDC 0.7.x) and added Mock-IAM mocking the needed services.
- Integration test postman suite
    - Updated test for EDC > new syntax and models.
    - Added tests for new information (demand, planned production, delivery).
    - Added tests for SubmodelDescriptor Setup in DTR.
    - Refatorings for tests.
- **Master Data handling**:
    - Store Catena-X ID for product twins of the partner in material partner relationship. Needed due to Digital Twin
      shared asset approach.
    - Configuration to either let the backend generate your Catena-X ID or to ship it via interface.

### Removed

- Implementation of CX-0122 version 1.0.0 (Request and Response Endpoint)
- CI: Veracode as license expired

### Known Knowns

#### Upgradeability

As currently no active user was known migrations of data are not yet supported. The chart technically is upgradeable.

#### Data Sovereignty

For productive use the following enhancements are encouraged

- UI to create and manage contract offers and contracts
- possibility to add contract references
- possibility to use multiple Framework or Usage Purpose Constraints in contract policies
- possibility to define contract policies per partner
- user feedback on why negotiations failed (currently refresh runs asynchronously and the admin may only get sufficient
  insights from the logs)

#### Styleguide

To be checked

## [v1.0.0](https://github.com/eclipse-tractusx/puris/releases/tag/v1.0.0)

### Added

- Initial version implementing CX-0122 version 1.0.0

### Changed

- N/A

### Removed

- N/A

### Security

- N/A

### Known Knowns

The application currently is not compliant to the Catena-X Styleguide. It will be updated for R24.05.
