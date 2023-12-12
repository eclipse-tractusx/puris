# frontend

![Version: 1.0.0](https://img.shields.io/badge/Version-1.0.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: main](https://img.shields.io/badge/AppVersion-main-informational?style=flat-square)

A Helm chart for Kubernetes deployment of the PURIS Frontend

**Homepage:** <https://github.com/eclipse-tractusx/puris>

## Prerequisites
- Kubernetes 1.19+
- Helm 3.2.0+
- Running [PURIS backend](https://github.com/eclipse-tractusx/puris)

## TL;DR
```shell
$ helm install frontend --namespace puris --create-namespace .
```

## Source Code

* <https://github.com/eclipse-tractusx/puris>

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution | list | `[{"podAffinityTerm":{"labelSelector":{"matchExpressions":[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]},"topologyKey":"kubernetes.io/hostname"},"weight":100}]` | Rules for the scheduler to find a pod |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions | list | `[{"key":"app.kubernetes.io/name","operator":"DoesNotExist"}]` | Matching Expressions as key and operators for the pod affinity |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.topologyKey | string | `"kubernetes.io/hostname"` | Topology key of the Kubernetes cluster |
| autoscaling.enabled | bool | `false` | Enable or disable the autoscaling of pods |
| autoscaling.maxReplicas | int | `100` | Number of maximum replica pods for autoscaling |
| autoscaling.minReplicas | int | `1` | Number of minimum replica pods for autoscaling |
| autoscaling.targetCPUUtilizationPercentage | int | `80` | Value of CPU usage in percentage for autoscaling decisions |
| fullnameOverride | string | `""` | Possibility to override the fullname |
| image.pullPolicy | string | `"Always"` | THe policy for the image pull process |
| image.repository | string | `"tractusx/app-puris-frontend"` | Repository of the docker image |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| imagePullSecrets | list | `[]` | List of used secrets |
| ingress.annotations | object | `{}` | Annotations for the Ingress controller |
| ingress.className | string | `"nginx"` | Class name for the Ingress controller |
| ingress.enabled | bool | `false` | Enable the Ingress |
| ingress.hosts | list | `[{"host":"puris-customer-frontend.int.demo.catena-x.net","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}]` | Hosts for the Ingress controller |
| ingress.tls | list | `[]` | TLS certificates for the Ingress controller |
| livenessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":10,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":1}` | Checks whether a pod is alive or not |
| livenessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a liveness probe |
| livenessProbe.initialDelaySeconds | int | `10` | Delay in seconds after which an initial liveness probe is checked |
| livenessProbe.periodSeconds | int | `10` | Wait time in seconds between liveness probes |
| livenessProbe.successThreshold | int | `1` | Number of trys until a pod is marked alive |
| livenessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the liveness probe |
| nameOverride | string | `""` | Possibility to override the name |
| nodeSelector | object | `{}` | Constrains for the node selector |
| podAnnotations | object | `{}` | Annotations added to a running pod |
| podSecurityContext | object | `{}` | Added security contexts for a pod |
| puris.appName | string | `"PURIS"` | The name of the app displayed in the frontend |
| puris.baseUrl | string | `"http://172.17.0.2:30001/catena/"` | The base URL for the backend base URL without further endpoints |
| puris.endpointCustomer | string | `"stockView/customer?ownMaterialNumber="` | The endpoint for the customers own material number for the stock view |
| puris.endpointMaterialStocks | string | `"stockView/material-stocks"` | The endpoint for material stocks for the stock view |
| puris.endpointMaterials | string | `"stockView/materials"` | The endpoint for materials for the stock view |
| puris.endpointPartnerProductStocks | string | `"stockView/partner-product-stocks?ownMaterialNumber="` | The endpoint for the partners product stocks and their material numbers for the stock view |
| puris.endpointProductStocks | string | `"stockView/product-stocks"` | The endpoint for product stocks for the stock view |
| puris.endpointProducts | string | `"stockView/products"` | The endpoint for products for the stock view |
| puris.endpointUpdatePartnerProductStocks | string | `"stockView/update-partner-product-stock?ownMaterialNumber="` | The endpoint for updating the partners product stocks and their material numbers for the stock view |
| readinessProbe | object | `{"failureThreshold":3,"initialDelaySeconds":10,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":1}` | Checks if the pod is fully ready to operate |
| readinessProbe.failureThreshold | int | `3` | Number of failures (threshold) for a readiness probe |
| readinessProbe.initialDelaySeconds | int | `10` | Delay in seconds after which an initial readiness probe is checked |
| readinessProbe.periodSeconds | int | `10` | Wait time in seconds between readiness probes |
| readinessProbe.successThreshold | int | `1` | Number of trys until a pod is marked ready |
| readinessProbe.timeoutSeconds | int | `1` | Timeout in seconds of the readiness probe |
| replicaCount | int | `1` | Number of replicas of the Kubernetes deployment |
| resources.limits | object | `{"cpu":"500m","memory":"128Mi"}` | Maximum resource limits of CPU und memory |
| resources.requests | object | `{"cpu":"100m","memory":"128Mi"}` | Minimum requested resources for CPU und memory |
| securityContext | object | `{"allowPrivilegeEscalation":false,"runAsGroup":3000,"runAsNonRoot":true,"runAsUser":1000}` | Security configurations |
| securityContext.allowPrivilegeEscalation | bool | `false` | Get more privileges than the parent process |
| securityContext.runAsGroup | int | `3000` | Configures the group id of a user for a run |
| securityContext.runAsNonRoot | bool | `true` | Configures the non-root privileges for a run |
| securityContext.runAsUser | int | `1000` | Configures the user id for a run |
| service.port | int | `8080` | The port of the service |
| service.type | string | `"NodePort"` | Type of the service |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| tolerations | list | `[]` | Constrains for tolerations |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.2](https://github.com/norwoodj/helm-docs/releases/v1.11.2)
