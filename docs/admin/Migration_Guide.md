# Migration Guide

This migration guide is based on the `chartVersion` of the chart that also bumps the `appVersion` of the application. If you don't rely on the provided helm chart, consider the changes of the chart as mentioned below manually.

<!-- TOC -->
- [Migration Guide](#migration-guide)
  - [Version 5.0.x to 6.0.x](#version-50x-to-60x)
    - [The endpoint for Material Partner Relations was changed](#the-endpoint-for-material-partner-relations-was-changed)
  - [Version 4.2.x to 5.0.x](#version-42x-to-50x)
    - [StockView Controller Needs Mandatory Parameter for Routes material-stocks and product-stocks](#stockview-controller-needs-mandatory-parameter-for-routes-material-stocks-and-product-stocks)
  - [Version 4.1.x to 4.2.x](#version-41x-to-42x)
    - [Suppress Contract Creation for Digital Twin Registry (DTR)](#suppress-contract-creation-for-digital-twin-registry-dtr)
  - [Version 4.0.x to 4.1.x](#version-40x-to-41x)
    - [Database changes to support new notification functionality](#database-changes-to-support-new-notification-functionality)
  - [Version 3.0.x to 4.0.x](#version-30x-to-40x)
    - [Moving IDP related configuration to its own section](#moving-idp-related-configuration-to-its-own-section)
    - [Adding Liquibase Database Migrations](#adding-liquibase-database-migrations)
  - [Version 2.8.x to 3.0.x](#version-28x-to-30x)
    - [Fix of NameOverride](#fix-of-nameoverride)
    - [Adding YAML Anchor for Frontend URL](#adding-yaml-anchor-for-frontend-url)
    - [Handling of Protocols HTTP and HTTPS](#handling-of-protocols-http-and-https)
    - [CORS allowed origins](#cors-allowed-origins)
    - [Conformance to CX-0002 "Digital Twins in Catena-X"](#conformance-to-cx-0002-digital-twins-in-catena-x)
      - [TASK Asset update](#task-asset-update)
      - [TASK Shell \& Submodel Descriptor Updates](#task-shell--submodel-descriptor-updates)
    - [Further Improvements](#further-improvements)
  - [Version 2.8.x to Version 2.9.0](#version-28x-to-version-290)
  - [Older Versions](#older-versions)
  - [NOTICE](#notice)
<!-- TOC -->

> [!WARNING]
> Bitnami does change their update and versioning policy starting with 2025-08-28. To install the existing charts with its bitnami dependencies, please consider to manually specify the properties `image.repository` and `image.tag` specifying for the following dependencies:
> 
> - postgresql (image: bitnamilegacy/postgresql:15.4.0-debian-11-r45)
> 
> You have the following options to specify the container image:
> 
> 1. Specify in `values.yaml` below `postgresql`.
> 
> ```yaml
> postgresql: 
>   image: 
>     repository: bitnamilegacy/postgresql
>     tag: 15.4.0-debian-11-r45
> ```
> 
> 2. Set during installation.
> 
> ```bash
> helm install puris -n tractusx-dev/puris \
>   --set postgresql.image.repository=bitnamilegacy/postgresql
>   --set postgresql.image.tag=15.4.0-debian-11-r45
> ```
> 
> Notes:
> 
> - Deploying an older version of the software may have used an older postgresql version. This is NOT applicable for the PURIS charts.
> - The community is working out on how to resolve the issue.

## Version 5.0.x to 6.0.x

### The endpoint for Material Partner Relations was changed

In case you used the backend route `materialpartnerrelations` to create new material partner relations you need to update your API calls. Instead of query paramaters the endpoint now expects the material partner relations as a json body.

The following is an example of such a body:

```json
{
  "partnerBpnl": "BPNL1234567890ZZ",
  "ownMaterialNumber": "MNR-7307-AU340474.002",
  "partnerMaterialNumber": "MNR-8101-ID146955.001",
  "partnerSuppliesMaterial": true,
  "partnerBuysMaterial": false
}
```

Please note that with this change the properties `partnerCxNumber` and `nameAtManufacturer` were removed from the creation payload.

For further documentation of the changed endpoint please refer to the swagger-ui hosted at `your-backend-address/catena/swagger-ui/index.html` or the [latest version hosted at the api hub.](https://eclipse-tractusx.github.io/api-hub/puris/)

## Version 4.2.x to 5.0.x

### StockView Controller Needs Mandatory Parameter for Routes material-stocks and product-stocks

In case you made use of the backend routes `stockView/material-stocks` or `stockView/product-stocks`, both now need a mandatory parameter `ownMaterialNumber` accepting the base64 encoded material number (same as for production, demand and delivery). This is done to harmonize the API and fix [issue#979](https://github.com/eclipse-tractusx/puris/issues/979).

Due to low usage volumes, we decided to not keep this part downward compatible.

Please refer to the swagger-ui hosted at `your-backend-address/catena/swagger-ui/index.html` or the [latest version hosted at the api hub.](https://eclipse-tractusx.github.io/api-hub/puris/)

## Version 4.1.x to 4.2.x

### Suppress Contract Creation for Digital Twin Registry (DTR)

The property `puris.backend.dtr.edc.asset.register` steers the creation of a DTR asset created during startup and the contract definition during partner onboarding. If set to false, these steps are suppressed. This allows adopters to run the application on enablement services used by other applications. 

## Version 4.0.x to 4.1.x

### Database changes to support new notification functionality

To enable the new forwarding and resolving functionality of notifications as well as align with the standard the following database changes are made:

- new field `resolving_measure_description` added
- `source_notification_id` renamed to `source_disruption_id`
- `related_notification_id` removed
- new table `notification_related_notification_ids`

All field changes apply to the tables `own_demand_and_capacity_notification` as well as `reported_demand_and_capacity_notification`.

The changes will automatically be applied by Liquibase if you've followed the instructions of the previous migration.

**YOUR TASK**: Due to the incomplete implementation of notifications in the old release it is not guaranteed that previously received or sent notifications are valid. If you have already used notifications verify that they are valid.

## Version 3.0.x to 4.0.x

### Moving IDP related configuration to its own section

With the overhaul of the authentication, the IDP values are now used by frontend and backend both. As a result all values of `frontend.puris.keycloak` were moved to the `idp` section.

Due to the new authentication the IDP configuration is no longer optional and the `idp.disabled` property is no longer supported

**YOUR TASK**: Move all keys except `disabled` of `frontend.puris.keycloak` to the new idp section.

### Adding Liquibase Database Migrations

With chart version 4.0.0 you can use the automatic migrations provided
via [backend](../../backend/src/main/resources/db/changelog).

To do so, set `backend.puris.jpa.hibernate.ddl-auto` to `validate`.

With this release the field `last_updated_on_date_time` is added to following relations

- `own_delivery`
- `own_demand`
- `own_production`

Further for `appVersion = 3.1.0` the database schema has been used as baseline.

## Version 2.8.x to 3.0.x

*This migration guide covers the changes provided from [appVersion 3.0.0 to appVersion 3.1.0](https://github.com/eclipse-tractusx/puris/compare/3.0.0...3.1.0).*

### Fix of NameOverride

`nameOverride` has been implemented incorrectly so that has not been considered in the templates ([pr#856](https://github.com/eclipse-tractusx/puris/pull/856)).

As this change is a breaking change (label selectors are immutable) during migrations version [`2.9.0`](#version-28x-to-version-290) SHOULD NOT be used.

The chart in version 3.0.x deletes the `Deployment` resources of the frontend and backend once pre-upgrade via hook.
This means that the application has a (small downtime).

Please note that chart version `3.0.0` had a bug not considering `backend.fullnameOverride`, `frontend.fullnameOverride` and the release name (see [pr#885](https://github.com/eclipse-tractusx/puris/pull/884)).

In case your chart relies on the `fullnameOverride` option, it may be that all chart upgrades, will delete your deployments first.

### Adding YAML Anchor for Frontend URL

There has already been a YAML anchor for the backend url (`global.domain.backend.ingress`).
Now the chart also provides a YAML anchor for the frontend url (`global.domain.frontend.ingress`).

**YOUR TASK**: Consider switching to the new syntax. Your old syntax still works.

### Handling of Protocols HTTP and HTTPS

The default values have been inconsistent. Thus, now `https` is used for defaults.
For Frontend and Backend related urls, the protocol will be defaulted if missing.

**YOUR TASK**: This is just an improvement for fail safety. If your deployment works, you already used a protocol.

### CORS allowed origins

The allowed origins in the backend now must be configured to the frontend. This is an increase in security and has been introduced with the websocket to feedback information to the frontend dynamically (see [pr#847](https://github.com/eclipse-tractusx/puris/pull/847)).

**YOUR TASK**: You need to configure `backend.puris.allowedOrigins` according to your frontend's ingress address. Please consider the [YAML anchor](#adding-yaml-anchor-for-frontend-url).

### Conformance to CX-0002 "Digital Twins in Catena-X"

We noticed issues in the conformance to digital twins, as you can see in the following PRs:

- fix(submodel-endpoints): throw 501 for operations not implemented [#850](https://github.com/eclipse-tractusx/puris/pull/850)
- fix: expose and contract submodels following CX-0002 [#849](https://github.com/eclipse-tractusx/puris/pull/849)
- fix(dtr): register dtr asset with api/v3 prefix [#824](https://github.com/eclipse-tractusx/puris/pull/824)

This results in the following changes:

- the edc asset of the digital twin registry needs to have `api/v3` appended to `dataAddress.baseUrl`
- the submodel urls have been incorrect for two reasons:
  - all (except for `PartTypeInstance`) had a trailing `/` that must be removed in shell descriptors field `href`.
  - the path segment `submodel` is missing at the end of the shell descriptor's field `href`.

During the fixes, we changed some configurations:

- `backend.puris.dtr.url` now needs to contain the suffix for the v3 api (`/api/v3`)

**YOUR TASK**: You need to update `backend.puris.dtr.url`. We don't provide a migration for the DTR or the EDC. 
Thus, you need to do the following things manually: 

- Terminate existing contracts for the Digital Twin Registry that have been created by PURIS FOSS.
- Follow the instructions in the subchapter

#### TASK Asset update

Either delete the submodel asset for the DTR manually or update according to the definition mentioned above.
If you delete it,

- restart PURIS to create the EDC assets with the correct paths and corresponding contract defintions
- re-register (PUT with the same information) the `material partner reeelationships` (NOTE: read next task first) 

Note: You can identify the asset for the DTR using the `assetId` = `DigitalTwinRegistry@<your BPNL>`.

#### TASK Shell & Submodel Descriptor Updates

You can manually update all Shell and Submodel Descriptors or remove the ones managed by PURIS FOSS.
If you delete them,

- restart PURIS to create the assets with the correct paths and corresponding contract defintions
- re-register (PUT with the same information) the `material partner relationships` (NOTE: read next task first) so that PURIS FOSS creates the Submodel Descriptors with the latest asset Ids and correct paths.

Note: You can determine the technical identifiers for your twins using the following information:

- material (inbound) twin:  See `MaterialPartnerRelationship.partnerCXNumber`
- product (outbound) twin: See `DigitalTwinMapping.productTwinId`

### Further Improvements

- The `jpa.hibernate.ddl-auto` setting is set to update only (don't drop tables) and parameter is passthroughed correctly ([pr#838](https://github.com/eclipse-tractusx/puris/pull/838))

## Version 2.8.x to Version 2.9.0

Chart version 2.9.0 has been incorrectly labeled as non-breaking (see issue). 

It is NOT RECOMMENDED to be used. Instead, refer to migration to [Version 3.0.0](#version-28x-to-version-300).

## Older Versions

[TRG 2.09](https://eclipse-tractusx.github.io/docs/release/trg-1/trg-1-09) has been introduced with R25.06. Thus, no migration guidelines are provided for older versions.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
