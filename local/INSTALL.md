# Initial Setup
1. Generate keys
```shell
cd local
sh generate-keys.sh
```
2. Define remaining secrets in `/local/.env`
   - set root token for vault instance `VAULT_DEV_ROOT_TOKEN_ID` (e.g. "4Ko6r3UcHM4dXnOGmPKTHds3")
   - set password for edc control plane `EDC_API_PW` (e.g. "password")
   - set user `PG_USER` and password `PG_PW` for postgres (e.g. "edc-pg-user" and "edc-pg-passw0rd")
   - set vault secrets dir as mapped via volume (e.g. `/vault/secrets/`)

# Start
```shell
docker-compose up
```
or use
```
sh restart.sh
```
Wait for the startup and visit http://localhost:3000/

## Notes on debugging

### DAPS
The omejdn-daps does not provide any further logging configuration.
It may make sense to log the whole tokens or responses to decode the JWT or similar.

Requires ruby, which can be installed on Ubuntu as follows:
```shell
sudo apt-get install ruby
```

Then download the respective [omejdn release](https://github.com/Fraunhofer-AISEC/omejdn-server/releases/tag/v1.7.1) and unzip it.
In the `omejdn-server/omejdn.rb`
- search for token POST endpoint ("endpoint '/token', ['POST'],")
- go to end of endpoint definition (most left-hand end)
- add your echo / log upfront the status codes return (e.g. "puts.response.compact.to_json")
- build the omejdn server
```shell
docker build -t omejdn-server:local
```

Finally update the `./daps/docker-compose.yaml` to use this image instead.

### Vault & Certs
When having problems with the certs or the vault, one need to delete the vault container.
Following script helps faster restarting
```shell
cd local
sh restart.sh
```
