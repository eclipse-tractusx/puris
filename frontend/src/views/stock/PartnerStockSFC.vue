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
    <div class="flex flex-col mt-6">
        <div class="flex flex-row justify-between items-center">
            <h3
                class="bold text-2xl"
                v-if="this.selectedMaterialOrProductId === ''"
            >
                Your {{ this.partnerRole }}s' stocks for no material.
            </h3>
            <h3 class="bold text-2xl" v-else>
                Your {{ this.partnerRole }}s' stocks for
                {{ this.selectedMaterialOrProductId }}.
            </h3>
            <button class="btn-primary"
                    @click="updateMaterialOrProduct()"
                    :disabled="this.selectedMaterialOrProductId === ''"
            >
                Update Partner Stocks
            </button>
        </div>
        <p v-if="this.partnerRole === 'supplier'">
            <i>
                Info: These are your suppliers' stocks (your potential inputs)
                at his site that have not yet been sent to you. <br>
                Please choose a material from above table before clicking the "Update Partner Stocks" button.
            </i>
        </p>
        <p v-else-if="this.partnerRole === 'customer'">
            <i>
                Info: These are your customers' stocks (your recent outputs) at
                his site that have not yet been used for production. <br>
                Please choose a material from above table before clicking the "Update Partner Stocks" button.
            </i>
        </p>
        <div class="overflow-x-auto min-h-60 max-h-80">
            <table class="mt-2 w-full">
                <thead>
                <tr class="text-left">
                    <th>Supplier</th>
                    <th>Quantity</th>
                    <th>Is Blocked</th>
                    <th>BPNS</th>
                    <th>BPNA</th>
                    <th>Last updated on</th>
                    <th>
                        Customer Order Number<br/>Customer Order Pos.
                        Number
                    </th>
                    <th>Supplier Order Number</th>
                </tr>
                </thead>
                <tbody>

                <template v-if="availableMaterialsOrProducts.length === 0">
                    <tr v-for="index in 3" :key="index" class="empty-row">
                        <td v-for="index in 8" :key="index"/>
                    </tr>
                </template>

                <template
                    v-else
                    v-for="stock in availableMaterialsOrProducts"
                    :key="stock.partner.bpnl"
                >
                    <tr>
                        <td>
                            {{ stock.partner.name }}<br/>({{
                                stock.partner.bpnl
                            }})
                        </td>
                        <td>
                            {{ stock.quantity }}
                            {{
                                getUomValueForUomKey(stock.measurementUnit)
                            }}
                        </td>
                        <td>{{ stock.isBlocked }}</td>
                        <td>{{ stock.stockLocationBpns }}</td>
                        <td>{{ stock.stockLocationBpna }}</td>
                        <td>{{ stock.lastUpdatedOn }}</td>
                        <td>
                            {{ stock.customerOrderNumber }}<br/>{{
                                stock.customerOrderPositionNumber
                            }}
                        </td>
                        <td>{{ stock.supplierOrderNumber }}</td>
                    </tr>
                </template>

                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
import UnitOfMeasureUtils from "@/services/UnitOfMeasureUtils";

export default {
    name: "PartnerStockSFC",

    props: {
        selectedMaterialOrProductId: {type: String, required: true},
        partnerRole: {type: String, required: true},
    },
    data() {
        return {
            backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
            backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
            endpointGetReportedMaterialStocks: import.meta.env
                .VITE_ENDPOINT_REPORTED_MATERIAL_STOCKS,
            endpointGetReportedProductStocks: import.meta.env
                .VITE_ENDPOINT_REPORTED_PRODUCT_STOCKS,
            endpointUpdateReportedMaterialStocks: import.meta.env
                .VITE_ENDPOINT_UPDATE_REPORTED_MATERIAL_STOCKS,
            availableMaterialsOrProducts: [],
            endpointUpdateReportedProductStocks: import.meta.env
                .VITE_ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS,
        };
    },
    created() {
        if (this.selectedMaterialOrProductId !== "") {
            if (this.partnerRole === "supplier") {
                this.getAvailableMaterials();
            } else if (this.partnerRole === "customer") {
                this.getAvailableProducts();
            }
        }
    },
    methods: {
        getAvailableMaterials() {
            fetch(
                this.backendURL +
                this.endpointGetReportedMaterialStocks +
                this.selectedMaterialOrProductId,
                {
                    headers: {
                        "X-API-KEY": this.backendApiKey,
                    },
                }
            )
                .then((res) => res.json())
                .then((data) => (this.availableMaterialsOrProducts = data))
                .catch((err) => console.log(err));
        },
        getAvailableProducts() {
            console.info(this.selectedMaterialOrProductId);
            fetch(
                this.backendURL +
                this.endpointGetReportedProductStocks +
                this.selectedMaterialOrProductId,
                {
                    headers: {
                        "X-API-KEY": this.backendApiKey,
                    },
                }
            )
                .then((res) => res.json())
                .then((data) => (this.availableMaterialsOrProducts = data))
                .catch((err) => console.log(err));
        },
        updateMaterialOrProduct() {
            if (this.partnerRole === "customer") {
                console.log("Fetching from customers");
                fetch(this.backendURL +
                    this.endpointUpdateReportedProductStocks +
                    this.selectedMaterialOrProductId,
                    {
                        headers: {
                            "X-API-KEY": this.backendApiKey,
                        },
                    }
                )
                    .then((res) => res.json())
                    .then((data) => console.log(data))
                    .catch((err) => console.log(err));
            }

            if (this.partnerRole == "supplier") {
                console.log("Fetching from suppliers");
                fetch(
                    this.backendURL +
                    this.endpointUpdateReportedMaterialStocks +
                    this.selectedMaterialOrProductId,
                    {
                        headers: {
                            "X-API-KEY": this.backendApiKey,
                        },
                    }
                )
                    .then((res) => res.json())
                    .then((data) => console.log(data))
                    .catch((err) => console.log(err));
            }
        },
        getUomValueForUomKey(key) {
            return UnitOfMeasureUtils.findUomValueByKey(key);
        },
    },
};
</script>

<style scoped>
.empty-row {
    height: 5vh;
}
</style>
