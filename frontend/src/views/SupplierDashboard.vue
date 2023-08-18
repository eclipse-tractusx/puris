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
                    <select  onload="addOptionToDropdown(dropdown-customer,dropdownCustomerData)" name="ddc" id="dropdown-customer" class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option value="Customer1">Customer 1</option>
                        <option value="Customer2">Customer 2</option>
                    </select>
                </div>
                <div class="mt-2">
                    <div>
                        <label for="dropdown-material" class="text-xl ">Material: </label>
                    </div>
                    <select onload="addOptionToDropdown(dropdown-material,dropdownMaterialData);" name="ddm" id="dropdown-material" class="w-60 py-2 px-4 bg-gray-200 text-gray-700 border border-gray-200 rounded focus:bg-white focus:outline-none focus:border-gray-500">
                        <option value="central-control-unit">Central Control Unit</option>
                        <option value="steering-wheel">Steering Wheel</option>
                        <option value="wheel">Wheel</option>
                    </select>
                    <button
                        class="mt-auto float-right bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                        id="showBtn"
                        type="submit"
                    >
                        Show
                    </button>
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

                        <td class="firstRow ">Tue, 01.08.2023</td>
                        <td class="firstRow">Wed, 02.08.2023</td>
                        <td class="firstRow">Thu, 03.08.2023</td>
                        <td class="firstRow">Fr, 04.08.2023</td>
                        <td class="firstRow">Sa, 05.08.2023</td>
                        <td class="firstRow">Su, 06.08.2023</td>
                        <td class="firstRow ">Mo, 07.08.2023</td>

                        <td class="firstRow">Tue, 08.08.2023</td>
                        <td class="firstRow">Wed, 09.08.2023</td>
                        <td class="firstRow">Thu, 10.08.2023</td>
                        <td class="firstRow">Fr, 11.08.2023</td>
                        <td class="firstRow">Sa, 12.08.2023</td>
                        <td class="firstRow ">Su, 13.08.2023</td>
                        <td class="firstRow">Mo, 14.08.2023</td>

                        <td class="firstRow">Tue, 15.08.2023</td>
                        <td class="firstRow">Wed, 16.08.2023</td>
                        <td class="firstRow">Thu, 17.08.2023</td>
                        <td class="firstRow">Fr, 18.08.2023</td>
                        <td class="firstRow ">Sa, 19.08.2023</td>
                        <td class="firstRow">So, 20.08.2023</td>
                        <td class="firstRow">Mo, 21.08.2023</td>

                        <td class="firstRow">Tue, 22.08.2023</td>
                        <td class="firstRow">Wed, 23.08.2023</td>
                        <td class="firstRow">Thu, 24.08.2023</td>
                        <td class="firstRow">Fr, 25.08.2023</td>
                        <td class="firstRow">Sa, 26.08.2023</td>
                        <td class="firstRow">So, 27.08.2023</td>
                        <td class="firstRow">Mo, 28.08.2023</td>

                    </tr>
                    <tr>
                        <td class="firstColumn">Demand (Actual)</td>

                        <td class="">100</td>
                        <td class="">300</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">50</td>
                        <td class="">350</td>
                        <td class="">150</td>

                        <td class="">100</td>
                        <td class="">300</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">50</td>
                        <td class="">350</td>
                        <td class="">150</td>

                        <td class="">100</td>
                        <td class="">300</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">50</td>
                        <td class="">350</td>
                        <td class="">150</td>

                        <td class="">100</td>
                        <td class="">300</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">50</td>
                        <td class="">350</td>
                        <td class="">150</td>
                    </tr>
                    <tr>
                        <td class="firstColumn secondLastRow">Demand (Additional)</td>

                        <td class=" secondLastRow">0</td>
                        <td class="secondLastRow">100</td>
                        <td class="secondLastRow">50</td>
                        <td class="secondLastRow">200</td>
                        <td class="secondLastRow">150</td>
                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">250</td>

                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">100</td>
                        <td class="secondLastRow">50</td>
                        <td class="secondLastRow">200</td>
                        <td class="secondLastRow">150</td>
                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">250</td>

                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">100</td>
                        <td class="secondLastRow">50</td>
                        <td class="secondLastRow">200</td>
                        <td class="secondLastRow">150</td>
                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">250</td>

                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">100</td>
                        <td class="secondLastRow">50</td>
                        <td class="secondLastRow">200</td>
                        <td class="secondLastRow">150</td>
                        <td class="secondLastRow">0</td>
                        <td class="secondLastRow">250</td>
                    </tr>
                    <tr>
                        <td class="firstColumn">Demand (Total)</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">400</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>
                    </tr>

                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"> </td>
                    </tr>

                    <tr>
                        <td class="font-bold text-xl firstRow firstColumn">Your Own Information</td>

                        <td class="firstRow "></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>

                        <td class="firstRow "></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>

                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>

                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                        <td class="firstRow"></td>
                    </tr>

                    <tr>
                        <td class="firstColumn ">Production</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">100</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">100</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">100</td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>

                        <td class="">100</td>
                        <td class="">400</td>
                        <td class="">450</td>
                        <td class="">100  </td>
                        <td class="">200</td>
                        <td class="">350</td>
                        <td class="">400</td>
                    </tr>

                    <!-- line separator -->
                    <tr>
                        <td class="firstColumn"></td>
                    </tr>

                    <tr>
                        <td class="text-center firstColumn"> <b>Your Stock:</b> &ensp; 300 pieces </td>

                        <!-- <td class=" text-center" colspan="">Test Number</td> -->

                    </tr>
                </table>

            </div>
        </div>
    </main>
</template>

<script setup>
/*
var dropdownCustomerData = ["Customer 1", "Customer 2"];
var dropdownMaterialData = ["Central Control Unit", "Steering Wheel", "Wheel"];

export default{
  name: "SupplierDashboard",

  props:{
    dropdownCustomerData: "Customer 1", "Customer 2",
    dropdownMaterialData: "Central Control Unit", "Steering Wheel" "Wheel"
  },
  data() {
    return{

    }
  },
  methods: {
    addOptionToDropdown(selectId, optionData) {
      for(const val of optionData){
        var option = document.createElement("option");
        option.value = val;
        option.text = val.charAt(0).toUpperCase() + val.slice(1);
        selectId.appendChild(option);
      }
    }
  }
}
 */
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
