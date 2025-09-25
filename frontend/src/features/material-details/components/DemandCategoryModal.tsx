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
import { useEffect, useMemo, useState } from 'react';
import { Datepicker, Input, Table } from '@catena-x/portal-shared-components';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Demand } from '@models/types/data/demand';
import { Box, Button, Dialog, DialogTitle, FormLabel, Grid, Stack } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { deleteDemand, postDemand } from '@services/demands-service';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { Close, Delete, Save } from '@mui/icons-material';
import { ModalMode } from '@models/types/data/modal-mode';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useNotifications } from '@contexts/notificationContext';

const createDemandColumns = (handleDelete?: (row: Demand) => void) => {
    const columns = [
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
    ] as const;
    if (handleDelete) {
        return [
            ...columns,
            {
                field: 'delete',
                headerName: '',
                sortable: false,
                disableColumnMenu: true,
                headerAlign: 'center',
                type: 'string',
                width: 30,
                renderCell: (data: { row: Demand }) => {
                    return (
                        <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Button variant="text" color="error" onClick={() => handleDelete(data.row)} data-testid="delete-demand">
                                <Delete></Delete>
                            </Button>
                        </Box>
                    );
                },
            },
        ] as const;
    }
    return columns;
};

type DemandCategoryModalProps = {
    open: boolean;
    demand: Partial<Demand> | null;
    demands: Demand[] | null;
    mode: ModalMode;
    onClose: () => void;
    onSave: () => void;
    onRemove?: (deletedUuid: string) => void;
};

const isValidDemand = (demand: Partial<Demand>) =>
    demand?.day && demand?.demandLocationBpns && demand?.quantity && demand.demandCategoryCode && demand?.measurementUnit && demand?.partnerBpnl;

export const DemandCategoryModal = ({ open, mode, onClose, onSave, onRemove, demand, demands }: DemandCategoryModalProps) => {
    const [temporaryDemand, setTemporaryDemand] = useState<Partial<Demand>>(demand ?? {});
    const { partners } = usePartners('material', temporaryDemand?.ownMaterialNumber ?? null);
    const { sites } = useSites();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);
    const dailyDemands = useMemo(
        () =>
            demands?.filter(
                (d) => d.day && new Date(d.day).toLocaleDateString() === new Date(demand?.day ?? Date.now()).toLocaleDateString()
            ),
        [demands, demand?.day]
    );

    const canDelete = Boolean(sites?.find(site => demands?.some(d => d.demandLocationBpns === site.bpns)));

    const handleSaveClick = (demand: Partial<Demand>) => {
        if (!isValidDemand(demand)) {
            setFormError(true);
            return;
        }
        setFormError(false);
        postDemand(demand)
            .then(() => {
                onSave();
                notify({
                    title: 'Demand Created',
                    description: 'The Demand has been saved successfully',
                    severity: 'success',
                });
            })
            .catch((error) => {
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                    description: error.status === 409 ? 'Date conflicting with another Demand' : error.error,
                    severity: 'error',
                });
            })
            .finally(onClose);
    };

    const handleClose = () => {
        setTemporaryDemand({});
        setFormError(false);
        onClose();
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

    useEffect(() => {
        if (demand) {
            setTemporaryDemand(demand);
        }
    }, [demand]);
    return (
        <>
            <Dialog open={open && demand !== null} onClose={handleClose} data-testid="demand-modal">
                <DialogTitle variant="h3" textAlign="center">
                    Demand Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    {mode === 'create' ? (
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
                                    value={sites?.find((s) => s.bpns === temporaryDemand.demandLocationBpns) ?? null}
                                    label="Demand Site*"
                                    placeholder="Select a Site"
                                    data-testid="demand-location-field"
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
                                        value={temporaryDemand?.day}
                                        onChangeItem={(value) => setTemporaryDemand((curr) => ({ ...curr, day: value ?? undefined }))}
                                    />
                                </div>
                            </Grid>
                            <Grid item xs={6}>
                                <LabelledAutoComplete
                                    id="category"
                                    options={DEMAND_CATEGORY}
                                    getOptionLabel={(option) => option?.value ?? ''}
                                    onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, demandCategoryCode: value?.key }))}
                                    isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                    value={
                                        {
                                            key: temporaryDemand.demandCategoryCode,
                                            value: DEMAND_CATEGORY.find((c) => c.key === temporaryDemand.demandCategoryCode)?.value,
                                        }
                                    }
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
                                    id="uom"
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
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <LabelledAutoComplete
                                    id="partner"
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
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <LabelledAutoComplete
                                    id="supplierLocationBpns"
                                    options={partners?.find((s) => s.bpnl === temporaryDemand.partnerBpnl)?.sites ?? []}
                                    getOptionLabel={(option) => option.name ?? ''}
                                    disabled={!temporaryDemand?.partnerBpnl}
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
                    ) : (
                        <Table
                            title={`Material Demand ${
                                temporaryDemand?.day
                                    ? ' on ' +
                                      new Date(temporaryDemand?.day).toLocaleDateString('en-GB', {
                                          weekday: 'long',
                                          day: '2-digit',
                                          month: '2-digit',
                                          year: 'numeric',
                                      })
                                    : ''
                            }`}
                            density="standard"
                            getRowId={(row) => row.uuid}
                            columns={createDemandColumns(canDelete ? handleDelete : undefined)}
                            rows={dailyDemands ?? []}
                            hideFooter
                        />
                    )}
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {mode === 'create' && (
                            <Button
                                variant="contained"
                                color="primary"
                                sx={{ display: 'flex', gap: '.25rem' }}
                                onClick={() => handleSaveClick(temporaryDemand)}
                                data-testid="save-demand-button"
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
