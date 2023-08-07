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
  <main class="flex flex-col">
    <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
      View and Manage Stocks
    </h1>

    <div>
      <form
          @submit.prevent="addOrUpdateStock(this.changedStock)"
          class="w-max max-w-lg"
      >
        <div class="flex flex-row justify-start space-x-3">
          <div class="space-x-2">
            <input
                type="radio"
                v-model="this.changedStock.type"
                value="Material"
                @change="toggleMaterialOrProduct(changedStock)"
            />
            <label>Material</label>
          </div>

          <div class="space-x-2">
            <input
                type="radio"
                v-model="this.changedStock.type"
                value="Product"
                @change="toggleMaterialOrProduct(changedStock)"
            />
            <label>Product</label>
          </div>
        </div>

        <div>
          <label for="material">Material</label>
          <select
              class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
              id="materialSelect"
              v-model="this.changedStock.materialId"
              :disabled="this.changedStock.type === 'Product'"
          >
            <option v-for="material in this.bdMaterials" :value="material.ownMaterialNumber">
              {{ material.ownMaterialNumber }} ({{ material.description }})
            </option>
          </select>
        </div>
        <div>
          <label for="product">Product</label>
          <select
              class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
              id="productSelect"
              v-model="this.changedStock.productId"
              :disabled="this.changedStock.type === 'Material'"
              @change="onProductChange(this.changedStock.productId)"
          >
            <option v-for="product in this.bdProducts" :value="product.ownMaterialNumber">
              {{ product.ownMaterialNumber }} ({{ product.description }})
            </option>
          </select>
        </div>
        <div>
          <label for="allocatedToCustomer">Allocated to Customer</label>
          <select
            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
            id="allocatedToCustomer"
            v-model="this.changedStock.allocatedToCustomer"
            :disabled="this.changedStock.type === 'Material'"
          >
            <option v-for="customer in this.bdCustomers" :value="customer.uuid">
              {{ customer.name }}
            </option>
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
              class="btn-primary"
              id="stockBtn"
          >
            Add or Update
          </button>
        </div>
      </form>

      <!-- separator -->
      <div id="materialAndProductStockTables" class="flex flex-col space-y-2 max-w-max">
        <StockTableSFC
            title="Material Stocks"
            :stocks="this.bdMaterialStocks"
            :partnerRole="'supplier'"
        />
        <StockTableSFC
            title="Product Stocks"
            :stocks="this.bdProductStocks"
            :partnerRole="'customer'"
        />
      </div>
    </div>
  </main>
</template>

<script>
import StockTableSFC from "@/views/stock/StockTableSFC.vue";

