## Project Installation

1. Clone the project
2. Make sure the tractusx-edc is running with all its components (control plane, data plane, ...)
3. Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
    - (Java) Use `mvn install` to build the project and run the generated `.jar` file
    - (Docker) Run `docker build .` and `docker run -d -p 8081:8081 CONTAINERID`
    - (Kubernetes) Run `helm install puris-backend`
4a. Run the application with role specific settings:
    - Use `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=customer` to start with customer setup
    or
    - Use `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=supplier`to start with supplier setup

5. Done! The Swagger UI should be available at 
    - (Java & Docker) `http://YOURIP:8081/catena/swagger-ui/index.html`
    - (Kubernetes) `http://CLUSTERIP:30001/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward
