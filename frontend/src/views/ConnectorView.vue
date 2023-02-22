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
  inject: ["baseUrl"],
  data() {
    return {
      connector: {
        name: "",
        url: "",
      },
      connectors: [],
    };
  },
  methods: {
    getConnectors() {
      let vm = this;
      fetch(vm.baseUrl + "/externalConnector/all")
        .then((response) => response.json())
        .then((data) => {
          vm.connectors = data;
        });
    },
    createConnector(from) {
      let vm = this;
      let jsonData = {};
      jsonData["name"] = from.name;
      jsonData["url"] = from.url;

      fetch(vm.baseUrl + "/externalConnector/create", {
        method: "POST",
        body: JSON.stringify(jsonData),
        headers: { "Content-type": "application/json; charset=UTF-8" },
      })
        .then((response) => response.text())
        .then((_json) => vm.getConnectors())
        .catch((err) => window.alert(err));
    },
  },
  mounted() {
    let vm = this;
    vm.getConnectors();
  },
};
</script>

<template>
  <main>
    <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
      Create External Connectors
    </h1>
    <div class="grid grid-flow-col auto-cols-max">
      <div
        class="text-center block p-4 w-max bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 mx-4"
      >
        <div>
          <h2
            class="w-max mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
          >
            Create Connector
          </h2>
        </div>
        <div>
          <form class="w-max max-w-lg">
            <div>
              <label for="connectorName">Connector Name</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="text"
                id="connectorName"
                placeholder="Enter Name"
                v-model="connector.name"
              />
            </div>
            <div>
              <label for="connectorUrl">Connector Url</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="text"
                id="connectorUrl"
                placeholder="Enter Connector URL"
                v-model="connector.url"
              />
            </div>
          </form>
          <div class="text-center">
            <button
              class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
              id="createBtn"
              type="create"
              v-on:click="createConnector(connector)"
            >
              Create Connector
            </button>
          </div>
        </div>
      </div>
      <div
        class="min-h-max min-w-max h-full text-center block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 mx-4"
      >
        <li class="list-none" v-for="conn in connectors">
          <div
            class="grid grid-cols-2 gap-4 text-center mx-4 my-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
          >
            <div>
              <h1
                class="mb-2 text-xl text-center font-bold tracking-tight text-gray-900 dark:text-white"
              >
                {{ conn.name }}
              </h1>
              <h2
                class="font-normal text-medium text-center text-gray-700 dark:text-gray-400"
              >
                URL: {{ conn.url }}
              </h2>
            </div>
          </div>
        </li>
      </div>
    </div>
  </main>
</template>
