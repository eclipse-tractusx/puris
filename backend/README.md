<div align="center">
  <h2 align="center">PURIS Backend</h2>
  The backend of the Catena-X Predictive Unit Real-Time Information Service (PURIS)
</div>

## Table of Contents
- [Prerequirements](#prerequirements)
- [Getting Started](#getting-started)
- [License](#license)


## Prerequirements
The following things are needed to start PURIS:

- A Java Runtime Environment + Maven or an equivalent Docker setup
- A running product-EDC instance with the Catena-X Backend Application
  - More information can be found in the [product-edc documentation](https://github.com/catenax-ng/product-edc)


## Getting Started
1. Clone the project
2. Make sure the product-edc is running with all its components (control plane, data plane, ...)
3. Change the `src/main/resources/application.properties` or the respective environment 
   variables to configure the port, the URL of the EDC control plane, backend application etc.
4. Run the application:
    - (Java) Use `mvn install` to build the project and run the generated `.jar` file
    - (Docker) Run `docker build .` and `docker run -d -p 8081:8081 CONTAINERID`
5. Done! The Swagger UI should be available at `http://YOURIP:8081/catena/swagger-ui/index.html`
6. It is highly suggested to install and run the PURIS frontend afterward


## License
The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.
