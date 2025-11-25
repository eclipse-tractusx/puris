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
import { LabelledAutoComplete } from "@components/ui/LabelledAutoComplete";
import { useNotifications } from "@contexts/notificationContext";
import { config } from "@models/constants/config";
import { Material } from "@models/types/data/stock";
import { Close, Send } from "@mui/icons-material";
import { Input } from '@catena-x/portal-shared-components';
import { Box, Button, Dialog, DialogTitle, Grid, InputLabel, Stack } from "@mui/material";
import { useEffect, useState } from "react";
import { InfoButton } from "@components/ui/InfoButton";


const isValidMaterial = (material: Partial<Material>) =>
    !!material.ownMaterialNumber?.trim() &&
    !!material.name?.trim() &&
    (!config.app.GENERATE_MATERIAL_CATENAX_ID || !!material.materialNumberCx?.trim()) &&
    !!(material.materialFlag || material.productFlag);

const DIRECTIONS = [
    { key: 'inbound', value: 'Inbound' },
    { key: 'outbound', value: 'Outbound' },
    { key: 'bidirectional', value: 'Bidirectional' },
] as const;

type DirectionKey = (typeof DIRECTIONS)[number]['key'];

type MaterialInformationModalProps = {
    open: boolean;
    material?: Material | null;
    onClose: () => void;
    onSave: (material: Partial<Material>) => Promise<void> | void;
};

export const MaterialInformationModal = ({
    open,
    material,
    onClose,
    onSave,
}: MaterialInformationModalProps) => {
    const [temporaryMaterial, setTemporaryMaterial] = useState<Partial<Material>>({});
    const [direction, setDirection] = useState<DirectionKey | null>(null);
    const [formError, setFormError] = useState(false);
    const { notify } = useNotifications();

    useEffect(() => {
        if (!open) return;

        setTemporaryMaterial({
                ownMaterialNumber: '',
                name: '',
                materialNumberCx: null,
                materialFlag: false,
                productFlag: false,
            });
            setDirection(null);
    }, [open, material]);

    const handleClose = () => {
        setFormError(false);
        setTemporaryMaterial({});
        setDirection(null);
        onClose();
    };

    const handleDirectionChange = (value: { key: DirectionKey; value: string } | null) => {
        const key = value?.key ?? null;
        setDirection(key);

        setTemporaryMaterial((prev) => ({
            ...prev,
            materialFlag: key === 'outbound' || key === 'bidirectional',
            productFlag: key === 'inbound' || key === 'bidirectional',
        }));
    };

    const handleSaveClick = async () => {
        const sanitizedMaterial: Partial<Material> = {...temporaryMaterial, materialNumberCx: temporaryMaterial.materialNumberCx || null };
        if (!isValidMaterial(sanitizedMaterial)) {
            setFormError(true);
            return;
        }

        setFormError(false);

        try {
            await onSave(sanitizedMaterial);
            notify({
                title: 'Material created',
                description: 'Material has been created',
                severity: 'success',
            });

            handleClose();
        } catch (error: any) {
            notify({
                title: 'Error saving material',
                description: error?.message ?? 'Unknown error',
                severity: 'error',
            });
            handleClose();
        }
    };

    return (
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle variant="h3" textAlign="center">New Material</DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                
                <Grid container spacing={1} padding=".25rem">
                    <>
                        <Grid item xs={12}>
                            <InputLabel>Material Number*</InputLabel>
                            <Input
                                id="material-number"
                                type="text"
                                value={temporaryMaterial.ownMaterialNumber ?? ''}
                                onChange={(event) =>
                                    setTemporaryMaterial({
                                        ...temporaryMaterial,
                                        ownMaterialNumber: event.target.value,
                                    })
                                }
                                placeholder="Enter material number"
                                error={formError && !temporaryMaterial.ownMaterialNumber?.trim()}
                                data-testid="material-modal-material-number"
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <InputLabel>Name*</InputLabel>
                            <Input
                                id="material-name"
                                type="text"
                                value={temporaryMaterial.name ?? ''}
                                onChange={(event) =>
                                    setTemporaryMaterial({
                                        ...temporaryMaterial,
                                        name: event.target.value,
                                    })
                                }
                                placeholder="Enter material name"
                                error={formError && !temporaryMaterial.name?.trim()}
                                data-testid="material-modal-name"
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <InputLabel>Global Asset Id {config.app.GENERATE_MATERIAL_CATENAX_ID && '*'} 
                                <InfoButton text={`Your configuration ${config.app.GENERATE_MATERIAL_CATENAX_ID ? 'does not' : 'does'} generate the UUID for you. If you specify an outbound or bidirectional twin (a product), it makes sense to specify the UUID in case you want to also communicate this number to your customers.`} />
                            </InputLabel>
                            <Input
                                id="material-global-asset-id"
                                type="text"
                                value={temporaryMaterial.materialNumberCx ?? ''}
                                onChange={(event) =>
                                    setTemporaryMaterial({
                                        ...temporaryMaterial,
                                        materialNumberCx: event.target.value.trim() || null,
                                    })
                                }
                                placeholder="Enter global Asset Id"
                                error={formError && !temporaryMaterial.materialNumberCx?.trim()}
                                data-testid="material-modal-material-number-cx"
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <LabelledAutoComplete
                                id="direction"
                                options={DIRECTIONS}
                                getOptionLabel={(option) => option.value ?? ''}
                                isOptionEqualToValue={(option, value) => option?.key === value.key}
                                onChange={(_, value) => handleDirectionChange(value)}
                                value={DIRECTIONS.find((dt) => dt.key === direction) ?? null}
                                label="Direction*"
                                placeholder="Select direction"
                                error={formError && !direction}
                                data-testid="material-modal-direction"
                            />
                        </Grid>
                    </>
                </Grid>
                <Box
                    display="flex"
                    gap="1rem"
                    width="100%"
                    justifyContent="end"
                    marginTop="1rem"
                >
                    <Button
                        variant="outlined"
                        color="primary"
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleClose}
                    >
                        <Close /> Close
                    </Button>
                    <Button
                        variant="contained"
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleSaveClick}
                    >
                        <Send /> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};
