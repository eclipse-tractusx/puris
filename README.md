<div align="center">
  <h2 align="center">PURIS</h2>
  The Catena-X Predictive Unit Real-Time Information Service (PURIS) for Short Term Demand and Capacity Management
</div>

## Overview

The project is made of a backend and a frontend. Look into the respective folders and their documentation to get
information about prerequirements and getting started guides.

## License

The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.

## Notice for Docker Image

This application provides container images for demonstration purposes.

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/puris
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfiles:
    - Frontend: https://github.com/eclipse-tractusx/puris/blob/main/frontend/Dockerfile
    - Backend: https://github.com/eclipse-tractusx/puris/blob/main/backend/Dockerfile
- Project license: [Apache License, Version 2.0](https://github.com/eclipse-tractusx/puris/blob/main/LICENSE)

**Used Base Image [Frontend]**

`nginxinc/nginx-unprivileged:alpine`

- DockerHub: https://hub.docker.com/r/nginxinc/nginx-unprivileged
- GitHub project: https://github.com/nginxinc/docker-nginx-unprivileged

**Used Base Image [Backend]**
`eclipse-temurin:17-jre-alpine`

- DockerHub: https://hub.docker.com/_/eclipse-temurin
- GitHub project: https://github.com/adoptium/containers

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc
from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies
with any relevant licenses for all software contained within.
