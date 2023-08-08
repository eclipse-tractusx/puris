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
  <div class="flex flex-row">
    <!--class="grid grid-rows-1 grid-flow-col gap-4"> -->
    <div class="basis-1/2 pr-5">
      <h2 class="text-center bold text-3xl">{{ title }}</h2>
      <table class="">
        <tr class="text-left">
          <th>ID</th>
          <th>Name</th>
          <th>Quantity</th>
          <th v-if="partnerRole=='customer'">Allocated to customer</th>
        </tr>
        <tr
            v-for="stock in stocks"
            :key="stock.id"
            @click="selectStock(stock.material.materialNumberCustomer, stock.material.uuid)"
            :class="{ highlight: stock.material.materialNumberCustomer === selectedStockId }"
        >
          <td>{{ stock.material.materialNumberCustomer }}</td>
          <td>{{ stock.material.name }}</td>
          <td>{{ stock.quantity }} pieces</td>
          <td v-if="partnerRole=='customer'">{{ stock.allocatedToCustomerPartner.name }}</td>
        </tr>
      </table>
    </div>
    <div class="basis-1/2">
      <PartnerStockSFC
          :selectedMaterialOrProductId="this.selectedStockId"
          :materialUuid="this.selectedStockUuid"
          :partnerRole="this.partnerRole"
          :key="this.selectedStockId"
      />
      <!--
      <h2 class="text-center bold text-3xl">You're currently seeing your suppliers' stocks for {{this.selectedMaterial.name}} [{{ this.selectedMaterial.id }}]</h2>
      -->
    </div>
  </div>
</template>

<script>
import PartnerStockSFC from "@/views/stock/PartnerStockSFC.vue";

export default {
  name: "StockTableSFC",
  components: {PartnerStockSFC},

  props: {
    title: {type: String, required: true, default: "Stock"},
    stocks: {type: Array, required: true},
    partnerRole: {type: String, required: true, default: ""},
  },

  data() {
    return {
      selectedStockId: "",
      selectedStockUuid: "",
    };
  },
  methods: {
    selectStock(stockId, stockUuid) {
      if(this.partnerRole == 'customer')
        return;
      this.selectedStockId = stockId;
      this.selectedStockUuid = stockUuid;
    },
  },
};
</script>

<style scoped>
.highlight {
  background-color: orange;
}

table {
  width: 100%;
}
</style>
