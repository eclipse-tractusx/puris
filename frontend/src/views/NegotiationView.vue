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
      catalog: {},
    };
  },
  methods: {
    getCatalog() {
      let vm = this;
      fetch(vm.baseUrl + "/edc/negotiations")
        .then((response) => response.json())
        .then((data) => {
          vm.catalog = data;
        });
    },
    startTransfer(orderId, url) {
      const idArray = orderId.split(":");
      const edcEncoded = encodeURIComponent(url);
      let vm = this;
      fetch(
        vm.baseUrl +
          "/edc/startTransfer?orderId=" +
          idArray[0] +
          "&connectorAddress=" +
          edcEncoded +
          "&transferId=" +
          idArray[1] +
          "&contractId=" +
          idArray[1]
      )
        .then((response) => response.text())
        .then((json) => window.alert(json));
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
      View EDC Negotiations
    </h1>
    <li class="list-none" v-for="offer in catalog">
      <div
        class="text-center mx-4 my-8 block p-6 w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700"
      >
        <div>
          <h1
            class="mb-2 text-xl font-bold tracking-tight text-gray-900 dark:text-white"
          >
            Negotiation {{ offer.id }}
          </h1>
          <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
            Counterparty: {{ offer.counterPartyAddress }}
          </h2>
          <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
            State: {{ offer.state }}
          </h2>
          <h2 class="font-normal text-medium text-gray-700 dark:text-gray-400">
            Type: {{ offer.type }}
          </h2>
          <h2
            v-if="offer.contractAgreementId"
            class="font-normal text-medium text-gray-700 dark:text-gray-400"
          >
            Agreement ID: {{ offer.contractAgreementId }}
          </h2>
        </div>
        <button
          v-if="offer.state == 'CONFIRMED' && offer.type == 'CONSUMER'"
          class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
          id="orderBtn"
          type="submit"
          v-on:click="
            startTransfer(
              offer.contractAgreementId,
              offer.counterPartyAddress
            )
          "
        >
          Start Transfer
        </button>
      </div>
    </li>
  </main>
</template>
