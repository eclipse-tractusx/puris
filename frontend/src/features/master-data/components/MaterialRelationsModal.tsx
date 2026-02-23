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
import { Box, Button, Dialog, DialogTitle, Grid, InputLabel, Stack } from '@mui/material';
import { useEffect, useState } from 'react';
import { getUnitOfMeasurement } from '@util/helpers';
import { MaterialRelation } from '@models/types/data/material-relation';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { DateTime } from '@components/ui/DateTime';

const isValidMr = (mr: Partial<MaterialRelation>) =>
  !!mr.parentMaterialNumber?.trim() &&
  !!mr.childMaterialNumber?.trim() &&
  typeof mr.quantity === 'number' &&
  mr.quantity > 0 &&
  !!mr.measurementUnit &&
  (
    !mr.validFrom ||
    !mr.validTo ||
    new Date(mr.validTo) >= new Date(mr.validFrom)
  );

type MaterialRelationModalProps = {
    open: boolean;
    allMaterials: Material[];
    mrs: MaterialRelation[];
    onClose: () => void;
    onSave: (material: Partial<MaterialRelation>) => Promise<void> | void;
};

export const MaterialRelationModal = ({ open, allMaterials, mrs, onClose, onSave }: MaterialRelationModalProps) => {
    const [temporaryMr, setTemporaryMr] = useState<Partial<MaterialRelation>>({});
    const [formError, setFormError] = useState(false);
    const { notify } = useNotifications();
    const parentBase = allMaterials.filter(m => m.productFlag);
    const childBase  = allMaterials.filter(m => m.materialFlag);

    const validParentMaterials = parentBase.filter((mat) =>
    !mrs.some((existingMr) =>
        existingMr.parentMaterialNumber === mat.ownMaterialNumber &&
        existingMr.childMaterialNumber === temporaryMr.childMaterialNumber
    )
    );

    const validChildMaterials = childBase.filter((mat) =>
    !mrs.some((existingMr) =>
        existingMr.parentMaterialNumber === temporaryMr.parentMaterialNumber &&
        existingMr.childMaterialNumber === mat.ownMaterialNumber
    )
    );


    useEffect(() => {
        if (!open) return;

        setTemporaryMr({
            parentMaterialNumber: '',
            childMaterialNumber: '',
            quantity: 0,
            measurementUnit: undefined,
        });
    }, [open, setTemporaryMr]);

    const handleClose = () => {
        setFormError(false);
        setTemporaryMr({});
        onClose();
    };

    const handleSaveClick = async () => {
        if (!isValidMr(temporaryMr)) {
            setFormError(true);
            return;
        }
        setFormError(false);

        try {
            await onSave(temporaryMr);
            notify({
                title: 'Material Relation created',
                description: 'Material relation has been created',
                severity: 'success',
            });
        } catch (error: any) {
            notify({
                title: error.status === 409 ? 'Conflict' : 'Error saving Material Relation',
                description: error?.message,
                severity: 'error',
            });
        }
        handleClose();
    };

    return (
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle variant="h3" textAlign="center">
                New Material Relation
            </DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                <Grid container spacing={1} padding=".25rem">
                    <>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="parent-material"
                                disabled={validParentMaterials.length === 0}
                                options={validParentMaterials}
                                getOptionLabel={(option) => `${option.name} (${option.ownMaterialNumber})`}
                                isOptionEqualToValue={(option, value) => option?.ownMaterialNumber === value.ownMaterialNumber}
                                onChange={(_, value) =>
                                    setTemporaryMr({
                                        ...temporaryMr,
                                        parentMaterialNumber: value?.ownMaterialNumber ?? '',
                                    })
                                }
                                value={validParentMaterials.find((mat) => mat.ownMaterialNumber === temporaryMr.parentMaterialNumber) ?? null}
                                label="Parent Material*"
                                placeholder={validParentMaterials.length === 0 ? "No valid parent materials available" : "Select material"}
                                error={formError && !temporaryMr.parentMaterialNumber?.trim()}
                                data-testid="material-relations-modal-parent-material"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="child-material"
                                options={validChildMaterials}
                                disabled={validChildMaterials.length === 0}
                                getOptionLabel={(option) => `${option.name} (${option.ownMaterialNumber})`}
                                isOptionEqualToValue={(option, value) => option?.ownMaterialNumber === value.ownMaterialNumber}
                                onChange={(_, value) =>
                                    setTemporaryMr({
                                        ...temporaryMr,
                                        childMaterialNumber: value?.ownMaterialNumber ?? '',
                                    })
                                }
                                value={validChildMaterials.find((p) => p.ownMaterialNumber === temporaryMr.childMaterialNumber) ?? null}
                                label="Child Material*"
                                placeholder={validChildMaterials.length === 0 ? "No valid child materials available" : "Select child material"}
                                error={formError && !temporaryMr.childMaterialNumber?.trim()}
                                data-testid="material-relations-modal-child-material"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <InputLabel>Quantity*</InputLabel>
                            <Input
                                id="quantity"
                                type="number"
                                placeholder="Enter quantity"
                                value={temporaryMr.quantity ?? ''}
                                error={formError && !temporaryMr.quantity}
                                onChange={(e) =>
                                    setTemporaryMr((curr) => ({
                                        ...curr,
                                        quantity: e.target.value ? parseFloat(e.target.value) : undefined,
                                    }))
                                }
                                sx={{ marginTop: '.5rem' }}
                                data-testid="material-relation-modal-quantity"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="uom"
                                options={UNITS_OF_MEASUREMENT}
                                getOptionLabel={(option) => option?.value ?? ''}
                                label="UOM*"
                                placeholder="Select unit"
                                error={formError && !temporaryMr.measurementUnit}
                                onChange={(_, value) => setTemporaryMr((curr) => ({ ...curr, measurementUnit: value?.key }))}
                                value={
                                    temporaryMr.measurementUnit
                                        ? { key: temporaryMr.measurementUnit, value: getUnitOfMeasurement(temporaryMr.measurementUnit) }
                                        : null
                                }
                                isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                data-testid="material-relations-modal-uom"
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <DateTime
                                label="Valid From"
                                placeholder="Pick a start Date"
                                locale="de"
                                error={
                                    formError &&
                                    (
                                        (!!temporaryMr.validFrom && new Date(temporaryMr.validFrom) > new Date()) ||
                                        (!!temporaryMr.validFrom && !!temporaryMr.validTo &&
                                        new Date(temporaryMr.validTo) < new Date(temporaryMr.validFrom))
                                    )
                                }
                                value={temporaryMr.validFrom ?? null}
                                onValueChange={(date) =>
                                    setTemporaryMr({ ...temporaryMr, validFrom: date ?? undefined })
                                }
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <DateTime
                                label="Valid To"
                                placeholder="Pick an end Date"
                                locale="de"
                                error={
                                    formError &&
                                    (
                                        (!!temporaryMr.validTo && new Date(temporaryMr.validTo) > new Date()) ||
                                        (!!temporaryMr.validFrom && !!temporaryMr.validTo &&
                                        new Date(temporaryMr.validTo) < new Date(temporaryMr.validFrom))
                                    )
                                }
                                value={temporaryMr.validTo ?? null}
                                onValueChange={(date) =>
                                    setTemporaryMr({ ...temporaryMr, validTo: date ?? undefined })
                                }
                            />
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
