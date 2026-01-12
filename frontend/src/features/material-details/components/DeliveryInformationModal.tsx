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
import { Table } from '@catena-x/portal-shared-components';
import { Delivery } from '@models/types/data/delivery';
import { Close, Edit,  Delete } from '@mui/icons-material';
import { Box, Button, Dialog, DialogTitle, Grid, Stack, Tooltip } from '@mui/material';
import { deleteDelivery } from '@services/delivery-service';
import { getUnitOfMeasurement } from '@util/helpers';
import { useEffect, useMemo, useState } from 'react';
import { INCOTERMS } from '@models/constants/incoterms';
import { Site } from '@models/types/edc/site';
import { useNotifications } from '@contexts/notificationContext';
import { DirectionType } from '@models/types/erp/directionType';
import { TextToClipboard } from '@components/ui/TextToClipboard';
import { useDataModal } from '@contexts/dataModalContext';

const createDeliveryColumns = (handleDelete: (row: Delivery) => void, handleEdit: (row: Delivery) => void) => {
    return [
        {
            field: 'dateOfDeparture',
            headerName: 'Departure Time',
            headerAlign: 'center',
            width: 150,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                        flexDirection="column"
                    >
                        {new Date(data.row.dateOfDeparture!).toLocaleString('de-DE', {
                            day: '2-digit',
                            month: '2-digit',
                            year: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                        })}
                        <Box fontSize=".9em">({data.row.departureType.split('-')[0]})</Box>
                    </Box>
                );
            },
        },
        {
            field: 'dateofArrival',
            headerName: 'Arrival Time',
            headerAlign: 'center',
            width: 150,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                        flexDirection="column"
                    >
                        {new Date(data.row.dateOfArrival!).toLocaleString('de-DE', {
                            day: '2-digit',
                            month: '2-digit',
                            year: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                        })}
                        <Box fontSize=".9em">({data.row.arrivalType.split('-')[0]})</Box>
                    </Box>
                );
            },
        },
        {
            field: 'quantity',
            headerName: 'Quantity',
            headerAlign: 'center',
            width: 120,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {`${data.row.quantity} ${getUnitOfMeasurement(data.row.measurementUnit)}`}
                    </Box>
                );
            },
        },
        {
            field: 'partnerBpnl',
            headerName: 'Partner',
            headerAlign: 'center',
            width: 200,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        <TextToClipboard text={data.row.partnerBpnl} />
                    </Box>
                );
            },
        },
        {
            field: 'customerOrderNumber',
            headerName: 'Customer Order Number',
            sortable: false,
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                    >
                        {data.row.customerOrderNumber ? (
                            <TextToClipboard text={data.row.customerOrderNumber} />
                        ) : (
                            '-'
                        )}
                    </Box>
                );
            },
        },
        {
            field: 'customerOrderPositionNumber',
            headerName: 'Customer Order Position',
            sortable: false,
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                    >
                        {data.row.customerOrderPositionNumber ? (
                            <TextToClipboard text={data.row.customerOrderPositionNumber} />
                        ) : (
                            '-'
                        )}
                    </Box>
                );
            },
        },
        {
            field: 'supplierOrderNumber',
            headerName: 'Supplier Order Number',
            sortable: false,
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                    >
                        {data.row.supplierOrderNumber ? (
                            <TextToClipboard text={data.row.supplierOrderNumber} />
                        ) : (
                            '-'
                        )}
                    </Box>
                );
            },
        },
        {
            field: 'trackingNumber',
            headerName: 'Tracking Number',
            headerAlign: 'center',
            width: 130,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        <TextToClipboard text={data.row.trackingNumber} />
                    </Box>
                );
            },
        },
        {
            field: 'incoterm',
            headerName: 'Incoterm',
            headerAlign: 'center',
            width: 150,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        textAlign="center"
                        alignItems="center"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                    >
                        <Box>{INCOTERMS.find((i) => i.key === data.row.incoterm)?.value ?? '-'}</Box>
                        <Box>({data.row.incoterm})</Box>
                    </Box>
                );
            },
        },
        {
            field: 'lastUpdatedOnDateTime',
            headerName: 'Updated',
            headerAlign: 'center',
            width: 100,
            renderCell: (data: { row: Delivery }) => (
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
            headerAlign: 'center',
            type: 'string',
            width: 20,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {!data.row.reported && (
                            <Tooltip title="Deliveries with actual arrival cannot be edited." disableFocusListener disableTouchListener disableHoverListener={data.row.arrivalType !== 'actual-arrival'}>
                                <span>
                                    <Button variant="text" onClick={() => handleEdit(data.row)} data-testid="edit-delivery" disabled={data.row.arrivalType === 'actual-arrival'}>
                                        <Edit></Edit>
                                    </Button>
                                </span>
                            </Tooltip>
                        )}
                    </Box>
                );
            },
        },
        {
            field: 'delete',
            headerName: '',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            width: 20,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {!data.row.reported && (
                            <Button variant="text" color="error" onClick={() => handleDelete(data.row)} data-testid="delete-delivery">
                                <Delete></Delete>
                            </Button>
                        )}
                    </Box>
                );
            },
        },
    ] as const;
};

