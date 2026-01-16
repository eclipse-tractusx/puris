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
import { Input} from '@catena-x/portal-shared-components';
import { DateTime } from '@components/ui/DateTime';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Delivery } from '@models/types/data/delivery';
import { Close, Save } from '@mui/icons-material';
import { Box, Button, Dialog, DialogTitle, FormLabel, Grid, Stack, capitalize } from '@mui/material';
import { postDelivery, updateDelivery } from '@services/delivery-service';
import { getUnitOfMeasurement, isValidOrderReference } from '@util/helpers';
import { useEffect, useState } from 'react';
import { INCOTERMS } from '@models/constants/incoterms';
import { ARRIVAL_TYPES, DEPARTURE_TYPES } from '@models/constants/event-type';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';
import { useSiteDesignations } from '../hooks/useSiteDesignations';
import { useNotifications } from '@contexts/notificationContext';
import { DirectionType } from '@models/types/erp/directionType';
import { Site } from '@models/types/edc/site';

export type DeliveryInformationCreationModalProps = {
    open: boolean;
    direction: DirectionType;
    site: Site | null;
    onClose: () => void;
    onSave: (d?: Delivery) => void;
    onRemove?: (deletedUuid: string) => void;
    delivery: Delivery | null;
    deliveries: Delivery[];
};

const isValidDelivery = (delivery: Partial<Delivery>, previousDeliveryState: Delivery | null) =>
    delivery &&
    delivery.ownMaterialNumber &&
    delivery.originBpns &&
    delivery.partnerBpnl &&
    delivery.destinationBpns &&
    typeof delivery.quantity === 'number' &&
    delivery.quantity > 0 &&
    delivery.measurementUnit &&
    delivery.incoterm &&
    delivery.dateOfDeparture &&
    delivery.dateOfArrival &&
    delivery.departureType &&
    delivery.arrivalType &&
    new Date(delivery.dateOfArrival) >= new Date(delivery.dateOfDeparture) &&
    (delivery.departureType !== 'actual-departure' || (previousDeliveryState?.departureType === 'actual-departure' || new Date(delivery.dateOfDeparture) <= new Date())) &&
    (delivery.arrivalType !== 'actual-arrival' || (previousDeliveryState?.arrivalType === 'actual-arrival' || new Date(delivery.dateOfArrival) <= new Date())) &&
    isValidOrderReference(delivery);

