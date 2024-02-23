<!--
 Copyright (c) 2022,2024 Volkswagen AG
 Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2022,2024 Contributors to the Eclipse Foundation

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
    <main>
        <header class="banner">
            <div class="banner-text">
                IMPORTANT: Please note that the data shown may be <b>competitively sensitive</b> and, according to
                appliable
                antitrust laws,<b> must not </b>be shared with competitors. Please consult your legal department, if
                necessary.
            </div>
        </header>
        <div class="text-gray-900 ">

            <!-- First content bubble-->
            <div
                class="grid bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto p-2.5 outerBox">
                <div class="mt-2">
                    <div>
                        <label for="dropdown-products" class="text-xl ">Products: </label>
                    </div>
                    <select v-model="dropdownProducts" id="dropdown-products"
                            @change="getAllCustomers(dropdownProducts);getCustomerStocks(dropdownProducts);resetDropdownsFromProducts();hoverUpdateCustomerDataButton = false"
                            name="ddp"
                            class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a product</option>
                        <option v-for="item in fetchedProducts" :value="item">
                            {{ item.ownMaterialNumber + "  (" + item.description + ")" }}
                        </option>
                    </select>
                </div>
                <div>
                    <div>
                        <label for="dropdown-customer" class="text-xl">Customer: </label>
                    </div>
                    <select v-model="dropdownCustomer" id="dropdown-customer" @change="resetDropdownsFromCustomer()"
                            :disabled="dropdownProducts === ''"
                            class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a customer</option>
                        <option v-for="item in fetchedCustomers" :value="item">{{ item.name }}</option>
                    </select>
                </div>
                <div class="mt-2">
                    <div>
                        <label class="text-xl ">Location: </label>
                    </div>
                    <select v-model="dropdownBPNS" id="dropdown-bpns" @change="resetDropdownsFromBpns()"
                            :disabled="dropdownCustomer === ''"
                            class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a site</option>
                        <option v-for="item in dropdownCustomer.sites" :value="item"> {{ item.name }}</option>
                    </select>
                    <div class="mt-2">
                        <select v-model="dropdownBPNA" id="dropdown-bpna" :disabled="dropdownBPNS === ''"
                                class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                            <option disabled value="" selected hidden>Choose an address</option>
                            <option value="allAddresses" @click="emptyTotalDemandArray();aggregateAllAddressesData()">
                                All addresses
                            </option>
                            <option v-for="(item,index) in dropdownBPNS.addresses" :value="item"
                                    @click="emptyTotalDemandArray();mockDataInput(index)">
                                {{ item.streetAndNumber + ", " + item.zipCodeAndCity }}
                            </option>
                        </select>
                        <div v-if="hoverUpdateCustomerDataButton && dropdownProducts === ''"
                             class="float-right font-bold"
                             id="hintMessage"
                        >
                            You need to select a product first !
                        </div>
                    </div>
                </div>
            </div>

            <!-- Second content bubble-->
            <div id="secondBubble"
                 class="grid auto-cols-max bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto outerBox">
                <div class="pb-2.5">
                    <button
                        class="float-right bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                        id="updateCustomerDataBtn"
                        type="submit"
                        @click="updateCustomerStocks(dropdownProducts)"
                        :disabled="dropdownProducts === ''"
                        @mouseover="hoverUpdateCustomerDataButton = true"
                        @mouseleave="hoverUpdateCustomerDataButton = false"
                    >
                        Update Customer Data
                    </button>
                </div>
                <!-- Line seperator-
                <p class="border-b-gray-200 border-b"></p>
                -->
                <table>
                    <tr>
                        <th class="text-xl firstRow firstColumn">Customer Information</th>

                        <th v-for="day in datesData" :value="day" class="firstRow">{{ day }}</th>

                    </tr>
                    <tr id="demandActual">
                        <td class="firstColumn">Demand (Actual)</td>

                        <td v-for="item in mockDemandActual" :value="item">{{ item }}</td>

                    </tr>
                    <tr id="demandAdditional">
                        <td class="firstColumn secondLastRow">Demand (Additional)</td>

                        <td v-for="item in mockDemandAdditional" :value="item" class="secondLastRow">{{ item }}</td>

                    </tr>

                    <tr id="demandTotal">
                        <td class="firstColumn">Demand (Total)</td>

                        <td v-for="(item) in (addDemands())"
                            :value="item">{{ item }}
                        </td>
                    </tr>
                    <tr>
                        <td class="firstColumn">
                            Customer Stock:
                            <template v-if="dropdownBPNA !== ''">
                                <span>
                                    {{ filterCustomerStocks(dropdownCustomer.bpnl, dropdownBPNS.bpns, dropdownBPNA) }}
                                </span>
                            </template>
                        </td>
                    </tr>
                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"></td>
                    </tr>

                    <tr>
                        <th class="text-xl firstRow firstColumn ">Your Own Information</th>
                        <!-- upper border of production items -->
                        <th class="firstRow" v-for="item in datesData">
                            {{}}
                        </th>

                    </tr>

                    <tr id="production">
                        <td class="firstColumn ">Production</td>

                        <td v-for="(item, index) in mockProduction"
                            :style="changeBgColor(index,item)">
                            {{ item }}
                        </td>
                    </tr>
                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"></td>
                    </tr>
                    <!-- -------------- -->
                    <tr>
                        <td class="text-center firstColumn">
                            Your Stock:
                            <template v-if="dropdownBPNA !== ''">
                              <span>
                                  {{ filterAllProductStocks(dropdownProducts.ownMaterialNumber, dropdownCustomer.bpnl) }}
                              </span>
                            </template>
                        </td>
                        <!-- <td class=" text-center" colspan="">Test Number</td> -->

                    </tr>
                </table>

            </div>
        </div>
    </main>
