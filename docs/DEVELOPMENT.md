## Setup development database

For local development a postgresql database is needed. The file local/docker-compose-dev-postgres.yaml provides a 
postgres that can be started freshly for development:

```shell
cd local
# create .env manually or run sh generate-keys.sh
docker compose -f docker-compose-dev-postgres.yaml up

# update your application.properties (src) accordingly to user and password
# work as you want. Shutdown database if needed:
docker compose -f docker-compose-dev-postgres.yaml down
```

_NOTE: For testing purposes HyperSql is still used but excluded for spring run._

## Keeping dependencies-files up to date
### Backend

Navigate to the `./backend` folder and run:  
```
mvn org.eclipse.dash:license-tool-plugin:license-check   
cp DEPENDENCIES ../DEPENDENCIES_BACKEND
```
The first line runs the maven license tool with the parameters specified in the 
`./backend/pom.xml` and produces a DEPENDENCIES file in the .`/backend` folder.  
Then this file gets copied to the PURIS-project root folder under the name `DEPENDENCIES_BACKEND`. 
Both files should be updated prior to any pull request.  

### Frontend
```
# move to a persistent folder. Could also be ~/jars.
mv org.eclipse.dash.licenses-1.0.2.jar ~/coding/org.eclipse.dash.licenses-1.0.2.jar
vim ~/.bashrc
# add following line using i
alias eclipseDashTool='java -jar ~/coding/org.eclipse.dash.licenses-1.0.2.jar'
# esc, qw -> enter to save and exit
source ~/.bashrc
# cd to puris-frontend
cd frontend
eclipseDashTool package-lock.json -project automotive.tractusx -summary ../DEPENDENCIES_FRONTEND
```

## Frontend container building workaround to use environment variables for vue

### The mechanism for docker is the following:
- `.env` has vite variables
- `.env.dockerbuild` has the vite variable that maps on an environment variable (`VITE_BACKEND_BASE_URL=$BACKEND_BASE_URL`)
- `src/config.json` has the environment variable names and the environment variable to substring in a json format.

### When building the container:
1. `.env.dockerbuild` is used
2. vite / vue builds the application into a dest folder, that will be served by nginx

> Result for the .env: <br> VITE_BACKEND_BASE_URL won't write a variable value BUT a placeholder into the built files ($BACKEND_BASE_URL)

### When building the container, there is a "start-nginx.sh" file that does the following
1. Collects the environment variables (set for the docker container / set via helm as `BACKEND_BASE_URL`)
2. Looks-up the "to replace string" from `config.json` (e.g., for `BACKEND_BASE_URL`, it will search for `$BACKEND_BASE-URL` in the built files)
3. Does the replacement in the built files
4. Starts nginx

# Testing Helm Charts with local images

When changing the helm charts due to changes of e.g. environment variables, one should test locally whether the changes
work.

First thing one should check is whether the templates may be resolved / substituted correctly and if your changes are 
defaulted correctly:
```shell
cd charts/puris
helm template .
>> no error is thrown, chart is resolved, changes are done correctly
```

Now build your images as explained in the respective install.mds.
- [backend](../backend/INSTALL.md)
- [frontend](../frontend/INSTALL.md)

Now you need to update your Chart.yml and values.yml:
- Chart.yml: change `appVersion` to your build tag (e.g., `dev`)
- values.yml: for both frontend and backend change the image
  - `repository` should be set to the image name used during docker build (e.g., `puris-backend`, `puris-frontend`)
  - `pullPolicy` should be set to `Never`

Now depending on your runtime environment you need to load the images into it (we assume, you built puris-backend:dev 
and (puris-frontend:dev):
```shell
# minikube 
minikube image load puris-backend:dev
minikube image load puris-frontend:dev
# validate that your image is listed and compare digest with local image
minikube image ls --format table | grep puris-backend
docker image ls | grep puris-backend
minikube image ls --format table | grep puris-frontend
docker image ls | grep puris-frontend
```
```shell
# kind
kind load puris-backend:dev
kind load puris-frontend:dev
# validate that your image is listed and compare digest with local image
docker ps 
>> locate the container-id of you kind cluster
docker exec -it {container-id} crictl images | grep puris-backend
docker image ls | grep puris-backend
docker exec -it {container-id} crictl images | grep puris-frontend
docker image ls | grep puris-frontend
```
**ATTENTION: MAKE SURE THAT THE IMAGE ID IN YOUR KUBERNETES ENVIRONMENT IS THE SAME AS IN YOUR LOCAL DOCKER.**

Else you can delete images as follows:

```shell
# minikube
minikube image delete puris-backend:dev
minikube image delete puris-frontend:dev
```

```shell
# kind
ocker ps 
>> locate the container-id of you kind cluster
docker exec -it {container-id} crictl rmi puris-backend:dev
docker exec -it {container-id} crictl rmi puris-frontend:dev
```
