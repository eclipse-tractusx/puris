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
      pendingOrders: [],
      sentOrders: [],
    };
  },
  methods: {
    getPendingOrders() {
      let vm = this;
      fetch(vm.baseUrl + "/orders/orders/pending")
        .then((response) => response.json())
        .then((data) => {
          vm.pendingOrders = data;
        });
    },
    getSentOrders() {
      let vm = this;
      fetch(vm.baseUrl + "/orders/orders/sent")
        .then((response) => response.json())
        .then((data) => {
          vm.sentOrders = data;
        });
    },
    publishAtEdc(order) {
      let vm = this;
      fetch(vm.baseUrl + "/edc/publish?orderId=" + order.orderId)
        .then((response) => response.text())
        .then((json) => {
          window.alert(json);
          vm.getPendingOrders();
          vm.getSentOrders();
        })
        .catch((err) => window.alert(err));
    },
    deleteOrder(order) {
      let vm = this;
      fetch(vm.baseUrl + "/orders/order?id=" + order.orderId, {
        method: "DELETE",
      })
        .then((response) => response.text())
        .then((json) => {
          console.log(json);
          vm.getPendingOrders();
        })
        .catch((err) => window.alert(err));
    },
    parseDate(dateString) {
      let date = new Date(dateString);
      return date.toLocaleDateString("de-DE");
    },
  },
  mounted() {
    let vm = this;
    vm.getPendingOrders();
    vm.getSentOrders();
  },
};
</script>

<template>
  <main class="text-center">
    <h1 class="text-center bold text-5xl mb-6 pb-6">Manage Orders</h1>
    <div class="grid grid-flow-col auto-cols-max">
      <div
        class="mx-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 w-96"
      >
        <h5
          class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
        >
          Pending Call-offs
        </h5>
        <div>
          <li class="list-none" v-for="ord in pendingOrders">
            <div
              class="text-center mx-4 my-8 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
            >
              <h1
                class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
              >
                {{ ord.name }}
              </h1>
              <h2
                class="font-normal text-medium text-gray-700 dark:text-gray-400"
              >
                Supplier: {{ ord.supplierInformation.supplierName }}
              </h2>
              <h2
                class="font-normal text-medium text-gray-700 dark:text-gray-400"
              >
                Positions:
              </h2>
              <li class="list-none" v-for="pos in ord.orderPositions">
                <h2
                  class="font-small text-small text-gray-700 dark:text-gray-400"
                >
                  {{ pos.itemId }}: {{ pos.quantity }} {{ pos.quantityUnit }}
                </h2>
              </li>
              <button
                class="my-8 bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
                id="orderBtn"
                type="submit"
                v-on:click="publishAtEdc(ord)"
              >
                Publish EDC
              </button>
              <button
                class="my-8 bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                id="orderBtn"
                type="submit"
                v-on:click="deleteOrder(ord)"
              >
                Delete
              </button>
            </div>
          </li>
        </div>
      </div>

      <div
        class="mx-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 w-96"
      >
        <h5
          class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
        >
          Call-offs to Supplier (one-down)
        </h5>
        <div>
          <li class="list-none" v-for="ord in sentOrders">
            <div
              class="text-center mx-4 my-8 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
            >
              <h1
                class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
              >
                {{ ord.name }}
              </h1>
              <h2
                class="font-normal text-medium text-gray-700 dark:text-gray-400"
              >
                Supplier: {{ ord.supplierInformation.supplierName }}
              </h2>
              <h2
                class="font-normal text-medium text-gray-700 dark:text-gray-400"
              >
                Positions:
              </h2>
              <li class="list-none" v-for="pos in ord.orderPositions">
                <h2
                  class="font-small text-small text-gray-700 dark:text-gray-400"
                >
                  {{ pos.itemId }}: {{ pos.quantity }} {{ pos.quantityUnit }}
                </h2>
              </li>
            </div>
          </li>
        </div>
      </div>
    </div>
  </main>
</template>
