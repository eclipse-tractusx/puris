<!--
 Copyright (c) 2022,2023 Volkswagen AG
 Copyright (c) 2022,2023 Contributors to the Eclipse Foundation

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
<script>
import aboutPage from '@/assets/aboutPage.json';

export default {
    data() {
        return {
            information: aboutPage,
        };
    },
};
</script>
<template>
    <main>
         <div class="text-gray-900 grid bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 dark:hover:bg-gray-700  w-[100%] overflow-auto p-2.5">
             <table>
                 <tr v-for="item in information" :id="item.header">
                     <td class="font-bold">{{item.header}}</td>
                     <td>
                         <a v-if="item.hasOwnProperty('link')" :href="item.link" id="fakelink">{{item.body}}</a>
                         <p v-else> {{item.body}}</p>
                     </td>
                 </tr>
             </table>
         </div>
    </main>
</template>
<style scoped>
#fakelink{
    color: #0000EE;
}
#fakelink:hover{
    color: #0000FF;
}
</style>
