# Creating a local testing and development setup via Docker

## Additional information for Windows users

If you want to use the local setup on a Windows machine, it seems advisable to use the Windows subsystem for Linux (WSL). 
An installation guide can be found [here](https://learn.microsoft.com/en-us/windows/wsl/install). 

Also see this [additional information about using Docker in combination with WSL2](https://docs.docker.com/desktop/wsl/)

## Initial Setup
In case you had any previous installations of this project on your machine, it is advisable to remove them via the script
(see below in the Notes on debugging section).

Run the following script to generate the necessary keys. It will also create an .env file in the ./local folder. 
Make sure to have `openssl` and `jq` installed in your shell.  openssl is pre-installed on most operating systems. jq can be 
installed via the usual installation repositories, see [here](https://jqlang.github.io/jq/download/)
```shell
cd local
sh generate-keys.sh
```

## Build
If you are doing a fresh install and everytime you edited the code of the PURIS frontend or backend you have to create a 
new build of docker images for the PURIS frontend/backend. 

For creating a docker image of the frontend, navigate your shell to the frontend folder and run 

```
docker build -t puris-frontend:dev .
```

For creating a docker image of the backend, navigate your shell to the backend folder and run

```
docker build -t puris-backend:dev .
```

Please see the INSTALL.md documents in the [frontend](../frontend/INSTALL.md) and [backend](../backend/INSTALL.md)

The default image tag is 'dev'. Remember to also adjust the tag in the docker-compose.yaml if you want to use different 
tags. 

## Start
First start the infrastructure by navigating your shell to the local folder and running 

```shell
docker compose -f docker-compose-infrastructure.yaml up
```
After the MIW container has finished booting, use this script (also in the local folder) to initialise two wallets for customer and supplier: 
```shell
sh init-wallets.sh
```
Then start the PURIS demonstrator containers via: 
```shell
docker compose up
```
Wait for the startup and visit http://localhost:3000/ for the customer's frontend or http://localhost:3001/ for the supplier side. 

Whenever you have edited the source code in the frontend or backend and you want to test these changes, we recommend that you 
stop all the containers, which are not part of the infrastructure, by deleting the volumes, i.e. run  

```
docker compose down -v
```

Then, build a new docker image for the respective component (frontend and/or backend) as described above and then restart via 

```shell
docker compose up
```

In general, it is not necessary to restart the infrastructure, if you had to rebuild frontend or backend. 
However, in rare cases there may be issues with the MIW. If this 
happens, you should use the cleanup script as mentioned in the debugging section below and then repeat the above-mentioned 
steps beginning with the Initial Setup section. 

## Notes on debugging

### Vault & Certs
When having problems with the certs or the vault, one may need to delete the vault container.
The following script stops all infrastructure containers as well as the PURIS demonstrator containers: 
```shell
cd local
sh cleanup.sh
```
Then start your containers again with the aforementioned commands. 
