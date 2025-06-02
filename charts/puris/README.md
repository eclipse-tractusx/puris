# puris

![Version: 3.0.0](https://img.shields.io/badge/Version-3.0.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 3.1.0](https://img.shields.io/badge/AppVersion-3.1.0-informational?style=flat-square)

A helm chart for Kubernetes deployment of PURIS

**Homepage:** <https://github.com/eclipse-tractusx/puris>

## Prerequisites
- Kubernetes 1.19+
- Helm 3.2.0+

## Install

To install the chart with the release name `puris`:

```shell
$ helm repo add tractusx-dev https://eclipse-tractusx.github.io/charts/dev
$ helm install puris tractusx-dev/puris
```
To install the helm chart into your cluster with your values:

```shell
$ helm install -f your-values.yaml puris tractusx-dev/puris
```

To use the helm chart as a dependency:

```yaml
dependencies:
  - name: puris
    repository: https://eclipse-tractusx.github.io/charts/dev
    version: YOUR_VERSION
```

## Source Code

* <https://github.com/eclipse-tractusx/puris>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql | 12.12.x |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution | list | `[{"podAffinityTerm":{"labelSelector":{"matchExpressions":[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]},"topologyKey":"kubernetes.io/hostname"},"weight":100}]` | Rules for the scheduler to find a pod |
| backend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions | list | `[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]` | Matching Expressions as key and operators for the pod affinity |
| backend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.topologyKey | string | `"kubernetes.io/hostname"` | Topology key of the Kubernetes cluster |
| backend.autoscaling.enabled | bool | `false` | Enable or disable the autoscaling of pods |
| backend.env | object | `{}` | Extra environment variables that will be passed onto the backend deployment pods |
| backend.image.pullPolicy | string | `"IfNotPresent"` | THe policy for the image pull process |
| backend.image.repository | string | `"tractusx/app-puris-backend"` | Repository of the docker image |
| backend.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| backend.imagePullSecrets | list | `[]` | List of used secrets |
| backend.ingress.annotations | object | `{"kubernetes.io/ingress.class":"nginx","nginx.ingress.kubernetes.io/backend-protocol":"HTTP","nginx.ingress.kubernetes.io/force-ssl-redirect":"true","nginx.ingress.kubernetes.io/ssl-passthrough":"true"}` | Annotations for the Ingress controller |
| backend.ingress.annotations."kubernetes.io/ingress.class" | string | `"nginx"` | Kubernetes Ingress class annotation for direct bindings |
| backend.ingress.annotations."nginx.ingress.kubernetes.io/backend-protocol" | string | `"HTTP"` | The backend protocol type (e.g. HTTP) |
| backend.ingress.annotations."nginx.ingress.kubernetes.io/force-ssl-redirect" | string | `"true"` | Force redirects from HTTP to HTTPS |
| backend.ingress.annotations."nginx.ingress.kubernetes.io/ssl-passthrough" | string | `"true"` | Pass SSL traffic to the backend ports |
| backend.ingress.enabled | bool | `false` | Enable the Ingress |
| backend.ingress.hosts | list | `[{"host":"your-backend-host-address.com","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}]` | Hosts for the Ingress controller |
| backend.ingress.tls | list | `[]` | TLS certificates for the Ingress controller |
| backend.livenessProbe | object | `{"failureThreshold":1,"initialDelaySeconds":0,"periodSeconds":5,"successThreshold":1,"timeoutSeconds":1}` | Checks whether a pod is alive or not |
| backend.livenessProbe.failureThreshold | int | `1` | Number of failures (threshold) for a liveness probe |
| backend.livenessProbe.initialDelaySeconds | int | `0` | Delay in seconds after which an initial liveness probe is checked |
| backend.livenessProbe.periodSeconds | int | `5` | Wait time in seconds between liveness probes |
| backend.livenessProbe.successThreshold | int | `1` | Number of trys until a pod is marked alive |
| backend.livenessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the liveness probe |
| backend.nameOverride | string | `""` | Possibility to override the name |
| backend.nodeSelector | object | `{}` | Constrains for the node selector |
| backend.podAnnotations | object | `{}` | Annotations added to a running pod |
| backend.podSecurityContext | object | `{}` | Added security contexts for a pod |
| backend.puris.allowedOrigins | list | `["your-frontend-host-address.com"]` | Allowed origins for the frontend. Must contain protocol (http/https). If protocol is missing, it's defaulted based on ingress configuration. |
| backend.puris.api.key | string | `"test"` | The API key of the PURIS application |
| backend.puris.api.rootDir | string | `"/catena"` | The root directory of the API |
| backend.puris.baseurl | string | `"your-backend-host-address.com"` | Base url of the PURIS backend. Must contain protocol (http/https). If protocol is missing, it's defaulted based on ingress configuration. |
| backend.puris.datasource.driverClassName | string | `"org.postgresql.Driver"` | Driver class name of the database |
| backend.puris.datasource.password | string | `""` | Password for the database user. Ignored if postgres.enabled is true. |
| backend.puris.datasource.url | string | `"jdbc:postgresql://postgresql-name:5432/puris-database"` | URL of the database. Ignored if postgres.enabled is true. |
| backend.puris.datasource.username | string | `"db-user"` | Username of the database. Ignored if postgres.enabled is true. |
| backend.puris.daysofsupplysubmodel.apiassetid | string | `"daysofsupply-api-asset"` | Asset ID for DaysOfSupplySubmodel API |
| backend.puris.deliverysubmodel.apiassetid | string | `"deliverysubmodel-api-asset"` | Asset ID for DeliverySubmodel API |
| backend.puris.demandsubmodel.apiassetid | string | `"demandsubmodel-api-asset"` | Asset ID for DemandSubmodel API |
| backend.puris.demonstrator.role | string | `nil` | Current role of the PURIS demonstrator. Default value should be empty. Can be set to "customer" or "supplier" to enable demonstration setup |
| backend.puris.dtr.idp.clients.edc.id | string | `"FOSS-EDC-CLIENT"` | id of the client that has a service account with roles to view the DTR. Used by the application to create DTR asset in the edc with read only access. See Admin Guide. Mandatory if backend.puris.dtr.idp.enabled = true. |
| backend.puris.dtr.idp.clients.edc.secret.alias | string | `"path/secret-name"` | alias for the vault used by the EDC in which the secret is stored. Mandatory if backend.puris.dtr.idp.enabled = true. |
| backend.puris.dtr.idp.clients.puris.id | string | `"FOSS-PURIS-CLIENT"` | id of the client that has a service account with roles to manage the DTR. Used by the application to create and update digital twins. See Admin Guide. Mandatory if backend.puris.dtr.idp.enabled = true. |
| backend.puris.dtr.idp.clients.puris.secret | string | `""` | secret of the client with write access (no vault alias). No default value will be created if empty. Mandatory if backend.puris.dtr.idp.enabled = true. |
| backend.puris.dtr.idp.enabled | bool | `true` | enables the usage of the IDP for the DTR. |
| backend.puris.dtr.idp.tokenurl | string | `"https://keycloak-service.com/realms/your-realm/openid-connect/token"` | token url of the idp for your specific realm. May be different to other idp token url in this config. Must contain protocol (http/https). Mandatory if backend.puris.dtr.idp.enabled = true. |
| backend.puris.dtr.url | string | `"https://localhost:4243/api/v3"` | Endpoint for DTR including api/v3 prefix. Must contain protocol (http/https). |
| backend.puris.edc.controlplane.host | string | `"172.17.0.2"` |  |
| backend.puris.edc.controlplane.key | string | `"password"` | Key for the EDC control plane |
| backend.puris.edc.controlplane.management.url | string | `"https://your-edc-address:8181/management"` | Url to the EDC controlplane management of the edc. Must contain protocol (http/https). |
| backend.puris.edc.controlplane.protocol.url | string | `"https://your-edc-address:8184/api/v1/dsp"` | Url to the EDC controlplane protocol API of the edc. Must contain protocol (http/https). |
| backend.puris.edc.dataplane.public.url | string | `"https://your-data-plane:8285/api/public/"` | Url of one of your data plane's public api. Must contain protocol (http/https). |
| backend.puris.erpadapter.authkey | string | `"x-api-key"` | The auth key to be used on your ERP adapter's request api |
| backend.puris.erpadapter.authsecret | string | `""` | The auth secret to be used on your ERP adapter's request api. Reused from existing secret. Secret key "puris-erpadapter-authsecret". |
| backend.puris.erpadapter.enabled | bool | `false` | Toggles usage of the ERP adapter |
| backend.puris.erpadapter.refreshinterval | int | `720` | Interval between two requests to the erp adapter for the same issue (minutes) |
| backend.puris.erpadapter.timelimit | int | `7` | Period since last received partner request after which no more new update requests to the erp adapter will be sent (days) |
| backend.puris.erpadapter.url | string | `"https://my-erpadapter:8080"` | The url of your ERP adapter's request api. Must contain protocol (http/https). |
| backend.puris.existingSecret | string | `"secret-puris-backend"` | Secret for backend passwords. For more information look into 'backend-secrets.yaml' file. |
| backend.puris.frameworkagreement.credential | string | `"DataExchangeGovernance"` | The name of the framework agreement. Starting with Uppercase and using CamelCase. |
| backend.puris.frameworkagreement.version | string | `"1.0"` | The version of the framework agreement, NEEDS TO BE PUT AS "STRING"! |
| backend.puris.generatematerialcatenaxid | bool | `true` | Flag that decides whether the auto-generation feature of the puris backend is enabled. Since all Material entities are required to have a CatenaX-Id, you must enter any pre-existing CatenaX-Id via the materials-API of the backend, when you are inserting a new Material entity to the backend's database. If a CatenaX-Id was not assigned to your Material so far, then this feature can auto-generate one randomly. In a real-world-scenario, you must then use this randomly generated CatenaX-Id for the lifetime of that Material entity. |
| backend.puris.itemstocksubmodel.apiassetid | string | `"itemstocksubmodel-api-asset"` | Asset ID for ItemStockSubmodel API |
| backend.puris.jpa.hibernate.ddl-auto | string | `"update"` | Initialises SQL database with Hibernate property "update" to allow Hibernate to add things to schema so that it doesn't drop tables |
| backend.puris.jpa.properties.hibernate.enable_lazy_load_no_trans | bool | `true` | Enables "Lazy load no trans" property to fetch of each lazy entity to open a temporary session and run inside a separate transaction |
| backend.puris.notification.apiassetid | string | `"notification-api-asset"` | Asset ID for Notification API |
| backend.puris.own.bpna | string | `"BPNA4444444444ZZ"` | Own BPNA of the EDC |
| backend.puris.own.bpnl | string | `"BPNL4444444444XX"` | Own BPNL of the EDC |
| backend.puris.own.bpns | string | `"BPNS4444444444XX"` | Own BPNS of the EDC |
| backend.puris.own.country | string | `"Germany"` | Own country |
| backend.puris.own.name | string | `"YOUR-COMPANY-NAME"` | Own name (self-description) |
| backend.puris.own.site.name | string | `"YOUR-SITE-NAME"` | Own site name |
| backend.puris.own.streetnumber | string | `"Musterstraße 110A"` | Own street and number |
| backend.puris.own.zipcodeandcity | string | `"12345 Musterhausen"` | Own zipcode and city |
| backend.puris.productionsubmodel.apiassetid | string | `"productionsubmodel-api-asset"` | Asset ID for ProductionSubmodel API |
| backend.puris.purpose.name | string | `"cx.puris.base"` | The name of the purpose to use for submodel contracts |
| backend.puris.purpose.version | string | `"1"` | The version of the purpose to use for submodel contracts. NEEDS TO BE PUT AS "STRING"! |
| backend.readinessProbe | object | `{"failureThreshold":1,"initialDelaySeconds":0,"periodSeconds":5,"successThreshold":1,"timeoutSeconds":1}` | Checks if the pod is fully ready to operate |
| backend.readinessProbe.failureThreshold | int | `1` | Number of failures (threshold) for a readiness probe |
| backend.readinessProbe.initialDelaySeconds | int | `0` | Delay in seconds after which an initial readiness probe is checked |
| backend.readinessProbe.periodSeconds | int | `5` | Wait time in seconds between readiness probes |
| backend.readinessProbe.successThreshold | int | `1` | Number of trys until a pod is marked ready |
| backend.readinessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the readiness probe |
| backend.replicaCount | int | `1` | Number of replicas of the Kubernetes deployment |
| backend.resources.limits | object | `{"cpu":"3000m","memory":"2048Mi"}` | Maximum resource limits of CPU und memory |
| backend.resources.requests | object | `{"cpu":"1000m","memory":"2048Mi"}` | Minimum requested resources for CPU und memory |
| backend.securityContext | object | `{"allowPrivilegeEscalation":false,"runAsGroup":3000,"runAsNonRoot":true,"runAsUser":8877}` | Security configurations |
| backend.securityContext.allowPrivilegeEscalation | bool | `false` | Get more privileges than the parent process |
| backend.securityContext.runAsGroup | int | `3000` | Configures the group id of a user for a run |
| backend.securityContext.runAsNonRoot | bool | `true` | Configures the non-root privileges for a run |
| backend.securityContext.runAsUser | int | `8877` | Configures the user id for a run |
| backend.service.port | int | `8081` | The port of the service |
| backend.service.type | string | `"ClusterIP"` | Type of the service |
| backend.serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| backend.serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| backend.serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| backend.startupProbe.enabled | bool | `true` |  |
| backend.startupProbe.failureThreshold | int | `5` | Number of failures (threshold) for a readiness probe |
| backend.startupProbe.initialDelaySeconds | int | `120` | Delay in seconds after which an initial readiness probe is checked |
| backend.startupProbe.periodSeconds | int | `30` | Wait time in seconds between readiness probes |
| backend.startupProbe.successThreshold | int | `1` | Number of trys until a pod is marked ready |
| backend.tolerations | list | `[]` | Constrains for tolerations |
| frontend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution | list | `[{"podAffinityTerm":{"labelSelector":{"matchExpressions":[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]},"topologyKey":"kubernetes.io/hostname"},"weight":100}]` | Rules for the scheduler to find a pod |
| frontend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions | list | `[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]` | Matching Expressions as key and operators for the pod affinity |
| frontend.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.topologyKey | string | `"kubernetes.io/hostname"` | Topology key of the Kubernetes cluster |
| frontend.autoscaling.enabled | bool | `false` | Enable or disable the autoscaling of pods |
| frontend.autoscaling.maxReplicas | int | `100` | Number of maximum replica pods for autoscaling |
| frontend.autoscaling.minReplicas | int | `1` | Number of minimum replica pods for autoscaling |
| frontend.autoscaling.targetCPUUtilizationPercentage | int | `80` | Value of CPU usage in percentage for autoscaling decisions |
| frontend.env | object | `{}` | Extra environment variables that will be passed onto the frontend deployment pods |
| frontend.image.pullPolicy | string | `"IfNotPresent"` | THe policy for the image pull process |
| frontend.image.repository | string | `"tractusx/app-puris-frontend"` | Repository of the docker image |
| frontend.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| frontend.imagePullSecrets | list | `[]` | List of used secrets |
| frontend.ingress.annotations | object | `{}` | Annotations for the Ingress controller |
| frontend.ingress.className | string | `"nginx"` | Class name for the Ingress controller |
| frontend.ingress.enabled | bool | `false` | Enable the Ingress |
| frontend.ingress.hosts | list | `[{"host":"your-frontend-host-address.com","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}]` | Hosts for the Ingress controller |
| frontend.ingress.tls | list | `[]` | TLS certificates for the Ingress controller |
| frontend.livenessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":10,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":1}` | Checks whether a pod is alive or not |
| frontend.livenessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a liveness probe |
| frontend.livenessProbe.initialDelaySeconds | int | `10` | Delay in seconds after which an initial liveness probe is checked |
| frontend.livenessProbe.periodSeconds | int | `10` | Wait time in seconds between liveness probes |
| frontend.livenessProbe.successThreshold | int | `1` | Number of trys until a pod is marked alive |
| frontend.livenessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the liveness probe |
| frontend.nameOverride | string | `""` | Possibility to override the name |
| frontend.nodeSelector | object | `{}` | Constrains for the node selector |
| frontend.podAnnotations | object | `{}` | Annotations added to a running pod |
| frontend.podSecurityContext | object | `{}` | Added security contexts for a pod |
| frontend.puris.appName | string | `"PURIS"` | The name of the app displayed in the frontend |
| frontend.puris.baseUrl | string | `"your-backend-host-address.com"` | The base URL for the backend base URL without further endpoints. Must contain protocol (http/https). If protocol is missing, it's defaulted based on ingress configuration. |
| frontend.puris.endpointCustomer | string | `"stockView/customer?ownMaterialNumber="` | The endpoint for the customers who buy a material identified via the own material number for the stock view |
| frontend.puris.endpointDaysOfSupply | string | `"days-of-supply"` | The endpoint for the days of supply submodel |
| frontend.puris.endpointDelivery | string | `"delivery"` | The endpoint for the delivery submodel |
| frontend.puris.endpointDemand | string | `"demand"` | The endpoint for the demand submodel |
| frontend.puris.endpointDemandAndCapacityNotification | string | `"demand-and-capacity-notification"` | The endpoint for demand and capacity notifications |
| frontend.puris.endpointErpScheduleUpdate | string | `"erp-adapter/trigger"` | The endpoint for scheduling an update of erp data (currently only stock supported) |
| frontend.puris.endpointMaterialStocks | string | `"stockView/material-stocks"` | The endpoint for material stocks for the stock view |
| frontend.puris.endpointMaterials | string | `"stockView/materials"` | The endpoint for materials for the stock view |
| frontend.puris.endpointPartners | string | `"partners"` | The endpoint for partner information |
| frontend.puris.endpointProductStocks | string | `"stockView/product-stocks"` | The endpoint for product stocks for the stock view |
| frontend.puris.endpointProduction | string | `"production"` | The endpoint for the production submodel |
| frontend.puris.endpointProductionRange | string | `"production/range"` | The endpoint for the production range of the production submodel |
| frontend.puris.endpointProducts | string | `"stockView/products"` | The endpoint for products for the stock view |
| frontend.puris.endpointReportedMaterialStocks | string | `"stockView/reported-material-stocks?ownMaterialNumber="` | The endpoint for the partners' (supplier) material stocks that they potentially will deliver to me |
| frontend.puris.endpointReportedProductStocks | string | `"stockView/reported-product-stocks?ownMaterialNumber="` | The endpoint for the partners' (customer) product stocks that they received from me |
| frontend.puris.endpointSupplier | string | `"stockView/supplier?ownMaterialNumber="` | The endpoint for the suppliers who buy a material identified via the own material number for the stock view |
| frontend.puris.endpointUpdateReportedMaterialStocks | string | `"stockView/update-reported-material-stocks?ownMaterialNumber="` | The endpoint for triggering an update of your material stocks on your partners side |
| frontend.puris.endpointUpdateReportedProductStocks | string | `"stockView/update-reported-product-stocks?ownMaterialNumber="` | The endpoint for triggering an update of your product stocks on your partners side |
| frontend.puris.keycloak.clientId | string | `"appXYZ"` | Name of the client which is used for the application. |
| frontend.puris.keycloak.disabled | bool | `true` | Disable the Keycloak integration. |
| frontend.puris.keycloak.realm | string | `"Catena-X"` | Name of the Realm of the keycloak instance. |
| frontend.puris.keycloak.redirectUrlFrontend | string | `"your-frontend-host-address.com"` | URL to use as keycloak redirect url. |
| frontend.puris.keycloak.url | string | `"https://idp.com/auth"` | The URL to the IDP that should be used. Must contain protocol (http/https). |
| frontend.puris.rateLimiting.burst | int | `30` | Burst rate limiting for nginx. |
| frontend.puris.rateLimiting.limit | string | `"10m"` | Bucket zone limit for rate limiting in nginx. |
| frontend.puris.rateLimiting.rate | string | `"10r/s"` | Allowed rates per second for nginx rate limiting. |
| frontend.readinessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":10,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":1}` | Checks if the pod is fully ready to operate |
| frontend.readinessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a readiness probe |
| frontend.readinessProbe.initialDelaySeconds | int | `10` | Delay in seconds after which an initial readiness probe is checked |
| frontend.readinessProbe.periodSeconds | int | `10` | Wait time in seconds between readiness probes |
| frontend.readinessProbe.successThreshold | int | `1` | Number of trys until a pod is marked ready |
| frontend.readinessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the readiness probe |
| frontend.replicaCount | int | `1` |  |
| frontend.resources.limits | object | `{"cpu":"600m","memory":"128Mi"}` | Maximum resource limits of CPU und memory |
| frontend.resources.requests | object | `{"cpu":"200m","memory":"128Mi"}` | Minimum requested resources for CPU und memory |
| frontend.securityContext | object | `{"allowPrivilegeEscalation":false,"runAsGroup":3000,"runAsNonRoot":true,"runAsUser":101}` | Security configurations |
| frontend.securityContext.allowPrivilegeEscalation | bool | `false` | Get more privileges than the parent process |
| frontend.securityContext.runAsGroup | int | `3000` | Configures the group id of a user for a run |
| frontend.securityContext.runAsNonRoot | bool | `true` | Configures the non-root privileges for a run |
| frontend.securityContext.runAsUser | int | `101` | Configures the user id for a run |
| frontend.service.port | int | `8080` | The port of the service |
| frontend.service.type | string | `"ClusterIP"` | Type of the service |
| frontend.serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| frontend.serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| frontend.serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| frontend.tolerations | list | `[]` | Constrains for tolerations |
| global.domain.backend.ingress | string | `"your-backend-host-address.com"` |  |
| global.domain.frontend.ingress | string | `"your-frontend-host-address.com"` |  |
| postgresql.auth.database | string | `"postgres"` | Name of the database. |
| postgresql.auth.existingSecret | string | `"secret-puris-postgres-init"` | Secret containing the password. For more information look into 'backend-secrets-postgres.yaml' file. |
| postgresql.auth.password | string | `""` | Password for the custom database user. Secret-key 'password' |
| postgresql.auth.passwordPostgres | string | `""` | Password for the database. Secret-key 'postgres-password'. |
| postgresql.auth.username | string | `"puris"` | Username for the custom database user. |
| postgresql.enabled | bool | `true` | Enable postgres by default, set to false to use existing postgres. Make sure to set backend.puris.jpa.hibernate.ddl-auto accordingly (by default database is created using hibernate ddl from backend). |
| postgresql.service | object | `{"ports":{"postgresql":5432}}` | Possibility to override the name  nameOverride: "" |
| postgresql.service.ports.postgresql | int | `5432` | Port of postgres database. |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.3](https://github.com/norwoodj/helm-docs/releases/v1.11.3)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
