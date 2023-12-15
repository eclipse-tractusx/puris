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

#### Run without Ingress 

2. Run the application:
```shell
helm install puris charts/puris \
    --namespace puris \
    --create-namespace 
```
3. Forward ports for services:
```shell
kubectl -n puris port-forward svc/frontend 8080:8080
kubectl -n puris port-forward svc/backend 8081:8081
```
4. Done! The applications should be available at `http://localhost:<forwarded-port>`.

#### Run with Ingress

Precondition: please refer to your runtime environment's official documentation on how to enable ingress.
- [minikube](https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/)
- [kind](https://kind.sigs.k8s.io/docs/user/ingress/)

2. Run the application:
```shell
helm install puris charts/puris \
    --namespace puris \
    --create-namespace \
    --set frontend.ingress.enabled=true \
    --set backend.ingress.enabled=true
```
3. Edit /etc/hosts:
```shell
# If you are using minikube use minikube ip to get you clusterIp, for kind this is localhost (127.0.0.1)
sudo vim /etc/hosts
>> add entry for frontend "<cluster ip> <frontend-url.top-level-domain>"
>> add entry for backend "<cluster ip> <backend-url.top-level-domain>"
>> :wq! (save changes)
```
4. Done! The applications should be available at:
    - (frontend) `http://your-frontend-host-address.com`
    - (backend) `http://your-backend-host-address.com`
