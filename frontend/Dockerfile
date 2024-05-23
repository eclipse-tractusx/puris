#
# Copyright (c) 2022,2024 Volkswagen AG
# Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
# Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
FROM node:lts-alpine as build

ARG NPM_BUILD_MODE=dockerbuild
ENV JQ_VERSION=1.6

# Determine architecture and download the appropriate version of jq
RUN ARCH="$(uname -m)" && \
    if [ "${ARCH}" = "x86_64" ]; then \
        JQ_BINARY="jq-linux-amd64"; \
    elif [ "${ARCH}" = "aarch64" ]; then \
        JQ_BINARY="jq-linux-arm64"; \
    else \
        echo "Unsupported architecture: ${ARCH}" && exit 1; \
    fi && \
    wget --no-check-certificate -O /usr/bin/jq "https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/${JQ_BINARY}" && \
    chmod +x /usr/bin/jq

WORKDIR /app
COPY . .

RUN jq 'to_entries | map_values({ (.key) : ("$" + .key) }) | reduce .[] as $item ({}; . + $item)' ./src/config.json > ./src/config.tmp.json && mv ./src/config.tmp.json ./src/config.json

RUN npm install && npm run ${NPM_BUILD_MODE}

RUN chmod -R 777 /app/dist

# Copy Legal information for distributions, the star ones are copied by workflow
COPY DOCKER_NOTICE.md README.md NOTICE.md* SECURITY.md* LICENSE* DEPENDENCIES* /app/dist/

# uses 101 restricted user
FROM nginxinc/nginx-unprivileged:alpine
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