export default {
  name: "StockView",
  components: {StockTableSFC},

  data() {
    return {
      backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
      endpointMaterials: import.meta.env.VITE_ENDPOINT_MATERIALS,
      endpointProducts: import.meta.env.VITE_ENDPOINT_PRODUCTS,
      endpointMaterialStocks: import.meta.env.VITE_ENDPOINT_MATERIAL_STOCKS,
      endpointProductStocks: import.meta.env.VITE_ENDPOINT_PRODUCT_STOCKS,
      endpointCustomer: import.meta.env.VITE_ENDPOINT_CUSTOMER,
      bdMaterials: [],
      bdProducts: [],
      bdMaterialStocks: [],
      bdProductStocks: [],
      bdCustomers: [],
      changedStock: {
        materialId: "",
        productId: "",
        type: "Material",
        quantity: "",
        unitOfMeasure: "",
        allocatedToCustomer: "",
      },
      site: {
        bpns: "BPNS12345678910ZZZ",
        name: "Wolfsburg Hauptwerk",
      },
    };
  },
  mounted() {
    fetch(this.backendURL + this.endpointMaterials)
      .then(res => res.json())
      .then(data => this.bdMaterials = data)
      .catch(err => console.log(err));

    fetch(this.backendURL + this.endpointProducts)
      .then(res => res.json())
      .then(data => this.bdProducts = data)
      .catch(err => console.log(err));

    this.fetchMaterialStocks();

    this.fetchProductStocks();
  },
  methods: {
    addOrUpdateStock(changedStock) {
      if (changedStock.type === "Material") {
        var existingMaterialStocks = this.bdMaterialStocks.filter(
            (stock) => (stock.material.materialNumberCustomer === changedStock.materialId)
        );

        if (existingMaterialStocks.length === 1) { // Update existing material stock
          var existingMaterialStock = existingMaterialStocks[0];
          existingMaterialStock.quantity = changedStock.quantity;

          this.putData(this.backendURL + this.endpointMaterialStocks, existingMaterialStock);
          this.fetchMaterialStocks();
        } else { // Create new material stock
            // 1. Determine product
            var existingMaterial = this.bdMaterials.filter(
                (m) => m.materialNumberCustomer === changedStock.materialId
            )[0];

            // 2. Create Stock
            var newStock = {
                uuid: "",
                material: { materialNumberCustomer: existingMaterial.ownMaterialNumber } ,
                quantity: changedStock.quantity,
                unitOfMeasure: existingMaterial.unitOfMeasure,
                allocatedToCustomerPartner: existingCustomer,
                type: "MATERIAL",
                atSiteBpnl: this.site.bpns
            };

            var newMaterialStock = JSON.parse(JSON.stringify(newStock));

            this.postData(this.backendURL + this.endpointMaterialStocks, newMaterialStock);
            this.fetchMaterialStocks();
        }
      } else if (changedStock.type === "Product") {
        var existingProductStocks = this.bdProductStocks.filter(
            (stock) => (stock.material.materialNumberSupplier === changedStock.productId) &&
                (stock.allocatedToCustomerPartner.uuid === changedStock.allocatedToCustomer)
        );

        if (existingProductStocks.length === 1) { // Update existing product stock
          var existingProductStock = existingProductStocks[0];
            existingProductStock.quantity = changedStock.quantity;

          this.putData(this.backendURL + this.endpointProductStocks, existingProductStock);
          this.fetchProductStocks();
        } else { // Create new product stock
          // 1. Determine product
            var existingProduct = this.bdProducts.filter(
              (p) => p.ownMaterialNumber === changedStock.productId
          )[0];

          // 2. Determine partner
            var existingCustomer = this.bdCustomers.filter(
                (c) => c.uuid === changedStock.allocatedToCustomer
            )[0];

          // 3. Create Stock
          newStock = {
              uuid: "",
              material: {materialNumberSupplier: existingProduct.ownMaterialNumber},
              quantity: changedStock.quantity,
              unitOfMeasure: existingProduct.unitOfMeasure,
              allocatedToCustomerPartner: existingCustomer,
              type: "PRODUCT",
              atSiteBpnl: this.site.bpns
          };

          var newProductStock = JSON.parse(JSON.stringify(newStock));

          this.postData(this.backendURL + this.endpointProductStocks, newProductStock);
          this.fetchProductStocks();
        }
      }
    },
    toggleMaterialOrProduct(changedStock) {
      if (changedStock.type === "Material") {
        changedStock.productId = "";
      } else if (changedStock.type === "Product") {
        changedStock.materialId = "";
      }
    },
    fetchMaterialStocks(){
        fetch(this.backendURL + this.endpointMaterialStocks)
            .then(res => res.json())
            .then(data => this.bdMaterialStocks = data)
            .catch(err => console.log(err));
    },
    fetchProductStocks(){
        fetch(this.backendURL + this.endpointProductStocks)
            .then(res => res.json())
            .then(data => this.bdProductStocks = data)
            .catch(err => console.log(err));
    },
    putData(address, data) {
      fetch(address, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
        .then(res => res.json())
        .then(data => console.log(data))
        .catch(err => console.log(err));
    },
    postData(address, data) {
      fetch(address, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
        .then(res => res.json())
        .then(data => console.log(data))
        .catch(err => console.log(err));
    },
    onProductChange(productId) {
      fetch(this.backendURL + this.endpointCustomer + productId)
        .then(res => res.json())
        .then(data => this.bdCustomers = data)
        .catch(err => console.log(err));
    }
  }
};
</script>

<style scoped></style>
