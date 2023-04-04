<!--
 Copyright (c) 2023 Volkswagen AG
 Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2023 Contributors to the Eclipse Foundation

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
<template>
  <div v-if="this.selectedMaterialOrProductId === ''">
    <h2 class="text-center bold text-3xl">
      Your {{ this.partnerRole }}s' stocks for no material.
    </h2>
  </div>
  <div v-else class="grid">
    <h2 class="text-center bold text-3xl">
      Your {{ this.partnerRole }}s' stocks for
      {{ this.selectedMaterialOrProductId }}.
    </h2>
    <button
        class="btn-primary place-self-end"
        @click="updateMaterialOrProduct()"
    >
      Update
    </button>
    <table class="">
      <tr class="text-left">
        <th>Supplier</th>
        <th>Quantity</th>
        <th>Last updated on</th>
      </tr>
      <tr
          v-for="stock in availableMaterialsOrProducts"
          :key="stock.partnerBpnl"
      >
        <td>{{ stock.partnerName }} ({{ stock.partnerBpnl }})</td>
        <td>{{ stock.quantity.number }} {{ stock.quantity.unitOfMeasure }}</td>
        <td>{{ stock.lastUpdatedOn }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
export default {
  name: "PartnerStockSFC",

  props: {
    selectedMaterialOrProductId: {type: String, required: true},
    partnerRole: {type: String, required: true},
  },
  data() {
    return {
      availableMaterialsOrProducts: [],
    };
  },
  created() {
    if (this.selectedMaterialOrProductId !== "") {
      if (this.partnerRole === "supplier") {
        this.availableMaterialsOrProducts = this.getAvailableMaterials(
            this.selectedMaterialOrProductId
        );
      } else if (this.partnerRole === "customer") {
        this.availableMaterialsOrProducts = this.getAvailableProducts(
            this.selectedMaterialOrProductId
        );
      }
    }
  },
  methods: {
    getAvailableMaterials(materialId) {
      if (materialId === null) {
        return [];
      }
      if (materialId === "M4711") {
        return [
          {
            partnerBpnl: "BPNS123456789ZZ",
            partnerName: "Test Supplier 1",
            quantity: {
              number: 20,
              unitOfMeasure: "pcs",
            },
            lastUpdatedOn: "2023-03-04, 15:15",
          },
        ];
      } else if (materialId === "M4712") {
        return [
          {
            partnerBpnl: "BPNS123466789ZZ",
            partnerName: "Test Supplier 2",
            quantity: {
              number: 50,
              unitOfMeasure: "pcs",
            },
            lastUpdatedOn: "2023-03-04, 15:15",
          },
          {
            partnerBpnl: "BPNS123666789ZZ",
            partnerName: "Test Supplier 3",
            quantity: {
              number: 10,
              unitOfMeasure: "pcs",
            },
            lastUpdatedOn: "2023-03-04, 15:15",
          },
        ];
      } else if (materialId === "M4713") {
        return [];
      }
    },
    getAvailableProducts(productId) {
      if (productId === null) {
        return [];
      }
      if (productId === "P4711") {
        return [
          {
            partnerBpnl: "BPNS123456799ZZ",
            partnerName: "Test Customer 1",
            quantity: {
              number: 20,
              unitOfMeasure: "pcs",
            },
            lastUpdatedOn: "2023-03-04, 15:15",
          },
        ];
      }
    },
    updateMaterialOrProduct() {
      var options = {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      }
      var currentDate = new Date();
      var currentDateString = currentDate.toLocaleString("de-DE", options);

      // Note: Reactivity needs proxy
      for (let i = 0; i < this.availableMaterialsOrProducts.length; i++) {
        this.availableMaterialsOrProducts[i].lastUpdatedOn = currentDateString;
      }


    }
  },
};
</script>

<style scoped></style>
