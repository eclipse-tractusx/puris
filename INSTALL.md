## Project Installation
### Running using mvn (local develpment) or Running using docker (deployment)
See the `INSTALL.md` files in the [backend](./backend/INSTALL.md) and [frontend](./frontend/INSTALL.md) folder.
### Running using helm (deployment)
1. Run the application:
```shell
cd charts/puris/

helm install backend --namespace puris --create-namespace . --set frontend.ingress.enabled=true --set backend.ingress.enabled=true
```
2. Done! The applications should be available at
    - (frontend) `http://YOURIP:30000`
    - (backend) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`

