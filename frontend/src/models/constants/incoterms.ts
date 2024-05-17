/*
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
*/

type Incoterm = {
    key: string;
    value: string;
    responsibility: 'customer' | 'partial' | 'supplier';
}

export const INCOTERMS: Incoterm[] = [
    {
        key: 'EXW',
        value: 'EX Works',
        responsibility: 'customer',
    },
    {
        key: 'FCA',
        value: 'Free Carrier',
        responsibility: 'partial',
    },
    {
        key: 'FAS',
        value: 'Free Alongside Ship',
        responsibility: 'partial',
    },
    {
        key: 'FOB',
        value: 'Free On Board',
        responsibility: 'partial',
    },
    {
        key: 'CFR',
        value: 'Cost and Freight',
        responsibility: 'partial',
    },
    {
        key: 'CIF',
        value: 'Cost Insurance Freight',
        responsibility: 'partial',
    },
    {
        key: 'DAP',
        value: 'Delivered At Place',
        responsibility: 'supplier',
    },
    {
        key: 'DPU',
        value: 'Delivered at Place Unloaded',
        responsibility: 'supplier',
    },
    {
        key: 'CPT',
        value: 'Carriage Paid To',
        responsibility: 'supplier',
    },
    {
        key: 'CIP',
        value: 'Carriage Insurance Paid',
        responsibility: 'supplier',
    },
    {
        key: 'DDP',
        value: 'Delivered Duty Paid',
        responsibility: 'supplier',
    },
];
