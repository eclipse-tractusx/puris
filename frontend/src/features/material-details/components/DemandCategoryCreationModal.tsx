/*
Copyright (c) 2025 IAV
Copyright (c) 2025 Contributors to the Eclipse Foundation

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
    Box,
    Button,
    Dialog,
    DialogTitle,
    FormLabel,
    Grid,
    Stack,
} from '@mui/material';
import { Input, Datepicker } from '@catena-x/portal-shared-components';
import { Close, Save } from '@mui/icons-material';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Demand } from '@models/types/data/demand';
import { getUnitOfMeasurement } from '@util/helpers';
import { useSites } from '@features/stock-view/hooks/useSites';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { postDemand, updateDemand } from '@services/demands-service';
import { useNotifications } from '@contexts/notificationContext';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';

type DemandCategoryModalProps = {
    open: boolean;
    onClose: () => void;
    onSave: (updated: Demand) => void
    demand: Partial<Demand> | null
};

const isValidDemand = (d: Partial<Demand>) =>
    d?.day &&
    d?.demandLocationBpns &&
    d?.quantity &&
    d?.demandCategoryCode &&
    d?.measurementUnit &&
    d?.partnerBpnl;

export const DemandCategoryCreationModal = ({
    open,
    onClose,
    onSave,
    demand
}: DemandCategoryModalProps) => {
    const { notify } = useNotifications();
    const { sites } = useSites();
    const { partners } = usePartners('material', demand?.ownMaterialNumber ?? null);
    const [temporaryDemand, setTemporaryDemand] = useState<Partial<Demand>>(demand ?? {});
    const [formError, setFormError] = useState(false);
    const [originalData, setOriginalData] = useState<Partial<Demand>>(demand ?? {});
    const mode = temporaryDemand?.uuid ? 'edit' : 'create';
    const isFormChanged = JSON.stringify(temporaryDemand) !== JSON.stringify(originalData);

    useEffect(() => {
        setTemporaryDemand(demand ?? {});
        setOriginalData(demand ?? {});
    }, [demand]);


    const handleSaveClickClick = () => {
        if (!isValidDemand(temporaryDemand)) {
            setFormError(true);
            return;
        }

        const method = mode === 'create' ? postDemand : updateDemand;
        const successLabel = mode === 'create' ? 'Demand Created' : 'Demand Updated';
        const successDescription = mode === 'create' ? 'The Demand has been added' : 'The Demand has been successfully updated';
        method({
            ...temporaryDemand,
            lastUpdatedOnDateTime: new Date()
        })
            .then((d) => {
                onSave(d);
                notify({
                    title: successLabel,
                    description: successDescription,
                    severity: 'success'
                });
            })
            .catch((error) => {
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                    description: error.status === 409 ? 'Demand conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            }).finally(() => handleClose());
    };

    const handleClose = () => {
        setFormError(false);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle variant="h3" textAlign="center">
                Demand Information
            </DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                <Grid container spacing={2} padding=".25rem">
                    <GridItem label="Material Number" value={temporaryDemand.ownMaterialNumber ?? ''} />

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="demandLocationBpns"
                            options={sites ?? []}
                            getOptionLabel={(option) => option.name ?? ''}
                            error={formError}
                            isOptionEqualToValue={(option, value) => option?.bpns === value.bpns}
                            onChange={(_, value) =>
                                setTemporaryDemand({ ...temporaryDemand, demandLocationBpns: value?.bpns ?? undefined })
                            }
                            value={sites?.find((s: any) => s.bpns === temporaryDemand.demandLocationBpns) ?? null}
                            label="Demand Site*"
                            placeholder="Select a Site"
                            data-testid="demand-location-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <FormLabel sx={{ marginBottom: '.5rem', display: 'block' }}>Day*</FormLabel>
                        <div className="date-picker" data-testid="demand-day-field">
                            <Datepicker
                                id="day"
                                label=""
                                hiddenLabel
                                placeholder="Pick a Day"
                                locale="de"
                                error={formError && !temporaryDemand?.day}
                                readOnly={false}
                                defaultValue={temporaryDemand?.day}
                                onChangeItem={(value) => setTemporaryDemand((curr) => ({ ...curr, day: value ?? undefined }))}
                            />
                        </div>
                    </Grid>

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="demandCategoryCode"
                            options={DEMAND_CATEGORY}
                            getOptionLabel={(option) => option?.value ?? ''}
                            onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, demandCategoryCode: value?.key }))}
                            isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            value={{
                                key: temporaryDemand.demandCategoryCode,
                                value: DEMAND_CATEGORY.find((c) => c.key === temporaryDemand.demandCategoryCode)?.value,
                            }}
                            label="Category*"
                            placeholder="Select category"
                            error={formError && !temporaryDemand?.demandCategoryCode}
                            data-testid="demand-category-field"
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <FormLabel>Quantity*</FormLabel>
                        <Input
                            type="number"
                            placeholder="Enter quantity"
                            value={temporaryDemand.quantity ?? ''}
                            error={formError && !temporaryDemand?.quantity}
                            onChange={(e) =>
                                setTemporaryDemand((curr) =>
                                    parseFloat(e.target.value) >= 0
                                        ? { ...curr, quantity: parseFloat(e.target.value) }
                                        : { ...curr, quantity: 0 }
                                )
                            }
                            sx={{ marginTop: '.5rem' }}
                            data-testid="demand-quantity-field"
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="measurementUnit"
                            options={UNITS_OF_MEASUREMENT}
                            getOptionLabel={(option) => option?.value ?? ''}
                            onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, measurementUnit: value?.key }))}
                            isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            value={
                                temporaryDemand.measurementUnit
                                    ? {
                                        key: temporaryDemand.measurementUnit,
                                        value: getUnitOfMeasurement(temporaryDemand.measurementUnit),
                                    }
                                    : null
                            }
                            label="UOM*"
                            placeholder="Select unit"
                            error={formError && !temporaryDemand?.measurementUnit}
                            data-testid="demand-uom-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="partnerBpnl"
                            options={partners ?? []}
                            getOptionLabel={(option) => option?.name ?? ''}
                            isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                            value={partners?.find((s) => s.bpnl === temporaryDemand.partnerBpnl) ?? null}
                            onChange={(_, value) =>
                                setTemporaryDemand({ ...temporaryDemand, partnerBpnl: value?.bpnl ?? undefined })
                            }
                            label="Partner*"
                            placeholder="Select a Partner"
                            error={formError && !temporaryDemand?.partnerBpnl}
                            data-testid="demand-partner-field"
                            disabled={mode === 'edit'}
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="supplierLocationBpns"
                            options={partners?.find((s) => s.bpnl === temporaryDemand.partnerBpnl)?.sites ?? []}
                            getOptionLabel={(option) => option.name ?? ''}
                            disabled={!temporaryDemand?.partnerBpnl || mode === 'edit'}
                            isOptionEqualToValue={(option, value) => option?.bpns === value.bpns}
                            onChange={(_, value) =>
                                setTemporaryDemand({ ...temporaryDemand, supplierLocationBpns: value?.bpns ?? undefined })
                            }
                            value={
                                partners
                                    ?.find((s) => s.bpnl === temporaryDemand.partnerBpnl)
                                    ?.sites.find((s) => s.bpns === temporaryDemand.supplierLocationBpns) ?? null
                            }
                            label="Expected Supplier Site"
                            placeholder="Select a Site"
                            data-testid="demand-supplier-site-field"
                        />
                    </Grid>
                </Grid>

                <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                    <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                        <Close></Close> Close
                    </Button>
                    <Button
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleSaveClickClick}
                        data-testid="save-delivery-button"
                        disabled={mode === 'edit' && !isFormChanged}
                    >
                        <Save></Save> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};