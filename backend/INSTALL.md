## Project Installation

The first steps are always the same:
1. Clone the project
2. Make sure the PURIS backend and the tractusx-edc is running with all its components

Depending on your needs of deployment, follow the following steps

### Running using mvn (local development) and infrastructure services in kubernetes
1. Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
2. Run the application:
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.config.location="./src/main/resources/application.properties"

3. It is highly suggested to install, configure and run the PURIS frontend afterward

### Running using docker (deployment)

1. First build a docker image: 
```
cd backend

docker build -t puris-backend:dev .
```

2. Optionally (one can set properties via environment variables to docker): Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
3. Run the application:
```shell
cd backend

docker build -t puris-backend:dev .

# A use docker 
docker run -d --rm -p 8081:8081 --name backend -e server.port=8082 puris-backend:dev CONTAINERID

# B use docker-compose
cd ..
cd local
docker-compose up
```
4. Done! The Swagger UI should be available at
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
5. It is highly suggested to install and run the PURIS frontend afterward (unless you're using local/docker-compose.yaml)

### Running using helm (deployment)
1. Run the application:

```shell
cd charts/puris/charts/backend

helm install backend --namespace puris --create-namespace . --set ingress.enabled=true
```
2. Done! The Swagger UI should be available at
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
    - (Kubernetes) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
3. It is highly suggested to install and run the PURIS frontend afterward
