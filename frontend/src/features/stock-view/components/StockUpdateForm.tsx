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
import { Autocomplete, Checkbox, capitalize } from '@mui/material';
import { Input, LoadingButton } from '@catena-x/portal-shared-components';

import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { Stock, StockType } from '@models/types/data/stock';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { getUnitOfMeasurement } from '@util/helpers';

import { useSites } from '../hooks/useSites';
import { usePartners } from '../hooks/usePartners';
import { validateStock } from '@util/stock-helpers';

type StockUpdateFormProps<T extends StockType> = {
    items: MaterialDescriptor[] | null;
    type: T;
    selectedItem: Stock | null;
    isSaving: boolean;
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

export const StockUpdateForm = <T extends StockType>({ items, type, selectedItem, isSaving, onSubmit }: StockUpdateFormProps<T>) => {
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
        <form className="p-5">
            <div className="flex gap-5 justify-center">
                <div className="w-[32rem]">
                    <Autocomplete
                        id="material"
                        value={
                            newStock?.material
                                ? {
                                      ownMaterialNumber:
                                          type === 'material'
                                              ? newStock?.material?.materialNumberCustomer
                                              : newStock?.material?.materialNumberSupplier,
                                      description: selectedItem?.material?.name,
                                  }
                                : null
                        }
                        options={items ?? []}
                        getOptionLabel={(option) => option.ownMaterialNumber ?? ''}
                        renderInput={(params) => (
                            <Input
                                {...params}
                                label={`${capitalize(type)}*`}
                                placeholder={`Select a ${type}`}
                                error={formError && !newStock?.material}
                            />
                        )}
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
                    <Autocomplete
                        id="partner"
                        value={newStock?.material ? newStock?.partner : null}
                        options={partners ?? []}
                        getOptionLabel={(option) => option?.name ?? ''}
                        renderInput={(params) => (
                            <Input
                                {...params}
                                label="Partner*"
                                placeholder="Select a Partner"
                                error={formError && !newStock?.partner}
                            />
                        )}
                        onChange={(_, value) => dispatch({ type: 'partner', payload: value ?? null })}
                        isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                    />
                    <div className="grid grid-cols-3 gap-2">
                        <div className="col-span-2">
                            <Input
                                id="quantity"
                                label="Quantity*"
                                type="number"
                                placeholder="Enter quantity"
                                value={newStock?.quantity ?? ''}
                                error={formError && !newStock?.quantity}
                                onChange={(event) => dispatch({ type: 'quantity', payload: event.target.value })}
                            />
                        </div>
                        <Autocomplete
                            id="uom"
                            value={
                                newStock && newStock.measurementUnit
                                    ? { key: newStock.measurementUnit, value: getUnitOfMeasurement(newStock.measurementUnit) }
                                    : null
                            }
                            options={UNITS_OF_MEASUREMENT}
                            getOptionLabel={(option) => option?.value ?? ''}
                            renderInput={(params) => (
                                <Input
                                    {...params}
                                    label="UOM*"
                                    placeholder="Select unit"
                                    error={formError && !newStock?.measurementUnit}
                                />
                            )}
                            onChange={(_, value) => dispatch({ type: 'measurementUnit', payload: value?.key ?? null })}
                            isOptionEqualToValue={(option, value) => option?.key === value?.key}
                        />
                    </div>
                    <div className="flex items-center justify-end pt-7">
                        <Checkbox
                            id="isBlocked"
                            checked={newStock?.isBlocked ?? false}
                            onChange={(event) => dispatch({ type: 'isBlocked', payload: event.target.checked })}
                        />
                        <label htmlFor="isBlocked"> is Blocked </label>
                    </div>
                </div>
                <div className="w-[32rem]">
                    <Autocomplete
                        id="site"
                        value={newStock?.stockLocationBpns ?? null}
                        options={sites?.map((site) => site.bpns) ?? []}
                        renderInput={(params) => (
                            <Input
                                {...params}
                                label="Stock Location BPNS*"
                                placeholder="Select a Site"
                                error={formError && !newStock?.stockLocationBpns}
                            />
                        )}
                        onChange={(_, value) => dispatch({ type: 'stockLocationBpns', payload: value ?? null })}
                    />
                    <Autocomplete
                        id="site"
                        value={newStock?.stockLocationBpna ?? null}
                        options={sites?.find((site) => site.bpns === newStock?.stockLocationBpns)?.addresses.map((addr) => addr.bpna) ?? []}
                        renderInput={(params) => (
                            <Input
                                {...params}
                                label="Stock Location BPNA*"
                                placeholder="Select an Address"
                                error={formError && !newStock?.stockLocationBpna}
                            />
                        )}
                        onChange={(_, value) => dispatch({ type: 'stockLocationBpna', payload: value ?? null })}
                    />
                    <div className="grid grid-cols-2 gap-2">
                        <Input
                            id="customer-order-number"
                            label="Customer Order Number"
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
                        <Input
                            id="customer-order-position"
                            label="Customer Order Position Number"
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
                    </div>
                    <Input
                        id="supplier-order-number"
                        label="Supplier Order Number"
                        type="text"
                        value={newStock?.supplierOrderNumber ?? ''}
                        disabled={!newStock?.customerOrderNumber}
                        error={formError && !!newStock?.customerOrderNumber && !newStock?.supplierOrderNumber}
                        onChange={(event) => dispatch({ type: 'supplierOrderNumber', payload: event.target.value })}
                    />
                </div>
            </div>
            <div className="mt-7 mx-auto w-48">
                <LoadingButton
                    className="w-full"
                    variant="contained"
                    color="primary"
                    loading={isSaving}
                    loadIndicator="Saving..."
                    onButtonClick={handleSubmit}
                    label="Add or Update"
                    fullWidth={true}
                ></LoadingButton>
            </div>
        </form>
    );
};
