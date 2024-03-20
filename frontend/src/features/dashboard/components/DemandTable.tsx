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

import { TableWithRowHeader } from '@components/TableWithRowHeader';
import { Stock } from '@models/types/data/stock';
import { Site } from '@models/types/edc/site';
import { createDateColumnHeaders } from '../util/helpers';
import { Box, Typography } from '@mui/material';
import { Delivery } from '@models/types/data/delivery';

const createDemandRows = (numberOfDays: number, stocks: Stock[], site: Site) => {
    const demands = { ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => ({ ...acc, [index]: 30 }), {}) };
    const deliveries = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => ({ ...acc, [index]: index % 3 === 0 ? 45 : 0 }), {}),
    };
    const currentStock = stocks.find((s) => s.stockLocationBpns === site.bpns)?.quantity ?? 0;
    const itemStock = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce(
            (acc, _, index) => ({
                ...acc,
                [index]:
                    (index === 0 ? currentStock : acc[(index - 1) as keyof typeof acc]) +
                    deliveries[index as keyof typeof deliveries] -
                    demands[index as keyof typeof demands],
            }),
            {}
        ),
    };
    const daysOfSupply = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce(
            (acc, _, index) => ({
                ...acc,
                [index]: Math.max(itemStock[index as keyof typeof itemStock] / demands[index as keyof typeof demands], 0).toFixed(2),
            }),
            {}
        ),
    };
    return [
        { id: 'demand', name: 'Demand', ...demands },
        { id: 'itemStock', name: 'Item Stock', ...itemStock },
        { id: 'daysOfSupply', name: 'Days of Supply', ...daysOfSupply },
        { id: 'delivery', name: 'Delivery', ...deliveries },
    ];
};

type DemandTableProps = { numberOfDays: number; stocks: Stock[] | null; site: Site; onDeliveryClick: (delivery: Delivery) => void };

export const DemandTable = ({ numberOfDays, stocks, site, onDeliveryClick }: DemandTableProps) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleDeliveryClick = (cellData: any) => {
        if (cellData.id !== 'delivery') return;
        if (cellData.value === 0) return;
        onDeliveryClick({
            quantity: cellData.value,
            etd: cellData.colDef.headerName,
            origin: {
                bpns: site?.bpns,
            },
            destination: {
                bpns: site?.bpns,
            },
        });
    };
    return (
        <>
            <Box display="flex" justifyContent="start" width="100%" gap="0.5rem" marginBlock="0.5rem" paddingLeft=".5rem">
                <Typography variant="caption1" component="h3" fontWeight={600}> Site: </Typography>
                {site.name} ({site.bpns})
            </Box>
            <TableWithRowHeader
                title=""
                noRowsMsg="Select a Material to show the customer demand"
                columns={createDateColumnHeaders(numberOfDays)}
                rows={createDemandRows(numberOfDays, stocks ?? [], site)}
                onCellClick={handleDeliveryClick}
                getRowId={(row) => row.id}
                hideFooter={true}
            />
        </>
    );
};
