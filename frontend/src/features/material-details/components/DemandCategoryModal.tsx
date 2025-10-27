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
import { Demand } from '@models/types/data/demand';
import { Box, Button, Dialog, DialogTitle, Stack } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { Close, Delete, Edit } from '@mui/icons-material';
import { useMemo } from 'react';
import { useDataModal } from '@contexts/dataModalContext';
import { deleteDemand } from '@services/demands-service';
import { Table } from '@catena-x/portal-shared-components';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useNotifications } from '@contexts/notificationContext';

type DemandCategoryModalProps = {
    open: boolean;
    demand: Partial<Demand> | null;
    demands: Demand[];
    onClose: () => void;
    onSave: () => void;
    onRemove?: (deletedUuid: string) => void;
};


const createDemandColumns = (handleDelete?: (row: Demand) => void, handleEdit?: (row: Demand) => void) => {
    return [
        {
            field: 'quantity',
            headerName: 'Quantity',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            flex: 1,
            renderCell: (data: { row: Demand }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {`${data.row.quantity} ${getUnitOfMeasurement(data.row.measurementUnit)}`}
                    </Box>
                );
            },
        },
        {
            field: 'partner',
            headerName: 'Partner',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            flex: 1,
            renderCell: (data: { row: Demand }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {data.row.partnerBpnl}
                    </Box>
                );
            },
        },
        {
            field: 'supplierLocationBpns',
            headerName: 'Expected Supplier Location',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            flex: 1,
            renderCell: (data: { row: Demand }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {data.row.supplierLocationBpns}
                    </Box>
                );
            },
        },
        {
            field: 'category',
            headerName: 'Demand Category',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            flex: 1,
            renderCell: (data: { row: Demand }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {DEMAND_CATEGORY.find((cat) => cat.key === data.row.demandCategoryCode)?.value ?? DEMAND_CATEGORY[0].value}
                    </Box>
                );
            },
        },
        {
            field: 'lastUpdatedOnDateTime',
            headerName: 'Updated',
            headerAlign: 'center',
            flex: 1.5,
            renderCell: (data: { row: Demand }) => (
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
            renderCell: (data: { row: Demand }) => (
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
            renderCell: ({ row }: { row: Demand }) => (
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


export const DemandCategoryModal = ({ open, onClose, onRemove, demand, demands }: DemandCategoryModalProps) => {
    const { sites } = useSites();
    const { notify } = useNotifications();
    const dailyDemands = useMemo(
        () =>
            demands?.filter(
                (d) => d.day && new Date(d.day).toLocaleDateString() === new Date(demand?.day ?? Date.now()).toLocaleDateString()
            ),
        [demands, demand?.day]
    );
    const { openDialog } = useDataModal();
    const canDelete = Boolean(sites?.find(site => demands?.some(d => d.demandLocationBpns === site.bpns)));
    const handleEdit = (row: Demand) => {
        openDialog('demand', { ...row }, dailyDemands, 'edit');
    };

    const handleDelete = async (row: Demand) => {
        if (row.uuid) {
            try {
                await deleteDemand(row.uuid);
                onRemove?.(row.uuid);
            } catch (error) {
                notify({
                    title: 'Error deleting demand',
                    description: 'Failed to delete the demand',
                    severity: 'error',
                });
            }
        }
    };


    return (
        <>
            <Dialog open={open && demand !== null} onClose={onClose} data-testid="demand-modal">
                <DialogTitle variant="h3" textAlign="center">
                    Demand Information
                </DialogTitle>

                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    <Table
                        title={`Material Demand ${demand?.day
                            ? ' on ' +
                            new Date(demand?.day).toLocaleDateString('en-GB', {
                                weekday: 'long',
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                            })
                            : ''
                            }`}
                        getRowId={(row) => row.uuid}
                        columns={createDemandColumns(canDelete ? handleDelete : undefined, handleEdit)}
                        rows={dailyDemands ?? []}
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
