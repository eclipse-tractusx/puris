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
          :key="stock.supplierPartner.bpnl"
      >
        <td v-if="this.selectedMaterialOrProductId == stock.material.materialNumberCustomer">{{ stock.supplierPartner.name }} ({{ stock.supplierPartner.bpnl }})</td>
        <td v-if="this.selectedMaterialOrProductId == stock.material.materialNumberCustomer">{{ stock.quantity }} pieces</td>
        <td v-if="this.selectedMaterialOrProductId == stock.material.materialNumberCustomer">{{ stock.lastUpdatedOn }}</td>
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
      backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
      endpointPartnerProductStocks: import.meta.env.VITE_ENDPOINT_PARTNER_PRODUCT_STOCKS,
      endpointUpdatePartnerProductStock: import.meta.env.VITE_ENDPOINT_UPDATE_PARTNER_PRODUCT_STOCK,
      availableMaterialsOrProducts: [],
    };
  },
  created() {
    if (this.selectedMaterialOrProductId !== "") {
      if (this.partnerRole === "supplier") {
        this.getAvailableMaterials();
      }
      // else if (this.partnerRole === "customer") {
      //   this.getAvailableProducts();
      // }
    }
  },
  methods: {
    getAvailableMaterials() {
      fetch(this.backendURL + this.endpointPartnerProductStocks + this.selectedMaterialOrProductId)
        .then(res => res.json())
        .then(data => this.availableMaterialsOrProducts = data)
        .catch(err => console.log(err));
    },
    // getAvailableProducts() {
    //   fetch(this.backendURL + this.endpointPartnerProductStocks)
    //     .then(res => res.json())
    //     .then(data => this.availableMaterialsOrProducts = data)
    //     .catch(err => console.log(err));
    // },
    updateMaterialOrProduct() {
      fetch(this.backendURL + this.endpointUpdatePartnerProductStock + this.selectedMaterialOrProductId)
        .then(res => res.json())
        .then(data => console.log(data))
        .catch(err => console.log(err));
    }
  },
};
</script>

<style scoped></style>
