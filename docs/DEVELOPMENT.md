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
ll

## Cleaning the vault

Sometimes it may be helpful to remove old transfers or edr tokens if you resetup your edc but not your vault.

As per version `0.9.0` (started with a `0.5.0` or `0.4.0` version), the edc creates in the root of the vault secrets:

- transfers in form of <uuid>
- edrs for uuids in form of edr--<uuid>

Download & install the vault CLI

> The vault deletion process is to
> - remove the versions. If you remove them, they remain on the system.
> - destroy the versions. Now they're gone, too but the path still remains.
> - remove metatdata from secret path. Now the whole secret has been removed.
    > Note: only do this in a controlled environment

```shell
export VAULT_ADDR=TODO
export VAULT_TOKEN=TODO

export KV_PATH=your-puris-kv-path

# select uuids and edr--uuid
secrets=$(vault kv list -format=json $KV_PATH | jq -r '.[] | select(test("^(edr--[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$"))')

# Count the number of UUID-like secrets
count=$(echo "$secrets" | wc -l)

# Print the number of secrets found
echo "Number of UUID-like secrets found: $count"

for secret in $secrets; do
    
    # Remove the 'data/' prefix if it exists
    secret_name=$(echo "$secret" | sed 's/^data\///')
    
    echo "Deleting secret: $secret"
    vault kv delete "$KV_PATH/$secret"
    echo "Destroy version 1 permanently: $secret"
    vault kv destroy -versions=1 "$KV_PATH/$secret"
    echo "Remove path by removing metadata: $secret"
    vault kv metadata delete "$KV_PATH/$secret"
done
```

## Keeping dependencies-files up to date

### Backend

Navigate to the `./backend` folder and run:

```
mvn org.eclipse.dash:license-tool-plugin:license-check
```

This line runs the maven license tool with the parameters specified in the
`./backend/pom.xml` and produces a `DEPENDENCIES_BACKEND` file in the root folder of this project.

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
eclipseDashTool package-lock.json -project automotive.tractusx -summary ../DEPENDENCIES_FRONTEND -batch 100
```

### mock-util-service

Temprorary also the mock-util-service needs to be kept up to date. First add the alias same as for frontend to your
`.bashrc` and then use the following command:

```shell
cd local/iam-mock
cat requirements.txt | grep -v \# \
| sed -E -e 's|([^= ]+)==([^= ]+)|pypi/pypi/-/\1/\2|' -e 's| ||g' \
| sort | uniq \
| eclipseDashTool -project automotive.tractusx -summary DEPENDENCIES -
```

Note: Dash action provided by eclipse-tractusx/sig-infra does not provide to opportunity for python.

## Frontend container building workaround to use environment variables for vue

### The mechanism for docker is the following:

- `.env` has vite variables
- `.env.dockerbuild` has the vite variable that maps on an environment
  variable (`VITE_BACKEND_BASE_URL=$BACKEND_BASE_URL`)
- `src/config.json` has the environment variable names and the environment variable to substring in a json format.

### When building the container:

1. `.env.dockerbuild` is used
2. vite / vue builds the application into a dest folder, that will be served by nginx

> Result for the .env: <br> VITE_BACKEND_BASE_URL won't write a variable value BUT a placeholder into the built files (
> $BACKEND_BASE_URL)

### When building the container, there is a "start-nginx.sh" file that does the following

1. Collects the environment variables (set for the docker container / set via helm as `BACKEND_BASE_URL`)
2. Looks-up the "to replace string" from `config.json` (e.g., for `BACKEND_BASE_URL`, it will search
   for `$BACKEND_BASE-URL` in the built files)
3. Does the replacement in the built files
4. Starts nginx

# Local installations with Helm

## Local Installation

Different to installations from the official repo (see e.g. [Helm README.md](../charts/puris/README.md)), you need to
first install dependencies.

```shell
cd ../charts/puris
helm repo add bitnami https://charts.bitnami.com/bitnami
helm dependency update
```

Then install the application to your needs:

### Run with Ingress (recommended)

Precondition: please refer to your runtime environment's official documentation on how to enable ingress.

- [minikube](https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/)
- [kind](https://kind.sigs.k8s.io/docs/user/ingress/)

Run the application

```shell
helm install puris . \
    --namespace puris \
    --create-namespace \
    --set frontend.ingress.enabled=true \
    --set backend.ingress.enabled=true
