<!--
 Copyright (c) 2023, 2024 Volkswagen AG
 Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation

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
  <div class="flex flex-col">
    <!--class="grid grid-rows-1 grid-flow-col gap-4"> -->
    <div class="">
      <h2 class="text-center bold text-3xl">{{ title }}</h2>
        <h3 class="bold text-2xl">Your Stocks</h3>
        <p v-if="this.ownRole==='customer'">
            <i>Info: These are your material stocks (your inputs) at your site.
            <b>Please select one of the material stocks to see the stocks your supplier still got on stock.</b></i>
        </p>
        <p v-if="this.ownRole==='supplier'">
            <i>Info: These are your product stocks (your outputs) at your site.
            <b>Please select one of the product stocks to see the stocks your customer still got on stock.</b></i>
        </p>
      <table class="">
        <tr class="text-left">
          <th>Material (ID)</th>
          <th>Quantity</th>
          <th>Allocated to Partner</th>
          <th>Is Blocked</th>
          <th>BPNS</th>
          <th>BPNA</th>
          <th>Customer Order Number<br> Customer Order Pos. Number</th>
          <th>Supplier Order Number</th>
        </tr>
        <tr
            v-for="stock in stocks"
            :key="stock.uuid"
            @click="selectStock(stock.material.materialNumberCustomer, stock.uuid)"
            :class="{ highlight: stock.material.materialNumberCustomer === selectedMaterialId }"
        >
          <td>{{ stock.material.name }}<br>({{ stock.material.materialNumberCustomer }})</td>
          <td>{{ stock.quantity }} {{ getUomValueForUomKey(stock.measurementUnit) }}</td>
          <td v-if="stock.type == 'PRODUCT'">{{ stock.allocatedToPartner.name }}<br>({{ stock.allocatedToPartner.bpnl }})</td>
          <td v-if="stock.type == 'MATERIAL'"></td>
          <td>{{stock.isBlocked}}</td>
          <td>{{stock.stockLocationBpns}}</td>
          <td>{{stock.stockLocationBpna}}</td>
          <td>{{stock.customerOrderNumber}}<br>{{stock.customerOrderPositionNumber}}</td>
          <td>{{stock.supplierOrderNumber}}</td>
        </tr>
      </table>
    </div>
    <div class="">
      <PartnerStockSFC
          :selectedMaterialOrProductId="this.selectedMaterialId"
          :partnerRole="this.partnerRole"
          :key="this.selectedMaterialId"
      />
      <!--
      <h2 class="text-center bold text-3xl">You're currently seeing your suppliers' stocks for {{this.selectedMaterial.name}} [{{ this.selectedMaterial.id }}]</h2>
      -->
    </div>
  </div>
</template>

<script>
import PartnerStockSFC from "@/views/stock/PartnerStockSFC.vue";
import UnitOfMeasureUtils from "@/services/UnitOfMeasureUtils";

export default {
  name: "StockTableSFC",
  components: {PartnerStockSFC},

  props: {
    title: {type: String, required: true, default: "Stock"},
    stocks: {type: Array, required: true},
      ownRole: {type: String, required: true, default: "customer"},
    partnerRole: {type: String, required: true, default: ""},
  },

  data() {
    return {
      selectedMaterialId: "",
      selectedStockUuid: "",
      materialNumberCustomer : ""
    };
  },
  methods: {
    selectStock(materialId, stockUuid) {
      if(this.partnerRole == 'customer')
        return;
      this.selectedMaterialId = materialId;
      this.selectedStockUuid = stockUuid;
    },
    getUomValueForUomKey(key){
        return UnitOfMeasureUtils.findUomValueByKey(key);
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
