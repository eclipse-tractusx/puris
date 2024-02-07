# Deployment View

## Local Deployment

Overall the Deployment locally using the [INSTALL.md in local](../../local/INSTALL.md) looks similar to the following
graphic.

![Local Deployment of two PURIS clients with MVD](img/07-deployment.svg)

**Helm / Kubernetes**

One can configure the two local helm environments using the product helm chart and
the [mxd tutorial](https://github.com/eclipse-tractusx/tutorial-resources/tree/main/mxd).

## ArgoCD Deployment (e.g. INT)

The very basic deployment for one PURIS FOSS looks as follows:

![Argo CD Deployment of one PURIS client](img/07-deployment-argo.svg)

The keycloak may be configured to be used. Also a decentral instance may be connected to the frontend. Refer to the
[helm docs](../../charts/puris/README.md) for further information.

The chart allows also to either install the database as a dependency or bring your own.
