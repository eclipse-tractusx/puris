<!--
 Copyright (c) 2023 Volkswagen AG
 Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 Copyright (c) 2023 Contributors to the Eclipse Foundation

 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.

 This program and the accompanying products are made available under the
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
        <div class="text-gray-900 ">

            <!-- First content bubble-->
            <div class="grid bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto p-2.5 outerBox">
                <div class="mt-2">
                    <div>
                        <label for="dropdown-products" class="text-xl ">Products: </label>
                    </div>
                    <select v-model="dropdownProducts" id="dropdown-products" @change="getAllCustomers(dropdownProducts)" name="ddp" class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a product</option>
                        <option v-for="item in fetchedProducts" :value="item" >{{item.ownMaterialNumber + "  (" + item.description + ")"}}</option>
                    </select>
                </div>
                <div>
                    <div>
                        <label for="dropdown-customer" class="text-xl">Customer: </label>
                    </div>
                    <select v-model="dropdownCustomer" id="dropdown-customer" name="ddc" class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a customer</option>
                        <option v-for="item in fetchedCustomers" :value="item">{{item.name}}</option>
                    </select>
                </div>
                <div class="mt-2">
                    <div>
                        <label class="text-xl ">Location: </label>
                    </div>
                    <select v-model="dropdownBPNS" id="" name="ddbpns"  class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a site</option>
                        <option v-for="item in dropdownCustomer.sites" :value="item"> {{item.name}} </option>
                    </select>
                    <div class="mt-2">
                        <select v-model="dropdownBPNA" id="" name="ddbpna"  class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                            <option disabled value="" selected hidden>Choose an address</option>
                            <option v-for="item in dropdownBPNS.addresses" :value="item" @click="emptyTotalDemandArray()"> {{item.streetAndNumber + ", " + item.zipCodeAndCity}} </option>
                        </select>
                    </div>
                </div>
            </div>

            <!-- Second content bubble-->
            <div id="secondBubble" class="grid auto-cols-max bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto outerBox" >
                <div class="pb-2.5">
                    <button
                        class="float-right bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                        id="updateCustomerDataBtn"
                        type="submit"
                    >
                        Update Customer Data
                    </button>
                </div>
                <!-- Line seperator-
                <p class="border-b-gray-200 border-b"></p>
                -->

                <table>
                    <tr>
                        <td class="font-bold text-xl firstRow firstColumn">Customer Information</td>

                        <td v-for="day in datesData" :value="day" class="firstRow">{{day}}</td>

                    </tr>
                    <tr  id="demandActual">
                        <td class="firstColumn">Demand (Actual)</td>

                        <td v-for="item in dropdownBPNA.demandActual " :value="item">{{item}}</td>

                    </tr>
                    <tr id="demandAdditional">
                        <td class="firstColumn secondLastRow">Demand (Additional)</td>

                        <td v-for="item in dropdownBPNA.demandAdditional" :value="item" class="secondLastRow">{{item}}</td>

                    </tr>

                    <tr id="demandTotal">
                        <td class="firstColumn">Demand (Total)</td>

                        <td v-if="(dropdownBPNA.demandActual != null)"
                            v-for="(item,index) in (addDemands(dropdownBPNA))"
                            :ref="setTotalDemand(item,index)"
                            :value="item">{{item}}</td>
                    </tr>
                    <tr>
                        <td class="firstColumn">
                            Customer Stock:
                            <span>100</span>
                        </td>
                    </tr>
                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"> </td>
                    </tr>

                    <tr>
                        <td class="font-bold text-xl firstRow firstColumn ">Your Own Information</td>
                        <!-- upper border of production items -->
                        <td class="firstRow" v-for="item in datesData">
                            {{}}
                        </td>

                    </tr>

                    <tr id="production">
                        <td class="firstColumn ">Production</td>

                        <td v-for="(item, index) in dropdownBPNA.production"
                            :style="changeBgColor(index,item)">
                            {{item}}</td>
                    </tr>
                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"></td>
                    </tr>
                    <!-- -------------- -->
                    <tr>
                        <template v-if="dropdownBPNA.currentStock !== null">
                            <td class="text-center firstColumn">
                              Your Stock:
                              <span> {{dropdownBPNA.currentStock}} </span>
                            </td>
                        </template>
                        <!-- <td class=" text-center" colspan="">Test Number</td> -->

                    </tr>
                </table>

            </div>
        </div>
    </main>
