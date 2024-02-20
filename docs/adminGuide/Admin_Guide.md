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

When not using the Helm deployment, mainly the environment variables with the prefixes `BACKEND_` and `ENDPOINT_` are
needed for configuration. Please refer to the respective local deployment for exact information.

The Backend secures all APIs except for the swagger ui with a backend key.
This api key must be configured in the backend via `backend.puris.api.key` (docker `PURIS_API_KEY`). In Helm, it's
automatically set to the frontend.
Additionally spring provides the context path that adds a path for the backend servlets.

The Frontend therefore needs the following two variables:

- `frontend.puris.baseUrl` (docker `BACKEND_BASE_URL`) - used to assemble requests to the backend in
  format: `http://<hostname:port>/<context path>` where context path is assembled automatically via template. Is
  automatically assembled via helm.
- `frontend.puris.endpointXYZ` (docker `ENDPOINT_XYZ`) - endpoints of the backend used. But they are hardcoded in the
  backend.

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

All variables with the `frontend.puris.keycloak.` (docker prefix is `IDP_`) prefix are needed for configuration.
The `disable` property (docker `IDP_DISABLE`) can be set to `true` to not use an idp (only recommended for development
purposes). Refer to the respective deployment specific files.

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

## Configure Framework Agreement Credential Usage

To configure the usage of a framework agreement credential, that is automatically enforced by the EDC during contracting
(see further details in [ARC42 - Chapter 8](../arc42/08_concepts.md)), the following properties need to be configures:

- `backend.frameworkagreement.use` (docker `PURIS_FRAMEWORKAGREEMENT_USE`) = true
- `backend.frameworkagreement.credential` (docker `PURIS_FRAMEWORKAGREEMENT_CREDENTIAL`) = 'puris' (NOTE: not available
  for R24.03)

_**ATTENTION**: If the credential is NOT listed in the Connector Standard (CX-0018) of the current release, then the
Tractus-X EDC will NOT technically enforce the credential by checking the availability in the Managed Identity Wallet.
Thus, it may seem that the Credential is available, but isn't. Same applies to typos._

_Note: Please refer to
the [Portal's documentation on how to sign use case agreements](https://github.com/eclipse-tractusx/portal-assets/blob/main/docs/user/06.%20Certificates/01.%20UseCase%20Participation.md)._

## Rate Limiting using nginx

Rate limiting is by default enabled in the puris frontend served by nginx and can be dynamically configured.
In order to adjust any variables of nginx's rate limiting or disable it, one has to modify the respective variables in either the
local docker deployment by setting the necessary environment variables, or by modifying the variables in the helm chart values.yaml.

These variables then get dynamically injected in the nginx.conf file, which is then copied to the docker image to be used by nginx.
That means that the rate limiting can be disabled by modifying the nginx.conf file in the frontend folder. This is also the place
to insert and override any other nginx configurations.

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

### Onboard Stock Information

One may use the `StockView` related interfaces to add stocks after adding the master data to e.g. regularly update /
overwrite the existing stocks.

| Interface      | Route                      | Purpose                               |
|----------------|----------------------------|---------------------------------------|
| Product Stock  | /stockView/product-stocks  | Add stocks allocated to your customer |
| Material Stock | /stockView/material-stocks | Add stocks allocated to your supplier |

## Postgres

The PURIS Backend uses a postgres Database. The helm installation already has a dependency that may be installed with
the chart. Optionally it may be disabled to use your own installation. Refer to the overall
[INSTALL.md](../../INSTALL.md) for further information.
