/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

import { Input, Table } from '@catena-x/portal-shared-components';
import { useState } from 'react';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import Autocomplete from '@mui/material/Autocomplete';

type Customer = {
    name: string;
    materials: Material[];
};

type Material = {
    name: string;
    demandActual: number[];
    demandAdditional: number[];
    production: number[];
};

const mockCustomerDemands: Customer[] = [
    {
        name: 'Customer 1',
        materials: [
            {
                name: 'Central Control Unit',
                demandActual: [
                    398, 23, 183, 53, 341, 492, 282, 80, 48, 199, 417, 223, 242, 263, 262, 185, 313, 78, 209, 405, 7, 134, 362, 196, 247,
                    248, 336, 302,
                ],
                demandAdditional: [],
                production: [
                    398, 23, 183, 53, 341, 492, 282, 80, 48, 199, 417, 223, 242, 263, 262, 185, 313, 78, 209, 405, 7, 134, 362, 196, 247,
                    248, 336, 302,
                ],
            },
            {
                name: 'Steering Wheel',
                demandActual: [
                    342, 294, 48, 32, 243, 180, 113, 395, 5, 477, 223, 31, 193, 418, 472, 338, 45, 219, 149, 324, 92, 28, 129, 481, 235,
                    348, 132, 259,
                ],
                demandAdditional: [],
                production: [
                    342, 294, 48, 32, 243, 180, 113, 395, 5, 477, 223, 31, 193, 418, 472, 338, 45, 219, 149, 324, 92, 28, 129, 481, 235,
                    348, 132, 259,
                ],
            },
            {
                name: 'Wheel',
                demandActual: [
                    311, 152, 173, 496, 418, 17, 79, 267, 22, 426, 103, 396, 469, 362, 299, 112, 105, 180, 141, 1, 133, 9, 476, 93, 118,
                    373, 394, 376,
                ],
                demandAdditional: [],
                production: [
                    311, 152, 173, 496, 418, 17, 79, 267, 22, 426, 103, 396, 469, 362, 299, 112, 105, 180, 141, 1, 133, 9, 476, 93, 118,
                    373, 394, 376,
                ],
            },
        ],
    },
    {
        name: 'Customer 2',
        materials: [
            {
                name: 'Central Control Unit',
                demandActual: [
                    399, 238, 16, 187, 317, 496, 134, 189, 264, 15, 357, 203, 322, 388, 1, 65, 423, 441, 119, 28, 417, 460, 218, 129, 217,
                    5, 63, 198,
                ],
                demandAdditional: [
                    100, 50, 75, 0, 150, 500, 0, 20, 150, 0, 175, 200, 0, 40, 100, 50, 75, 0, 150, 500, 0, 20, 150, 0, 175, 200, 0, 40,
                ],
                production: [
                    499, 288, 66, 187, 467, 996, 134, 209, 314, 15, 532, 403, 322, 248, 101, 115, 498, 441, 269, 528, 417, 460, 218, 129,
                    392, 205, 63, 198,
                ],
            },
            {
                name: 'Steering Wheel',
                demandActual: [
                    299, 252, 313, 63, 497, 35, 351, 426, 419, 86, 127, 374, 6, 66, 120, 82, 89, 286, 162, 327, 454, 500, 98, 10, 140, 415,
                    368, 178,
                ],
                demandAdditional: [
                    100, 0, 50, 200, 150, 0, 75, 100, 0, 50, 200, 150, 0, 75, 100, 0, 50, 200, 150, 0, 75, 100, 0, 50, 200, 150, 0, 75,
                ],
                production: [
                    399, 252, 313, 263, 647, 35, 426, 526, 419, 136, 327, 374, 6, 141, 220, 82, 139, 486, 312, 327, 454, 600, 98, 60, 340,
                    565, 368, 253,
                ],
            },
            {
                name: 'Seats',
                demandActual: [
                    100, 200, 834, 325, 989, 442, 121, 609, 964, 789, 331, 923, 22, 315, 947, 956, 732, 422, 878, 425, 562, 737, 370, 904,
                    727, 706, 823, 459,
                ],
                demandAdditional: [
                    22, 300, 0, 200, 50, 350, 150, 100, 300, 400, 200, 50, 350, 150, 100, 300, 400, 200, 50, 350, 150, 100, 300, 400, 200,
                    50, 350, 150,
                ],
                production: [
                    122, 500, 940, 237, 977, 626, 915, 196, 749, 382, 48, 982, 95, 14, 831, 23, 542, 142, 10, 664, 333, 731, 611, 797, 366,
                    485, 732, 357,
                ],
            },
        ],
    },
];

