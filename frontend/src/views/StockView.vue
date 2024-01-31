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
    <main class="flex flex-col mb-7">
        <header class="banner">
            <div class="banner-text">
                IMPORTANT: Please note that the data shown may be <b>competitively sensitive</b> and, according to appliable
                antitrust laws,<b> must not </b>be shared with competitors. Please consult your legal department, if necessary.
            </div>
        </header>
        <h1 class="w-full text-center bold text-5xl mb-6 pb-6">
            View and Manage Stocks
        </h1>

        <div class="divide-y-4">
            <form
                @submit.prevent="addOrUpdateStock(this.changedStock)"
                class="flex flex-row"
            >
                <!-- First Column -->
                <div class="basis-1/2 mr-28 flex flex-col">
                    <div class="flex flex-row justify-start space-x-3">
                        <div class="space-x-2">
                            <input
                                type="radio"
                                v-model="stockType"
                                value="Material"
                                @change="toggleMaterialOrProduct"
                            />
                            <label>Material</label>
                        </div>

                        <div class="space-x-2">
                            <input
                                type="radio"
                                v-model="stockType"
                                value="Product"
                                @change="toggleMaterialOrProduct"
                            />
                            <label>Product</label>
                        </div>
                    </div>
                    <div>
                        <DisableableSelectInput
                            id="materialSelect"
                            label="Material *"
                            :value="changedStock.materialId"
                            :disabled="changedStock.type === 'Product'"
                            :options="bdMaterials"
                            @input="onMaterialChange"
                            :required="stockType === 'Material'"
                        />
                    </div>
                    <div>
                        <DisableableSelectInput
                            id="productSelect"
                            label="Product *"
                            :value="changedStock.productId"
                            :disabled="changedStock.type === 'Material'"
                            :options="bdProducts"
                            @input="onProductChange"
                            :required="stockType === 'Product'"
                        />
                    </div>
                    <div>
                        <label for="allocatedToPartner">
                            Allocated to Partner *
                        </label>
                        <select
                            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                            id="allocatedToPartner"
                            v-model="this.changedStock.partnerBpnl"
                            required
                            :disabled="this.changedStock.materialId === '' && this.changedStock.productId === ''"
                        >
                            <option
                                v-for="option in partnerOptions"
                                :value="option.value"
                                :key="option.value"
                            >
                                {{ option.label }}
                            </option>
                        </select>
                    </div>
                    <div class="flex flex-row">
                        <div class="flex flex-col basis-2/3 mr-4">
                            <label for="Quantity">Quantity *</label>
                            <input
                                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                                type="number"
                                id="quantityInput"
                                v-model="this.changedStock.quantity"
                                required
                            />
                        </div>
                        <div class="flex flex-col basis-1/3">
                            <label for="measurementUnit">UOM *</label>
                            <select
                                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                                id="measurementUnit"
                                v-model="this.changedStock.measurementUnit"
                                required
                            >
                                <option
                                    v-for="item in unitsOfMeasureJson"
                                    :value="item.key"
                                    :key="item.key"
                                >
                                    {{ item.value }}
                                </option>
                            </select>
                        </div>
                    </div>

                    <div class="text-center">
                        <button class="btn-primary" id="stockBtn">
                            Add or Update
                        </button>
                    </div>
                </div>

                <!-- Second Column -->
                <div class="basis-1/2 flex flex-col justify-end">
                    <div>
                        <input
                            type="checkbox"
                            id="isBlocked"
                            name="isBlockedCheckbox"
                            class="mr-2"
                            v-model="this.changedStock.isBlocked"
                        />
                        <label for="isBlocked">Is Blocked</label>
                    </div>
                    <div>
                        <label for="stockLocationBPNS">
                            Stock Location BPNS *
                        </label>
                        <select
                            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                            id="stockLocationBPNS"
                            v-model="this.changedStock.bpns"
                            required
                        >
                            <option
                                v-for="site in bdSitesWithAddresses"
                                :value="site"
                                :key="site.bpns"
                            >
                                {{ site.bpns }} - {{ site.name }}
                            </option>
                        </select>
                    </div>
                    <div>
                        <label for="stockLocationBPNA">
                            Stock Location BPNA *
                        </label>
                        <select
                            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                            id="stockLocationBPNA"
                            v-model="this.changedStock.bpna"
                            required
                        >
                            <option
                                v-for="address in this.changedStock.bpns
                                    .addresses"
                                :value="address.bpna"
                                :key="address.bpna"
                            >
                                {{ address.bpna }} -
                                {{ address.streetAndNumber }} -
                                {{ address.zipCodeAndCity }}
                            </option>
                        </select>
                    </div>
                    <div class="flex flex-row justify-between">
                        <div class="grow mr-4">
                            <label for="customerOrderNumber">
                                Customer Order Number
                            </label>
                            <input
                                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                                id="customerOrderNumber"
                                v-model="this.changedStock.customerOrderNumber"
                            />
                        </div>
                        <div class="grow">
                            <label for="customerOrderPositionNumber">
                                Customer Order Position Number
                            </label>
                            <input
                                class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                                id="customerOrderPositionNumber"
                                v-model="
                                    this.changedStock
                                        .customerOrderPositionNumber
                                "
                                :required="hasCustomerOrderNumber"
                                :disabled="!hasCustomerOrderNumber"
                            />
                        </div>
                    </div>
                    <div>
                        <label for="supplierOrderNumber">
                            Supplier Order Number
                        </label>
                        <input
                            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
                            id="supplierOrderNumber"
                            v-model="this.changedStock.supplierOrderNumber"
                            :disabled="!hasCustomerOrderNumber"
                        />
                    </div>
                    <!-- Empty div -->
                    <div style="height: 54.5px"></div>
                </div>
            </form>

            <!-- separator -->
            <div
                id="materialAndProductStockTables"
                class="flex flex-col space-y-2 max-w-max divide-y-4"
            >
                <StockTableSFC
                    title="Material Stocks"
                    :stocks="this.bdMaterialStocks"
                    :ownRole="'customer'"
                    :partnerRole="'supplier'"
                />
                <StockTableSFC
                    title="Product Stocks"
                    :stocks="this.bdProductStocks"
                    :ownRole="'supplier'"
                    :partnerRole="'customer'"
                />
            </div>
        </div>
    </main>
