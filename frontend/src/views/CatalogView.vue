<!--
 Copyright (c) 2022,2023 Volkswagen AG
 Copyright (c) 2022,2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
 
 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.
 
 This program and the accompanying materials are made available under the
 terms of the Apache License, Version 2.0 which is available at
 https://www.apache.org/licenses/LICENSE-2.0.
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations
 under the License.
 
 SPDX-License-Identifier: Apache-2.0
-->

<script>
export default {
    data() {
        return {
            backendUrl: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            edc: {
                url: "",
            },
            catalog: {},
        };
    },
    methods: {
        getCatalog(url) {
            const edcEncoded = encodeURIComponent(url);
            fetch(this.backendUrl + "edc/catalog?dspUrl=" + edcEncoded, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((response) => response.json())
                .then((data) => {
                    this.catalog = data;
                });
        },
    },
    mounted() {},
};
</script>

<template>
    <main>
        <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
            View EDC Catalog
        </h1>
        <div class="grid grid-rows-1 grid-flow-col gap-4">
            <form class="w-max max-w-lg">
                <div>
                    <label for="orderName">EDC DSP URL</label>
                    <input
                        class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                        type="text"
                        id="edcUrlInput"
                        placeholder="Enter url"
                        v-model="edc.url"
                    />
                </div>
            </form>
            <button
                class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                id="orderBtn"
                type="submit"
                v-on:click="getCatalog(edc.url)"
            >
                Get Catalog
            </button>
        </div>
        <li class="list-none" v-for="offer in catalog['dcat:dataset']">
            <div
                class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
            >
                <div>
                    <h1
                        class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
                    >
                        Catalog Item's Asset ID "{{ offer["asset:prop:type"] }}"
                    </h1>
                    <h2
                        class="font-normal text-medium text-gray-700 dark:text-gray-400"
                    >
                        Asset Prop type: "{{ offer["asset:prop:type"] }}"
                    </h2>
                    <div>
                        Following permissions are defined: <br />
                        <ul v-if="offer['odrl:permission']">
                            <li
                                class="list-none"
                                v-for="constraint in offer['odrl:permission']"
                            >
                                {{ constraint["odrl:or"]["odrl:leftOperand"] }}
                                {{
                                    constraint["odrl:or"]["odrl:operator"][
                                        "@id"
                                    ]
                                }}
                                {{ constraint["odrl:or"]["odrl:rightOperand"] }}
                            </li>
                        </ul>
                        <ul v-else>
                            None
                        </ul>
                    </div>
                    <div>
                        Following prohibitions are defined: <br />
                        <ul v-if="offer['odrl:prohibition']">
                            <li
                                class="list-none"
                                v-for="constraint in offer['odrl:prohibition']"
                            >
                                {{ constraint["odrl:or"]["odrl:leftOperand"] }}
                                {{
                                    constraint["odrl:or"]["odrl:operator"][
                                        "@id"
                                    ]
                                }}
                                {{ constraint["odrl:or"]["odrl:rightOperand"] }}
                            </li>
                        </ul>
                        <ul v-else>
                            None
                        </ul>
                    </div>
                    <div>
                        Following obligations are defined: <br />
                        <ul v-if="offer['odrl:obligation']">
                            <li
                                class="list-none"
                                v-for="constraint in offer['odrl:obligation']"
                            >
                                {{ constraint["odrl:or"]["odrl:leftOperand"] }}
                                {{
                                    constraint["odrl:or"]["odrl:operator"][
                                        "@id"
                                    ]
                                }}
                                {{ constraint["odrl:or"]["odrl:rightOperand"] }}
                            </li>
                        </ul>
                        <ul v-else>
                            None
                        </ul>
                    </div>
                </div>
            </div>
        </li>
    </main>
</template>
