/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)

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
import { Box, Button, capitalize, Dialog, DialogTitle, Stack } from '@mui/material';
import { Close, Delete, Edit } from '@mui/icons-material';
import { Stock, StockType } from '@models/types/data/stock';
import { deleteStocks } from '@services/stocks-service';
import { useNotifications } from '@contexts/notificationContext';
import { useDataModal } from '@contexts/dataModalContext';
import { getUnitOfMeasurement } from '@util/helpers';
import { DirectionType } from '@models/types/erp/directionType';
import { useSites } from '@features/stock-view/hooks/useSites';

export type StockModalProps = {
    open: boolean;
    onClose: () => void;
    onRemove?: (uuid: string) => void;
    stock: Partial<Stock> | null;
    stocks: Stock[];
    stockType: StockType;
    onSave: (d?: Stock) => void;
};

const createStockColumns = (
    handleDelete?: (row: Stock) => void, handleEdit?: (row: Stock) => void
) => {
    return [
        {
            field: 'quantity',
            headerName: 'Quantity',
            headerAlign: 'center',
            flex: 1.2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {`${data.row.quantity} ${getUnitOfMeasurement(data.row.measurementUnit)}`}
                </Box>
            ),
        },
        {
            field: 'stockLocationBpns',
            headerName: 'Site',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.stockLocationBpns}
                </Box>
            ),
        },
        {
            field: 'partner',
            headerName: 'Partner',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.partner?.name}
                </Box>
            ),
        },
        {
            field: 'lastUpdatedOn',
            headerName: 'Updated',
            headerAlign: 'center',
            flex: 1.5,
            renderCell: (data: { row: Stock }) => (
                <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    <Box>{new Date(data.row.lastUpdatedOn).toLocaleDateString('en-GB')}</Box>
                    <Box>{new Date(data.row.lastUpdatedOn).toLocaleTimeString('en-GB')}</Box>
                </Stack>
            ),
        },
        {
            field: 'customerOrderNumber',
            headerName: 'Order Reference',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.customerOrderNumber ? (
                        <Stack>
                            <Box>{`${data.row.customerOrderNumber} / ${data.row.customerOrderPositionNumber}  `}</Box>
                            <Box>{data.row.supplierOrderNumber || '-'}</Box>
                        </Stack>
                    ) : (
                        '-'
                    )}
                </Box>
            ),
        },
        {
            field: 'isBlocked',
            headerName: 'Blocked',
            headerAlign: 'center',
            flex: 1,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.isBlocked ? 'Yes' : 'No'}
                </Box>
            ),
        },
        {
            field: 'edit',
            headerName: '',
            sortable: false,
            disableColumnMenu: true,
            width: 20,
            renderCell: (data: { row: Stock }) => (
                handleEdit ? (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        <Button variant="text" onClick={() => handleEdit(data.row)} data-testid="edit-stock">
                            <Edit />
                        </Button>

                    </Box>
                ) : null
            ),
        },
        {
            field: 'delete',
            headerName: '',
            width: 40,
            sortable: false,
            disableColumnMenu: true,
            renderCell: ({ row }: { row: Stock }) => (
                handleDelete ? (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        <Button
                            variant="text"
                            color="error"
                            onClick={() => handleDelete(row)}
                        >
                            <Delete />
                        </Button>
                    </Box>
                ) : null
            ),
        },
    ] as const;
};

export const StockModal = ({
    open,
    onClose,
    onRemove,
    stock,
    stocks,
    stockType,
}: StockModalProps) => {
    const { sites } = useSites();
    const { notify } = useNotifications();
    const { openDialog } = useDataModal();
    const direction = stockType == 'material' ? DirectionType.Inbound : DirectionType.Outbound
    const isReported = Boolean(!sites?.find(site => stocks?.some(s => s.stockLocationBpns === site.bpns)));

    const handleEdit = (row: Stock) => {
        openDialog('stock', { ...row }, stocks, 'edit', direction);
    };
    const handleDelete = async (row: Stock) => {
        if (row.uuid) {
            try {
                await deleteStocks(stockType, row.uuid);
                onRemove?.(row.uuid);
                notify({
                    title: 'Stock deleted',
                    description: 'The stock record was successfully deleted.',
                    severity: 'success',
                });
            } catch (e) {
                notify({
                    title: 'Error',
                    description: 'Failed to delete stock record.',
                    severity: 'error',
                });
            }
        }
    };

    return (
        <>
            <Dialog open={open && stock !== null} onClose={onClose} data-testid="stock-modal">
                <DialogTitle variant="h3" textAlign="center">
                    {capitalize('view')} {capitalize(stockType)} Stock
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    <Table
                        title={`Current ${stockType} stock`}
                        getRowId={(row) => row.uuid}
                        columns={createStockColumns(!isReported ? handleDelete : undefined, !isReported ? handleEdit : undefined)}
                        rows={stocks}
                        density="standard"
                        hideFooter
                    />
                    <Box display="flex" justifyContent="flex-end" marginTop="2rem">
                        <Button variant="outlined" onClick={onClose}>
                            <Close /> Close
                        </Button>
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};