export const DeliveryCreationModal = ({
    open,
    direction,
    onClose,
    onSave,
    delivery,
}: DeliveryInformationCreationModalProps) => {
    const [temporaryDelivery, setTemporaryDelivery] = useState<Partial<Delivery>>({});
    const { partners } = usePartners(
        direction === DirectionType.Outbound ? 'product' : 'material',
        temporaryDelivery?.ownMaterialNumber ?? null
    );
    const { siteDesignations } = useSiteDesignations(delivery?.ownMaterialNumber ?? null, direction);
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);
    const mode = temporaryDelivery?.uuid ? 'edit' : 'create';
    const sites = siteDesignations?.reduce((acc: Site[], sd) => temporaryDelivery.partnerBpnl && sd.partnerBpnls.includes(temporaryDelivery.partnerBpnl) ? [...acc, sd.site] : acc, []) ?? [];
    const handleSaveClick = () => {
        temporaryDelivery.customerOrderNumber ||= undefined;
        temporaryDelivery.customerOrderPositionNumber ||= undefined;
        temporaryDelivery.supplierOrderNumber ||= undefined;
        if (!isValidDelivery(temporaryDelivery, delivery)) {
            setFormError(true);
            return;
        }
        setFormError(false);
        const method = mode === 'create' ? postDelivery : updateDelivery;
        const successLabel = mode === 'create' ? 'Delivery Added' : 'Delivery updated';
        const successDescription = mode === 'create' ? 'The Delivery has been added' : 'The Delivery has been successfully updated';
        method(temporaryDelivery)
            .then((d) => {
                onSave(d);
                notify({
                    title: successLabel,
                    description: successDescription,
                    severity: 'success',
                });
            })
            .catch((error) => {
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                    description: error.status === 409 ? 'Delivery conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            })
            .finally(() => onClose());
    };
    
    const renderSiteSelectors = () => {
        const ownSiteSelecor = (
            <Grid item xs={6}>
                <LabelledAutoComplete
                    id="ownBpns"
                    options={sites ?? []}
                    getOptionLabel={(option) => option.name ?? ''}
                    disabled={!temporaryDelivery?.partnerBpnl}
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
                    data-testid="delivery-own-bpns-field"
                />
            </Grid>
        );
        const partnerSiteSelector = (
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
                    data-testid="delivery-partner-bpns-field"
                />
            </Grid>
        );
        return direction === DirectionType.Inbound ? (
                <>
                    {partnerSiteSelector}
                    {ownSiteSelecor}
                </>
            ) : (
                <>
                    {ownSiteSelecor}
                    {partnerSiteSelector}
                </>
            )
    };
    const handleClose = () => {
        setFormError(false);
        setTemporaryDelivery({});
        onClose();
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
                    {capitalize(mode)} Delivery Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    <Grid container spacing={1} padding=".25rem">
                        <GridItem label="Material Number" value={temporaryDelivery.ownMaterialNumber ?? ''} />
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
                                data-testid="delivery-partner-field"
                                disabled={mode === 'edit'}
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
                                error={formError && 
                                    (
                                        !temporaryDelivery?.departureType || 
                                        (
                                            temporaryDelivery?.departureType === 'actual-departure' &&
                                            (
                                                !temporaryDelivery.dateOfDeparture ||
                                                new Date(temporaryDelivery.dateOfDeparture) > new Date()
                                            )
                                        )
                                    )
                                }
                                data-testid="delivery-departure-type-field"
                                disabled={mode === 'edit' && delivery?.departureType === 'actual-departure'}
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
                                    (
                                        !temporaryDelivery?.arrivalType ||
                                        (
                                            temporaryDelivery?.arrivalType === 'actual-arrival' && 
                                            (
                                                temporaryDelivery?.departureType !== 'actual-departure' || 
                                                !temporaryDelivery.dateOfArrival ||
                                                new Date(temporaryDelivery.dateOfArrival) > new Date()
                                            )
                                        )
                                    )
                                }
                                data-testid="delivery-arrival-type-field"
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
                            ></LabelledAutoComplete>
                        </Grid>
                        <Grid item xs={6} display="flex" alignItems="end" data-testid="delivery-departure-time-field">
                            <DateTime
                                label="Departure Time*"
                                placeholder="Pick Departure Date"
                                locale="de"
                                error={
                                    formError &&
                                    (!temporaryDelivery.dateOfDeparture ||
                                        (temporaryDelivery.departureType === 'actual-departure' &&
                                            new Date(temporaryDelivery.dateOfDeparture) > new Date()) ||
                                        (!!temporaryDelivery.dateOfArrival &&
                                            new Date(temporaryDelivery.dateOfArrival) < new Date(temporaryDelivery.dateOfDeparture)))
                                }
                                value={temporaryDelivery?.dateOfDeparture ?? null}
                                onValueChange={(date) => setTemporaryDelivery({ ...temporaryDelivery, dateOfDeparture: date ?? undefined })}
                                disabled={mode === 'edit' && delivery?.departureType === 'actual-departure'}
                            />
                        </Grid>
                        <Grid item xs={6} display="flex" alignItems="end" data-testid="delivery-arrival-time-field">
                            <DateTime
                                label="Arrival Time*"
                                placeholder="Pick Arrival Date"
                                locale="de"
                                error={
                                    formError &&
                                    (!temporaryDelivery?.dateOfArrival ||
                                        (temporaryDelivery?.arrivalType === 'actual-arrival' &&
                                            new Date(temporaryDelivery.dateOfArrival) > new Date()))
                                }
                                value={temporaryDelivery?.dateOfArrival ?? null}
                                onValueChange={(date) => setTemporaryDelivery({ ...temporaryDelivery, dateOfArrival: date ?? undefined })}
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
                            />
                        </Grid>
                        {renderSiteSelectors()}
                        <Grid item xs={6}>
                            <FormLabel>Quantity*</FormLabel>
                            <Input
                                hiddenLabel
                                type="number"
                                value={temporaryDelivery.quantity ?? ''}
                                error={formError && (temporaryDelivery?.quantity == null || temporaryDelivery.quantity <= 0)}
                                onChange={(e) =>
                                    setTemporaryDelivery((curr) => ({
                                        ...curr,
                                        quantity: e.target.value ? parseFloat(e.target.value) : undefined,
                                    }))
                                }
                                sx={{ marginTop: '.5rem' }}
                                data-testid="delivery-quantity-field"
                                disabled={delivery?.arrivalType === 'actual-arrival'}
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
                                data-testid="delivery-uom-field"
                                disabled={mode === 'edit'}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <FormLabel>Tracking Number</FormLabel>
                            <Input
                                id="tracking-number"
                                hiddenLabel
                                type="text"
                                value={temporaryDelivery?.trackingNumber ?? ''}
                                onChange={(event) => setTemporaryDelivery({ ...temporaryDelivery, trackingNumber: event.target.value })}
                                sx={{ marginTop: '.5rem' }}
                                data-testid="delivery-tracking-number-field"
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="incoterm"
                                value={
                                    temporaryDelivery.incoterm ? INCOTERMS.find((i) => i.key === temporaryDelivery.incoterm) ?? null : null
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
                                data-testid="delivery-incoterm-field"
                                disabled={mode === 'edit' && delivery?.departureType === 'actual-departure'}
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
                                data-testid="delivery-customer-order-number-field"
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
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
                                data-testid="delivery-customer-order-position-field"
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <FormLabel>Supplier Order Number</FormLabel>
                            <Input
                                id="supplier-order-number"
                                type="text"
                                error={formError && !!temporaryDelivery.supplierOrderNumber && !temporaryDelivery.customerOrderNumber}
                                value={temporaryDelivery?.supplierOrderNumber ?? ''}
                                onChange={(event) =>
                                    setTemporaryDelivery({ ...temporaryDelivery, supplierOrderNumber: event.target.value })
                                }
                                data-testid="delivery-supplier-order-number-field"
                                disabled={mode === 'edit' && delivery?.arrivalType === 'actual-arrival'}
                            />
                        </Grid>
                    </Grid>
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        <Button
                            variant="contained"
                            color="primary"
                            sx={{ display: 'flex', gap: '.25rem' }}
                            onClick={() => handleSaveClick()}
                            data-testid="save-delivery-button"
                        >
                            <Save></Save> Save
                        </Button>
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};
