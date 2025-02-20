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
import { Input, Table } from '@catena-x/portal-shared-components';
import { DateTime } from '@components/ui/DateTime';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Delivery } from '@models/types/data/delivery';
import { Close, Delete, Save } from '@mui/icons-material';
import { Box, Button, Dialog, DialogTitle, FormLabel, Grid, Stack, capitalize } from '@mui/material';
import { deleteDelivery, postDelivery } from '@services/delivery-service';
import { getUnitOfMeasurement, isValidOrderReference } from '@util/helpers';
import { useEffect, useMemo, useState } from 'react';
import { INCOTERMS } from '@models/constants/incoterms';
import { ARRIVAL_TYPES, DEPARTURE_TYPES } from '@models/constants/event-type';
import { ModalMode } from '@models/types/data/modal-mode';
import { Site } from '@models/types/edc/site';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useNotifications } from '@contexts/notificationContext';
import { DirectionType } from '@models/types/erp/directionType';

const createDeliveryColumns = (handleDelete: (row: Delivery) => void) => {
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
                        <Box fontSize=".9em">({data.row.departureType.split('-')[0]})</Box>
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
                        {data.row.partnerBpnl}
                    </Box>
                );
            },
        },
        {
            field: 'customerOrderNumber',
            headerName: 'Order Reference',
            sortable: false,
            headerAlign: 'center',
            width: 200,
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
                            <>
                                <Box>{`${data.row.customerOrderNumber} / ${data.row.customerOrderPositionNumber}`}</Box>
                                <Box>{data.row.supplierOrderNumber}</Box>
                            </>
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
                        {data.row.trackingNumber}
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
            field: 'delete',
            headerName: '',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            width: 30,
            renderCell: (data: { row: Delivery }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {!data.row.reported && (
                            <Button variant="text" color="error" onClick={() => handleDelete(data.row)}>
                                <Delete></Delete>
                            </Button>
                        )}
                    </Box>
                );
            },
        },
    ] as const;
};

const isValidDelivery = (delivery: Partial<Delivery>) =>
    delivery.ownMaterialNumber &&
    delivery.originBpns &&
    delivery.partnerBpnl &&
    delivery.destinationBpns &&
    delivery.quantity &&
    delivery.measurementUnit &&
    delivery.incoterm &&
    delivery.dateOfDeparture &&
    delivery.dateOfArrival &&
    delivery.departureType &&
    delivery.arrivalType &&
    delivery.dateOfArrival >= delivery.dateOfDeparture &&
    (delivery.departureType !== 'actual-departure' || delivery.dateOfDeparture <= new Date()) &&
    (delivery.arrivalType !== 'actual-arrival' || delivery.dateOfArrival <= new Date()) &&
    isValidOrderReference(delivery);

type DeliveryInformationModalProps = {
    open: boolean;
    mode: ModalMode;
    direction: DirectionType;
    site: Site | null;
    onClose: () => void;
    onSave: () => void;
    delivery: Delivery | null;
    deliveries: Delivery[];
};

