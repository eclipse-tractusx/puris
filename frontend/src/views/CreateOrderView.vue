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
      order: {
        name: "",
        desc: "",
        customer: "Customer",
        supplier: "Supplier",
      },
      tempDemand: {
        part: "CX-test",
        date: "",
        amount: 0,
        note: "",
      },
      demands: [],
    };
  },
  methods: {
    createDemand(from) {
      if (from.part != null && from.date != null && from.amount != null) {
        let dem = {};
        dem.part = from.part;
        dem.date = from.date;
        dem.amount = from.amount;
        dem.note = from.note;
        this.demands.push(dem);
      } else {
        window.alert("Order is incomplete!");
      }
    },
    createOrder(from) {
      let jsonData = {};
      let customerInformation = {};
      let supplierInformation = {};
      jsonData["name"] = from.name;
      jsonData["orderId"] = this.createUUID();
      jsonData["orderDate"] = new Date().toISOString();
      jsonData["description"] = from.desc;
      var selected_customer = from.customer;
      customerInformation["customerName"] = selected_customer;
      customerInformation["customerId"] = selected_customer;
      var selected_supplier = from.supplier;
      supplierInformation["supplierName"] = selected_supplier;
      supplierInformation["supplierId"] = selected_supplier;
      jsonData["customerInformation"] = customerInformation;
      jsonData["supplierInformation"] = supplierInformation;
      jsonData["orderPositions"] = this.createPositions(this.demands);

      fetch(this.baseUrl + "/orders/order", {
        method: "POST",
        body: JSON.stringify(jsonData),
        headers: { "Content-type": "application/json; charset=UTF-8" },
      })
        .then((response) => response.text())
        .then((json) => window.alert("Successfull created order: " + json))
        .catch((err) => window.alert(err));
    },
    createUUID() {
      return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(
        /[xy]/g,
        function (c) {
          var r = (Math.random() * 16) | 0,
            v = c === "x" ? r : (r & 0x3) | 0x8;
          return v.toString(16);
        }
      );
    },
    createPositions(from) {
      let positions = [];
      from.forEach((element) => {
        let jsonData = {};
        jsonData["positionId"] = this.createUUID();
        jsonData["itemName"] = element.part;
        jsonData["itemId"] = element.part;
        jsonData["desiredDate"] = element.date;
        jsonData["quantity"] = element.amount;
        jsonData["quantityUnit"] = "pcs";
        jsonData["note"] = element.note;
        positions.push(jsonData);
      });
      return positions;
    },
    deletePosition(position) {
      this.demands = this.demands.filter(function (value, index, arr) {
        return value !== position;
      });
    },
    parseDate(dateString) {
      let date = new Date(dateString);
      return date.toLocaleDateString("de-DE");
    },
  },
};
</script>

<template>
  <main>
    <h1 class="text-center bold text-5xl mb-6 pb-6">Create Call-off</h1>
    <div class="grid grid-flow-col auto-cols-max">
      <div
        class="text-center block p-4 w-max bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 mx-4"
      >
        <div>
          <h2
            class="w-max mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
          >
            Create Call-off
          </h2>
        </div>
        <div>
          <form class="w-max max-w-lg">
            <div>
              <label for="orderName">Name of the Call-off</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="text"
                id="orderName"
                placeholder="Enter name"
                v-model="order.name"
              />
            </div>
            <div>
              <label for="orderDescription">Description (optional)</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="text"
                id="orderDescription"
                placeholder="Description"
                v-model="order.description"
              />
            </div>
            <div>
              <label for="customer">Buyer (one-up)</label>
              <select
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                id="customerSelect"
                v-model="order.customer"
              >
                <option selected="selected">Customer</option>
              </select>
            </div>
            <div>
              <label for="supplier">Supplier (one-down)</label>
              <select
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                id="supplierSelect"
                v-model="order.supplier"
              >
                <option selected="selected">Supplier</option>
              </select>
            </div>
          </form>
          <div class="text-center">
            <button
              class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
              id="orderBtn"
              type="submit"
              v-on:click="createOrder(this.order)"
            >
              Create Call-off
            </button>
          </div>
        </div>
      </div>
      <div
        class="text-center block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 mx-4"
      >
        <div>
          <h3
            class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
          >
            Specification of Call-off
          </h3>
        </div>
        <div>
          <form class="w-full max-w-lg">
            <div>
              <label for="part">Part Number</label>
              <select
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                id="partSelect"
                v-model="tempDemand.part"
              >
                <option selected="selected">CX-test</option>
              </select>
            </div>
            <div>
              <label for="desiredDate">Estimated Time of Departure - ETD</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="date"
                id="desiredDate"
                v-model="tempDemand.date"
              />
            </div>
            <div>
              <label for="quantity">Number of pieces</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="number"
                step="1"
                id="quantity"
                v-model="tempDemand.amount"
              />
            </div>
            <div>
              <label for="note">Note (optional)</label>
              <input
                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                type="text"
                id="note"
                v-model="tempDemand.note"
              />
            </div>
          </form>
          <button
            class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
            id="position"
            type="submit"
            v-on:click="createDemand(this.tempDemand)"
          >
            Add
          </button>
        </div>
      </div>
      <div>
        <div
          class="min-h-max min-w-max h-full text-center block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700 mx-4"
        >
          <div>
            <h3
              class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white"
            >
              Demands
            </h3>
          </div>
          <div>
            <li class="list-none" v-for="dem in this.demands">
              <div
                class="grid grid-cols-2 gap-4 text-center mx-4 my-4 block p-6 max-w-sm bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
              >
                <div>
                  <h2
                    class="font-normal text-medium text-gray-700 dark:text-gray-400"
                  >
                    Part: {{ dem.part }}
                  </h2>
                  <h2
                    class="font-normal text-medium text-gray-700 dark:text-gray-400"
                  >
                    ETD: {{ this.parseDate(dem.date) }}
                  </h2>
                  <h2
                    class="font-normal text-medium text-gray-700 dark:text-gray-400"
                  >
                    Quantity: {{ dem.amount }} pcs
                  </h2>
                </div>
                <button
                  class="my-4 p-4 bg-red-500 hover:bg-red-600 text-white font-bold rounded-full"
                  id="orderBtn"
                  type="submit"
                  v-on:click="deletePosition(dem)"
                >
                  <img
                    class="stroke-white float-left"
                    src="@/assets/icons/trash.svg"
                    alt=""
                  />Delete
                </button>
              </div>
            </li>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