export type DeliveryInformationModalProps = {
    open: boolean;
    direction: DirectionType;
    site: Site | null;
    onClose: () => void;
    onSave: (d?: Delivery) => void;
    onRemove?: (deletedUuid: string) => void;
    delivery: Delivery | null;
    deliveries: Delivery[];
};

export const DeliveryInformationModal = ({
    open,
    direction,
    site,
    onClose,
    onRemove,
    delivery,
    deliveries,
}: DeliveryInformationModalProps) => {
    const [temporaryDelivery, setTemporaryDelivery] = useState<Partial<Delivery>>(delivery ?? {});
    const { notify } = useNotifications();
    const { openDialog } = useDataModal();
    const dailyDeliveries = useMemo(
        () =>
            deliveries?.filter(
                (d) =>
                    (direction === DirectionType.Inbound &&
                        (!site || d.destinationBpns === site?.bpns) &&
                        new Date(d.dateOfArrival).toLocaleDateString() === new Date(delivery!.dateOfArrival).toLocaleDateString()) ||
                    (direction === DirectionType.Outbound &&
                        (!site || d.originBpns === site?.bpns) &&
                        new Date(d.dateOfDeparture).toLocaleDateString() === new Date(delivery!.dateOfDeparture).toLocaleDateString())
            ) ?? [],
        [deliveries, delivery, direction, site]
    );

    const handleClose = () => {
        setTemporaryDelivery({});
        onClose();
    };

    const handleDelete = async (row: Delivery) => {
        if (row.uuid) {
            try {
                await deleteDelivery(row.uuid);
                onRemove?.(row.uuid);
            } catch (error) {
                notify({
                    title: 'Error deleting delivery',
                    description: 'Failed to delete the delivery',
                    severity: 'error',
                });
            }
        }
    };
    const handleEdit = (row: Delivery) => {
        openDialog('delivery', {...row}, deliveries, 'edit');
    };

    useEffect(() => {
        if (delivery) {
            setTemporaryDelivery(delivery);
        }
    }, [delivery]);
    return (
        <>
            <Dialog open={open && delivery !== null} onClose={handleClose} data-testid="delivery-modal">
                <DialogTitle variant="h3" textAlign="center">
                    Delivery Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '95vw', minWidth: '60rem' }}>
                    <Grid item xs={12}>
                        <Table
                            title={`${direction === DirectionType.Outbound ? 'Outgoing Shipments' : 'Incoming Deliveries'} on ${
                                direction === DirectionType.Outbound
                                    ? new Date(temporaryDelivery.dateOfDeparture!).toLocaleDateString('en-UK', {
                                            weekday: 'long',
                                            day: '2-digit',
                                            month: '2-digit',
                                            year: 'numeric',
                                        })
                                    : new Date(temporaryDelivery.dateOfArrival!).toLocaleDateString('en-UK', {
                                        weekday: 'long',
                                        day: '2-digit',
                                        month: '2-digit',
                                        year: 'numeric',
                                    })
                            }`}
                            density="standard"
                            getRowId={(row) => row.uuid}
                            columns={createDeliveryColumns(handleDelete, handleEdit)}
                            rows={dailyDeliveries}
                            hideFooter
                            disableRowSelectionOnClick
                        />
                    </Grid>
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};
