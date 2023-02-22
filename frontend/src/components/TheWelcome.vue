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
      dash: {
        orders: 0,
        ordersSent: 0,
        responses: 0,
      },
    };
  },
  methods: {
    getOrderNum() {
      let vm = this;
      fetch(vm.baseUrl + "/dashboard/data")
        .then((response) => response.json())
        .then((data) => {
          console.log(data);
          vm.dash.orders = data.orders;
          vm.dash.ordersSent = JSON.parse(data.ordersSent).length;
          vm.dash.responses = JSON.parse(data.responses).length;
        });
    },
  },
  mounted() {
    let vm = this;
    vm.getOrderNum();
  },
};
</script>

<template>
  <div class="grid grid-flow-col auto-cols-max">
    <div
      class="text-center mx-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
    >
      <h1
        class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
      >
        Created Call-offs
      </h1>
      <h2 class="font-normal text-2xl text-gray-700 dark:text-gray-400">
        {{ dash.orders }}
      </h2>
    </div>

    <div
      class="text-center mx-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
    >
      <h1
        class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
      >
        Published Call-offs
      </h1>
      <h2 class="font-normal text-2xl text-gray-700 dark:text-gray-400">
        {{ dash.ordersSent }}
      </h2>
    </div>

    <div
      class="text-center mx-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
    >
      <h1
        class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
      >
        Transfers
      </h1>
      <h2 class="font-normal text-2xl text-gray-700 dark:text-gray-400">
        {{ dash.responses }}
      </h2>
    </div>
  </div>
</template>
