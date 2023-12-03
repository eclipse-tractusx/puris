# Running the Keyckloak with a Sample Realm

Runs a Keycloak with an `admin` user overall and a user `puris_user` with the roles `PURIS_ADMIN` and `PURIS_USER` in
the client `Cl3-PURIS`. Username = Password.  

```sh 
cd local/keycloak
docker run -p 10081:8080 --name keycloak \
-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
-v ./puris-config/:/opt/keycloak/data/import \
quay.io/keycloak/keycloak:23.0 \
start-dev --import-realm
```
