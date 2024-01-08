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
        <div class="">
            <h2 class="text-center bold text-3xl">{{ title }}</h2>
            <h3 class="bold text-2xl">Your Stocks</h3>
            <p v-if="this.ownRole === 'customer'">
                <i>
                    Info: These are your material stocks (your inputs) at your
                    site.
                    <b>
                        Please select one of the material stocks to see the
                        stocks your supplier still got on stock.
                    </b>
                </i>
            </p>
            <p v-if="this.ownRole === 'supplier'">
                <i>
                    Info: These are your product stocks (your outputs) at your
                    site.
                    <b>
                        Please select one of the product stocks to see the
                        stocks your customer still got on stock.
                    </b>
                </i>
            </p>
            <table class="w-full">
                <thead>
                    <tr class="text-left">
                        <th>Material (ID)</th>
                        <th>Quantity</th>
                        <th>Allocated to Partner</th>
                        <th>Is Blocked</th>
                        <th>BPNS</th>
                        <th>BPNA</th>
                        <th>
                            Customer Order Number<br />
                            Customer Order Pos. Number
                        </th>
                        <th>Supplier Order Number</th>
                    </tr>
                </thead>
                <tbody>
                    <template v-for="row in tableRows" :key="row.index">
                        <tr
                            :class="{
                                'empty-row': row.isEmpty,
                                highlight:
                                    !row.isEmpty &&
                                    row.index === this.selectedRowIndex,
                            }"
                            @click="row.isEmpty ? null : selectStock(row.index)"
                        >
                            <template v-if="row.isEmpty">
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                            </template>
                            <template v-else>
                                <td v-if="this.ownRole === 'customer'">
                                    {{ row.stock.material.name }}<br />({{
                                        row.stock.material
                                            .materialNumberCustomer
                                    }})
                                </td>
                                <td v-if="this.ownRole === 'supplier'">
                                    {{ row.stock.material.name }}<br />({{
                                        row.stock.material
                                            .materialNumberSupplier
                                    }})
                                </td>
                                <td>
                                    {{ row.stock.quantity }}
                                    {{
                                        getUomValueForUomKey(
                                            row.stock.measurementUnit
                                        )
                                    }}
                                </td>
                                <td>
                                    {{ row.stock.partner.name }}<br />({{
                                        row.stock.partner.bpnl
                                    }})
                                </td>
                                <td>{{ row.stock.isBlocked }}</td>
                                <td>{{ row.stock.stockLocationBpns }}</td>
                                <td>{{ row.stock.stockLocationBpna }}</td>
                                <td>
                                    {{ row.stock.customerOrderNumber }}<br />{{
                                        row.stock.customerOrderPositionNumber
                                    }}
                                </td>
                                <td>{{ row.stock.supplierOrderNumber }}</td>
                            </template>
                        </tr>
                    </template>
                </tbody>
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
    components: { PartnerStockSFC },

    props: {
        title: { type: String, required: true, default: "Stock" },
        stocks: { type: Array, required: true },
        ownRole: { type: String, required: true, default: "customer" },
        partnerRole: { type: String, required: true, default: "" },
    },

    data() {
        return {
            selectedMaterialId: "",
            selectedRowIndex: "",
            materialNumberCustomer: "",
        };
    },
    computed: {
        tableRows() {
            if (this.stocks.length === 0) {
                // Generate three empty rows
                return Array.from({ length: 3 }, (_, index) => ({
                    index,
                    isEmpty: true,
                }));
            } else {
                // Generate rows with data
                return this.stocks.map((stock, index) => ({
                    index,
                    stock,
                    isEmpty: false,
                }));
            }
        },
    },
    methods: {
        selectStock(rowIndex) {
            let materialId;
            if (this.ownRole === "customer") {
                materialId =
                    this.tableRows[rowIndex].stock.material
                        .materialNumberCustomer;
            } else if (this.ownRole === "supplier") {
                materialId =
                    this.tableRows[rowIndex].stock.material
                        .materialNumberSupplier;
            } else return;

            this.selectedMaterialId = materialId;
            this.selectedRowIndex = rowIndex;
        },
        getUomValueForUomKey(key) {
            return UnitOfMeasureUtils.findUomValueByKey(key);
        },
    },
};
</script>

<style scoped>
.highlight {
    background-color: orange;
}

.empty-row {
    height: 5vh;
}
</style>
