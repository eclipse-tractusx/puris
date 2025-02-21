#!/bin/bash

#
# Copyright (c) 2023 Volkswagen AG
# Copyright (c) 2023 Contributors to the Eclipse Foundation
# Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)

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

docker compose down -v
docker compose -f docker-compose-infrastructure.yaml down -v
docker compose -f docker-compose-newman.yaml down -v
docker image rm local-vault
docker image rm local-mock-util-service
rm .env
rm seed-bdrs.sh
rm ./vault/secrets -r
rm ./iam-mock/keys -r
rm ./postman/tmp.json
rm ./postman/local.postman_environment.json
echo "Deleted .env, vault/secrets, seed-bdr.sh script and postman environment."
