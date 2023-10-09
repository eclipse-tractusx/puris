## Project Installation

The first steps are always the same:
1. Clone the project
2. Make sure the PURIS backend and the tractusx-edc is running with all its components

Depending on your needs of deployment, follow the following steps

### Running using mvn (local develpment)
3. Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
```shell
# build and run the generated .jar file
mvn install 

# run for demo or development puroposes
# customer role
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.config.location="./src/main/resources/application-customer.properties"

# supplier role
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.config.location="./src/main/resources/application-supplier.properties"
```
5. Done! The Swagger UI should be available at 
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
    - (Kubernetes) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward

### Running using docker (deployment)
3. Optional (one can set properties via environment variables to docker): Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
```shell
cd backend

docker build -t puris-backend:dev .

# A use docker 
docker run -d --rm -p 8081:8081 --name backend -e server.port=8082 puris-backend:dev CONTAINERID

# B use docker-compose
docker-compose up
```
5. Done! The Swagger UI should be available at
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
    - (Kubernetes) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward

### Running using helm (deployment)
Change the `src/main/resources/application.properties` or the respective environment
variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
```shell
cd backend/charts

helm install puris-backend
```
5. Done! The Swagger UI should be available at
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
    - (Kubernetes) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward
