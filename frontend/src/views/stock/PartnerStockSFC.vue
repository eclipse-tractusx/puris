<template>
  <div v-if="this.selectedStock === ''">
    <h2 class="text-center bold text-3xl">
      You're currently seeing your suppliers' stocks for no material.
    </h2>
  </div>
  <div v-else>
    <h2 class="text-center bold text-3xl">
      You're currently seeing your suppliers' stocks for
      {{ this.selectedStock }}.
    </h2>
    {{ this.selectedStock }}
  </div>
</template>

<script>
export default {
  name: "PartnerStockSFC",

  props: {
    selectedStock: { required: true },
  },
};
</script>

<style scoped></style>
