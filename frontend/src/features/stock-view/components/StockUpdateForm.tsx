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

import { useEffect, useReducer, useState } from 'react';
import { Box, Button, Checkbox, Grid, InputLabel, Stack, capitalize } from '@mui/material';
import { Input } from '@catena-x/portal-shared-components';

import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { Stock, StockType } from '@models/types/data/stock';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { getUnitOfMeasurement } from '@util/helpers';

import { useSites } from '../hooks/useSites';
import { usePartners } from '../hooks/usePartners';
import { validateStock } from '@util/stock-helpers';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';

type StockUpdateFormProps<T extends StockType> = {
    items: MaterialDescriptor[] | null;
    type: T;
    selectedItem: Stock | null;
    onSubmit: (stock: Stock) => void;
};

const stockReducer = (
    state: Partial<Stock>,
    action: {
        type: keyof Stock | 'replace';
        payload: Stock[keyof Stock] | Stock;
    }
) => {
    if (action.type === 'replace') {
        return action.payload as Stock;
    }
    return {
        ...state,
        [action.type]: action.payload as Stock[typeof action.type],
    };
};

export const StockUpdateForm = <T extends StockType>({ items, type, selectedItem, onSubmit }: StockUpdateFormProps<T>) => {
    const [newStock, dispatch] = useReducer(stockReducer, selectedItem ?? {});
    const [formError, setFormError] = useState(false);
    const { sites } = useSites();
    const { partners } = usePartners(
        type,
        (type === 'material' ? newStock?.material?.materialNumberCustomer : newStock?.material?.materialNumberSupplier) ?? null
    );

    useEffect(() => {
        setFormError(false);
        dispatch({ type: 'replace', payload: selectedItem });
    }, [selectedItem]);

    const handleSubmit = () => {
        if (validateStock(newStock)) {
            setFormError(false);
            onSubmit(newStock as Stock);
        } else {
            setFormError(true);
        }
    };

    return (
        <Box sx={{ px: 2, width: '100%' }}>
            <form>
                <Grid container marginInline="auto" spacing={1}>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="material"
                            value={
                                newStock?.material
                                    ? {
                                          ownMaterialNumber:
                                              type === 'material'
                                                  ? newStock?.material?.materialNumberCustomer
                                                  : newStock?.material?.materialNumberSupplier,
                                          description: selectedItem?.material?.name,
                                          name: selectedItem?.material?.name,
                                      }
                                    : null
                            }
                            sx={{ width: '100%' }}
                            options={items ?? []}
                            getOptionLabel={(option) => option.ownMaterialNumber ?? ''}
                            label={`${capitalize(type)}*`}
                            placeholder={`Select a ${type}`}
                            error={formError && !newStock?.material}
                            isOptionEqualToValue={(option, value) => option?.ownMaterialNumber === value?.ownMaterialNumber}
                            onChange={(_, newValue) =>
                                dispatch({
                                    type: 'material',
                                    payload: newValue
                                        ? {
                                              ...(type === 'material'
                                                  ? { materialFlag: true, productFlag: false }
                                                  : { materialFlag: false, productFlag: true }),
                                              uuid: null,
                                              materialNumberCustomer: type === 'material' ? newValue?.ownMaterialNumber ?? null : null,
                                              materialNumberSupplier: type === 'product' ? newValue?.ownMaterialNumber ?? null : null,
                                              materialNumberCx: null,
                                              name: newValue?.description ?? '',
                                          }
                                        : undefined,
                                })
                            }
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="partner"
                            value={newStock?.material ? newStock?.partner : null}
                            options={partners ?? []}
                            getOptionLabel={(option) => option?.name ?? ''}
                            label="Partner*"
                            placeholder="Select a Partner"
                            error={formError && !newStock?.partner}
                            onChange={(_, value) => dispatch({ type: 'partner', payload: value ?? null })}
                            isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                        />
                    </Grid>
                    <Grid item xs={6} container spacing={1}>
                        <Grid item xs={6}>
                            <InputLabel>Quantity*</InputLabel>
                            <Input
                                id="quantity"
                                type="number"
                                placeholder="Enter quantity"
                                sx={{ marginTop: '.5rem' }}
                                value={newStock?.quantity ?? ''}
                                error={formError && !newStock?.quantity}
                                onChange={(event) => dispatch({ type: 'quantity', payload: event.target.value })}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <LabelledAutoComplete
                                id="uom"
                                value={
                                    newStock && newStock.measurementUnit
                                        ? { key: newStock.measurementUnit, value: getUnitOfMeasurement(newStock.measurementUnit) }
                                        : null
                                }
                                options={UNITS_OF_MEASUREMENT}
                                getOptionLabel={(option) => option?.value ?? ''}
                                label="UOM*"
                                placeholder="Select unit"
                                error={formError && !newStock?.measurementUnit}
                                onChange={(_, value) => dispatch({ type: 'measurementUnit', payload: value?.key ?? null })}
                                isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            />
                        </Grid>
                    </Grid>
                    <Grid item xs={6} alignContent="end">
                        <Stack direction="row" alignItems="center">
                            <Checkbox
                                id="isBlocked"
                                checked={newStock?.isBlocked ?? false}
                                onChange={(event) => dispatch({ type: 'isBlocked', payload: event.target.checked })}
                            />
                            <InputLabel htmlFor="isBlocked"> is Blocked </InputLabel>
                        </Stack>
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="site"
                            value={newStock?.stockLocationBpns ?? null}
                            options={sites?.map((site) => site.bpns) ?? []}
                            label="Stock Location BPNS*"
                            placeholder="Select a Site"
                            error={formError && !newStock?.stockLocationBpns}
                            onChange={(_, value) => dispatch({ type: 'stockLocationBpns', payload: value ?? null })}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="site"
                            value={newStock?.stockLocationBpna ?? null}
                            options={
                                sites?.find((site) => site.bpns === newStock?.stockLocationBpns)?.addresses.map((addr) => addr.bpna) ?? []
                            }
                            label="Stock Location BPNA*"
                            placeholder="Select an Address"
                            error={formError && !newStock?.stockLocationBpna}
                            onChange={(_, value) => dispatch({ type: 'stockLocationBpna', payload: value ?? null })}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <InputLabel>Customer Order Number*</InputLabel>
                        <Input
                            hiddenLabel
                            id="customer-order-number"
                            type="text"
                            value={newStock?.customerOrderNumber ?? ''}
                            onChange={(event) => {
                                dispatch({ type: 'customerOrderNumber', payload: event.target.value });
                                if (event.target.value === '' || event.target.value === null) {
                                    dispatch({ type: 'customerOrderPositionNumber', payload: '' });
                                    dispatch({ type: 'supplierOrderNumber', payload: '' });
                                }
                            }}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <InputLabel>Customer Order Position Number</InputLabel>
                        <Input
                            hiddenLabel
                            id="customer-order-position"
                            type="text"
                            value={newStock?.customerOrderPositionNumber ?? ''}
                            disabled={!newStock?.customerOrderNumber}
                            error={formError && !!newStock?.customerOrderNumber && !newStock?.customerOrderPositionNumber}
                            onChange={(event) =>
                                dispatch({
                                    type: 'customerOrderPositionNumber',
                                    payload: event.target.value,
                                })
                            }
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <InputLabel>Supplier Order Number</InputLabel>
                        <Input
                            hiddenLabel
                            id="supplier-order-number"
                            type="text"
                            value={newStock?.supplierOrderNumber ?? ''}
                            disabled={!newStock?.customerOrderNumber}
                            error={formError && !!newStock?.customerOrderNumber && !newStock?.supplierOrderNumber}
                            onChange={(event) => dispatch({ type: 'supplierOrderNumber', payload: event.target.value })}
                        />
                    </Grid>
                </Grid>
                <Button onClick={handleSubmit} sx={{ display: 'block', marginLeft: 'auto', marginTop: '2rem' }}>
                    Add or Update
                </Button>
            </form>
        </Box>
    );
};
