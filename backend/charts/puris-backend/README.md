# puris-backend

![Version: 0.0.1](https://img.shields.io/badge/Version-0.0.1-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.1-snapshot](https://img.shields.io/badge/AppVersion-0.0.1--snapshot-informational?style=flat-square)

A Helm chart for Kubernetes deployment of the PURIS Backend

**Homepage:** <https://github.com/eclipse-tractusx/puris-backend>

## Prerequisites
- Kubernetes 1.19+
- Helm 3.2.0+

## TL;DR
```shell
$ helm install puris-backend --namespace puris --create-namespace .
```

## Source Code

* <https://github.com/eclipse-tractusx/puris-backend>

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution | list | `[{"podAffinityTerm":{"labelSelector":{"matchExpressions":[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]},"topologyKey":"kubernetes.io/hostname"},"weight":100}]` | Rules for the scheduler to find a pod |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions | list | `[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]` | Matching Expressions as key and operators for the pod affinity |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.topologyKey | string | `"kubernetes.io/hostname"` | Topology key of the Kubernetes cluster |
| api.rootDir | string | `"/catena"` | The root directory of the API |
| autoscaling.enabled | bool | `false` | Enable or disable the autoscaling of pods |
| datasource.driverClassName | string | `"org.hsqldb.jdbc.JDBCDriver"` | Driver class name of the database |
| datasource.password | string | `""` | Password for the database user |
| datasource.url | string | `"jdbc:hsqldb:mem:testdb;DB_CLOSE_DELAY=-1"` | URL of the database |
| datasource.username | string | `"sa"` | Username of the database |
| edc.backend.url | string | `"http://172.17.0.2:32084"` | URL of the EDC backend service |
| edc.controlplane.data.port | int | `30091` | Data port of the EDC control plane |
| edc.controlplane.host | string | `"172.17.0.2"` | IP address of the EDC control plane |
| edc.controlplane.key | string | `"password"` | Key for the EDC control plane |
| fullnameOverride | string | `""` | Possibility to override the fullname |
| image.pullPolicy | string | `"IfNotPresent"` | THe policy for the image pull process |
| image.repository | string | `"ghcr.io/catenax-ng/tx-puris-backend/puris-backend"` | Repository of the docker image |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| imagePullSecrets | list | `[]` | List of used secrets |
| ingress.annotations."kubernetes.io/ingress.class" | string | `"nginx"` | Kubernetes Ingress class annotation for direct bindings |
| ingress.annotations."nginx.ingress.kubernetes.io/backend-protocol" | string | `"HTTP"` | The backend protocol type (e.g. HTTP) |
| ingress.annotations."nginx.ingress.kubernetes.io/force-ssl-redirect" | string | `"true"` | Force redirects from HTTP to HTTPS |
| ingress.annotations."nginx.ingress.kubernetes.io/ssl-passthrough" | string | `"true"` | Pass SSL traffic to the backend ports |
| ingress.enabled | bool | `true` | Enable the Ingress |
| ingress.hosts | list | `[{"host":"home.int.demo.catena-x.net","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}]` | Hosts for the Ingress controller |
| ingress.tls | list | `[{"hosts":["home.int.demo.catena-x.net"],"secretName":"tls-secret"}]` | TLS certificates for the Ingress controller |
| livenessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":250,"periodSeconds":25,"successThreshold":1,"timeoutSeconds":1}` | Checks whether a pod is alive or not |
| livenessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a liveness probe |
| livenessProbe.initialDelaySeconds | int | `250` | Delay in seconds after which an initial liveness probe is checked |
| livenessProbe.periodSeconds | int | `25` | Wait time in seconds between liveness probes |
| livenessProbe.successThreshold | int | `1` | Number of trys until a pod is marked alive |
| livenessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the liveness probe |
| nameOverride | string | `""` | Possibility to override the name |
| nodeSelector | object | `{}` | Constrains for the node selector |
| podAnnotations | object | `{}` | Annotations added to a running pod |
| podSecurityContext | object | `{}` | Added security contexts for a pod |
| readinessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":250,"periodSeconds":25,"successThreshold":1,"timeoutSeconds":1}` | Checks if the pod is fully ready to operate |
| readinessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a readiness probe |
| readinessProbe.initialDelaySeconds | int | `250` | Delay in seconds after which an initial readiness probe is checked |
| readinessProbe.periodSeconds | int | `25` | Wait time in seconds between readiness probes |
| readinessProbe.successThreshold | int | `1` | Number of trys until a pod is marked ready |
| readinessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the readiness probe |
| replicaCount | int | `1` | Number of replicas of the Kubernetes deployment |
| resources.limits | object | `{"cpu":"2000m","memory":"2048Mi"}` | Maximum resource limits of CPU und memory |
| resources.requests | object | `{"cpu":"1000m","memory":"2048Mi"}` | Minimum requested resources for CPU und memory |
| securityContext | object | `{"allowPrivilegeEscalation":false,"runAsGroup":3000,"runAsNonRoot":true,"runAsUser":1000}` | Security configurations |
| securityContext.allowPrivilegeEscalation | bool | `false` | Get more privileges than the parent process |
| securityContext.runAsGroup | int | `3000` | Configures the group id of a user for a run |
| securityContext.runAsNonRoot | bool | `true` | Configures the non-root privileges for a run |
| securityContext.runAsUser | int | `1000` | Configures the user id for a run |
| service.port | int | `8081` | The port of the service |
| service.type | string | `"NodePort"` | Type of the service |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| tolerations | list | `[]` | Constrains for tolerations |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
