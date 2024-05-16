# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v2.0.0](https://github.com/eclipse-tractusx/puris/releases/tag/v2.0.0)

### Added

- Implementation of the following standards relying on Digital Twins and Industry Core
    - Delivery Information Exchange CX-0118, version 2.0.0
    - Short-Term Material Demand Exchange CX-0120, version 2.0.0
    - Planned Production Output Exchange CX-0121, version 2.0.0
    - Item Stock Exchange CX-0122, version 2.0.0
- Data Sovereignty
    - Added Usage Purpose as mandatory for Submodel
- Infrastructure
    - Digital Twin Registry is mandatory (app version)
    - Update to Tractus-x EDC Version 0.7.3 (app version)
    - Mock version of IATP compliant MIW for local deployment

### Changed

- Framework Credential now is mandatory (removed `backend.frameworkagreement.use`)

### Removed

- Implementation of CX-0122 version 1.0.0

### Security

- Identity Provider (IDP) support for DTR
    - One user for EDC (read)
    - One user for PURIS (manage)

### Known Knowns

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
