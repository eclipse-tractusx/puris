/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2025 IAV GmbH

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

import { useEffect, useState } from 'react';
import {
    Dialog,
    DialogTitle,
    Grid,
    Button,
    Stack,
    FormLabel,
    Box,
    capitalize,
} from '@mui/material';
import { Input } from '@catena-x/portal-shared-components';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { DateTime } from '@components/ui/DateTime';
import { Save, Close } from '@mui/icons-material';
import { Production } from '@models/types/data/production';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { getUnitOfMeasurement, isValidOrderReference } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { useSites } from '@features/stock-view/hooks/useSites';
import { postProduction, updateProduction } from '@services/productions-service';
import { useNotifications } from '@contexts/notificationContext';
import { GridItem } from '@components/ui/GridItem';
import { withDefaultProductionTime } from '@util/production-helpers';

type ProductionCategoryCreationModalProps = {
    open: boolean;
    production: Partial<Production> | null;
    onClose: () => void;
    onSave: (d?: Production) => void;
};

const isValidProduction = (production: Partial<Production>) =>
    production &&
    production.productionSiteBpns &&
    production.estimatedTimeOfCompletion &&
    typeof production.quantity === 'number' && production.quantity >= 0 &&
    production.measurementUnit &&
    production.partner &&
    isValidOrderReference(production);

export const PlannedProductionCreationModal = ({
    open,
    production,
    onClose,
    onSave,
}: ProductionCategoryCreationModalProps) => {
    const [temporaryProduction, setTemporaryProduction] = useState<Partial<Production>>(production ?? {});
    const { partners } = usePartners('product', temporaryProduction?.material?.materialNumberSupplier ?? null);
    const { sites } = useSites();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);
    const [originalData, setOriginalData] = useState<Partial<Production>>(production ?? {});
    const mode = temporaryProduction?.uuid ? 'edit' : 'create';
    const isFormChanged = JSON.stringify(temporaryProduction) !== JSON.stringify(originalData);
    useEffect(() => {
        setTemporaryProduction(production ?? {});
        setOriginalData(production ?? {});
    }, [production]);

    const handleSaveClick = () => {
        temporaryProduction.customerOrderNumber ||= undefined;
        temporaryProduction.customerOrderPositionNumber ||= undefined;
        temporaryProduction.supplierOrderNumber ||= undefined;

        if (!isValidProduction(temporaryProduction)) {
            setFormError(true);
            return;
        }

        setFormError(false);
        const method = mode === 'create' ? postProduction : updateProduction;
        const successLabel = mode === 'create' ? 'Production Created' : 'Production Updated';
        const successDescription = mode === 'create' ? 'The Production has been added' : 'The Production has been successfully updated';

        method({
            ...temporaryProduction,
            lastUpdatedOnDateTime: new Date().toISOString(),
        })
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
                    description: error.status === 409 ? 'Production conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            })
            .finally(() => handleClose());
    };

    const handleClose = () => {
        setFormError(false);
        setTemporaryProduction({})
        onClose();
    };

    return (
        <Dialog open={open && production !== null} onClose={handleClose}>
            <DialogTitle textAlign="center" variant="h3">
                {capitalize(mode)} Production Information
            </DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                <Grid container spacing={2} padding=".25rem">
                    <GridItem label="Material Number" value={temporaryProduction.material?.materialNumberSupplier ?? ''} />

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="productionSiteBpns"
                            options={sites ?? []}
                            getOptionLabel={(option) => option.name ?? ''}
                            error={formError && !temporaryProduction.productionSiteBpns}
                            isOptionEqualToValue={(option, value) => option.bpns === value.bpns}
                            onChange={(_, value) => setTemporaryProduction({ ...temporaryProduction, productionSiteBpns: value?.bpns })}
                            value={sites?.find((s) => s.bpns === temporaryProduction.productionSiteBpns) ?? null}
                            label="Production Site*"
                            placeholder="Select a Site"
                            data-testid="production-site-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>
                    <Grid item xs={6} display="flex" alignItems="end" data-testid="production-completion-time-field">
                        <DateTime
                            label="Estimated Completion Time*"
                            placeholder="Pick Production Date"
                            locale="de"
                            error={formError}
                            value={temporaryProduction.estimatedTimeOfCompletion ?? null}
                            onValueChange={(date) => 
                                setTemporaryProduction({
                                    ...temporaryProduction,
                                    estimatedTimeOfCompletion: temporaryProduction.estimatedTimeOfCompletion == null
                                        ? withDefaultProductionTime(date)
                                        : date ?? undefined
                                })
                            }
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="partner"
                            options={partners ?? []}
                            getOptionLabel={(option) => option?.name ?? ''}
                            label="Partner*"
                            placeholder="Select a Partner"
                            error={formError && !temporaryProduction.partner}
                            onChange={(_, value) => setTemporaryProduction({ ...temporaryProduction, partner: value ?? undefined })}
                            value={temporaryProduction.partner ?? null}
                            isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                            data-testid="production-partner-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <FormLabel>Quantity*</FormLabel>
                        <Input
                            id="quantity"
                            type="number"
                            placeholder="Enter quantity"
                            value={temporaryProduction.quantity ?? ''}
                            error={formError && !temporaryProduction.quantity}
                            onChange={(e) =>
                                setTemporaryProduction((curr) => ({
                                    ...curr,
                                    quantity: e.target.value ? parseFloat(e.target.value) : undefined,
                                }))
                            }
                            sx={{ marginTop: '.5rem' }}
                            data-testid="production-quantity-field"
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="uom"
                            options={UNITS_OF_MEASUREMENT}
                            getOptionLabel={(option) => option?.value ?? ''}
                            label="UOM*"
                            placeholder="Select unit"
                            error={formError && !temporaryProduction.measurementUnit}
                            onChange={(_, value) => setTemporaryProduction((curr) => ({ ...curr, measurementUnit: value?.key }))}
                            value={
                                temporaryProduction.measurementUnit
                                    ? { key: temporaryProduction.measurementUnit, value: getUnitOfMeasurement(temporaryProduction.measurementUnit) }
                                    : null
                            }
                            isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            data-testid="production-uom-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <FormLabel>Customer Order Number</FormLabel>
                        <Input
                            id="customer-order-number"
                            type="text"
                            error={formError && !isValidOrderReference(temporaryProduction)}
                            value={temporaryProduction.customerOrderNumber ?? ''}
                            onChange={(e) => setTemporaryProduction({ ...temporaryProduction, customerOrderNumber: e.target.value })}
                            data-testid="production-customer-order-number-field"
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <FormLabel>Customer Order Position</FormLabel>
                        <Input
                            id="customer-order-position-number"
                            type="text"
                            error={formError && !isValidOrderReference(temporaryProduction)}
                            value={temporaryProduction.customerOrderPositionNumber ?? ''}
                            onChange={(e) => setTemporaryProduction({ ...temporaryProduction, customerOrderPositionNumber: e.target.value })}
                            data-testid="production-customer-order-position-field"
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <FormLabel>Supplier Order Number</FormLabel>
                        <Input
                            id="supplier-order-number"
                            type="text"
                            value={temporaryProduction.supplierOrderNumber ?? ''}
                            onChange={(e) => setTemporaryProduction({ ...temporaryProduction, supplierOrderNumber: e.target.value })}
                            data-testid="production-supplier-order-number-field"
                        />
                    </Grid>
                </Grid>

                <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                    <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                        <Close /> Close
                    </Button>
                    <Button
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleSaveClick}
                        data-testid="save-production-button"
                        disabled={mode === 'edit' && !isFormChanged}
                    >
                        <Save /> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};
