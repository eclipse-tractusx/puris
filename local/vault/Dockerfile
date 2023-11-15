#
# Copyright (c) 2023 Volkswagen AG
# Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
# Copyright (c) 2023 Contributors to the Eclipse Foundation
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
FROM vault:1.12.2

RUN apk --no-cache add bash

EXPOSE 8200
COPY ./put-keys.sh /put-keys.sh

RUN chmod 755 put-keys.sh #&& chmod -R 755 ./vault/secrets #&& /usr/sbin/setcap cap_ipc_lock=+ep /bin/vault

# already is a non-root user
USER vault

ENTRYPOINT exec "./put-keys.sh"
