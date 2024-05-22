# Running the Keyckloak with a Sample Realm (Frontend)

Runs a Keycloak with an `admin` user overall and a user `puris_user` with the role `PURIS_USER` and a user `puris_admin`
with the role `PURIS_ADMIN` in the client `Cl3-PURIS`. Username = Password.

```sh 
cd local/keycloak
docker run -p 10081:8080 --name keycloak \
-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
-v ./puris-config/:/opt/keycloak/data/import \
quay.io/keycloak/keycloak:23.0.1 \
start-dev --import-realm
```

## Updating the data

If you would like to update the keycloak data, you can do as follows:

```shell
# Create temporary keycloak data docker volume
docker volume create kc-temp-data

# Start keycloak and import existing realm file from puris-config:
docker run -p 8081:8080 --name kc-temp \
-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
-v ./puris-config/:/opt/keycloak/data/import \
-v kc-temp-data:/opt/keycloak/data \
quay.io/keycloak/keycloak:23.0.1 \
start-dev 

# Open http://localhost:8181 and modify the realms

# Stop and remove container
docker stop kc-temp && docker rm kc-temp

# Create local export directory and change owner to fix 'permission denied' issue
mkdir kc-temp-export
sudo chown 1000:1000 kc-temp-export

# Run export
docker run -p 8081:8080 --name kc-temp \
-v kc-temp-data:/opt/keycloak/data \
-v ./kc-temp-export:/opt/keycloak/data/export \
quay.io/keycloak/keycloak:23.0.0 \
export --dir /opt/keycloak/data/export

# Cleanup
docker stop kc-temp && docker rm kc-temp
docker volume rm kc-temp-data

# The exported realm files will be in the directory `./kc-temp-export`.
# move them to puris-config
# make sure to remove the array "org.keycloak.keys.KeyProvider" (contains unneeded credentials)
# from realm file
```

## Updating Data via compose (DTR)

When running keycloak in the compose one may perform changes via the admin console. If these changes need to be applied
to the local setup in future, an export is needed. This export can be done as follows.

1. Do the export in the docker container

```shell
# create interactive shell in docker container of keycloak with changes
docker exec -it keycloak /bin/sh

# change directory and create export folder
cd /opt/keycloak
mkdir exports

# perform export (note: some things can't be exported when stil running - for us should be ok)
bin/kc.sh export --dir exports
```

2. Get the export to the import files

```shell
# assume you opened the shell in local/keycloak/supplier
docker cp keycloak:/opt/keycloak/exports import
```

The import should already consider all files in the directory on container creation of the keycloak

## Verify your DTR configuration

The DTR supports the client_credentials grant_type. Thus, the following configuration needs to be done:

- configure a client for the application in question (e.g. for the EDC)
- in the client enable authentication and service account roles
- save
- in the client, add roles following the dtr documentation
- in the client > service account tab -> add roles accordingly

The PURIS applications needs two clients:

- one for the EDC having read access for twins (used when discovering and reading twins as partner)
- one for the DTR having administrative privileges (used to create and update shell-descriptors)

To verify your role and client configuration is working, you can do as follows using the local deployment:

```shell
# open shell to any container e.g. dtr
docker exec -it keycloak /bin/sh

CLIENT_ID=YOUR CLIENT ID
CLIENT_SECRET=YOUR CLIENT SECRET
REALM=YOUR REALM
DTR_ADDRESS=YOUR DTR address like http://dtr-supplier:4243/api/v3/shell-descriptors

# Get token and save to BEARER_TOKEN
RESPONSE=$(wget -qO- --post-data "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET" \
  --header="Content-Type: application/x-www-form-urlencoded" \
   http://keycloak:8080/realms/$REALM/protocol/openid-connect/token)
 
BEARER_TOKEN=$(echo "$RESPONSE" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

# Get all shell-descriptors (validates role view_digital_twin if no 400)
wget --header="Authorization: Bearer $BEARER_TOKEN" $DTR_ADDRESS
```

Note: verification using postman on host does not work because the iss claim uses the external port. 

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
