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
      edc: {
        url: "",
      },
      catalog: {},
    };
  },
  methods: {
    getCatalog(url) {
      const edcEncoded = encodeURIComponent(url);
      fetch(this.baseUrl + "/edc/catalog?idsUrl=" + edcEncoded)
        .then((response) => response.json())
        .then((data) => {
          this.catalog = data;
        });
    },
    startNegotiation(id) {
      const idArray = id.split(":");
      const edcEncoded = encodeURIComponent(this.edc.url);
      fetch(
        this.baseUrl +
          "/edc/startNegotiation?orderId=" +
          idArray[0] +
          "&connectorAddress=" +
          edcEncoded
      )
        .then((response) => response.text())
        .then((json) => window.alert(json));
    },
  },
  mounted() {},
};
</script>

<template>
  <main>
    <h1 class="w-full text-center bold text-5xl mb-6 pb-6">View EDC Catalog</h1>
    <div class="grid grid-rows-1 grid-flow-col gap-4">
      <form class="w-max max-w-lg">
        <div>
          <label for="orderName">EDC Url</label>
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
        v-on:click="getCatalog(this.edc.url)"
      >
        Get Catalog
      </button>
    </div>
    <li class="list-none" v-for="offer in this.catalog.contractOffers">
      <div
        class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
      >
        <div>
          <h1
            class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
          >
            Order {{ offer.id }}
          </h1>
          <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
            Description: {{ offer.asset.properties["asset:prop:description"] }}
          </h2>
        </div>
        <button
          class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
          id="orderBtn"
          type="submit"
          v-on:click="this.startNegotiation(offer.id)"
        >
          Start Negotiation
        </button>
      </div>
    </li>
  </main>
</template>
