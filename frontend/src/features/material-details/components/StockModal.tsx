/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation
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

import { useEffect, useState } from 'react';
import { Input, Table } from '@catena-x/portal-shared-components';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Stock, StockType } from '@models/types/data/stock';
import { Box, Button, Checkbox, Dialog, DialogTitle, FormLabel, Grid, InputLabel, Stack, capitalize } from '@mui/material';
import { getUnitOfMeasurement, isValidOrderReference } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { Close, Delete, Save } from '@mui/icons-material';
import { ModalMode } from '@models/types/data/modal-mode';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useNotifications } from '@contexts/notificationContext';
import { deleteStocks, postStocks } from '@services/stocks-service';
import { TextToClipboard } from '@components/ui/TextToClipboard';

const createStockColumns = (handleDelete?: (row: Stock) => void) => {
    const columns = [
        {
            field: 'quantity',
            headerName: 'Quantity',
            headerAlign: 'center',
            flex: 1.2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {`${data.row.quantity} ${getUnitOfMeasurement(data.row.measurementUnit)}`}
                </Box>
            ),
        },
        {
            field: 'stockLocationBpns',
            headerName: 'Site',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    <TextToClipboard text={data.row.stockLocationBpns} />
                </Box>
            ),
        },
        {
            field: 'partner',
            headerName: 'Partner',
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.partner?.name}
                </Box>
            ),
        },
        {
            field: 'lastUpdatedOn',
            headerName: 'Updated',
            headerAlign: 'center',
            flex: 1.5,
            renderCell: (data: { row: Stock }) => (
                <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    <Box>{new Date(data.row.lastUpdatedOn).toLocaleDateString('en-GB')}</Box>
                    <Box>{new Date(data.row.lastUpdatedOn).toLocaleTimeString('en-GB')}</Box>
                </Stack>
            ),
        },
        {
            field: 'customerOrderNumber',
            headerName: 'Customer Order Number',
            sortable: false,
            headerAlign: 'center',
            flex: 2,
            renderCell: (data: { row: Stock }) => {
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
            renderCell: (data: { row: Stock }) => {
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
            renderCell: (data: { row: Stock }) => {
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
            field: 'isBlocked',
            headerName: 'Blocked',
            headerAlign: 'center',
            flex: 1,
            renderCell: (data: { row: Stock }) => (
                <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                    {data.row.isBlocked ? 'Yes' : 'No'}
                </Box>
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
                renderCell: (data: { row: Stock }) => {
                    return (
                        <Box display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Button variant="text" color="error" onClick={() => handleDelete(data.row)} data-testid="delete-stock">
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

type StockModalProps = {
    open: boolean;
    mode: ModalMode;
    onClose: () => void;
    onSave: () => void;
    onRemove?: (deletedUuid: string) => void;
    stock: Partial<Stock> | null;
    stocks: Stock[];
    stockType: StockType;
};
const isValidStock = (stock: Partial<Stock>) =>
    stock &&
    typeof stock.quantity === 'number' && stock.quantity >= 0 &&
    stock.measurementUnit &&
    stock.partner &&
    stock.stockLocationBpns &&
    stock.stockLocationBpna &&
    isValidOrderReference(stock);

export const StockModal = ({ open, mode, onClose, onSave, onRemove, stock, stocks, stockType }: StockModalProps) => {
    const [temporaryStock, setTemporaryStock] = useState<Partial<Stock>>(stock ?? {});
    const { partners } = usePartners(stockType, temporaryStock?.material?.ownMaterialNumber ?? null);
    const { sites } = useSites();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);

    const handleSaveClick = () => {
        temporaryStock.customerOrderNumber ||= undefined;
        temporaryStock.customerOrderPositionNumber ||= undefined;
        temporaryStock.supplierOrderNumber ||= undefined;
        if (!isValidStock(temporaryStock)) {
            setFormError(true);
            return;
        }
        setFormError(false);
        postStocks(stockType, temporaryStock)
            .then(() => {
                onSave();
                notify({
                    title: 'Stock Created',
                    description: 'The Stock has been saved successfully',
                    severity: 'success',
                });
            })
            .catch((error) => {
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                    description: error.status === 409 ? 'Date conflicting with another Stock' : error.error,
                    severity: 'error',
                });
            })
            .finally(onClose);
    };
    const handleDelete = async (row: Stock) => {
        if (row.uuid) {
            try {
                await deleteStocks(stockType, row.uuid);
                onRemove?.(row.uuid);
            } catch (error) {
                notify({
                    title: 'Error deleting stock',
                    description: 'Failed to delete the stock',
                    severity: 'error',
                });
            }
        }
    };
    useEffect(() => {
        if (stock) setTemporaryStock(stock);
    }, [stock]);
    return (
        <>
            <Dialog open={open && stock !== null} onClose={onClose} data-testid="stock-modal">
                <DialogTitle variant="h3" textAlign="center">
                    {capitalize(mode)} {capitalize(stockType)} Stock
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '95vw' }}>
                    <Grid container spacing={2} padding=".25rem">
                        {mode === 'create' ? (
                            <>
                                <GridItem label="Material Number" value={temporaryStock.material?.ownMaterialNumber ?? ''} />
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        sx={{ margin: '0' }}
                                        id="partner"
                                        options={partners ?? []}
                                        getOptionLabel={(option) => option?.name ?? ''}
                                        label="Partner*"
                                        placeholder="Select a Partner"
                                        error={formError && !temporaryStock?.partner}
                                        onChange={(_, value) => setTemporaryStock({ ...temporaryStock, partner: value ?? undefined })}
                                        value={temporaryStock.partner ?? null}
                                        isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                                        data-testid="stock-partner-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="stockLocationBpns"
                                        options={sites ?? []}
                                        getOptionLabel={(option) => option.name ?? ''}
                                        error={formError}
                                        isOptionEqualToValue={(option, value) => option?.bpns === value.bpns}
                                        onChange={(_, value) =>
                                            setTemporaryStock({ 
                                                ...temporaryStock,
                                                stockLocationBpns: value?.bpns ?? undefined,
                                                stockLocationBpna: value ? temporaryStock.stockLocationBpna : undefined 
                                            })
                                        }
                                        value={sites?.find((s) => s.bpns === temporaryStock.stockLocationBpns) ?? null}
                                        label="Stock Site*"
                                        placeholder="Select a Site"
                                        data-testid="stock-site-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="stockLocationBpna"
                                        options={sites?.find(site => site.bpns === temporaryStock.stockLocationBpns)?.addresses ?? []}
                                        getOptionLabel={(option) => option.streetAndNumber ?? ''}
                                        error={formError}
                                        isOptionEqualToValue={(option, value) => option?.bpna === value.bpna}
                                        onChange={(_, value) =>
                                            setTemporaryStock({ ...temporaryStock, stockLocationBpna: value?.bpna ?? undefined })
                                        }
                                        value={sites?.find(site => 
                                            site.bpns === temporaryStock.stockLocationBpns)?.addresses?.find((a) => 
                                                a.bpna === temporaryStock.stockLocationBpna) ?? null
                                        }
                                        label="Stock Address*"
                                        placeholder="Select an address"
                                        disabled={!temporaryStock.stockLocationBpns}
                                        data-testid="stock-address-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Quantity*</FormLabel>
                                    <Input
                                        id="quantity"
                                        type="number"
                                        placeholder="Enter quantity"
                                        value={temporaryStock.quantity ?? ''}
                                        error={formError && (temporaryStock?.quantity == null || temporaryStock.quantity < 0)}
                                        onChange={(e) =>
                                            setTemporaryStock((curr) => ({
                                                ...curr,
                                                quantity: e.target.value ? parseFloat(e.target.value) : undefined,
                                            }))
                                        }
                                        sx={{ marginTop: '.5rem' }}
                                        data-testid="stock-quantity-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="uom"
                                        value={
                                            temporaryStock.measurementUnit
                                                ? {
                                                      key: temporaryStock.measurementUnit,
                                                      value: getUnitOfMeasurement(temporaryStock.measurementUnit),
                                                  }
                                                : null
                                        }
                                        options={UNITS_OF_MEASUREMENT}
                                        getOptionLabel={(option) => option?.value ?? ''}
                                        label="UOM*"
                                        placeholder="Select unit"
                                        error={formError && !temporaryStock?.measurementUnit}
                                        onChange={(_, value) => setTemporaryStock((curr) => ({ ...curr, measurementUnit: value?.key }))}
                                        isOptionEqualToValue={(option, value) => option?.key === value?.key}
                                        data-testid="stock-uom-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Customer Order Number</FormLabel>
                                    <Input
                                        id="customer-order-number"
                                        type="text"
                                        error={formError && !isValidOrderReference(temporaryStock)}
                                        value={temporaryStock?.customerOrderNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryStock({ ...temporaryStock, customerOrderNumber: event.target.value })
                                        }
                                        data-testid="stock-customer-order-number-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Customer Order Position</FormLabel>
                                    <Input
                                        id="customer-order-position-number"
                                        type="text"
                                        error={formError && !isValidOrderReference(temporaryStock)}
                                        value={temporaryStock?.customerOrderPositionNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryStock({
                                                ...temporaryStock,
                                                customerOrderPositionNumber: event.target.value,
                                            })
                                        }
                                        data-testid="stock-customer-order-position-field"
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <FormLabel>Supplier Order Number</FormLabel>
                                    <Input
                                        id="supplier-order-number"
                                        type="text"
                                        value={temporaryStock?.supplierOrderNumber ?? ''}
                                        onChange={(event) =>
                                            setTemporaryStock({
                                                ...temporaryStock,
                                                supplierOrderNumber: event.target.value ?? '',
                                            })
                                        }
                                        data-testid="stock-supplier-order-number-field"
                                    />
                                </Grid>
                                <Grid item xs={6} alignContent="end">
                                    <Stack direction="row" alignItems="center">
                                        <Checkbox
                                            id="isBlocked"
                                            checked={temporaryStock?.isBlocked ?? false}
                                            onChange={(event) => setTemporaryStock({ ...temporaryStock, isBlocked: event.target.checked })}
                                            data-testid="stock-is-blocked-field"
                                        />
                                        <InputLabel htmlFor="isBlocked"> is Blocked </InputLabel>
                                    </Stack>
                                </Grid>
                            </>
                        ) : (
                            <Grid item xs={12}>
                                <Table
                                    title={`Current ${stockType} stock`}
                                    getRowId={(row) => row.uuid}
                                    columns={createStockColumns(sites?.find(s => stocks.some(stock => stock.stockLocationBpns === s.bpns)) ? handleDelete : undefined)}
                                    rows={stocks}
                                    hideFooter
                                    density="standard"
                                />
                            </Grid>
                        )}
                    </Grid>
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={onClose}>
                            <Close></Close> Close
                        </Button>
                        {mode === 'create' && (
                            <Button variant="contained" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleSaveClick} data-testid="save-stock-button">
                                <Save></Save> Save
                            </Button>
                        )}
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};