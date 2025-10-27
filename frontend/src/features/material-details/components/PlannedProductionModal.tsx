/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import { useMemo } from 'react';
import { Table } from '@catena-x/portal-shared-components';
import { Production } from '@models/types/data/production';
import { Box, Button, Dialog, DialogTitle, Stack } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { deleteProduction } from '@services/productions-service';
import { Close, Delete, Edit } from '@mui/icons-material';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useDataModal } from '@contexts/dataModalContext';
import { useNotifications } from '@contexts/notificationContext';

const createProductionColumns = (handleDelete?: (row: Production) => void, handleEdit?: (row: Production) => void) => {
    return [
        {
            field: 'estimatedTimeOfCompletion',
            headerName: 'Completion Time',
            headerAlign: 'center',
            flex: 1.5,
            renderCell: (data: { row: Production }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {new Date(data.row.estimatedTimeOfCompletion).toLocaleTimeString('de-DE')}
                </Box>
            ),
        },
        {
            field: 'quantity',
            headerName: 'Quantity',
            headerAlign: 'center',
            flex: 1.2,
            renderCell: (data: { row: Production }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {`${data.row.quantity} ${getUnitOfMeasurement(data.row.measurementUnit)}`}
                </Box>
            ),
        },
        {
            field: 'partner',
            headerName: 'Partner',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Production }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.partner?.name}
                </Box>
            ),
        },
        {
            field: 'customerOrderNumber',
            headerName: 'Order Reference',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Production }) => (
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
            field: 'lastUpdatedOnDateTime',
            headerName: 'Updated',
            headerAlign: 'center',
            flex: 1.5,
            renderCell: (data: { row: Production }) => (
                <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    <Box>{new Date(data.row.lastUpdatedOnDateTime).toLocaleDateString('en-GB')}</Box>
                    <Box>{new Date(data.row.lastUpdatedOnDateTime).toLocaleTimeString('en-GB')}</Box>
                </Stack>
            ),
        },
        {
            field: 'edit',
            headerName: '',
            sortable: false,
            disableColumnMenu: true,
            width: 20,
            renderCell: (data: { row: Production }) => (
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
            renderCell: ({ row }: { row: Production }) => (
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
        }
    ] as const;
};

type PlannedProductionModalProps = {
    open: boolean;
    onClose: () => void;
    onSave: () => void;
    onRemove?: (deletedUuid: string) => void;
    production: Partial<Production> | null;
    productions: Production[];
};

export const PlannedProductionModal = ({ open, onClose, onRemove, production, productions }: PlannedProductionModalProps) => {
    const { sites } = useSites();
    const { notify } = useNotifications();
    const { openDialog } = useDataModal();
    const dailyProductions = useMemo(
        () =>
            productions.filter(
                (p) =>
                    new Date(p.estimatedTimeOfCompletion).toLocaleDateString() ===
                    new Date(production?.estimatedTimeOfCompletion ?? Date.now()).toLocaleDateString()
            ),
        [productions, production?.estimatedTimeOfCompletion]
    );

    const canDelete = Boolean(sites?.find((site) => productions?.some((d) => d.productionSiteBpns === site.bpns)));

    const handleEdit = (row: Production) => {
        openDialog('production', { ...row }, dailyProductions, 'edit');
    };
    const handleDelete = async (row: Production) => {
        if (row.uuid) {
            try {
                await deleteProduction(row.uuid);
                onRemove?.(row.uuid);
            } catch (error) {
                notify({
                    title: 'Error deleting production',
                    description: 'Failed to delete the production',
                    severity: 'error',
                });
            }
        }
    };

    return (
        <>
            <Dialog open={open && production !== null} onClose={onClose} data-testid="production-modal">
                <DialogTitle variant="h3" textAlign="center">
                    Production Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>

                    <Table
                        title={`Planned Production ${production?.estimatedTimeOfCompletion
                            ? ' on ' +
                            new Date(production?.estimatedTimeOfCompletion).toLocaleDateString('en-GB', {
                                weekday: 'long',
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                            })
                            : ''
                            }`}
                        getRowId={(row) => row.uuid}
                        columns={createProductionColumns(canDelete ? handleDelete : undefined, handleEdit)}
                        rows={dailyProductions}
                        hideFooter
                        density="standard"
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
