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
import { Box, Button, Stack, Typography } from '@mui/material';
import { Delivery } from '@models/types/data/delivery';
import { Add } from '@mui/icons-material';
import { Demand } from '@models/types/data/demand';
import { ModalMode } from '@models/types/data/modal-mode';

const createDemandRow = (numberOfDays: number, demands: Demand[]) => {
    return {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => {
            const date = new Date();
            date.setDate(date.getDate() + index);
            const demand = demands
                .filter((d) => new Date(d.day).toDateString() === date.toDateString())
                .reduce((sum, d) => sum + d.quantity, 0);
            return { ...acc, [index]: demand };
        }, {}),
    };
};

const createDeliveryRow = (numberOfDays: number, deliveries: Delivery[], site: Site) => {
    return {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => {
            const date = new Date();
            date.setDate(date.getDate() + index);
            const delivery = deliveries
                .filter((d) => new Date(d.dateOfArrival ?? Date.now()).toDateString() === date.toDateString() && d.destinationBpns === site.bpns)
                .reduce((sum, d) => sum + d.quantity, 0);
            return { ...acc, [index]: delivery };
        }, {}),
    };
}

const createTableRows = (numberOfDays: number, stocks: Stock[], demands: Demand[], deliveries: Delivery[], site: Site) => {
    const demandRow = createDemandRow(numberOfDays, demands);
    const deliveryRow = createDeliveryRow(numberOfDays, deliveries, site);
    const currentStock = stocks.find((s) => s.stockLocationBpns === site.bpns)?.quantity ?? 0;
    const itemStock = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce(
            (acc, _, index) => ({
                ...acc,
                [index]:
                    index === 0 ? currentStock : (acc[(index - 1) as keyof typeof acc] +
                    deliveryRow[(index - 1) as keyof typeof deliveryRow] -
                    demandRow[(index - 1) as keyof typeof demandRow]),
            }),
            {}
        ),
    };
    return [
        { id: 'demand', name: 'Demand', ...demandRow },
        { id: 'itemStock', name: 'Projected Item Stock', ...itemStock },
        { id: 'delivery', name: 'Incoming Deliveries', ...deliveryRow },
    ];
};

type DemandTableProps = {
    numberOfDays: number;
    stocks: Stock[] | null;
    demands: Demand[] | null;
    deliveries: Delivery[] | null;
    site: Site;
    readOnly?: boolean;
    onDeliveryClick: (delivery: Partial<Delivery>, mode: ModalMode) => void;
    onDemandClick: (demand: Partial<Demand>, mode: ModalMode) => void;
};

export const DemandTable = ({ numberOfDays, stocks, demands, deliveries, site, readOnly, onDeliveryClick, onDemandClick }: DemandTableProps) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleCellClick = (cellData: any) => {
        if (cellData.value === 0) return;
        switch (cellData.id) {
            case 'delivery':
                onDeliveryClick({
                    quantity: cellData.value,
                    dateOfArrival: cellData.colDef.headerName,
                    destinationBpns: site.bpns,
                }, readOnly ? 'view' : 'edit');
                break;
            case 'demand':
                onDemandClick({
                    quantity: parseFloat(cellData.value),
                    demandLocationBpns: site.bpns,
                    day: new Date(cellData.colDef.headerName),
                }, readOnly ? 'view' : 'edit');
                break;
            default:
                break;
        }
    };
    return (
        <Stack spacing={2}>
            <Box display="flex" justifyContent="start" width="100%" gap="0.5rem" marginBlock="0.5rem" paddingLeft=".5rem">
                <Typography variant="caption1" component="h3" fontWeight={600}>
                    Site:
                </Typography>
                {site.name} ({site.bpns})
                {!readOnly && <Box marginLeft="auto" display="flex" gap="1rem">
                        <Button variant="contained" onClick={() => onDeliveryClick({ destinationBpns: site.bpns, departureType: 'estimated-departure', arrivalType: 'estimated-arrival' }, 'create')}>
                            <Add></Add> Add Delivery
                        </Button>
                        <Button variant="contained" onClick={() => onDemandClick({ demandLocationBpns: site.bpns }, 'create')} sx={{ marginLeft: 'auto' }} >
                            <Add></Add> Create Demand
                        </Button>
                    </Box>}
            </Box>
            <TableWithRowHeader
                title=""
                noRowsMsg="Select a Material to show the customer demand"
                columns={createDateColumnHeaders(numberOfDays)}
                rows={createTableRows(numberOfDays, stocks ?? [], demands ?? [], deliveries ?? [], site)}
                onCellClick={handleCellClick}
                getRowId={(row) => row.id}
                hideFooter={true}
            />
        </Stack>
    );
};
