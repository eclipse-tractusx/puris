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

import { createContext, ReactNode, useCallback, useContext, useMemo, useReducer, useState } from 'react';
import { Site } from '@models/types/edc/site';
import { ModalMode } from '@models/types/data/modal-mode';
import { Delivery } from '@models/types/data/delivery';
import { DemandCategoryModal } from '@features/dashboard/components/DemandCategoryModal';
import { PlannedProductionModal } from '@features/dashboard/components/PlannedProductionModal';
import { DeliveryInformationModal } from '@features/dashboard/components/DeliveryInformationModal';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { Demand } from '@models/types/data/demand';
import { Production } from '@models/types/data/production';
import { Material, Stock } from '@models/types/data/stock';
import { DirectionType } from '@models/types/erp/directionType';

export type DataCategory = 'production' | 'demand' | 'stock' | 'delivery';

export type DataCategoryTypeMap = {
    'production': Production;
    'demand': Demand;
    'delivery': Delivery;
    'stock': Stock;
}

type DataModalContext = {
    openDialog: <TCategory extends DataCategory>(
        category: TCategory,
        data: Partial<DataCategoryTypeMap[TCategory]>,
        list: DataCategoryTypeMap[TCategory][],
        mode: ModalMode,
        direction?: DirectionType,
        site?: Site | null
    ) => void;
    addOnSaveListener: (callback: (category: DataCategory) => void) => void;
    removeOnSaveListener: (callback: (category: DataCategory) => void) => void; 
};

const dataModalContext = createContext<DataModalContext | null>(null);

type DataModalProviderProps = {
    children: ReactNode;
    material?: Material;
};

export const DataModalProvider = ({ children, material }: DataModalProviderProps) => {
    const [state, dispatch] = useReducer(reducer, initialState);
    const [onSaveListeners, setOnSaveListeners] = useState<((category: DataCategory) => void)[]>([]);
    const materialNumber = useMemo(() => material?.ownMaterialNumber ?? '', [material]);

    const addOnSaveListener = useCallback((callback: (category: DataCategory) => void) => {
        setOnSaveListeners((prev) => [...prev, callback]);
    }, []);

    const removeOnSaveListener = useCallback((callback: (category: DataCategory) => void) => {
        setOnSaveListeners((prev) => prev.filter(cb => cb === callback));
    }, []);

    const onSave = useCallback((category: DataCategory) => {
        onSaveListeners.forEach((callback) => callback(category));
    }, [onSaveListeners]);

    const openDemandDialog = useCallback(
        (d: Partial<Demand>, mode: ModalMode, list: Demand[]) => {
            d.measurementUnit ??= 'unit:piece';
            d.demandCategoryCode ??= DEMAND_CATEGORY[0]?.key;
            d.ownMaterialNumber = materialNumber;
            dispatch({ type: 'demand', payload: d });
            dispatch({ type: 'demands', payload: list });
            dispatch({ type: 'demandDialogOptions', payload: { open: true, mode } });
        },
        [materialNumber]
    );

    const openProductionDialog = useCallback(
        (p: Partial<Production>, mode: ModalMode, list: Production[]) => {
            p.material ??= {
                materialFlag: true,
                productFlag: false,
                ownMaterialNumber: materialNumber,
                materialNumberSupplier: materialNumber,
                materialNumberCustomer: null,
                materialNumberCx: null,
                name: material?.name ?? '',
            };
            p.measurementUnit ??= 'unit:piece';
            dispatch({ type: 'production', payload: p });
            dispatch({ type: 'productions', payload: list });
            dispatch({ type: 'productionDialogOptions', payload: { open: true, mode } });
        },
        [material?.name, materialNumber]
    );

    const openDeliveryDialog = useCallback(
        (d: Partial<Delivery>, mode: ModalMode, direction: DirectionType, site: Site | null, list: Delivery[]) => {
            d.ownMaterialNumber = materialNumber;
            dispatch({ type: 'delivery', payload: d });
            dispatch({ type: 'deliveries', payload: list });
            dispatch({ type: 'deliveryDialogOptions', payload: { open: true, mode, direction, site } });
        },
        [materialNumber]
    );

    const openDialog = useCallback(
        <TCategory extends DataCategory>(
            category: TCategory,
            data: Partial<DataCategoryTypeMap[TCategory]>,
            list: DataCategoryTypeMap[TCategory][],
            mode: ModalMode,
            direction?: DirectionType,
            site?: Site | null
        ) => {
            list ||= [];
            if (category === 'delivery') {
                openDeliveryDialog(data as Partial<DataCategoryTypeMap['delivery']>, mode, direction ?? DirectionType.Inbound, site ?? null, list as Delivery[]);
            } else if (category === 'demand') {
                openDemandDialog(data as Partial<DataCategoryTypeMap['demand']>, mode, list as Demand[]);
            } else if (category === 'production') {
                openProductionDialog(data as Partial<DataCategoryTypeMap['production']>, mode, list as Production[]);
            }
        },
        [openDeliveryDialog, openDemandDialog, openProductionDialog]
    );
    return (
        <>
            <dataModalContext.Provider value={{ openDialog, addOnSaveListener, removeOnSaveListener }}>{children}</dataModalContext.Provider>
            <DemandCategoryModal
                {...state.demandDialogOptions}
                onClose={() => dispatch({ type: 'demandDialogOptions', payload: { open: false, mode: state.demandDialogOptions.mode } })}
                onSave={() => onSave('demand')}
                demand={state.demand}
                demands={state.demands}
            />
            <PlannedProductionModal
                {...state.productionDialogOptions}
                onClose={() => dispatch({ type: 'productionDialogOptions', payload: { open: false, mode: state.productionDialogOptions.mode } })}
                onSave={() => onSave('production')}
                production={state.production}
                productions={state.productions}
            />
            <DeliveryInformationModal
                {...state.deliveryDialogOptions}
                onClose={() => dispatch({ type: 'deliveryDialogOptions', payload: { ...state.deliveryDialogOptions, open: false } })}
                onSave={() => onSave('delivery')}
                delivery={state.delivery}
                deliveries={state.deliveries}
            />
        </>
    );
};

export function useDataModal() {
    const context = useContext(dataModalContext);
    if (context === null) {
        throw new Error('useDataModal must be used within a DataModalProvider');
    }
    return context;
}

type ModalState = {
    deliveryDialogOptions: { open: boolean; mode: ModalMode; direction: DirectionType; site: Site | null };
    demandDialogOptions: { open: boolean; mode: ModalMode };
    productionDialogOptions: { open: boolean; mode: ModalMode };
    delivery: Delivery | null;
    demand: Partial<Demand> | null;
    production: Partial<Production> | null;
    deliveries: Delivery[];
    demands: Demand[];
    productions: Production[];
};

type ModalAction = {
    type: keyof ModalState;
    payload: ModalState[keyof ModalState];
};

const reducer = (state: ModalState, action: ModalAction): ModalState => {
    return { ...state, [action.type]: action.payload };
};

const initialState: ModalState = {
    deliveryDialogOptions: { open: false, mode: 'edit', direction: DirectionType.Inbound, site: null },
    demandDialogOptions: { open: false, mode: 'edit' },
    productionDialogOptions: { open: false, mode: 'edit' },
    delivery: null,
    demand: null,
    production: null,
    deliveries: [],
    demands: [],
    productions: [],
};
