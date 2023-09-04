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
- A running tractusx-edc instance
- More information can be found in the [tractusx-edc documentation](https://github.com/eclipse-tractusx/tractusx-edc)

## Getting Started

See the [installation instructions](INSTALL.md) for information on how to start the application.

## License
The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.

## Notice for Docker Image
This application provides container images for demonstration purposes.

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/puris
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile Backend: https://github.com/eclipse-tractusx/puris/blob/main/backend/Dockerfile
- Project license: [Apache License, Version 2.0](https://github.com/eclipse-tractusx/puris/blob/main/backend/LICENSE)


**Used Base Image [Backend]**
- `maven:3.8.7-eclipse-temurin-17`
- DockerHub: https://hub.docker.com/_/maven/
- GitHub project: https://github.com/carlossg/docker-maven

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.
