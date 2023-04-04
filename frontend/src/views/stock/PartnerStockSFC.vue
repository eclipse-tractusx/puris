<template>
  <div v-if="this.selectedMaterialOrProductId === ''">
    <h2 class="text-center bold text-3xl">
      Your suppliers' stocks for no material.
    </h2>
  </div>
  <div v-else>
    <h2 class="text-center bold text-3xl">
      Your suppliers' stocks for
      {{ this.selectedMaterialOrProductId }}.
    </h2>
    <table class="">
      <tr class="text-left">
        <th>Supplier</th>
        <th>Quantity</th>
        <th>Last updated on</th>
      </tr>
      <tr v-for="material in availableMaterials" :key="material.id">
        <td>{{ material.supplierName }} ({{ material.supplierBpnl }})</td>
        <td>
          {{ material.quantity.number }} {{ material.quantity.unitOfMeasure }}
        </td>
        <td>{{ material.lastUpdatedOn }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
export default {
  name: "PartnerStockSFC",

  props: {
    selectedMaterialOrProductId: {required: true},
  },
  data() {
    return {
      availableMaterials: [],
    };
  },
  created() {
    if (this.selectedMaterialOrProductId !== "") {
      this.availableMaterials = this.getAvailableMaterials(
          this.selectedMaterialOrProductId
      );
    }
  },
  methods: {
    getAvailableMaterials(materialId) {
      console.log("MaterialId" + materialId);
      if (materialId === null) {
        return [];
      }
      if (materialId === "M4711") {
        return [
          {
            supplierBpnl: "BPNS123456789ZZ",
            supplierName: "Test Supplier 1",
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
            supplierBpnl: "BPNS123466789ZZ",
            supplierName: "Test Supplier 2",
            quantity: {
              number: 50,
              unitOfMeasure: "pcs",
            },
            lastUpdatedOn: "2023-03-04, 15:15",
          },
          {
            supplierBpnl: "BPNS123666789ZZ",
            supplierName: "Test Supplier 3",
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
  },
};
</script>

<style scoped></style>