</template>

<script>
import supplierDashboardMockData from "@/assets/supplierDashboardMockData.json";
export default{
  name: "SupplierDashboard",

  data() {
    return{
        // get environmental endpoints
        backendURL: import.meta.env.VITE_BACKEND_BASE_URL,
        backendApiKey: import.meta.env.VITE_BACKEND_API_KEY,
        endpointProducts: import.meta.env.VITE_ENDPOINT_PRODUCTS,
        endpointCustomers: import.meta.env.VITE_ENDPOINT_CUSTOMER,
        // Customer dropdown
        dropdownCustomer: "",
        // Material dropdown
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
        // Temp array for total demand
        totalDemand: [],
        // Customer Json
        customer: supplierDashboardMockData,
    };
  },
    mounted() {
      let currentDate = new Date();
      let updatedDate = new Date(currentDate.setDate(currentDate.getDate() + -1))

      for(let i=0;i<28;i++) {
          updatedDate = new Date(currentDate.setDate(currentDate.getDate() + 1))
          this.datesData[i] = updatedDate.getDate() + "." + (updatedDate.getMonth() + 1) + "." + updatedDate.getFullYear();
      }
      this.getAllProducts();
    },
    methods: {
    addDemands: function(productObject){
        var demandActual = productObject.demandActual;
        var demandAdditional = productObject.demandAdditional;
        let demandTotal = [];


        for (let i=0;i <demandActual.length;i++){
            if(demandAdditional[i] != undefined) {
                demandTotal.push(parseFloat(demandActual[i]) + parseFloat(demandAdditional[i]));
            } else {
                demandTotal.push(parseFloat(demandActual[i]));
            }
        }
        return demandTotal;
    },
    changeBgColor: function (index, production){
        if (production < this.totalDemand[index]){
            return {'background-color': 'red'};
        }
    },
    setTotalDemand: function (el, index){
        if(el && this.totalDemand[index] == null) {
            this.totalDemand.push(el);
        }
    },
    emptyTotalDemandArray: function (){
        this.totalDemand.length = 0;
    },
    testMethode: function (){
        for(const test of this.fetchedProducts){
            console.log(test.ownMaterialNumber)

            fetch( "http://localhost:8081/catena/stockView/supplier?ownMaterialNumber=" + test.ownMaterialNumber, {
                headers: {
                    "X-API-KEY": "test",
                },
            })
                .then((res) => res.json())
                .then((data) => (this.fetchedCustomers = data) && console.log(data))
                .catch((err) => console.log(err));

        }
    },
    getAllProducts: function (){
        fetch( this.backendURL + this.endpointProducts, {
            headers: {
                "X-API-KEY": this.backendApiKey,
            },
        })
            .then((res) => res.json())
            .then((data) => (this.fetchedProducts = data) && console.log(data))
            .catch((err) => console.log(err));
    },
    getAllCustomers: function (productNumber){
        console.log(productNumber)
        fetch( this.backendURL + this.endpointCustomers + productNumber.ownMaterialNumber,{
            headers: {
                "X-API-KEY": this.backendApiKey,
            },
        })
            .then((res) => res.json())
            .then((data) => (this.fetchedCustomers = data) && console.log(data))
            .catch((err) => console.log(err));
    },
  }
};

</script>

<style scoped>
#updateCustomerDataBtn {
    position: sticky;
    margin-top: 10px;
    right: 0;
    top: 0;

}
#showBtn{

}
th, td {
    padding: 10px;
    text-align: center;
}
div > .outerBox{
    margin: 20px 0px;
}
td.firstRow{
    font-weight: bold;
    border-bottom: 1px solid rgb(229 231 235 / var(--tw-border-opacity));;
}
.firstColumn{
    font-weight: bold;
    background-color: rgb(255 255 254 / var(--tw-border-opacity));
    border-right: 1px solid rgb(229 231 235 / var(--tw-border-opacity));
    position: sticky;
    left: 0;
    z-index: 1;
}
#secondBubble{
    padding-right: 10px;
}
#secondBubble:hover .firstColumn{
    background-color: rgb(244 244 246);
}
table {
    border-collapse: collapse;
}
.secondLastRow{
    border-bottom: 1px solid rgb(229 231 235 / var(--tw-border-opacity));;
}

</style>
