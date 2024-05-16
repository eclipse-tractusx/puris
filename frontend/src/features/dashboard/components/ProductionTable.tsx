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
import { Production } from '@models/types/data/production';
import { Add } from '@mui/icons-material';
import { ModalMode } from '@models/types/data/modal-mode';

const createProductionRow = (numberOfDays: number, productions: Production[]) => {
    return {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => {
            const date = new Date();
            date.setDate(date.getDate() + index);
            const prod = productions
                .filter((production) => new Date(production.estimatedTimeOfCompletion).toDateString() === date.toDateString())
                .reduce((sum, production) => sum + production.quantity, 0);
            return { ...acc, [index]: prod };
        }, {}),
    };
};

const createShipmentRow = (numberOfDays: number, deliveries: Delivery[], site: Site) => {
    return {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce((acc, _, index) => {
            const date = new Date();
            date.setDate(date.getDate() + index);
            const d = deliveries
                .filter(
                    (delivery) =>
                        new Date(delivery.dateOfDeparture!).toDateString() === date.toDateString() &&
                        delivery.originBpns === site.bpns
                )
                .reduce((sum, delivery) => sum + delivery.quantity!, 0);
            return { ...acc, [index]: d };
        }, {}),
    };
};

const createProductionTableRows = (
    numberOfDays: number,
    stocks: Stock[],
    productions: Production[],
    deliveries: Delivery[],
    site: Site
) => {
    const shipmentRow = createShipmentRow(numberOfDays, deliveries, site);
    const productionRow = createProductionRow(numberOfDays, productions);
    const stockQuantity = stocks.filter((stock) => stock.stockLocationBpns === site.bpns).reduce((acc, stock) => acc + stock.quantity, 0);
    const stockRow = {
        ...Object.keys(Array.from({ length: numberOfDays })).reduce(
            (acc, _, index) => ({
                ...acc,
                [index]:
                    index === 0
                        ? stockQuantity
                        : acc[(index - 1) as keyof typeof acc] -
                          shipmentRow[(index - 1) as keyof typeof shipmentRow] +
                          productionRow[(index - 1) as keyof typeof productionRow],
            }),
            {}
        ),
    };
    return [
        { id: 'shipment', name: 'Outgoing Shipments', ...shipmentRow },
        { id: 'itemStock', name: 'Projected Item Stock', ...stockRow },
        { id: 'plannedProduction', name: 'Planned Production', ...productionRow },
    ];
};

type ProductionTableProps = {
    numberOfDays: number;
    stocks: Stock[] | null;
    site: Site;
    productions: Production[] | null;
    deliveries: Delivery[];
    readOnly: boolean;
    onDeliveryClick: (delivery: Partial<Delivery>, mode: ModalMode) => void;
    onProductionClick: (production: Partial<Production>, mode: ModalMode) => void;
};

export const ProductionTable = ({
    numberOfDays,
    stocks,
    site,
    productions,
    deliveries,
    readOnly,
    onDeliveryClick,
    onProductionClick,
}: ProductionTableProps) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleCellClick = (cellData: any) => {
        if (cellData.value === 0) return;
        if (cellData.id === 'shipment') {
            onDeliveryClick(
                {
                    quantity: cellData.value,
                    dateOfDeparture: cellData.colDef.headerName,
                    originBpns: site.bpns,
                    destinationBpns: site.bpns,
                },
                readOnly ? 'view' : 'edit'
            );
        }
        if (cellData.id === 'plannedProduction') {
            const material = stocks?.length ? stocks[0].material : undefined;
            onProductionClick(
                {
                    quantity: parseFloat(cellData.value),
                    material,
                    estimatedTimeOfCompletion: new Date(cellData.colDef.headerName),
                    productionSiteBpns: site.bpns,
                },
                readOnly ? 'view' : 'edit'
            );
        }
    };
    return (
        <Stack spacing={2}>
            <Box
                display="flex"
                justifyContent="start"
                alignItems="center"
                width="100%"
                gap="0.5rem"
                marginBlock="0.5rem"
                paddingLeft=".5rem"
            >
                <Typography variant="caption1" component="h3" fontWeight={600}>
                    {' '}
                    Site:{' '}
                </Typography>
                {site.name} ({site.bpns})
                {!readOnly && (
                    <Box marginLeft="auto" display="flex" gap="1rem">
                        <Button variant="contained" onClick={() => onDeliveryClick({ originBpns: site.bpns, departureType: 'estimated-departure', arrivalType: 'estimated-arrival' }, 'create')}>
                            <Add></Add> Add Delivery
                        </Button>
                        <Button variant="contained" onClick={() => onProductionClick({ productionSiteBpns: site.bpns }, 'create')}>
                            <Add></Add> Add Production
                        </Button>
                    </Box>
                )}
            </Box>
            <TableWithRowHeader
                title=""
                noRowsMsg="Select a Site to show production data"
                columns={createDateColumnHeaders(numberOfDays)}
                onCellClick={handleCellClick}
                rows={site ? createProductionTableRows(numberOfDays, stocks ?? [], productions ?? [], deliveries ?? [], site) : []}
                getRowId={(row) => row.id}
                hideFooter={true}
            />
        </Stack>
    );
};
