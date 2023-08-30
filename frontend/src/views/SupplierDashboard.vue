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
    <main>
        <div class="text-gray-900 ">

            <!-- First content bubble-->
            <div class="grid bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto p-2.5 outerBox">
                <div>
                    <div>
                        <label for="dropdown-customer" class="text-xl">Customer: </label>
                    </div>
                    <select v-model="dropdownCustomer" id="dropdown-customer" name="ddc" class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a customer</option>
                        <option v-for="item in customer" :value="item">{{item.name}}</option>
                    </select>
                </div>
                <div class="mt-2">
                    <div>
                        <label for="dropdown-material" class="text-xl ">Material: </label>
                    </div>
                    <select v-model="dropdownMaterial" id="dropdown-material" name="ddm"  class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option disabled value="" selected hidden>Choose a material</option>
                        <option v-for="item in dropdownCustomer.materials " :value="item" @click="emptyTotalDemandArray()">{{item.name}}</option>
                    </select>
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

                        <td v-for="item in dropdownMaterial.demandActual " :value="item">{{item}}</td>

                    </tr>
                    <tr id="demandAdditional">
                        <td class="firstColumn secondLastRow">Demand (Additional)</td>

                        <td v-for="item in dropdownMaterial.demandAdditional" :value="item" class="secondLastRow">{{item}}</td>

                    </tr>

                    <tr id="demandTotal">
                        <td class="firstColumn">Demand (Total)</td>

                        <td v-if="(dropdownMaterial.demandActual != null)"
                            v-for="(item,index) in (addDemands(dropdownMaterial))"
                            :ref="setTotalDemand(item,index)"
                            :value="item">{{item}}</td>
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

                        <td v-for="(item, index) in dropdownMaterial.production"
                            :style="changeBgColor(index,item)">
                            {{item}}</td>
                    </tr>
                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"></td>
                    </tr>

                    <tr>
                        <template v-for="material in currentStocks">
                            <td class="text-center firstColumn" v-if="material.name === dropdownMaterial.name"> <b>Your Stock:</b> &ensp;{{material.value}} </td>
                        </template>
                        <!-- <td class=" text-center" colspan="">Test Number</td> -->

                    </tr>
                </table>

            </div>
        </div>
    </main>
</template>

<script>

export default{
  name: "SupplierDashboard",

  data() {
    return{
        // Customer dropdown
        dropdownCustomer: "",
        // Material dropdown
        dropdownMaterial: "",
        // Dates
        datesData: [],
        // Temp array for total demand
        totalDemand: [],
        // Customer Json
        customer: {
            customer1: {
                name: "Customer 1",
                materials: {
                    centralControlUnit: {
                        name: "Central Control Unit",
                        demandActual: [398,23,183,53,341,492,282,80,48,199,417,223,242,263,262,185,313,78,209,405,7,134,362,196,247,248,336,302],
                        demandAdditional: [],
                        production: [398,23,183,53,341,492,282,80,48,199,417,223,242,263,262,185,313,78,209,405,7,134,362,196,247,248,336,302],
                    },
                    steeringWheel: {
                        name: "Steering Wheel",
                        demandActual: [342,294,48,32,243,180,113,395,5,477,223,31,193,418,472,338,45,219,149,324,92,28,129,481,235,348,132,259],
                        demandAdditional: [],
                        production: [342,294,48,32,243,180,113,395,5,477,223,31,193,418,472,338,45,219,149,324,92,28,129,481,235,348,132,259],
                    },
                    wheel: {
                        name: "Wheel",
                        demandActual: [311,152,173,496,418,17,79,267,22,426,103,396,469,362,299,112,105,180,141,1,133,9,476,93,118,373,394,376],
                        demandAdditional: [],
                        production: [311,152,173,496,418,17,79,267,22,426,103,396,469,362,299,112,105,180,141,1,133,9,476,93,118,373,394,376],
                    }
                }
            },
            customer2: {
                name: "Customer 2",
                materials: {
                    centralControlUnit: {
                        name: "Central Control Unit",
                        demandActual: [399,238,16,187,317,496,134,189,264,15,357,203,322,388,1,65,423,441,119,28,417,460,218,129,217,5,63,198],
                        demandAdditional: [100,50,75,0,150,500,0,20,150,0,175,200,0,40,100,50,75,0,150,500,0,20,150,0,175,200,0,40],
                        production: [499,288,66,187,467,996,134,209,314,15,532,403,322,248,101,115,498,441,269,528,417,460,218,129,392,205,63,198],
                    },
                    steeringWheel: {
                        name: "Steering Wheel",
                        demandActual: [299,252,313,63,497,35,351,426,419,86,127,374,6,66,120,82,89,286,162,327,454,500,98,10,140,415,368,178],
                        demandAdditional: [100,0,50,200,150,0,75,100,0,50,200,150,0,75,100,0,50,200,150,0,75,100,0,50,200,150,0,75],
                        production: [399,252,313,263,647,35,426,526,419,136,327,374,6,141,220,82,139,486,312,327,454,600,98,60,340,565,368,253 ],
                    },
                    seats: {
                        name: "Seats",
                        demandActual:     [100,200,834,325,989,442,121,609,964,789,331,923,22,315,947,956,732,422,878,425,562,737,370,904,727,706,823,459],
                        demandAdditional: [22,300,0,200,50,350,150,100,300,400,200,50,350,150,100,300,400,200,50,350,150,100,300,400,200,50,350,150],
                        production:       [122,500,940,237,977,626,915,196,749,382,48,982,95,14,831,23,542,142,10,664,333,731,611,797,366,485,732,357],
                    }
                }
            },
        },
        currentStocks:{
            centralControlUnit: {
                value: 300,
                name: "Central Control Unit",
            },
            steeringWheel: {
                value: 200,
                name: "Steering Wheel",
            },
            wheel: {
                value: 100,
                name: "Wheel",
            },
            seats: {
                value: 400,
                name: "Seats",
            },
        },
    };
  },
    mounted() {
        setTimeout(()=> this.datesData = ["Tue, 01.08.2023", "Wed, 02.08.2023","Thu, 03.08.2023","Fr, 04.08.2023", "Sa, 05.08.2023", "Su, 06.08.2023", "Mo, 07.08.2023",
        "Tue, 08.08.2023", "Wed, 09.08.202", "Thu, 10.08.2023", "Fr, 11.08.2023", "Sa, 12.08.2023", "Su, 13.08.2023", "Mo, 14.08.2023", "Tue, 15.08.2023", "Wed, 16.08.2023",
        "Thu, 17.08.2023","Fr, 18.08.2023", "Sa, 19.08.2023", "So, 20.08.2023", "Mo, 21.08.2023", "Tue, 22.08.2023", "Wed, 23.08.2023", "Thu, 24.08.2023", "Fr, 25.08.2023",
        "Sa, 26.08.2023","So, 27.08.2023", "Mo, 28.08.2023"])
    },
    methods: {
    addDemands: function(materialObject){
        var demandActual = materialObject.demandActual;
        var demandAdditional = materialObject.demandAdditional;
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
