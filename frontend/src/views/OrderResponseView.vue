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
      delayed: [],
      onTime: [],
      backendData: [],
    };
  },
  methods: {
    getTransfers() {
      let vm = this;
      fetch(vm.baseUrl + "/edc/transfers")
        .then((response) => response.json())
        .then((data) => {
          data.forEach((element) => {
            if (element.state === "COMPLETED" && element.type === "CONSUMER") {
              vm.getBackend(element.dataRequest.contractId);
            }
          });
        });
    },
    getBackend(id) {
      let vm = this;
      const idArray = id.split(":");
      fetch(vm.baseUrl + "/edc/backend?transferId=" + idArray[1])
        .then((response) => response.json())
        .then((jsonObj) => {
          const jsonSet = {};
          jsonSet.key = idArray[0];
          jsonSet.value = jsonObj;
          vm.backendData.push(jsonSet);
        });
    },
    parseDate(dateString) {
      let date = new Date(dateString);
      return date.toLocaleDateString("de-DE");
    },
  },
  mounted() {
    let vm = this;
    vm.getTransfers();
  },
};
</script>

<template>
  <h1 class="text-center bold text-5xl mb-6 pb-6">Responses</h1>
  <li class="list-none" v-for="entry in backendData">
    <h1
      class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
    >
      Asset {{ entry.key }}
    </h1>
    <div
      class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
    >
      <json-viewer :value="entry.value" boxed copyable></json-viewer>
    </div>
  </li>
</template>
