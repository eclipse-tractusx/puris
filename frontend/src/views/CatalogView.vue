<!--
  - Copyright (c) 2022,2024 Volkswagen AG
  - Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
  -
  - See the NOTICE file(s) distributed with this work for additional
  - information regarding copyright ownership.
  -
  - This program and the accompanying materials are made available under the
  - terms of the Apache License, Version 2.0 which is available at
  - https://www.apache.org/licenses/LICENSE-2.0.
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  - WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  - License for the specific language governing permissions and limitations
  - under the License.
  -
  - SPDX-License-Identifier: Apache-2.0
  -->

<script>
export default {
    data() {
        return {
            backendUrl: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            edcUrl: "",
            catalog: {},
        };
    },
    methods: {
        getCatalog() {
            const edcEncoded = encodeURIComponent(this.edcUrl);
            fetch(this.backendUrl + "edc/catalog?dspUrl=" + edcEncoded, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((response) => response.json())
                .then((data) => {
                    this.catalog = data["dcat:dataset"].map((item) => {
                        return {
                            assetId: item["@id"],
                            assetType: item["asset:prop:type"],
                            assetVersion:
                                item[
                                    "https://w3id.org/catenax/ontology/common#version"
                                    ],
                            permissions:
                                item["odrl:hasPolicy"] &&
                                item["odrl:hasPolicy"]["odrl:permission"],
                            prohibitions:
                                item["odrl:hasPolicy"] &&
                                item["odrl:hasPolicy"]["odrl:prohibitions"],
                            obligations:
                                item["odrl:hasPolicy"] &&
                                item["odrl:hasPolicy"]["odrl:obligations"],
                        };
                    });
                    console.info(this.catalog);
                });
        },
    },
};
</script>

<template>
    <main>
        <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
            View EDC Catalog
        </h1>
        <div class="flex items-end gap-4">
            <div class="flex-grow">
                <label for="edcUrl">EDC DSP URL</label>
                <input
                    class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                    type="text"
                    id="edcUrlInput"
                    placeholder="Enter url"
                    v-model="edcUrl"
                />
            </div>
            <div>
                <button
                    class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded"
                    id="catalogBtn"
                    type="submit"
                    v-on:click="getCatalog"
                >
                    Get Catalog
                </button>
            </div>
        </div>
        <ul v-for="offer in catalog" v-bind:key="offer.assetId">
            <li class="list-none">
                <div
                    class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
                >
                    <div>
                        <h1
                            class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
                        >
                            Catalog Item's Asset ID "{{ offer.assetId }}"
                        </h1>
                        <h2
                            class="font-normal text-medium text-gray-700 dark:text-gray-400"
                        >
                            Asset Prop type: "{{ offer.assetType }}:{{
                            offer.assetVersion
                            }}"
                        </h2>
                        <div>
                            Following permissions are defined: <br/>
                            <ul v-if="offer.permissions">
                                <li class="list-none">
                                    {{
                                    offer.permissions["odrl:constraint"][
                                    "odrl:leftOperand"
                                    ]
                                    }}
                                    {{
                                    offer.permissions["odrl:constraint"][
                                    "odrl:operator"
                                    ]["@id"]
                                    }}
                                    {{
                                    offer.permissions["odrl:constraint"][
                                    "odrl:rightOperand"
                                    ]
                                    }}
                                </li>
                            </ul>
                            <ul v-else>
                                None
                            </ul>
                        </div>
                        <div>
                            Following prohibitions are defined: <br/>
                            <ul v-if="offer.prohibitions">
                                <li class="list-none">
                                    {{
                                    offer.prohibitions["odrl:constraint"][
                                    "odrl:leftOperand"
                                    ]
                                    }}
                                    {{
                                    offer.prohibitions["odrl:constraint"][
                                    "odrl:operator"
                                    ]["@id"]
                                    }}
                                    {{
                                    offer.prohibitions["odrl:constraint"][
                                    "odrl:rightOperand"
                                    ]
                                    }}
                                </li>
                            </ul>
                            <ul v-else>
                                None
                            </ul>
                        </div>
                        <div>
                            Following obligations are defined: <br/>
                            <ul v-if="offer.obligations">
                                <li class="list-none">
                                    {{
                                    offer.obligations["odrl:constraint"][
                                    "odrl:leftOperand"
                                    ]
                                    }}
                                    {{
                                    offer.obligations["odrl:constraint"][
                                    "odrl:operator"
                                    ]["@id"]
                                    }}
                                    {{
                                    offer.obligations["odrl:constraint"][
                                    "odrl:rightOperand"
                                    ]
                                    }}
                                </li>
                            </ul>
                            <ul v-else>
                                None
                            </ul>
                        </div>
                    </div>
                </div>
            </li>
        </ul>
    </main>
</template>