export const DeliveryInformationModal = ({
    open,
    mode,
    direction,
    site,
    onClose,
    onSave,
    delivery,
    deliveries,
}: DeliveryInformationModalProps) => {
    const [temporaryDelivery, setTemporaryDelivery] = useState<Partial<Delivery>>(delivery ?? {});
    const { partners } = usePartners(direction === DirectionType.Inbound ? 'product' : 'material', temporaryDelivery?.ownMaterialNumber ?? null);
    const { sites } = useSites();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);
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

    const handleSaveClick = () => {
        temporaryDelivery.customerOrderNumber ||= undefined;
        temporaryDelivery.customerOrderPositionNumber ||= undefined;
        temporaryDelivery.supplierOrderNumber ||= undefined;
        if (!isValidDelivery(temporaryDelivery)) {
            setFormError(true);
            return;
        }
        setFormError(false);

        postDelivery(temporaryDelivery)
            .then(() => {
                onSave();
                notify({
                        title: 'Delivery Added',
                        description: 'The Delivery has been added',
                        severity: 'success',
                    },
                );
            })
            .catch((error) => {
                notify({
                        title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                        description: error.status === 409 ? 'Delivery conflicting with an existing one' : error.error,
                        severity: 'error',
                    },
                );
            })
            .finally(() => onClose());
    };

    const handleClose = () => {
        setFormError(false);
        setTemporaryDelivery({});
        onClose();
    };

    const handleDelete = (row: Delivery) => {
        if (row.uuid) deleteDelivery(row.uuid).then(onSave);
    };

    useEffect(() => {
        if (delivery) {
            setTemporaryDelivery(delivery);
        }
    }, [delivery]);
    return (
        <>
            <Dialog open={open && delivery !== null} onClose={handleClose}>
                <DialogTitle variant="h3" textAlign="center">
                    {capitalize(mode)} Delivery Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    <Grid container spacing={1} padding=".25rem">
                        {mode === 'create' ? (
                            <>
                                <GridItem label="Material Number" value={temporaryDelivery.ownMaterialNumber ?? ''} />
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="partnerBpns"
                                        options={sites ?? []}
                                        getOptionLabel={(option) => option.name ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.bpns === value.bpns}
                                        onChange={(_, value) =>
                                            setTemporaryDelivery({
                                                ...temporaryDelivery,
                                                ...(direction === DirectionType.Outbound
                                                    ? { originBpns: value?.bpns ?? undefined }
                                                    : { destinationBpns: value?.bpns ?? undefined }),
                                            })
                                        }
                                        value={
                                            sites?.find(
                                                    (s) =>
                                                        (direction === DirectionType.Outbound
                                                            ? s.bpns === temporaryDelivery.originBpns
                                                            : s.bpns === temporaryDelivery.destinationBpns)
                                                ) ?? null
                                        }
                                        label={`${direction === DirectionType.Outbound ? 'Origin' : 'Destination'}*`}
                                        placeholder={`Select a ${direction === DirectionType.Outbound ? 'Origin' : 'Destination'} Site`}
                                        error={
                                            formError &&
                                            (direction === DirectionType.Outbound ? !temporaryDelivery.originBpns : !temporaryDelivery.destinationBpns)
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="departure-type"
                                        options={DEPARTURE_TYPES}
                                        getOptionLabel={(option) => option.value ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.key === value.key}
                                        onChange={(_, value) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, departureType: value?.key ?? undefined })
                                        }
                                        value={DEPARTURE_TYPES.find((dt) => dt.key === temporaryDelivery.departureType) ?? null}
                                        label="Departure Type*"
                                        placeholder="Select the type of departure"
                                        error={formError && !temporaryDelivery?.departureType}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="arrival-type"
                                        options={ARRIVAL_TYPES}
                                        getOptionLabel={(option) => option.value ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.key === value.key}
                                        onChange={(_, value) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, arrivalType: value?.key ?? undefined })
                                        }
                                        value={ARRIVAL_TYPES.find((dt) => dt.key === temporaryDelivery.arrivalType) ?? null}
                                        label="Arrival Type*"
                                        placeholder="Select the type of departure"
                                        error={
                                            formError &&
                                            (!temporaryDelivery?.arrivalType ||
                                                (temporaryDelivery?.arrivalType === 'actual-arrival' &&
                                                    temporaryDelivery?.departureType !== 'actual-departure'))
                                        }
                                    ></LabelledAutoComplete>
                                </Grid>
                                <Grid item xs={6} display="flex" alignItems="end">
                                    <DateTime
                                        label="Departure Time*"
                                        placeholder="Pick Departure Date"
                                        locale="de"
                                        error={
                                            formError &&
                                            (!temporaryDelivery.dateOfDeparture ||
                                                (temporaryDelivery.departureType === 'actual-departure' &&
                                                    temporaryDelivery.dateOfDeparture > new Date()) ||
                                                (!!temporaryDelivery.dateOfArrival &&
                                                    temporaryDelivery.dateOfArrival < temporaryDelivery.dateOfDeparture))
                                        }
                                        value={temporaryDelivery?.dateOfDeparture ?? null}
                                        onValueChange={(date) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, dateOfDeparture: date ?? undefined })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6} display="flex" alignItems="end">
                                    <DateTime
                                        label="Arrival Time*"
                                        placeholder="Pick Arrival Date"
                                        locale="de"
                                        error={
                                            formError &&
                                            (!temporaryDelivery?.dateOfArrival ||
                                                (temporaryDelivery?.arrivalType === 'actual-arrival' &&
                                                    temporaryDelivery?.dateOfArrival > new Date()))
                                        }
                                        value={temporaryDelivery?.dateOfArrival ?? null}
                                        onValueChange={(date) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, dateOfArrival: date ?? undefined })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        sx={{ margin: '0' }}
                                        id="partner"
                                        options={partners ?? []}
                                        getOptionLabel={(option) => option?.name ?? ''}
                                        label="Partner*"
                                        placeholder="Select a Partner"
                                        error={formError && !temporaryDelivery?.partnerBpnl}
                                        onChange={(_, value) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, partnerBpnl: value?.bpnl ?? undefined })
                                        }
                                        value={partners?.find((p) => p.bpnl === temporaryDelivery.partnerBpnl) ?? null}
                                        isOptionEqualToValue={(option, value) => option?.bpnl === value?.bpnl}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="partnerBpns"
                                        options={partners?.find((s) => s.bpnl === temporaryDelivery?.partnerBpnl)?.sites ?? []}
                                        getOptionLabel={(option) => option.name ?? ''}
                                        disabled={!temporaryDelivery?.partnerBpnl}
                                        isOptionEqualToValue={(option, value) => option?.bpns === value.bpns}
                                        onChange={(_, value) =>
                                            setTemporaryDelivery({
                                                ...temporaryDelivery,
                                                ...(direction === DirectionType.Inbound
                                                    ? { originBpns: value?.bpns ?? undefined }
                                                    : { destinationBpns: value?.bpns ?? undefined }),
                                            })
                                        }
                                        value={
                                            partners
                                                ?.find((s) => s.bpnl === temporaryDelivery?.partnerBpnl)
                                                ?.sites.find(
                                                    (s) =>
                                                        (direction === DirectionType.Inbound
                                                            ? s.bpns === temporaryDelivery.originBpns
                                                            : s.bpns === temporaryDelivery.destinationBpns)
                                                ) ?? null
                                        }
                                        label={`${direction === DirectionType.Inbound ? 'Origin' : 'Destination'}*`}
                                        placeholder={`Select a ${direction === DirectionType.Inbound ? 'Origin' : 'Destination'} Site`}
                                        error={
                                            formError &&
                                            (direction === DirectionType.Inbound ? !temporaryDelivery.originBpns : !temporaryDelivery.destinationBpns)
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Quantity*</FormLabel>
                                    <Input
                                        hiddenLabel
                                        type="number"
                                        value={temporaryDelivery.quantity ?? ''}
                                        error={formError && !temporaryDelivery?.quantity}
                                        onChange={(e) =>
                                            setTemporaryDelivery((curr) => ({
                                                ...curr,
                                                quantity: e.target.value ? parseFloat(e.target.value) : undefined,
                                            }))
                                        }
                                        sx={{ marginTop: '.5rem' }}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="uom"
                                        value={
                                            temporaryDelivery.measurementUnit
                                                ? {
                                                      key: temporaryDelivery.measurementUnit,
                                                      value: getUnitOfMeasurement(temporaryDelivery.measurementUnit),
                                                  }
                                                : null
                                        }
                                        options={UNITS_OF_MEASUREMENT}
                                        getOptionLabel={(option) => option?.value ?? ''}
                                        label="UOM*"
                                        placeholder="Select unit"
                                        error={formError && !temporaryDelivery?.measurementUnit}
                                        onChange={(_, value) => setTemporaryDelivery((curr) => ({ ...curr, measurementUnit: value?.key }))}
                                        isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Tracking Number</FormLabel>
                                    <Input
                                        id="tracking-number"
                                        hiddenLabel
                                        type="text"
                                        value={temporaryDelivery?.trackingNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, trackingNumber: event.target.value })
                                        }
                                        sx={{ marginTop: '.5rem' }}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="incoterm"
                                        value={
                                            temporaryDelivery.incoterm
                                                ? INCOTERMS.find((i) => i.key === temporaryDelivery.incoterm) ?? null
                                                : null
                                        }
                                        options={INCOTERMS.filter((i) =>
                                            direction === DirectionType.Inbound ? i.responsibility !== 'supplier' : i.responsibility !== 'customer'
                                        )}
                                        getOptionLabel={(option) => option?.value ?? ''}
                                        label="Incoterm*"
                                        placeholder="Select Incoterm"
                                        error={formError && !temporaryDelivery?.incoterm}
                                        onChange={(_, value) => setTemporaryDelivery((curr) => ({ ...curr, incoterm: value?.key }))}
                                        isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Customer Order Number</FormLabel>
                                    <Input
                                        id="customer-order-number"
                                        type="text"
                                        error={formError && !isValidOrderReference(temporaryDelivery)}
                                        value={temporaryDelivery?.customerOrderNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, customerOrderNumber: event.target.value })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Customer Order Position</FormLabel>
                                    <Input
                                        id="customer-order-position-number"
                                        type="text"
                                        error={formError && !isValidOrderReference(temporaryDelivery)}
                                        value={temporaryDelivery?.customerOrderPositionNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryDelivery({
                                                ...temporaryDelivery,
                                                customerOrderPositionNumber: event.target.value,
                                            })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Supplier Order Number</FormLabel>
                                    <Input
                                        id="supplier-order-number"
                                        type="text"
                                        error={
                                            formError && !!temporaryDelivery.supplierOrderNumber && !temporaryDelivery.customerOrderNumber
                                        }
                                        value={temporaryDelivery?.supplierOrderNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryDelivery({ ...temporaryDelivery, supplierOrderNumber: event.target.value })
                                        }
                                    />
                                </Grid>
                            </>
                        ) : (
                            <Grid item xs={12}>
                                {
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
                                        columns={createDeliveryColumns(handleDelete)}
                                        rows={dailyDeliveries}
                                        hideFooter
                                    />
                                }
                            </Grid>
                        )}
                    </Grid>
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {mode === 'create' && (
                            <Button
                                variant="contained"
                                color="primary"
                                sx={{ display: 'flex', gap: '.25rem' }}
                                onClick={() => handleSaveClick()}
                            >
                                <Save></Save> Save
                            </Button>
                        )}
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};
