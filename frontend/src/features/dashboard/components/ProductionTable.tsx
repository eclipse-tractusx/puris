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

const createProductionRows = (numberOfDays: number, stocks: Stock[], site: Site) => {
    const shipments = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => ({ ...acc, [index]: index % 3 === 1 ? 90 : 0 }), {}),
    };
    const production = { ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => ({ ...acc, [index]: 30 }), {}) };
    const stockQuantity = stocks.filter((stock) => stock.stockLocationBpns === site.bpns).reduce((acc, stock) => acc + stock.quantity, 0);
    const allocatedStocks = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce(
            (acc, _, index) => ({
                ...acc,
                [index]:
                    index === 0
                        ? stockQuantity
                        : acc[(index - 1) as keyof typeof acc] -
                          shipments[index as keyof typeof shipments] +
                          production[(index - 1) as keyof typeof production],
            }),
            {}
        ),
    };
    return [
        { id: 'shipment', name: 'Shipments', ...shipments },
        { id: 'itemStock', name: 'Item Stock', ...allocatedStocks },
        { id: 'plannedProduction', name: 'Planned Production', ...production },
    ];
};

type ProductionTableProps = { numberOfDays: number; stocks: Stock[] | null; site: Site, onDeliveryClick: (delivery: Delivery) => void };

export const ProductionTable = ({ numberOfDays, stocks, site, onDeliveryClick }: ProductionTableProps) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleDeliveryClick = (cellData: any) => {
        if (cellData.id !== 'shipment') return;
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
                <Typography variant="caption1" component="h3" fontWeight={600}>
                    Site:{' '}
                </Typography>{' '}
                {site.name} ({site.bpns})
            </Box>
            <TableWithRowHeader
                title=""
                noRowsMsg="Select a Site to show production data"
                columns={createDateColumnHeaders(numberOfDays)}
                onCellClick={handleDeliveryClick}
                rows={site ? createProductionRows(numberOfDays, stocks ?? [], site) : []}
                getRowId={(row) => row.id}
                hideFooter={true}
            />
        </>
    );
};
