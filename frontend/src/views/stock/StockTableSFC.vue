<template>
  <div class="grow flex flex-row content-evenly">
    <!--class="grid grid-rows-1 grid-flow-col gap-4"> -->
    <div>
      <h2 class="text-center bold text-3xl">{{ title }}</h2>
      <table class="table-auto">
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Quantity</th>
        </tr>
        <tr
          v-for="stock in stocks"
          :key="stock.id"
          @click="selectStock(stock.id)"
          :class="{ highlight: stock.id === selectedStock }"
        >
          <td>{{ stock.id }}</td>
          <td>{{ stock.name }}</td>
          <td>{{ stock.quantity }} {{ stock.unitOfMeasure }}</td>
        </tr>
      </table>
    </div>
    <div>
      <PartnerStockSFC
        selected-stock="this.selectedStock"
        :key="this.selectedStock"
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
  components: { PartnerStockSFC },

  props: {
    title: { type: String, required: true, default: "Stock" },
    stocks: { type: Array, required: true },
  },

  data() {
    return {
      selectedStock: "",
    };
  },

  methods: {
    selectStock(stockId) {
      this.selectedStock = stockId;
      console.log(stockId);
      console.log(this.selectedStock);
    },
  },
};
</script>

<style scoped>
.highlight {
  background-color: orange;
}
</style>