const dateColumns = [
    {
        field: 'name',
        headerName: '',
        width: 180,
    },
    ...[
        'Tue, 01.08.2023',
        'Wed, 02.08.2023',
        'Thu, 03.08.2023',
        'Fr, 04.08.2023',
        'Sa, 05.08.2023',
        'Su, 06.08.2023',
        'Mo, 07.08.2023',
        'Tue, 08.08.2023',
        'Wed, 09.08.202',
        'Thu, 10.08.2023',
        'Fr, 11.08.2023',
        'Sa, 12.08.2023',
        'Su, 13.08.2023',
        'Mo, 14.08.2023',
        'Tue, 15.08.2023',
        'Wed, 16.08.2023',
        'Thu, 17.08.2023',
        'Fr, 18.08.2023',
        'Sa, 19.08.2023',
        'So, 20.08.2023',
        'Mo, 21.08.2023',
        'Tue, 22.08.2023',
        'Wed, 23.08.2023',
        'Thu, 24.08.2023',
        'Fr, 25.08.2023',
        'Sa, 26.08.2023',
        'So, 27.08.2023',
        'Mo, 28.08.2023',
    ].map((date, index) => ({
        field: index.toString(),
        headerName: date,
        renderCell: (data: { row: { name: string; material?: Material } & { [key: string]: number } }) => {
            const isInsufficientProduction =
                data.row.id === 4 &&
                data.row.material &&
                data.row[index] < data.row.material.demandActual[index] + (data.row.material.demandAdditional[index] ?? 0);
            return (
                <span className={`grid place-items-center w-full h-full ${isInsufficientProduction ? ' text-red-400' : ''}`}>
                    {data.row[index]}
                </span>
            );
        },
    })),
];

const createTableRows = (material: Material) => {
    return [
        {
            id: 1,
            name: 'Demand (Actual)',
            ...material.demandActual.reduce((acc, value, index) => ({ ...acc, [index]: value }), {}),
        },
        {
            id: 2,
            name: 'Demand (Additional)',
            ...material.demandAdditional.reduce((acc, value, index) => ({ ...acc, [index]: value }), {}),
        },
        {
            id: 3,
            name: 'Demand (Total)',
            ...material.demandActual.reduce(
                (acc, value, index) => ({ ...acc, [index]: value + (material.demandAdditional[index] ?? 0) }),
                {}
            ),
        },
        {
            id: 4,
            material,
            name: 'Your Production',
            ...material.production.reduce((acc, value, index) => ({ ...acc, [index]: value }), {}),
        },
    ];
};

export const SupplierDashboardView = () => {
    const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
    const [selectedMaterial, setSelectedMaterial] = useState<Material | null>(null);
    const handleCustomerSelect = (customer: Customer | null) => {
        setSelectedCustomer(customer);
        setSelectedMaterial(null);
    };
    return (
        <div className="flex flex-col items-center w-full h-full">
            <ConfidentialBanner />
            <h1 className="text-3xl font-semibold text-gray-700">Supplier Dashboard</h1>
            <div className="flex gap-2 w-[64rem] mb-10">
                <Autocomplete
                    id="customer"
                    className="w-1/2"
                    value={selectedCustomer}
                    options={mockCustomerDemands ?? []}
                    getOptionLabel={(option) => option.name}
                    renderInput={(params) => <Input {...params} label="Customer*" placeholder="Select a Customer" />}
                    onChange={(_, newValue) => handleCustomerSelect(newValue)}
                />
                <Autocomplete
                    id="material"
                    className="w-1/2"
                    value={selectedMaterial}
                    options={selectedCustomer?.materials ?? []}
                    getOptionLabel={(option) => option.name}
                    renderInput={(params) => <Input {...params} label="Material*" placeholder="Select a Material" />}
                    onChange={(_, newValue) => setSelectedMaterial(newValue)}
                />
            </div>
            <div className="w-full overflow-x-auto">
                <Table
                    title={`Customer Demand ${selectedMaterial ? `for ${selectedMaterial.name}` : ''}`}
                    noRowsMsg="Select a Material to show the customer demand"
                    columns={dateColumns}
                    rows={selectedMaterial ? createTableRows(selectedMaterial) : []}
                    getRowId={(row) => row.id}
                    hideFooter={true}
                />
            </div>
        </div>
    );
}
