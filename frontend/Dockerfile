#
# Copyright (c) 2022 Volkswagen AG
# Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
# Copyright (c) 2022 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#
FROM node:26.13.1-alpine@sha256:6bd9ab43c0ec0190ca7b2d193906cb8b7edf6e7f4afa43ddeccd6452710bffb5 AS build

ARG NPM_BUILD_MODE=dockerbuild

WORKDIR /app

COPY package*.json ./

RUN npm install 

COPY . .

RUN npm run ${NPM_BUILD_MODE}

RUN chmod -R 777 /app/dist

# Copy Legal information for distributions, the star ones are copied by workflow
COPY DOCKER_NOTICE.md README.md NOTICE.md* SECURITY.md* LICENSE* DEPENDENCIES* LICENSE_non_code* /app/dist/

# uses 101 restricted user
FROM nginx/nginx-unprivileged:1.31.10-alpine@sha256:86628f2c9eaa5e84e3b98d25e26563c4a4f9b0ead9cf5656da4731d86e0621de
#FROM nginx:stable-alpine

# commly it would be .../html/js/.*js, but due to the project structure the JS files are in .../html/assets
ENV JSFOLDER=/usr/share/nginx/html/assets/*.js

# use root user to install moreutils for sponge in start-nginx to substring env workaround
USER root

COPY ./nginx.conf /etc/nginx/nginx.conf
RUN chmod 777 /etc/nginx/nginx.conf
COPY ./start-nginx.sh /usr/bin/start-nginx.sh

# use non-root user
USER 101

HEALTHCHECK --interval=30s --timeout=3s  CMD wget -O /dev/null http://localhost:8080 || exit 1

WORKDIR /usr/share/nginx/html
COPY --from=build /app/dist .
ENTRYPOINT [ "start-nginx.sh" ]
# nginx exposes at port 80
# nginx-unpriviledged exposes at port 8080
