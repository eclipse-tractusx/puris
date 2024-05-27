# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v2.0.1](https://github.com/eclipse-tractusx/puris/releases/tag/2.0.1)

The following Changelog lists the changes. Please refer to the [documentation](docs/README.md) for configuration needs
and understanding the concept changes.

The **need for configuration updates** is **marked bold**.

### Added

* Added Footer

### Changed

Frontend updates

* Labels are now above and not on the border of elements
* updated icons for catalog, negotiations, transfers, logout
* Font update for Role switching elements in stocks and dashboard view
* Handled modal dialog behavior to always let failed validations prevent closing
* Modal Dialog Demand
    * Day is marked as mandatory
    * Reset data when reopening
* Catalog View
    * Updated header itle + usage policy naming (previously Asset Action)
    * Added table view
    * increased responsiveness by loading animation
* Negotiation view
    * Added table view + Subheader
* Logout button now logs out again

Fixed constraints to exactly match on leftOperands during consumer side offer evaluation

### Removed

- EDR Endpoint as removed for edr api usage in appVersion 2.0.0

### Known Knowns

#### Security

The Backend is currently secured via API Key while the Frontend already uses an API-KEY.
See [Admin Guide](./docs/admin/Admin_Guide.md) for further information.

#### Upgradeability

As currently no active user was known migrations of data are not yet supported. The chart technically is upgradeable.

#### Data Sovereignty

For productive use the following enhancements are encouraged

* User FrontEnd available: Role Company Admin is able to query catalogue and see negotiations and transfers
  But company rules / policies need to be configured upfront in backend (via postman) to enable automatic contract
  negotiations, responsibility lies with Company Admin role
  --> add section in the User Manual describing this and the (legal) importance and responsibility behind defining these
  rules
* Currently only one standard policy per reg. connector / customer instance is supported (more precisely one for DTR,
  one for all submodels), negotiation happens automatically based on this
  --> enhance option to select partner and define specific policies (to be planned in context of BPDM Integration)
  --> UI for specific configuration by dedicated role (e.g. Comp Admin) and more flexible policy configuration (without
  code changes) is needed
* As a non-Admin user I do not have ability to view policies in detail --> transparency for users when interacting with
  and requesting / consuming data via dashboard / views on underlying usage policies to be enhanced
* ContractReference Constraint or configuration of policies specific to one partner only not implemented -->
  clarification of potential reference to "PURIS standard contract" and enabling of ContractReference for 24.08.
* unclear meaning of different stati in negotations --> add view of successfull contract agreeements wrt which data have
  been closed
* current logging only done on info level --> enhance logging of policies (currently only available at debug level)
* in case of non-matching policies (tested in various scenarios) no negotiation takes place -->
  **enhance visualization or specific Error message to user**
* no validation of the Schema "profile": "cx-policy:profile2405" (required to ensure interop with other PURIS apps)

#### Styleguide

Overall

* Brief description at the top of each page describing content would be nice for better user experience.

Dashboard

* Similar for Create Delivery (here SOME entries are reset but warnings stay) (**block**)

Stocks

* user needs better guidance to click on a stock to update it (else error prone to enter one
  slightly different attribute and Add instead of update)
* Refresh -- update request has been sent successfully. -> more information regarding data transfer needed for user

Catalog

* No action possible -> unclear to user when and how user will consume an offer

Negotiations

* Add filters for transparency (bpnl, state)

## [v2.0.0](https://github.com/eclipse-tractusx/puris/releases/tag/2.0.0)

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
    - Updated user guide and added it to the frontend (help feature)
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

#### Security

The Backend is currently secured via API Key while the Frontend already uses an API-KEY. See
[Admin Guide](./docs/admin/Admin_Guide.md) for further information.

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

Overall

- Brief description at the top of each page describing content would be nice for better user experience.
- Please use different icon for Catalog, Negotiations, Transfers - currently all with
- Logout button does not work
- Logout button has trash symbol (**block**)
- Footer Component missing

Dashboard

- DropDown Headers / Field Headers not correctly placed. Other views / data entry / filter screens to be checked.
- Dashboard currently has larger fond than all other Page Headers -> please unify
- Create Demand - Day is mandatory and should require an asterix
- Create Demand - Upon entering data and closing, SOME (not all) entries are preserved (and so are warnings for
  mandatory fields) --> upon closing and re-opening information should be reset to default. (**block**)
- Similar for Create Delivery (here SOME entries are reset but warnings stay) (**block**)

Stocks

- Switch between Material Stocks and Product Stocks -> same layout adaption as for Dashboard suggested (see above)
- Like the functionality that by clicking on a stock, data gets entered for add/update -> user needs better guidance to
  do this (else error prone to enter one slightly different attribute and Add instead of update)
- Refresh -- update request has been sent successfully. -> more information regarding data transfer needed for user

Catalog

- Usage Policies are called "Asset Action"
- Header Name should be "View Connector Catalog" or "View Partner Catalog"
- No action possible -> unclear to user when and how user will consume an offer
- List shows a couple of items and item view takes up quite some space --> maybe use a table instead to show more items
  in overview
- Delay in loading results and first "No Catalog available..." shown and then load items

Negotiations

- Similar as Catalog
- Add filters for transparency (bpnl, state)

## [v1.0.0](https://github.com/eclipse-tractusx/puris/releases/tag/1.0.0)

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
