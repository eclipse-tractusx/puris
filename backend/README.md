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

See the [installation instructions](INSTALL.md) for information on how to start the application.

## License
The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.

## Notice for Docker image
This application provides container images for demonstration purposes.

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/puris-backend
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile: https://github.com/eclipse-tractusx/item-relationship-service/blob/main/Dockerfile
- Project license: [Apache License, Version 2.0](https://github.com/eclipse-tractusx/puris-backend/blob/main/LICENSE) 
**Used base image**
- [eclipse-temurin:17-jre-alpine](https://github.com/adoptium/containers)
- Official Eclipse Temurin DockerHub page: https://hub.docker.com/_/eclipse-temurin
- Eclipse Temurin Project: https://projects.eclipse.org/projects/adoptium.temurin
- Additional information about the Eclipse Temurin images: https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.