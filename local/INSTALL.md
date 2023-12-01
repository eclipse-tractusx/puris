# Initial Setup
In case you had any previous installations of this project on your machine, it is advisable to remove them via the script
(see below in the Notes on debugging section).  

Run the following script to generate the necessary keys. It will also create an .env file in the ./local folder. 
Make sure to have `openssl` and `jq` installed in your shell.   
```shell
cd local
sh generate-keys.sh
```

# Build
If you are doing a fresh install and everytime you edited the code of the PURIS frontend or backend you have to create a 
new build of docker images for the PURIS frontend/backend. 

Please see the INSTALL.md documents in the [frontend](../frontend/INSTALL.md) and [backend](../backend/INSTALL.md)

The default image tag is 'dev'. Remember to also adjust the tag in the docker-compose.yaml if you want to use different 
tags. 

# Start
First start the infrastructure: 

```shell
docker compose -f docker-compose-infrastructure.yaml up
```
After the MIW container has finished booting, use this script to initialise two wallets for customer and supplier: 
```shell
sh ./init-wallets.sh
```
Then start the PURIS demonstrator containers via: 
```shell
docker compose up
```
Wait for the startup and visit http://localhost:3000/ for the customer's frontend or http://localhost:3001/ for the supplier side. 

## Notes on debugging

### Vault & Certs
When having problems with the certs or the vault, one may need to delete the vault container.
The following script stops all infrastructure containers as well as the PURIS demonstrator containers: 
```shell
cd local
sh cleanup.sh
```
Then start your containers again with the aforementioned commands. 
