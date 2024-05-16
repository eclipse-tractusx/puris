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
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Datepicker, Input, PageSnackbar, PageSnackbarStack, Table } from '@catena-x/portal-shared-components';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Demand } from '@models/types/data/demand';
import { Autocomplete, Box, Button, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { Notification } from '@models/types/data/notification';
import { deleteDemand, postDemand } from '@services/demands-service';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { Close, Delete, Save } from '@mui/icons-material';
import { ModalMode } from '@models/types/data/modal-mode';

const GridItem = ({ label, value }: { label: string; value: string }) => (
    <Grid item xs={6}>
        <Stack>
            <Typography variant="caption1" fontWeight={500}>
                {label}:
            </Typography>
            <Typography variant="body3" paddingLeft=".5rem">
                {value}
            </Typography>
        </Stack>
    </Grid>
);

const createDemandColumns = (handleDelete?: (row: Demand) => void) => {
    const columns = [
        {
            field: 'quantity',
            headerName: 'Quantity',
            sortable: false,
            disableColumnMenu: true,
            headerAlign: 'center',
            type: 'string',
            width: 120,
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
            width: 200,
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
            width: 200,
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
            width: 200,
            renderCell: (data: { row: Demand }) => {
                return (
                    <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                        {DEMAND_CATEGORY.find((cat) => cat.key === data.row.demandCategoryCode)?.value ?? DEMAND_CATEGORY[0].value}
                    </Box>
                );
            },
        },
    ] as const;
    if (handleDelete) {
        return [...columns, {
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
                        <Button variant="text" color="error" onClick={() => handleDelete(data.row)}>
                            <Delete></Delete>
                        </Button>
                    </Box>
                );
            },
        }] as const;
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
};

const isValidDemand = (demand: Partial<Demand>) =>
    demand?.day &&
    demand?.quantity &&
    demand.demandCategoryCode &&
    demand?.measurementUnit &&
    demand?.partnerBpnl;

