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
            @click="selectStock(stock.id)"
            :class="{ highlight: stock.id === selectedStockId }"
        >
          <td>{{ stock.id }}</td>
          <td>{{ stock.name }}</td>
          <td>{{ stock.quantity }} {{ stock.unitOfMeasure }}</td>
          <td v-if="partnerRole=='customer'">{{ stock.customer }}</td>
        </tr>
      </table>
    </div>
    <div class="basis-1/2">
      <PartnerStockSFC
          :selectedMaterialOrProductId="this.selectedStockId"
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
    };
  },
  created() {
    console.log("PartnerRole in StockTablesSFC: " + this.partnerRole);
  },

  methods: {
    selectStock(stockId) {
      this.selectedStockId = stockId;
      console.log(stockId);
      console.log(this.selectedStockId);
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
