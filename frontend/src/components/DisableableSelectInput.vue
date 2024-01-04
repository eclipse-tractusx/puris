<!--
 Copyright (c) 2024 Volkswagen AG
 Copyright (c) 2024 Contributors to the Eclipse Foundation

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
    <div>
        <label :for="id">{{ label }}</label>
        <select
            :id="id"
            class="appearance-none block w-full bg-gray-200 text-gray-700 border border-gray-200 rounded py-3 px-4 leading-tight focus:outline-none focus:bg-white focus:border-gray-500"
            :disabled="disabled"
            :value="value"
            @change="updateSelectedValue"
        >
            <option
                v-for="option in options"
                :value="option.value"
                :key="option.value"
            >
                {{ option.ownMaterialNumber }}
            </option>
        </select>
    </div>
</template>

<script>
export default {
    props: {
        id: {
            type: String,
            required: true,
        },
        label: {
            type: String,
            required: true,
        },
        value: {
            required: true,
        },
        disabled: {
            type: Boolean,
            default: false,
        },
        options: {
            type: Array,
            required: true,
        },
    },
    methods: {
        updateSelectedValue(event) {
            const newValue = event.target.value;
            if (newValue !== this.value) {
                this.$emit("input", newValue);
            }
        },
    },
};
</script>

<style scoped></style>
