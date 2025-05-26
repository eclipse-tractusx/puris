#
# Copyright (c) 2025 Volkswagen AG
# Copyright (c) 2025 Contributors to the Eclipse Foundation
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

FROM cypress/included:14.2.0

WORKDIR /testing

RUN chown -R node:node /testing

# switch to node user to create files and folders as non-root and install playwright for node
USER node

COPY . .

RUN npm install playwright-webkit --save-dev

# switch back to root temporarily to install missing dependencies for playwright webkit
USER root

RUN npx playwright install-deps webkit

# switch back to node user to run the tests
USER node
