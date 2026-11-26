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
FROM maven:3.10.0-eclipse-temurin-21@sha256:3e18d95a0a49d18aa5b075e5ee3c30a98078c382850467dcd4caa49bb7dc4038 AS build
RUN mkdir -p /app/legal
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ /app/src/
RUN mvn clean package -DskipTests

# Copy Legal information for distributions, the star ones are copied by workflow
COPY DOCKER_NOTICE.md README.md NOTICE.md* SECURITY.md* LICENSE* DEPENDENCIES* LICENSE_non_code* /app/legal/

FROM eclipse-temurin:24.0.2_12-jre-alpine@sha256:4044b6c87cb088885bcd0220f7dc7a8a4aab76577605fa471945d2e98270741f
RUN mkdir /opt/app
WORKDIR /opt/app
COPY --from=build /app/target/*.jar /opt/app/puris-backend.jar
# Copy legal information for distributions
COPY --from=build /app/legal/* /opt/app/

RUN adduser -u 8877 --disabled-password --gecos "" --no-create-home nonroot && chmod 755 ./puris-backend.jar

USER nonroot

ENTRYPOINT ["sh", "-c", " java ${JAVA_OPTS} -jar ./puris-backend.jar ${0} ${@}"]