export const DemandCategoryModal = ({ open, mode, onClose, onSave, demand, demands }: DemandCategoryModalProps) => {
    const [temporaryDemand, setTemporaryDemand] = useState<Partial<Demand>>(demand ?? {});
    const { partners } = usePartners('material', temporaryDemand?.ownMaterialNumber ?? null);
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [formError, setFormError] = useState(false);
    const dailyDemands = useMemo(
        () =>
            demands?.filter(
                (d) => d.day && new Date(d.day).toLocaleDateString() === new Date(demand?.day ?? Date.now()).toLocaleDateString()
            ),
        [demands, demand?.day]
    );

    const handleSaveClick = useCallback(
        (demand: Partial<Demand>) => {
            if (!isValidDemand(demand)) {
                setFormError(true);
                return;
            }
            setFormError(false);
            postDemand(demand)
                .then(() => {
                    onSave();
                    setNotifications((ns) => [
                        ...ns,
                        {
                            title: 'Demand Created',
                            description: 'The Demand has been saved successfully',
                            severity: 'success',
                        },
                    ]);
                })
                .catch((error) => {
                    setNotifications((ns) => [
                        ...ns,
                        {
                            title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                            description: error.status === 409 ? 'Date conflicting with another Demand' : error.error,
                            severity: 'error',
                        },
                    ]);
                })
                .finally(onClose);
        },
        [onClose, onSave]
    );
    const handleDelete = (row: Demand) => {
        if (row.uuid) deleteDemand(row.uuid).then(onSave);
    };
    useEffect(() => {
        if (demand) {
            setTemporaryDemand(demand);
        }
    }, [demand]);
    return (
        <>
            <Dialog open={open && demand !== null} onClose={onClose}>
                <DialogTitle fontWeight={600} textAlign="center">
                    Demand Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem">
                    {mode === 'create' ? (
                        <Grid container spacing={2} width="32rem" padding=".25rem">
                            <GridItem label="Material Number" value={temporaryDemand.ownMaterialNumber ?? ''} />
                            <GridItem label="Site" value={temporaryDemand.demandLocationBpns ?? ''} />
                            <Grid item marginTop="1.5rem" xs={6}>
                                <Datepicker
                                    label="Day"
                                    placeholder="Pick a Day"
                                    locale="de"
                                    error={formError && !temporaryDemand?.day}
                                    readOnly={false}
                                    value={temporaryDemand?.day}
                                    onChangeItem={(value) => setTemporaryDemand((curr) => ({ ...curr, day: value ?? undefined }))}
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <Autocomplete
                                    id="category"
                                    clearIcon={false}
                                    options={DEMAND_CATEGORY}
                                    getOptionLabel={(option) => option?.value ?? ''}
                                    onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, demandCategoryCode: value?.key }))}
                                    isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                    value={
                                        temporaryDemand.demandCategoryCode
                                            ? {
                                                  key: temporaryDemand.demandCategoryCode,
                                                  value: DEMAND_CATEGORY.find((c) => c.key === temporaryDemand.demandCategoryCode)?.value,
                                              }
                                            : DEMAND_CATEGORY[0]
                                    }
                                    renderInput={(params) => (
                                        <Input
                                            {...params}
                                            label="Category*"
                                            placeholder="Select category"
                                            error={formError && !temporaryDemand?.demandCategoryCode}
                                        />
                                    )}
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <Input
                                    label="Quantity*"
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
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <Autocomplete
                                    id="uom"
                                    clearIcon={false}
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
                                    renderInput={(params) => (
                                        <Input
                                            {...params}
                                            label="UOM*"
                                            placeholder="Select unit"
                                            error={formError && !temporaryDemand?.measurementUnit}
                                        />
                                    )}
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <Autocomplete
                                    id="partner"
                                    options={partners ?? []}
                                    getOptionLabel={(option) => option?.name ?? ''}
                                    isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                                    value={partners?.find((s) => s.bpnl === temporaryDemand.partnerBpnl) ?? null}
                                    onChange={(_, value) =>
                                        setTemporaryDemand({ ...temporaryDemand, partnerBpnl: value?.bpnl ?? undefined })
                                    }
                                    renderInput={(params) => (
                                        <Input
                                            {...params}
                                            label="Partner*"
                                            placeholder="Select a Partner"
                                            error={formError && !temporaryDemand?.partnerBpnl}
                                        />
                                    )}
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <Autocomplete
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
                                    renderInput={(params) => (
                                        <Input
                                            {...params}
                                            label="Supplier Site"
                                            placeholder="Select a Site"
                                        />
                                    )}
                                />
                            </Grid>
                        </Grid>
                    ) : (
                        <Table
                            title={`Material Demand ${
                                temporaryDemand?.day
                                    ? ' on ' +
                                      new Date(temporaryDemand?.day).toLocaleDateString(undefined, {
                                          weekday: 'long',
                                          day: '2-digit',
                                          month: '2-digit',
                                          year: 'numeric',
                                      })
                                    : ''
                            }`}
                            density="standard"
                            getRowId={(row) => row.uuid}
                            columns={createDemandColumns(mode === 'view' ? undefined : handleDelete)}
                            rows={dailyDemands ?? []}
                            hideFooter
                        />
                    )}
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={onClose}>
                            <Close></Close> Close
                        </Button>
                        {mode === 'create' && (
                            <Button
                                variant="contained"
                                color="primary"
                                sx={{ display: 'flex', gap: '.25rem' }}
                                onClick={() => handleSaveClick(temporaryDemand)}
                            >
                                <Save></Save> Save
                            </Button>
                        )}
                    </Box>
                </Stack>
            </Dialog>
            <PageSnackbarStack>
                {notifications.map((notification, index) => (
                    <PageSnackbar
                        key={index}
                        open={!!notification}
                        severity={notification?.severity}
                        title={notification?.title}
                        description={notification?.description}
                        autoClose={true}
                        onCloseNotification={() => setNotifications((ns) => ns.filter((_, i) => i !== index) ?? [])}
                    />
                ))}
            </PageSnackbarStack>
        </>
    );
};