```

Edit /etc/hosts:

```shell
# If you are using minikube use minikube ip to get you clusterIp, for kind this is localhost (127.0.0.1)
sudo vim /etc/hosts
>> add entry for frontend "<cluster ip> <frontend-url.top-level-domain>"
>> add entry for backend "<cluster ip> <backend-url.top-level-domain>"
>> :wq! (save changes)
```

Done! The applications should be available at:

- (frontend) `http://your-frontend-host-address.com`
- (backend) `http://your-backend-host-address.com`

> **NOTE**
>
> Ingress must be enabled for your runtime once per cluster installation. /etc/hosts adoption once per system / url

### Run without Ingress

```shell
helm install puris . \
    --namespace puris \
    --create-namespace 
```

Forward ports for services:

```shell
kubectl -n puris port-forward svc/frontend 8080:8080
kubectl -n puris port-forward svc/backend 8081:8081
```

Done! The applications should be available at `http://localhost:<forwarded-port>`.

## Testing Helm Charts with Local Images

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

## Testing Workflows locally

[act](https://github.com/nektos/act) is a tool to run jobs within `./workflows` locally. Configuration of events can
be stored in `./act`

Install by downloading the released binaries and add them to your path.

```shell
cd <root dir of repo>

act --list
>> Stage  Job ID                       Job name                     Workflow name                                   Workflow file              Events                                      
>> 0      check-dependencies-backend   check-dependencies-backend   3rd Party Dependency Check (Eclipse Dash Tool)  dash-dependency-check.yml  workflow_dispatch,pull_request              
>> 0      check-dependencies-frontend  check-dependencies-frontend  3rd Party Dependency Check (Eclipse Dash Tool)  dash-dependency-check.yml  workflow_dispatch,pull_request              
>> 0      lint-test                    lint-test                    Lint and Test Charts                            helm-test.yml              pull_request,workflow_dispatch     

# run action with job-id lint-test for event as defined in pr-event.json
act --job lint-test -e .act/pr_event.json
```

## Writing and Testing Database Migrations

We use [liquibase](https://docs.liquibase.com/start/home.html)) for database migrations.

You can use the following commands to dump a changelog.

```shell
cd local 

# ensure there is a folder to mount
mkdir liquibase
chmod 777 liquibase
# needs to be removed manually via sudo as created with different user by liquibase
sudo rm liquibase/changelog.yaml

docker compose -f docker-compose-liquibase.yaml up liquibase-supplier
```

You can create a new migration by placing a yaml containing a `ChangeSet` into `../backend/src/resources/db/changelog/`.

If you have a new major release,

- add a folder `changelog-MAJOR.x`.
- include the folder in `../backend/src/main/resources/db/changelog/db.changelog-master.yaml`

Note: when using this approach, make sure to validate the `changeSets` and adding suitable `preConditions`.

# Notes on the release

## Run helm test locally for n kubernetes versions

Using act, you can run workflows locally. If you want to test how to use the workflow, update the file 
`.act/workflow_dispatch_helm_test.json` that contains the input parameters.

Check for supported kubernetes versions of kind per [release](https://github.com/kubernetes-sigs/kind/releases).

```shell
# root dir
act workflow_dispatch -j lint-test -e .act/workflow_dispatch_helm_test.json
```


## Check license files

For easier checks we created a small python script to check license files.

It searches for the common contributor (Contributors to the EF) and prints files, not containing that

```shell
cd scripts

python3 license-check.py
```

## Automation With Bruno

Bruno allows to specify tags in the meta information per request. In the [integration test suite](../local/bruno/puris-integration-test) uses the following tags:

| TAG                       | Usage                                                                                                                                                         |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| share-enablement-services | Use this tag in requests that create DTR entries and EDC Assets, Contracts and Transfers to simulate deployment scenarios of a shared environment.            |
| admin                     | Use this tag in requests you use for debugging. This way we can include common requests in the bruno collection without running those during test automation. |

All other requests are considered as common test to run during test automation (see [integration test documentation](../local/postman/README.md)).

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
