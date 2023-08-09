/*
 * Copyright (c) 2022,2023 Volkswagen AG
 * Copyright (c) 2022,2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import {createApp} from "vue";
import App from "./App.vue";
import router from "./router";
import "./index.css";
import JsonViewer from "vue-json-viewer";
import Config from "./config.json"

const app = createApp(App);
const publicEnvVar = import.meta.env.VITE_BASE_URL;

app.use(router);
app.use(JsonViewer);

console.log("config.json BACKEND_BASE_URL" + Config.BACKEND_BASE_URL);
console.log(".env.x VITE_BASE_URL" + publicEnvVar);

app.mount("#app");
app.provide("baseUrl", publicEnvVar);
