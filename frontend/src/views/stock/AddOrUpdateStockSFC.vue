<template>
  <form @submit.prevent="addOrUpdateStock(this.changedStock)" class="w-max max-w-lg">

    <div class="flex flex-row justify-start space-x-3">

      <div class="space-x-2">
        <input type="radio" v-model="this.changedStock.type" value="Material"/>
        <label>Material</label>
      </div>

      <div class="space-x-2">
        <input type="radio" v-model="this.changedStock.type" value="Product"/>
        <label>Product</label>
      </div>
    </div>

    <div>
      <label for="material">Material</label>
      <select
          class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
          id="materialSelect"
          v-model="this.changedStock.materialId"
      >
        <option v-for="material in this.materials" :value="material.materialId">
          {{ material.materialId }} ({{ material.name }})
        </option>
      </select>
    </div>
    <div>
      <label for="product">Product</label>
      <select
          class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
          id="productSelect"
          v-model="this.changedStock.productId"
      >
        <option selected="selected">P4711 (VW Golf)</option>
      </select>
    </div>
    <div>
      <label for="Quantity">Quantity</label>
      <input
          class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
          type="number"
          id="quantityInput"
          v-model="this.changedStock.quantity"
      />
    </div>

    <div class="text-center">
      <button
          class="my-8 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
          id="stockBtn"
      >
        Add or Update
      </button>
    </div>
  </form>
</template>

<script>
export default {
  name: "AddOrUpdateStockSFC",

  props:{
    materials: {type: Array, required: true},
    materialStocks: {type: Array, required: true},
  },

  data() {
    return {
      changedStock: {
        materialId: "",
        productId: "",
        type: "",
        quantity: "",
        unitOfMeasure: "",
      },
    }
  },
  methods: {
    addOrUpdateStock(materialOrProduct) {
      console.log(materialOrProduct)
      if (typeof materialOrProduct.type === 'undefined' || materialOrProduct.type === 'Material') {
        var existingMaterialStock = this.materialStocks.filter((stock) => stock.materialId === materialOrProduct.materialId)

        console.log(existingMaterialStock)
        if (existingMaterialStock.length === 1) {
          console.log("Update existing stock")
          var material = existingMaterialStock[0]
          material.quantity = materialOrProduct.quantity;

        } else {
          var existingMaterial = this.materials.filter((m) => m.materialId === materialOrProduct.materialId)
          var newStock = {
            materialId: materialOrProduct.materialId,
            name: existingMaterial.name,
            quantity: materialOrProduct.quantity,
            unitOfMeasure: existingMaterial.unitOfMeasure,
          }
          console.log("Push new stock: ")
          console.log(newStock)
          this.materialStocks.push(newStock);
        }
      } else if (materialOrProduct.type === 'Produdct') {
        return null;
      }
    }
  }
}
</script>

<style scoped>

</style>