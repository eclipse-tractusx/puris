# Running the Keyckloak with a Sample Realm

Runs a Keycloak with an `admin` user overall and a user `puris_user` with the role `PURIS_USER` and a user `puris_admin`
with the role `PURIS_ADMIN` in the client `Cl3-PURIS`. Username = Password.  

```sh 
cd local/keycloak
docker run -p 10081:8080 --name keycloak \
-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
-v ./puris-config/:/opt/keycloak/data/import \
quay.io/keycloak/keycloak:23.0 \
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
quay.io/keycloak/keycloak:23.0.0 \
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
