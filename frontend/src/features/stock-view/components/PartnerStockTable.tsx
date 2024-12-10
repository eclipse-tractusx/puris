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
import { Box, Button, Stack, Typography } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';

type PartnerStockTableProps<T extends StockType> = {
    type: T;
    materialName?: string | null;
    partnerStocks: Stock[];
    lastUpdated?: Date | null;
    onRefresh: () => void;
};

const partnerStockTableColumns = [
    {
        field: 'partner',
        valueGetter: (data: { row: Stock }) => data.row.partner?.name,
        headerName: 'Partner',
        flex: 5,
    },
    {
        field: 'quantity',
        valueGetter: (data: { row: Stock }) => data.row.quantity + ' ' + getUnitOfMeasurement(data.row.measurementUnit),
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
        renderCell: (params: { row: Stock }) => (
            <Stack>
                <Box>{params.row.customerOrderNumber}</Box>
                <Box>{params.row.customerOrderPositionNumber}</Box>
            </Stack>
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
        valueGetter: (data: { row: Stock }) => new Date(data.row.lastUpdatedOn)?.toLocaleString(),
        headerName: 'Last Updated On',
        flex: 3,
    },
];

export const PartnerStockTable = <T extends StockType>({
    type,
    materialName,
    partnerStocks,
    onRefresh,
    lastUpdated = null,
}: PartnerStockTableProps<T>) => {
    return (
        <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="end" spacing={1}>
                {lastUpdated && <Typography variant="body2">refresh requested at {lastUpdated.toLocaleTimeString()}</Typography>}
                <Button
                    onClick={() => onRefresh()}
                >Refresh Stocks</Button>
            </Stack>
            <Table
                title={`Your ${type === 'product' ? 'Customers' : 'Suppliers'}' Stocks ${materialName ? `for ${materialName}` : ''}`}
                noRowsMsg={
                    type === 'material'
                        ? 'Select a Material to show your suppliers stocks'
                        : 'Select a Product to show your suppliers stocks'
                }
                columns={partnerStockTableColumns}
                rows={partnerStocks ?? []}
                getRowId={(row) => row.uuid}
                hideFooter={true}
                sx={{flexGrow: 1, flexShrink: 1}}
            ></Table>
        </Stack>
    );
};
