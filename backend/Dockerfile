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
FROM maven:3-eclipse-temurin-26@sha256:69a6c37f0a91fca2c9d0cdd684c9c0ea7b1bc00d4196749c24440b2e02ad163d AS build
RUN mkdir -p /app/legal
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ /app/src/
RUN mvn clean package -DskipTests

# Copy Legal information for distributions, the star ones are copied by workflow
COPY DOCKER_NOTICE.md README.md NOTICE.md* SECURITY.md* LICENSE* DEPENDENCIES* LICENSE_non_code* /app/legal/

FROM eclipse-temurin:21.0.13.1_2-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
RUN mkdir /opt/app
WORKDIR /opt/app
COPY --from=build /app/target/*.jar /opt/app/puris-backend.jar
# Copy legal information for distributions
COPY --from=build /app/legal/* /opt/app/

RUN adduser -u 8877 --disabled-password --gecos "" --no-create-home nonroot && chmod 755 ./puris-backend.jar

USER nonroot

ENTRYPOINT ["sh", "-c", " java ${JAVA_OPTS} -jar ./puris-backend.jar ${0} ${@}"]
