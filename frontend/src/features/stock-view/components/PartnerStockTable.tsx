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

import { LoadingButton, Table } from '@catena-x/portal-shared-components';
import { MaterialStock, ProductStock } from '@models/types/data/stock';
import { usePartnerStocks } from '../hooks/usePartnerStocks';
import { getUnitOfMeasurement } from '@util/helpers';
import { refreshPartnerStocks } from '@services/stocks-service';
import { useState } from 'react';

type PartnerStockTableProps = {
    type: 'material' | 'product';
    materialNumber?: string | null;
};

const partnerStockTableColumns = [
    {
        field: 'partner',
        valueGetter: (data: { row: MaterialStock | ProductStock }) => data.row.partner?.name,
        headerName: 'Partner',
        flex: 5,
    },
    {
        field: 'quantity',
        valueGetter: (data: { row: MaterialStock | ProductStock }) =>
            data.row.quantity + ' ' + getUnitOfMeasurement(data.row.measurementUnit),
        headerName: 'Quantity',
        flex: 3,
    },
    {
        field: 'stockLocationBpns',
        headerName: 'BPNS',
        flex: 3,
    },
    {
        field: 'stockLocationBpna',
        headerName: 'BPNA',
        flex: 3,
    },
    {
        field: 'customerOrder',
        renderCell: (params: { row: MaterialStock | ProductStock }) => (
            <div className="flex flex-col">
                <span>{params.row.customerOrderNumber}</span>
                <span>{params.row.customerOrderPositionNumber}</span>
            </div>
        ),
        headerName: 'Customer Order',
        flex: 3,
    },
    {
        field: 'supplierOrderNumber',
        headerName: 'Supplier Order Number',
        flex: 3,
    },
    {
        field: 'lastUpdatedOn',
        valueGetter: (data: { row: MaterialStock }) => new Date(data.row.lastUpdatedOn)?.toLocaleString(),
        headerName: 'Last Updated On',
        flex: 3,
    },
];

export const PartnerStockTable = ({ materialNumber, type }: PartnerStockTableProps) => {
    const { partnerStocks } = usePartnerStocks(type, materialNumber);
    const [refreshing, setRefreshing] = useState(false);

    const handleStockRefresh = () => {
        setRefreshing(true);
        refreshPartnerStocks(type, materialNumber ?? null).then(()=>setRefreshing(false));
    };
    return (
        <div className="relative">
            <Table
                title={`Your ${type === 'material' ? 'Customers' : 'Suppliers'}' Stocks ${materialNumber ? `for Material ${materialNumber}` : ''}`}
                noRowsMsg={
                    type === 'material'
                        ? 'Select a Material to show your suppliers stocks'
                        : 'Select a Product to show your suppliers stocks'
                }
                columns={partnerStockTableColumns}
                rows={partnerStocks ?? []}
                getRowId={(row) => row.uuid}
                hideFooter={true}
            ></Table>
            <LoadingButton label='Refresh Stocks' loadIndicator='refreshing...' loading={refreshing} className="absolute top-8 end-8" variant="contained" onButtonClick={() => handleStockRefresh()} />
        </div>
    );
};
