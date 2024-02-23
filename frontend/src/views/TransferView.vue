<!--
 Copyright (c) 2022,2024 Volkswagen AG
 Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
 
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
            backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            catalog: {}
        };
    },
    methods: {
        getCatalog() {
            const url = this.backendURL + "edc/transfers";
            console.log("Fetching from " + url);
            fetch(url, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((response) => response.json())
                .then((data) => {
                    this.catalog = data;
                })
                .catch((err) => console.log(err));
        },
    },
    mounted() {
        let vm = this;
        vm.getCatalog();
    },
};
</script>

<template>
    <main>
        <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
            View EDC Transfers
        </h1>
        <li class="list-none" v-for="item in catalog">
            <div
                class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
            >
                <div>
                    <h1
                        class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
                    >
                        Transfer Id: {{ item["@id"] }}
                    </h1>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        Correlation Id: {{ item["edc:correlationId"] }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        State: {{ item["edc:state"] }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        StateTimeStamp: {{ new Date(item["edc:stateTimestamp"]) }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        Type: {{ item["edc:type"] }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        Asset Id: {{ item["edc:assetId"] }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        Contract Id: {{ item["edc:contractId"] }}
                    </h2>
                    <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
                        Connector Id: {{ item["edc:connectorId"] }}
                    </h2>
                </div>
            </div>
        </li>
    </main>
</template>