</template>

<script>
import StockTableSFC from "@/views/stock/StockTableSFC.vue";
import stockViewUom from "@/assets/stockViewUom.json";
import DisableableSelectInput from "@/components/DisableableSelectInput.vue";

export default {
    name: "StockView",
    components: { DisableableSelectInput, StockTableSFC },
    data() {
        return {
            backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            endpointMaterials: import.meta.env.VITE_ENDPOINT_MATERIALS,
            endpointProducts: import.meta.env.VITE_ENDPOINT_PRODUCTS,
            endpointMaterialStocks: import.meta.env
                .VITE_ENDPOINT_MATERIAL_STOCKS,
            endpointProductStocks: import.meta.env.VITE_ENDPOINT_PRODUCT_STOCKS,
            endpointCustomer: import.meta.env.VITE_ENDPOINT_CUSTOMER,
            endpointSupplier: import.meta.env.VITE_ENDPOINT_SUPPLIER,
            endpointBPNS: import.meta.env.VITE_ENDPOINT_PARTNER_OWNSITES,
            bdMaterials: [],
            bdProducts: [],
            bdMaterialStocks: [],
            bdProductStocks: [],
            bdCustomers: [],
            bdSuppliers: [],
            bdSitesWithAddresses: [],
            changedStock: {
                materialId: "",
                productId: "",
                type: "Material",
                quantity: "",
                measurementUnit: "",
                partnerBpnl: "",
                isBlocked: false,
                bpns: "",
                bpna: "",
                customerOrderNumber: null,
                customerOrderPositionNumber: null,
                supplierOrderNumber: null,
            },
            unitsOfMeasureJson: stockViewUom,
        };
    },
    computed: {
        stockType: {
            get() {
                return this.changedStock.type;
            },
            set(value) {
                this.changedStock.type = value;
            },
        },
        partnerOptions() {
            if (this.changedStock.type === "Product") {
                return this.bdCustomers.map((customer) => ({
                    value: customer.bpnl,
                    label: customer.name,
                }));
            } else {
                return this.bdSuppliers.map((supplier) => ({
                    value: supplier.bpnl,
                    label: supplier.name,
                }));
            }
        },
        hasCustomerOrderNumber() {
            return !!this.changedStock.customerOrderNumber;
        },
    },
    watch: {
        "changedStock.customerOrderNumber"(newVal) {
            if (newVal === "") {
                this.changedStock.customerOrderNumber = null;
                this.changedStock.customerOrderPositionNumber = null;
                this.changedStock.supplierOrderNumber = null;
            }
        },
        "changedStock.customerOrderPositionNumber"(newVal) {
            if (newVal === "") {
                this.changedStock.customerOrderPositionNumber = null;
            }
        },
        "changedStock.supplierOrderNumber"(newVal) {
            if (newVal === "") {
                this.changedStock.supplierOrderNumber = null;
            }
        },
    },
    mounted() {
        fetch(this.backendURL + this.endpointMaterials, {
            headers: {
                "X-API-KEY": this.backendApiKey,
            },
        })
            .then((res) => res.json())
            .then((data) => (this.bdMaterials = data))
            .catch((err) => console.log(err));

        fetch(this.backendURL + this.endpointProducts, {
            headers: {
                "X-API-KEY": this.backendApiKey,
            },
        })
            .then((res) => res.json())
            .then((data) => (this.bdProducts = data))
            .catch((err) => console.log(err));

        fetch(this.backendURL + this.endpointBPNS, {
            headers: {
                "X-API-KEY": this.backendApiKey,
            },
        })
            .then((res) => res.json())
            .then((data) => {
                this.bdSitesWithAddresses = data;
            })
            .catch((err) => console.log(err));

        this.fetchMaterialStocks();

        this.fetchProductStocks();
    },
    methods: {
        addOrUpdateStock(changedStock) {
            if (changedStock.type === "Material") {
                let existingMaterialStocks = this.bdMaterialStocks.filter(
                    (stock) =>
                        stock.material.materialNumberCustomer ===
                        this.changedStock.materialId &&
                        stock.partner.bpnl === this.changedStock.partnerBpnl &&
                        stock.stockLocationBpns ===
                        this.changedStock.bpns.bpns &&
                        stock.stockLocationBpna === this.changedStock.bpna &&
                        stock.isBlocked === this.changedStock.isBlocked &&
                        stock.customerOrderNumber ===
                        this.changedStock.customerOrderNumber &&
                        stock.customerOrderPositionNumber ===
                        this.changedStock.customerOrderPositionNumber &&
                        stock.supplierOrderNumber ===
                        this.changedStock.supplierOrderNumber
                );

                if (existingMaterialStocks.length === 1) {
                    // Update existing material stock
                    let existingMaterialStock = existingMaterialStocks[0];
                    existingMaterialStock.quantity = this.changedStock.quantity;
                    existingMaterialStock.measurementUnit =
                        this.changedStock.measurementUnit;

                    this.putData(
                        this.backendURL + this.endpointMaterialStocks,
                        existingMaterialStock,
                        () => {
                            this.fetchMaterialStocks();
                        }
                    );
                } else {
                    // Create new material stock
                    // 1. Determine product
                    const existingMaterial = this.bdMaterials.filter(
                        (m) =>
                            m.ownMaterialNumber === this.changedStock.materialId
                    )[0];

                    // 2. Determine partner
                    const existingSupplier = this.bdSuppliers.filter(
                        (s) => s.bpnl === this.changedStock.partnerBpnl
                    )[0];

                    // 3. Create Stock
                    const newStock = {
                        material: {
                            materialNumberCustomer:
                            existingMaterial.ownMaterialNumber,
                        },
                        quantity: this.changedStock.quantity,
                        measurementUnit: this.changedStock.measurementUnit,
                        partner: existingSupplier,
                        type: "MATERIAL",
                        stockLocationBpna: this.changedStock.bpna,
                        stockLocationBpns: this.changedStock.bpns.bpns,
                        isBlocked: this.changedStock.isBlocked,
                        customerOrderNumber:
                        this.changedStock.customerOrderNumber,
                        customerOrderPositionNumber:
                        this.changedStock.customerOrderPositionNumber,
                        supplierOrderNumber:
                        this.changedStock.supplierOrderNumber,
                    };

                    var newMaterialStock = JSON.parse(JSON.stringify(newStock));

                    this.postData(
                        this.backendURL + this.endpointMaterialStocks,
                        newMaterialStock,
                        () => {
                            this.fetchMaterialStocks();
                        }
                    );
                }
            } else if (changedStock.type === "Product") {
                let existingProductStocks = this.bdProductStocks.filter(
                    (stock) =>
                        stock.material.materialNumberSupplier ===
                        this.changedStock.productId &&
                        stock.partner.bpnl === this.changedStock.partnerBpnl &&
                        stock.isBlocked === this.changedStock.isBlocked &&
                        stock.stockLocationBpns ===
                        this.changedStock.bpns.bpns &&
                        stock.stockLocationBpna === this.changedStock.bpna &&
                        stock.customerOrderNumber ===
                        this.changedStock.customerOrderNumber &&
                        stock.customerOrderPositionNumber ===
                        this.changedStock.customerOrderPositionNumber &&
                        stock.supplierOrderNumber ===
                        this.changedStock.supplierOrderNumber
                );

                if (existingProductStocks.length === 1) {
                    // Update existing product stock
                    let existingProductStock = existingProductStocks[0];
                    existingProductStock.quantity = this.changedStock.quantity;
                    existingProductStock.measurementUnit =
                        this.changedStock.measurementUnit;

                    this.putData(
                        this.backendURL + this.endpointProductStocks,
                        existingProductStock,
                        () => {
                            this.fetchProductStocks();
                        }
                    );
                } else {
                    // Create new product stock
                    // 1. Determine product
                    const existingProduct = this.bdProducts.filter(
                        (p) =>
                            p.ownMaterialNumber === this.changedStock.productId
                    )[0];

                    // 2. Determine partner
                    const existingCustomer = this.bdCustomers.filter(
                        (c) => c.bpnl === this.changedStock.partnerBpnl
                    )[0];
                    console.log(existingCustomer);

                    // 3. Create Stock
                    const newStock = {
                        material: {
                            materialNumberSupplier:
                            existingProduct.ownMaterialNumber,
                        },
                        quantity: this.changedStock.quantity,
                        measurementUnit: this.changedStock.measurementUnit,
                        partner: existingCustomer,
                        type: "PRODUCT",
                        stockLocationBpna: this.changedStock.bpna,
                        stockLocationBpns: this.changedStock.bpns.bpns,
                        isBlocked: this.changedStock.isBlocked,
                        customerOrderNumber:
                        this.changedStock.customerOrderNumber,
                        customerOrderPositionNumber:
                        this.changedStock.customerOrderPositionNumber,
                        supplierOrderNumber:
                        this.changedStock.supplierOrderNumber,
                    };

                    const newProductStock = JSON.parse(
                        JSON.stringify(newStock)
                    );

                    this.postData(
                        this.backendURL + this.endpointProductStocks,
                        newProductStock,
                        () => {
                            this.fetchProductStocks();
                        }
                    );
                }
            }
        },
        toggleMaterialOrProduct() {
            if (this.stockType === "Material") {
                this.changedStock.productId = "";
            } else if (this.stockType === "Product") {
                this.changedStock.materialId = "";
            }
        },
        fetchMaterialStocks() {
            fetch(this.backendURL + this.endpointMaterialStocks, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.bdMaterialStocks = data))
                .then(() => console.info(this.bdMaterialStocks))
                .catch((err) => console.log(err));
        },
        fetchProductStocks() {
            fetch(this.backendURL + this.endpointProductStocks, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.bdProductStocks = data))
                .catch((err) => console.log(err));
        },
        putData(address, data, callback = null) {
            fetch(address, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "X-API-KEY": this.backendApiKey,
                },
                body: JSON.stringify(data),
            })
                .then((res) => res.json())
                .then((data) => console.log(data))
                .then((data) => {
                    if (callback && typeof callback === "function") {
                        callback(data);
                    }
                })
                .catch((err) => console.log(err));
        },
        postData(address, data, callback = null) {
            fetch(address, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-API-KEY": this.backendApiKey,
                },
                body: JSON.stringify(data),
            })
                .then((res) => res.json())
                .then((data) => console.log(data))
                .then((data) => {
                    if (callback && typeof callback === "function") {
                        callback(data);
                    }
                })
                .catch((err) => console.log(err));
        },
        onProductChange(productId) {
            // block events emitted
            if (typeof productId !== "string") {
                return;
            }

            this.changedStock.productId = productId;

            fetch(this.backendURL + this.endpointCustomer + productId, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.bdCustomers = data))
                .catch((err) => console.log(err));
        },
        onMaterialChange(materialId) {
            // block events emitted
            if (typeof materialId !== "string") {
                return;
            }

            this.changedStock.materialId = materialId;

            fetch(this.backendURL + this.endpointSupplier + materialId, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.bdSuppliers = data))
                .catch((err) => console.log(err));
        },
    },
};
</script>

<style scoped>
.banner {
    justify-content: center;
    background-color: burlywood;
    width: 100%;
    margin-bottom: 40px;
}

.banner-text {
    text-align: center;
    padding: 10px;
    color: red;
}
</style>
