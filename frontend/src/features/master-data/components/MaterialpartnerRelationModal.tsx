/*
Copyright (c) 2025 Volkswagen AG
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
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { useNotifications } from '@contexts/notificationContext';
import { Material } from '@models/types/data/stock';
import { Close, Send } from '@mui/icons-material';
import { Input } from '@catena-x/portal-shared-components';
import { Box, Button, Checkbox, Dialog, DialogTitle, Grid, InputLabel, Stack } from '@mui/material';
import { SyntheticEvent, useEffect, useState } from 'react';
import { Partner } from '@models/types/edc/partner';
import { MaterialPartnerRelation } from '@models/types/data/material-partner-relation';
import { InfoButton } from '@components/ui/InfoButton';

const isValidMpr = (mpr: Partial<MaterialPartnerRelation>) =>
    !!mpr.ownMaterialNumber?.trim() &&
    !!mpr.partnerBpnl?.trim() &&
    !!mpr.partnerMaterialNumber?.trim() &&
    (mpr.partnerSuppliesMaterial || mpr.partnerBuysMaterial);

type MaterialPartnerRelationModalProps = {
    open: boolean;
    materials: Material[];
    partners: Partner[];
    mprs: MaterialPartnerRelation[];
    onClose: () => void;
    onSave: (material: Partial<MaterialPartnerRelation>) => Promise<void> | void;
};

export const MaterialPartnerRelationModal = ({ open, materials, partners, mprs, onClose, onSave }: MaterialPartnerRelationModalProps) => {
    const [temporaryMpr, setTemporaryMpr] = useState<Partial<MaterialPartnerRelation>>({});
    const [formError, setFormError] = useState(false);
    const { notify } = useNotifications();
    const selectedMaterial = materials.find((mat) => mat.ownMaterialNumber === temporaryMpr.ownMaterialNumber);
    const validMaterials = materials.filter((mat) => !mprs.some((existingMpr) =>
            existingMpr.ownMaterialNumber === mat.ownMaterialNumber &&
            existingMpr.partnerBpnl === temporaryMpr.partnerBpnl
        ));
    const validPartners = partners.filter((partner) => !mprs.some((existingMpr) =>
            existingMpr.partnerBpnl === partner.bpnl &&
            existingMpr.ownMaterialNumber === temporaryMpr.ownMaterialNumber
        ));


    useEffect(() => {
        if (!open) return;

        setTemporaryMpr({
            ownMaterialNumber: '',
            partnerBpnl: '',
            partnerMaterialNumber: '',
            partnerSuppliesMaterial: false,
            partnerBuysMaterial: false,
        });
    }, [open, setTemporaryMpr]);

    const handleMaterialChange = (_: SyntheticEvent, value: Material | null) => {
        setTemporaryMpr({
            ...temporaryMpr,
            ownMaterialNumber: value?.ownMaterialNumber ?? '',
            partnerSuppliesMaterial: value?.materialFlag,
            partnerBuysMaterial: value?.productFlag,
        });
    };

    const handleClose = () => {
        setFormError(false);
        setTemporaryMpr({});
        onClose();
    };

    const handleSaveClick = async () => {
        if (!isValidMpr(temporaryMpr)) {
            setFormError(true);
            return;
        }
        setFormError(false);

        try {
            await onSave(temporaryMpr);
            notify({
                title: 'Material-Partner Relation created',
                description: 'Material-Partner relation has been created',
                severity: 'success',
            });
        } catch (error: any) {
            notify({
                title: error.status === 409 ? 'Conflict' : 'Error saving Material-Partner relation',
                description: error?.message,
                severity: 'error',
            });
        }
        handleClose();
    };

    return (
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle variant="h3" textAlign="center">
                New Material-Partner Relation
            </DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                <Grid container spacing={1} padding=".25rem">
                    <>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="material"
                                disabled={validMaterials.length === 0}
                                options={validMaterials}
                                getOptionLabel={(option) => `${option.name} (${option.ownMaterialNumber})`}
                                isOptionEqualToValue={(option, value) => option?.ownMaterialNumber === value.ownMaterialNumber}
                                onChange={handleMaterialChange}
                                value={validMaterials.find((mat) => mat.ownMaterialNumber === temporaryMpr.ownMaterialNumber) ?? null}
                                label="Material*"
                                placeholder={validMaterials.length === 0 ? "No valid materials for the selected partner" : "Select material"}
                                error={formError && !temporaryMpr.ownMaterialNumber?.trim()}
                                data-testid="mpr-modal-material"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="partner"
                                options={validPartners}
                                disabled={validPartners.length === 0}
                                getOptionLabel={(option) => `${option.name} (${option.bpnl})`}
                                isOptionEqualToValue={(option, value) => option?.bpnl === value.bpnl}
                                onChange={(_, value) =>
                                    setTemporaryMpr({
                                        ...temporaryMpr,
                                        partnerBpnl: value?.bpnl ?? '',
                                    })
                                }
                                value={validPartners.find((p) => p.bpnl === temporaryMpr.partnerBpnl) ?? null}
                                label="Partner*"
                                placeholder={validPartners.length === 0 ? "No valid partners for the selected material" : "Select partner"}
                                error={formError && !temporaryMpr.partnerBpnl?.trim()}
                                data-testid="mpr-modal-partner"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <InputLabel>Partner Material Number*</InputLabel>
                            <Input
                                id="material-number"
                                type="text"
                                value={temporaryMpr.partnerMaterialNumber ?? ''}
                                onChange={(event) =>
                                    setTemporaryMpr({
                                        ...temporaryMpr,
                                        partnerMaterialNumber: event.target.value,
                                    })
                                }
                                placeholder="Enter material number"
                                error={formError && !temporaryMpr.partnerMaterialNumber?.trim()}
                                data-testid="material-modal-material-number"
                            />
                        </Grid>
                        <Grid item xs={6} alignContent="end">
                            <Stack>
                                <Stack direction="row" alignItems="center" spacing=".25rem">
                                    <InputLabel>Relation Type*</InputLabel>
                                    <InfoButton text="These values are defaulted based on the material's definition (Inbound, Outbound, Bidirectional)" />
                                </Stack>
                                <Stack direction="row" alignItems="center">
                                    <Checkbox
                                        id="partnerSuppliesMaterial"
                                        disabled={!selectedMaterial?.materialFlag || !selectedMaterial.productFlag}
                                        checked={temporaryMpr?.partnerSuppliesMaterial ?? false}
                                        onChange={(event) => setTemporaryMpr({ ...temporaryMpr, partnerSuppliesMaterial: event.target.checked })}
                                        data-testid="mpr-modal-partner-supplies-material"
                                        />
                                    <InputLabel htmlFor="partnerSuppliesMaterial"> supplies material </InputLabel>
                                    <Checkbox
                                        id="partnerBuysMaterial"
                                        disabled={!selectedMaterial?.materialFlag || !selectedMaterial.productFlag}
                                        checked={temporaryMpr?.partnerBuysMaterial ?? false}
                                        onChange={(event) => setTemporaryMpr({ ...temporaryMpr, partnerBuysMaterial: event.target.checked })}
                                        data-testid="mpr-modal-partner-buys-material"
                                        />
                                    <InputLabel htmlFor="partnerBuysMaterial"> buys material </InputLabel>
                                </Stack>
                            </Stack>
                        </Grid>
                            
                    </>
                </Grid>
                <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                    <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                        <Close /> Close
                    </Button>
                    <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleSaveClick}>
                        <Send /> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};
