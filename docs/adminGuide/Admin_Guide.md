# Getting started

To start the application refer to the [Install.md](../../INSTALL.md).
It provides information on different deployments for first tests till full deployment via helm.

# Technical Deployment

The application mainly has the following components that need to be configured.

The subchapters give you an overview of needed configurations and how to get the software running. It links to the
respective deployment scenarios for further information.

| Deployment Scenario                  | File                                                                              |
|--------------------------------------|-----------------------------------------------------------------------------------|
| Local / Spring                       | [application.properties](../../backend/src/main/resources/application.properties) |
| Local / Docker Compose (Integration) | [docker-compose.yaml](../../local/docker-compose.yaml)                            |
| Kubernetes / Helm                    | [README.md](../../charts/puris/README.md)                                         |

## Configure Frontend to use Backend

Mainly one needs to configure the Frontend to reach the Backend. Thus, the Backend must be exposed or at least reachable
for the Frontend.

Mainly the environment variables with the prefixes `BACKEND_` and `ENDPOINT_` are needed for configuration. Please refer
to the respective deployments for exact information.

The Backend secures all APIs except for the swagger ui with a backend key.
This api key must be configured in the `application.properties` with the `puris.api.key` property.
Additionally spring provides the context path that adds a path for the backend servlets.

The Frontend therefore needs the following two variables:
- `BACKEND_BASE_URL` - used to assemble requests to the backend in format: `http://<hostname:port>/<context path>`
- `BACKEND_API_KEY `- the api key used in the backend
- `ENDPOINT_XYZ` - endpoints of the backend used. But they are hardcoded in the backend.

_Note: The API key header is hard coded to `X-API-KEY`._

## Configure EDC in Backend

Configuration of EDC, see e.g. [Tractus-EDC repository](https://github.com/eclipse-tractusx/tractusx-edc). Refer to the
[local deployment with docker compose](../../local/docker-compose.yaml) for an example configuration.

Configure EDC addresses in the Backend with prefix `edc.`. Refer to the respective deployments for more information.

## Integrate Keycloak into Frontend

The Frontend provides a keycloak integration:

It restricts the accessible views based on the client roles:
- `PURIS_USER` - Common views related to short-term information needs
- `PURIS_ADMIN` - EDC related views (may be used for debugging)

All variables with the `IDP_` prefix are needed for configuration. `IDP_DISABLE` can be set to `true` to not use an idp
(only recommended for development purposes). Refer to the respective deployment specific files.

Configuration and example:

To host an example keycloak instance, configure the following:

- `Realm` with name `Catena-X`
- Create `Client` with name `Cl3-PURIS`
  - `Client authentication` = false
  - `Authentication flow `> `Standard flow` = `true` (rest `false`)
  - `Access settings` 
    - `Valid redirect URIs` = `http:<frontend hostname with port>/*`
    - `Valid post logout redirect URIs` = `http:<frontend hostname with port>`
    - `Web origins` = `http://<frontend hostname with port>`
  - `Roles`: Create `PURIS_ADMIN`, `PURIS_USER`
- Create `users` as wanted
  - puris_test with same password (see credentials tab)
  - add roles in client

_Note: The application does NOT make use of the `Client Authentication` (private) feature of Keycloak Clients._

## Onboarding Your Data

The application, per solution strategy, tries to provide visualization and manipulation capabilities to exchange only
production related information.

_Note: The routes in the following always need to be used based on your backend address configuration including the 
context path._

### Onboard Master Data

The application provides the following endpoints to update master data for your partner. This data may not be entered manually.

You can use this collection as an example for the REST API calls.

| Interface                       | Route                     | Purpose                                                                                                        |
|---------------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------|
| Material                        | /materials                | Add materials that are flagged as material or product with your material number                                | 
| Partner                         | /partner                  | Add partners (customers / suppliers) with sites and addresses and edc Urls                                     |
| Site                            | /partners/putSite         | Adds a site to a partner including addresses                                                                   |
| Relationship Partner & Material | /materialpartnerrelations | Connect material and partner incl. partner-related material numbers and wether they supply or buy the material |

### Onboard Stock Information

One may use the `StockView` related interfaces to add stocks after adding the master data to e.g. regularly update /
overwrite the existing stocks.

| Interface      | Route                      | Purpose                               |
|----------------|----------------------------|---------------------------------------|
| Product Stock  | /stockView/product-stocks  | Add stocks allocated to your customer |
| Material Stock | /stockView/material-stocks | Add stocks allocated to your supplier |
