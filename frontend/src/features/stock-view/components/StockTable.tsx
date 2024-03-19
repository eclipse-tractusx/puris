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

import { Table } from '@catena-x/portal-shared-components';
import { Stock, StockType } from '@models/types/data/stock';
import { capitalize } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';

type StockTableProps = {
    type: StockType;
    stocks: Stock[];
    onSelection: (stock: Stock) => void;
};

const createStockTableColumns = (type: StockType) => [
    {
        field: 'materialNumber',
        headerName: capitalize(type),
        renderCell: (params: { row: Stock }) => (
            <div className="flex flex-col">
                <span>{params.row.material?.name}</span>
                <span>({type === 'material' ? params.row.material?.materialNumberCustomer : params.row.material?.materialNumberSupplier})</span>
            </div>
        ),
        flex: 4,
    },
    {
        field: 'quantity',
        valueGetter: (data: { row: Stock }) => data.row.quantity + ' ' + getUnitOfMeasurement(data.row.measurementUnit),
        headerName: 'Quantity',
        flex: 2,
    },
    {
        field: 'partner',
        renderCell: (params: { row: Stock }) => (
            <div className="flex flex-col">
                <span>{params.row.partner?.name}</span>
                <span>({params.row.partner?.bpnl})</span>
            </div>
        ),
        headerName: 'Allocated to Partner',
        flex: 4,
    },
    {
        field: 'isBlocked',
        headerName: 'Blocked',
        flex: 2,
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
        field: 'customerOrderNumber',
        headerName: 'Customer Order Number',
        flex: 3,
    },
    {
        field: 'customerOrderPositionNumber',
        headerName: 'Customer Order Position',
        flex: 3,
    },
    {
        field: 'supplierOrderNumber',
        headerName: 'Supplier Order Number',
        flex: 3,
    },
];
export const StockTable = ({ onSelection, type, stocks }: StockTableProps) => {
    return (
        <Table
            title="Your Stocks"
            columns={createStockTableColumns(type)}
            rows={stocks ?? []}
            noRowsMsg='No stocks available'
            getRowId={(row) => row.uuid}
            onRowClick={(e) => onSelection(e.row)}
            hideFooter={true}
        ></Table>
    );
}