</template>

<script>
import supplierDashboardMockData from "@/assets/supplierDashboardMockData.json";
import UnitOfMeasureUtils from "@/services/UnitOfMeasureUtils";

export default {
    name: "SupplierDashboard",

    data() {
        return {
            hoverUpdateCustomerDataButton: false,
            // get environmental endpoints
            backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            endpointProducts: import.meta.env.VITE_ENDPOINT_PRODUCTS,
            endpointCustomers: import.meta.env.VITE_ENDPOINT_CUSTOMER,
            endpointAllProductStocks: import.meta.env.VITE_ENDPOINT_PRODUCT_STOCKS,
            endpointReportedProductStocks: import.meta.env.VITE_ENDPOINT_REPORTED_PRODUCT_STOCKS,
            endpointUpdateReportedProductStocks: import.meta.env
                .VITE_ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS,
            // Customer dropdown
            dropdownCustomer: "",
            // Product dropdown
            dropdownProducts: "",
            // BPNS dropdown
            dropdownBPNS: "",
            // BPNA dropdown
            dropdownBPNA: "",
            // Dates
            datesData: [],
            // fetched Materials
            fetchedProducts: [],
            // fetched Customers
            fetchedCustomers: [],
            // fetched Customer Stocks
            fetchedCustomerStocks: [],
            // fetched all Product Stocks
            fetchedAllProductStocks: [],
            // Mockdata Json
            mockData: supplierDashboardMockData,
            // Mockdata temp Arrays
            mockDemandActual: [],
            mockDemandAdditional: [],
            mockTotalDemand: [],
            mockProduction: [],
        };
    },
    mounted() {
        let currentDate = new Date();
        let updatedDate;
        currentDate.setDate(currentDate.getDate() - 1)

        for (let i = 0; i < 28; i++) {
            updatedDate = new Date(currentDate.setDate(currentDate.getDate() + 1))
            this.datesData[i] = updatedDate.getDate() + "." + (updatedDate.getMonth() + 1) + "." + updatedDate.getFullYear();
        }
        this.getAllProducts();
        this.getAllProductStocks();
    },
    methods: {
        addDemands: function () {
            for (let i = 0; i < this.mockDemandActual.length; i++) {
                if (this.mockDemandAdditional[i] != undefined) {
                    this.mockTotalDemand[i] = (parseFloat(this.mockDemandActual[i]) + parseFloat(this.mockDemandAdditional[i]));
                } else {
                    this.mockTotalDemand[i] = (parseFloat(this.mockDemandActual[i]));
                }
            }
            return this.mockTotalDemand;
        },
        aggregateAllAddressesData: function () {
            var optionQuantityDdBpna = document.getElementById("dropdown-bpna").options.length - 2; // Minus 2 because of 'Choose an address' and 'All addresses'
            this.mockDemandActual = []
            this.mockDemandAdditional = []
            this.mockProduction = []

            for (let i = 0; i < optionQuantityDdBpna; i++) {
                for (let j = 0; j < this.mockData[i].demandActual.length; j++) {
                    // Check if mockDemandActual is empty
                    if (isNaN(this.mockDemandActual[j])) {
                        // yes --> give value by setting it from mockData.demandActual
                        this.mockDemandActual[j] = this.mockData[i].demandActual[j];
                    } else {
                        // no --> just add new value on top of old value (cant add with empty Array because of NaN)
                        this.mockDemandActual[j] += this.mockData[i].demandActual[j];
                    }
                    // Check if mockDemandAdditional is empty
                    if (isNaN(this.mockDemandAdditional[j])) {
                        // yes --> give value by setting it from mockData.demandAdditional
                        this.mockDemandAdditional[j] = this.mockData[i].demandAdditional[j];
                    } else {
                        /*
                          no --> just add new value on top of old value (cant add with empty Array because of NaN),
                          but only when mockData.demandAdditional isnt empty itself
                        */
                        if (!isNaN(this.mockData[i].demandAdditional[j])) {
                            this.mockDemandAdditional[j] += this.mockData[i].demandAdditional[j];
                        }
                    }
                    // Check if mockProduction is empty
                    if (isNaN(this.mockProduction[j])) {
                        // yes --> give value by setting it from mockData.production
                        this.mockProduction[j] = this.mockData[i].production[j];
                    } else {
                        // no --> just add new value on top of old value (cant add with empty Array because of NaN)
                        this.mockProduction[j] += this.mockData[i].production[j];
                    }
                }
            }
        },
        changeBgColor: function (index, production) {
            if (production < this.mockTotalDemand[index]) {
                return {'background-color': 'red'};
            }
        },
        emptyTotalDemandArray: function () {
            this.mockTotalDemand.length = 0;
        },
        mockDataInput: function (index) {
            this.mockDemandActual = this.mockData[index].demandActual;
            this.mockDemandAdditional = this.mockData[index].demandAdditional;
            this.mockProduction = this.mockData[index].production;
        },
        getAllProducts: function () {
            fetch(this.backendURL + this.endpointProducts, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.fetchedProducts = data))
                .catch((err) => console.log(err));
        },
        getAllCustomers: function (productNumber) {
            fetch(this.backendURL + this.endpointCustomers + productNumber.ownMaterialNumber, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.fetchedCustomers = data))
                .catch((err) => console.log(err));
        },
        getCustomerStocks: function (productNumber) {
            fetch(this.backendURL + this.endpointReportedProductStocks + productNumber.ownMaterialNumber, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.fetchedCustomerStocks = data))
                .catch((err) => console.log(err));
        },
        filterCustomerStocks: function (bpnl, bpns, bpna) {
            let filteredStocksMap = new Map();
            let string = "";

            for (let i = 0; i < this.fetchedCustomerStocks.length; i++) {
                // Filter through this.fetchedCustomerStocks where bpnl,bpns and bpna are equal to the chosen dropdowns
                if (bpnl === this.fetchedCustomerStocks[i].partner.bpnl && bpns === this.fetchedCustomerStocks[i].stockLocationBpns && (bpna.bpna === this.fetchedCustomerStocks[i].stockLocationBpna || bpna === "allAddresses")) {
                    var uom = this.getUomValueForUomKey(this.fetchedCustomerStocks[i].measurementUnit)

                    // Check if Unitofmeasure already exists
                    // If yes then delete the old (key,value) an add the value to the new (key,value)
                    if (filteredStocksMap.has(uom)) {
                        var value = parseFloat(filteredStocksMap.get(uom)) + parseFloat(this.fetchedCustomerStocks[i].quantity)
                        filteredStocksMap.set(uom, value)
                        // If no then just add (key,value)
                    } else {
                        filteredStocksMap.set(uom, this.fetchedCustomerStocks[i].quantity)
                    }
                }
            }
            for (let [key, value] of filteredStocksMap.entries()) {
                string += value + " " + key + ", "
            }

            return string.slice(0, -2);
        },
        updateCustomerStocks: function (material) {
            fetch(this.backendURL +
                this.endpointUpdateReportedProductStocks +
                material.ownMaterialNumber,
                {
                    headers: {
                        "X-API-KEY": this.backendApiKey,
                    },
                }
            )
                .then((res) => res.json())
                .then((data) => console.log(data))
                .catch((err) => console.log(err));
        },
        getAllProductStocks: function () {
            fetch(this.backendURL + this.endpointAllProductStocks, {
                headers: {
                    "X-API-KEY": this.backendApiKey,
                },
            })
                .then((res) => res.json())
                .then((data) => (this.fetchedAllProductStocks = data))
                .catch((err) => console.log(err));
        },
        filterAllProductStocks: function (material, bpnl) {
            let filteredStocksMap = new Map();
            let string = "";
            for (let i = 0; i < this.fetchedAllProductStocks.length; i++) {
                // Filter through this.fetchedAllProductStocks where bpnl and material are equal to the chosen dropdowns
                if (bpnl === this.fetchedAllProductStocks[i].partner.bpnl && material === this.fetchedAllProductStocks[i].material.materialNumberSupplier) {
                    var uom = this.getUomValueForUomKey(this.fetchedAllProductStocks[i].measurementUnit)

                    // Check if Unitofmeasure already exists
                    // If yes then delete the old (key,value) an add the value to the new (key,value)
                    if (filteredStocksMap.has(uom)) {
                        var value = parseFloat(filteredStocksMap.get(uom)) + parseFloat(this.fetchedAllProductStocks[i].quantity)
                        filteredStocksMap.set(uom, value)
                        // If no then just add (key,value)
                    } else {
                        filteredStocksMap.set(uom, this.fetchedAllProductStocks[i].quantity)
                    }
                }
            }
            for (let [key, value] of filteredStocksMap.entries()) {
                string += value + " " + key + ", "
            }

            return string.slice(0, -2);
        },
        getUomValueForUomKey(key) {
            return UnitOfMeasureUtils.findUomValueByKey(key);
        },
        resetDropdownsFromProducts() {
            this.dropdownCustomer = "";
            this.dropdownBPNS = "";
            this.dropdownBPNA = "";

            this.mockDemandActual = [];
            this.mockDemandAdditional = [];
            this.mockProduction = [];
            this.emptyTotalDemandArray();
        },
        resetDropdownsFromCustomer() {
            this.dropdownBPNS = "";
            this.dropdownBPNA = "";

            this.mockDemandActual = [];
            this.mockDemandAdditional = [];
            this.mockProduction = [];
            this.emptyTotalDemandArray()
        },
        resetDropdownsFromBpns() {
            this.dropdownBPNA = "";

            this.mockDemandActual = [];
            this.mockDemandAdditional = [];
            this.mockProduction = [];
            this.emptyTotalDemandArray()
        },
    }
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

#updateCustomerDataBtn, #hintMessage {
    position: sticky;
    margin-top: 10px;
    right: 0;
    top: 0;
}

#updateCustomerDataBtn:disabled {
    opacity: 0.6;
}

#updateCustomerDataBtn:disabled:hover {
    --tw-bg-opacity: 0.6;
    background-color: rgb(29 78 216 / var(--tw-bg-opacity));
}

th, td {
    padding: 10px;
    text-align: center;
}

div > .outerBox {
    margin: 20px 0px;
}

th.firstRow {
    font-weight: bold;
    border-bottom: 1px solid rgb(229 231 235 / var(--tw-border-opacity));;
}

.firstColumn {
    font-weight: bold;
    background-color: rgb(255 255 254 / var(--tw-border-opacity));
    border-right: 1px solid rgb(229 231 235 / var(--tw-border-opacity));
    position: sticky;
    left: 0;
    z-index: 1;
}

#secondBubble {
    padding-right: 10px;
}

#secondBubble:hover .firstColumn {
    background-color: rgb(244 244 246);
}

table {
    border-collapse: collapse;
}

.secondLastRow {
    border-bottom: 1px solid rgb(229 231 235 / var(--tw-border-opacity));;
}

</style>
