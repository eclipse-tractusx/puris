## Project Installation
### Running for development using mvn/npm or using docker
See the `INSTALL.md` files in the [backend](./backend/INSTALL.md) and [frontend](./frontend/INSTALL.md) folder.
### Running local integration testing user docker compose
See `Install.md` file in folder [local](./local/docker-compose.yaml) for integration testing environment with two deployed applications and EDCs.
### Running using helm (deployment)
1. Configure the application:
    1. Open the `values.yaml` file in [charts/puris](./charts/puris/values.yaml).
    2. Edit the following properties to your requirements:
        - **Ingress**(if you want to enable ingress) for frontend/backend, under *frontend.ingress.* and *backend.ingress.*
        - **EDC**, under *backend.puris.edc*
        - **Own data**, under *backend.puris.own*
        - **Current role for demonstrator**, under *backend.puris.demonstrator.role*
    > **NOTE**   
    Further information on the individual properties can be found in the following [README.md](./charts/puris/README.md).
2. Run the application
```shell
helm install puris charts/puris \
    --namespace puris \
    --create-namespace 
```
2. Done! The applications should be available at
    - (frontend) `http://YOURIP:30000`
    - (backend) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
