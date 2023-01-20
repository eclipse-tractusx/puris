## Project Installation

1. Clone the project
2. Make sure the product-edc is running with all its components (control plane, data plane, ...)
3. Change the `src/main/resources/application.properties` or the respective environment
   variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
    - (Java) Use `mvn install` to build the project and run the generated `.jar` file
    - (Docker) Run `docker build .` and `docker run -d -p 8081:8081 CONTAINERID`
5. Done! The Swagger UI should be available at `http://YOURIP:8081/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward