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

## Configure authentication

### IDP configuration

Authentication for frontend and backend uses a Keycloak IDP server. This authentication is configured with the following variables:

- `idp.url` (docker `IDP_URL`) - the base url of the IDP server used to assemble all necessary urls for authentication
- `idp.realm` (docker `IDP_REALM`) - the name of the authentication realm of the Keycloak instance
- `idp.clientId` (docker `IDP_CLIENT_ID`) - the id of the client used for authentication
- `idp.redirectUrlFrontend` (docker `IDP_REDIRECT_URL_FRONTEND`) - the default redirect url when logging in

For the ease of development, the local deployment using docker configures 2 app instances with shared users. This enables easier testing.

> NOTE:
> 
> This practice is not suited for productive environments

### API key configuration

In addition to the aforementioned IDP server, requests can also be authorized using an API key.

This api key must be configured in the backend via `backend.puris.api.key` (docker `PURIS_API_KEY`).

_Note: The API key header is hard coded to `X-API-KEY`._

### Role-based permissions

The IDP defines the following roles and permissions:

- `PURIS_USER`
  - Common views related to short-term information needs
  - only read access to master data
  - no access to EDC views and controller
- `PURIS_ADMIN`
  - EDC related views (may be used for debugging)
  - read and write access to master data
  - access to data import endpoints

Requests authorized via API key always use the role `PURIS_ADMIN`.

## Configure Frontend to use Backend

Mainly one needs to configure the Frontend to reach the Backend. Thus, the Backend must be exposed or at least reachable
for the Frontend.

When not using the Helm deployment, mainly the environment variables with the prefixes `BACKEND_` and `ENDPOINT_` are
needed for configuration. Please refer to the respective local deployment for exact information.

The Frontend therefore needs the following variables:

- `frontend.puris.baseUrl` (docker `BACKEND_BASE_URL`) - used to assemble requests to the backend in
  format: `http://<hostname:port>/<context path>` where context path is assembled automatically via template. Is
  automatically assembled via helm.
- `frontend.puris.endpointXYZ` (docker `ENDPOINT_XYZ`) - endpoints of the backend used. But they are hardcoded in the
  backend.

## Configure EDC in Backend

Configuration of EDC, see e.g. [Tractus-EDC repository](https://github.com/eclipse-tractusx/tractusx-edc). Refer to the
[local deployment with docker compose](../../local/docker-compose.yaml) for an example configuration.

Configure EDC addresses in the Backend with prefix `backend.puris.edc.`. Refer to the respective deployments for more
information.

## Configure DTR in Backend

Configuration of the DTR , see e.g. [Digital Twin Registry repository](https://github.com/eclipse-tractusx/sldt-digital-twin-registry). Refer to the [local deployment with docker compose](../../local/docker-compose.yaml) for an example configuration.

Configure the dtr url in the Backend via prefix `backend.puris.dtr.url`. Refer to the respective deployments for more information.

For running the PURIS FOSS application with a DTR that is used by other deployments, please refer to the section for [shared enablement services](#running-the-puris-foss-application-on-shared-enablement-services).

### DTR - IDP configuration

The DTR can be used with together with an IDP. The IDP can be configured in the Backend with prefix
`backend.puris.dtr.idp`. It can be enabled via the flag `enabled`. Beside the `tokenurl` two clients need to be
configured.

In theory the PURIS FOSS application allows to have one read-only client to be used for queries through the EDC and one
manage (all rights) client used by PURIS FOSS to create and manage digital twins.
In practice, the DTR reference implementation allows only one user, resulting in the following configuration:

| Helm                       | Value to set                                                              |
|----------------------------|---------------------------------------------------------------------------|
| `clients.puris.id`        | ID of the manage client                                                   |
| `clients.puris.secret`     | Secret of the manage client                                               |
| `clients.edc.id`           | ID of the manage client                                                   |
| `clients.edc.secret.alias` | **Path to secret in the vault** accessed by the edc for the manage client |

## Running the PURIS FOSS Application on Shared Enablement Services

PURIS FOSS still has limited capability to be operated on shared enablement services.

Shared enablement services are:

1. The Connector (EDC)
2. The Digital Twin Registry (DTR)

The following issues are known:

1. DTR: The PURIS FOSS can't be operated with overlapping material definitions. If another application creates submodels for the same material. PURIS FOSS overwrites the Digital Twins ignoring other access rights and submodels (see [chapter runtime view of arc42](../architecture/06_runtime_view.md)).
2. EDC: 
   - PURIS FOSS can't naturally reuse EDC assets. It provides own asset and contract definitions (see chapters [runtime view](../architecture/06_runtime_view.md) and [data sovereignty concepts](../architecture/08_concepts.md#data-sovereignty) of arc42).
   - PURIS FOSS **CAN** be operated with DTRs that are accessible to all members but are differentiated on lower digital twin level by [BPNL based access control](https://github.com/eclipse-tractusx/sldt-digital-twin-registry/blob/v0.9.0/docs/architecture/6-crosscutting-concepts.md#access-control-to-digital-twins-based-on-the-bpn-business-partner-number-tenantid). To do so you can set the helm chart property `puris.backend.dtr.edc.asset.register` to `false`.

### Supported Shared DTR Asset and Policy

If the helm chart property `puris.backend.dtr.edc.asset.register` is set to `false`. Then the following constraints need to hold:

The DTR Asset is configured to 

1. be the same DTR configured in `puris.backend.dtr.url`.
2. is accessible by all Partners (e.g. access policy = only CX membership is active and purpose = dtr or PURIS)

Please note: these conditions are not evaluated by PURIS FOSS and must be assessed by the administrator / operator.

PURIS FOSS will still create and maintain digital twins as if the DTR would only be used by PURIS FOSS.

## Data Sovereignty related configuration

With R24.05, always Framework Agreement and Usage Purpose Contract Policies need to be used. Refer to
[ARC42 - Chapter 8](../architecture/08_concepts.md) for the influence of these configurations.
TLDR; you define the definition of the policy you want to use and that you'll accept. PURIS FOSS only handles one policy
that is templated. You can only configure the name and version of the Framework Agreement Credential and the Usage
Purpose.

> NOTE:
> 
> The odrl:profile of Catena-X is currently hard-coded to `cx-policy:profile2405`.

### Framework Agreement

To configure the Framework Agreement credential, that is automatically enforced by the EDC during contracting
(see further details in [ARC42 - Chapter 8](../architecture/08_concepts.md)), the following properties need to be
configured.
The table contains
the puris defaults for release R24.05.

| Helm                                        | Docker                              | Configuration          |
|---------------------------------------------|-------------------------------------|------------------------|
| backend.puris.frameworkagreement.credential | PURIS_FRAMEWORKAGREEMENT_CREDENTIAL | DataExchangeGovernance |
| backend.puris.frameworkagreement.version    | PURIS_FRAMEWORKAGREEMENT_VERSION    | 1.0                    |

_**ATTENTION**: If the credential is NOT listed in
the [odrl profile](https://github.com/catenax-eV/cx-odrl-profile/blob/main/profile.md)
of the current release, then the Tractus-X EDC will NOT technically enforce the credential by checking the availability
in the Credential Service. Thus, it may seem that the Credential is available, but isn't. Same applies to typos._

_Note: Please refer to
the [Portal's documentation on how to sign use case agreements](https://github.com/eclipse-tractusx/portal-assets/blob/main/docs/user/06.%20Certificates/01.%20UseCase%20Participation.md)._

### Usage Purpose

To configure the Usage Purpose under which the assets may be used (see further details
in [ARC42 - Chapter 8](../architecture/08_concepts.md)),
the following properties need to be configured. The table contains the puris defaults for release R24.05.

| Helm                          | Docker                | Configuration |
|-------------------------------|-----------------------|---------------|
| backend.puris.purpose.name    | PURIS_PURPOSE_NAME    | cx.puris.base |
| backend.puris.purpose.version | PURIS_PURPOSE_VERSION | 1             |

_**ATTENTION**: Usage Purposes are no credentials than can be enforced technically. See a list of supported purposes
supported within Catena-X in the [odrl profile](https://github.com/catenax-eV/cx-odrl-profile/blob/main/profile.md)
of the current release._

## Configure ERP Update

To use an ERP Adapter, you need to configure the information below `backend.puris.erpadapter`:

| Helm                                     | Docker                           |
|------------------------------------------|----------------------------------|
| backend.puris.erpadapter.enabled         | PURIS_ERPADAPTER_ENABLED         |
| backend.puris.erpadapter.url             | PURIS_ERPADAPTER_URL             |
| backend.puris.erpadapter.authkey         | PURIS_ERPADAPTER_AUTHKEY         |
| backend.puris.erpadapter.refreshinterval | PURIS_ERPADAPTER_REFRESHINTERVAL |
| backend.puris.erpadapter.timelimit       | PURIS_ERPADAPTER_TIMELIMIT       |

The configuration of the refreshinterval and the timelimit are considered as follows:

- timelimit: whenever an erp update has been scheduled, then it is scheduled for n days. It gets rescheduled for n days 
whenever triggered. Thus, no further update will be done after n days of no trigger.
- refreshinterval: defines in minutes after which time an update is performed for the scheduled updates.

Refer to the [helm chart's README](../../charts/puris/README.md) for further information.

## Rate Limiting using nginx

Rate limiting is by default enabled in the puris frontend served by nginx and can be dynamically configured.
In order to adjust any variables of nginx's rate limiting or disable it, one has to modify the respective variables in
either

- the local docker deployment by setting the necessary environment variables, or
- by modifying the variables in the helm chart values.yaml (prefix `frontend.puris.rateLimiting`).

These variables then get dynamically injected in the nginx.conf file, which is then copied to the docker image to be
used by nginx.
That means that the rate limiting can be disabled by modifying the nginx.conf file in the frontend folder. This is also
the place to insert and override any other nginx configurations.

## Serving with HTTPS / SSL

Serving with SSL is available for Docker and Helm Deployment. In local deployment directly with mvn (backend) and
npm (frontend) it can be configured, too.

For docker configurations, see below. For helm, additionally set the related ingress (frontend, backend) as needed to
enabled and configure it.

### Frontend SSL Configuration

The Frontend uses a nginx-unprivileged image restricting access heavily. One can use the following configuration as a
starting point.

Let's assume the following structure:

```shell
ls
>> /
>> /ssl-certificates
>> /ssl-certificates/localhost.crt
>> /ssl-certificates/localhost.key
>> /nginx.conf
```

For testing purposes, create self-signed certificates:

``` sh
mkdir ssl-certificates
cd ssl-certificates

openssl req -x509 -out localhost.crt -keyout localhost.key \
  -newkey rsa:2048 -nodes -sha256 \
  -subj '/CN=localhost' -extensions EXT -config <( \
   printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
```

_NOTE: For productive use, you can use certificates provided by a Certificate Authority._

Create a nginx.conf to provide certificates for listening on 443 for tls.

``` conf
http {
    # other configurations 
    server {
        listen 443 ssl;
        server_name local-puris-frontend.com;

        ssl_certificate /etc/nginx/ssl/localhost.crt;
        ssl_certificate_key /etc/nginx/ssl/localhost.key;
        
        # TLS version >= 1.2
        ssl_protocols TLSv1.2 TLSv1.3;

        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
    }
}
```

Start the docker image mounting the certificates and the nginx.conf as follows:

``` sh

docker run --rm --name frontend \
  -v $(pwd)/ssl-certificates:/etc/nginx/ssl \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf \
  puris-frontend:dev
>> exposes at 8080, 443
```

If you want to use of the dns alias for localhost:443, make sure to edit your /etc/hosts file:

```sh
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <container_name_or_id>

sudo vim /etc/hosts
>>add entry like 172.17.0.2 local-puris-frontend.com
# :wq! (write and quit)
```

### Backend SSL Configuration

Spring provides the possibility to provide ssl certificates.

Let's assume the following structure:

```shell
ls
>> /
>> /ssl-certificates
>> /ssl-certificates/application.p12
>> /applicaiton-with-ssl.properties
```

For testing purposes, create self-signed certificates using java keytool and follow the prompts.
Remember the password. They generated key file is a pkcs12 keystore.

``` sh
mkdir ssl-certificates
cd ssl-certificates

keytool -genkeypair -alias application -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore application.p12 -validity 3650
```

_NOTE: For productive use, you can use certificates provided by a Certificate Authority._

Use your common application.properties and add the following section to the file. Name it e.g.,
application-with-ssl.properties.

```application.properties
server.ssl.enabled=false
#server.port=8443
server.ssl.bundle=server
spring.ssl.bundle.jks.server.key.alias=application
spring.ssl.bundle.jks.server.keystore.location=file:/opt/app/ssl-certificates/application.p12
spring.ssl.bundle.jks.server.keystore.password=
spring.ssl.bundle.jks.server.keystore.type=PKCS12
```

Finally pass the created keystore and properties file via docker:

```shell
docker run --rm -d -p 8433:8433 --name backend \
  -v $(pwd)/ssl-certificates/application.p12:/opt/app/ssl-certificates/application.p12 \
  -v $(pwd)/test.properties:/opt/app/test.properties \
  -e SPRING_CONFIG_LOCATION=/opt/app/test.properties \
  puris-backend:dev
```

### Troubleshooting SSL

When using self-signed certificates, the frontend may result in a CORS error. The error is likely no CORS related
problem. Please check if you created exceptions for both certificates, the frontend's and backend's certificates. You
can see a related error in the Developer Tools (F12) > Network tab > select preflight header > tab security.

## Onboarding Your Data

The application, per solution strategy, tries to provide visualization and manipulation capabilities to exchange only
production related information.

_Note: The routes in the following always need to be used based on your backend address configuration including the
context path._

### Identity Configuration

Your main information is added via the deployment. On Startup the application creates a partner for your own company.

The company data can be set via helm variables with prefix `backend.puris.own`. It will create one partner with one
site and one address. The data should be your company's legal entity. The legal entity can then be enrichted same as
other partners using the "Onboarding Master Data" interfaces.

### Onboard Master Data

The application provides the following endpoints to update master data for your partner. This data may not be entered
manually.

You can use this collection as an example for the REST API calls.

| Interface                       | Route                     | Purpose                                                                                                        |
|---------------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------|
| Material                        | /materials                | Add materials that are flagged as material or product with your material number                                | 
| Partner                         | /partner                  | Add partners (customers / suppliers) with sites and addresses and edc Urls                                     |
| Site                            | /partners/putSite         | Adds a site to a partner including addresses                                                                   |
| Relationship Partner & Material | /materialpartnerrelations | Connect material and partner incl. partner-related material numbers and wether they supply or buy the material |

Please note that since all Material entities are required to have a CatenaX-Id, you must enter any pre-existing
via the materials-API of the backend, when you are inserting a new Material entity to the backend's database.
If a CatenaX-Id was not assigned to your Material so far, then by having the ```puris.generatematerialcatenaxid``` set
to ```true``` you can auto-generate one randomly (this is the default-setting, by the way).
In a real-world-scenario, you must then use this randomly generated CatenaX-Id for the lifetime of that
Material entity.

**ATTENTION:** please wait some time after updating master data prior to create or update operational data because the
Twins are registered asynchronously when creating / updating material partner relationships (see
[runtime view](../architecture/06_runtime_view.md) for more details).

### Onboard operational data

One may use the `StockView` related respectively the operational interfaces listed below to add operational data after
adding the master data to e.g. regularly update / overwrite the existing data.

| Interface                  | Route                      | Purpose                                                                       |
|----------------------------|----------------------------|-------------------------------------------------------------------------------|
| Product Stock              | /stockView/product-stocks  | Add stocks allocated to your customer                                         |
| Material Stock             | /stockView/material-stocks | Add stocks allocated to your supplier                                         |
| Short-Term Material Demand | /demand                    | Add allocated demands for your supplier                                       |
| Planned Production Output  | /production                | Add allocated production outputs planned for your customer                    |
| Delivery Information       | /delivery                  | Add delivery information for your customer or supplier (depends on inco term) |

Please refer to the [Interface Documentation](../interfaceDoc) and the implementation for further information.

### Import operational data from documents

In order to improve the addition of data, the API offers an endpoint at `/file/upload` to import data for the standard Short Term Material Demand, Planned Production Output, Delivery Information and Item Stock.

Templates for the data upload can be found in `/frontend/public`.

> NOTE:
> 
> Importing data for a standard overwrites existing data. Make sure you do not lose any data.

## Postgres

The PURIS Backend uses a postgres Database. The helm installation already has a dependency that may be installed with
the chart. Optionally it may be disabled to use your own installation. Refer to the overall
[INSTALL.md](../../INSTALL.md) for further information.

## Encryption of confidential data at rest

Encryption at rest for databases works. It has been tested by either encrypting the docker folder or encrypting the
whole filesystem of the machine running.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
