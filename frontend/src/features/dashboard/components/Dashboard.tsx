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
import { usePartnerStocks } from '@features/stock-view/hooks/usePartnerStocks';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { Site } from '@models/types/edc/site';
import { useCallback, useReducer } from 'react';
import { DashboardFilters } from './DashboardFilters';
import { DemandTable } from './DemandTable';
import { ProductionTable } from './ProductionTable';
import { Box, Button, Stack, Typography, capitalize } from '@mui/material';
import { Delivery } from '@models/types/data/delivery';
import { DeliveryInformationModal } from './DeliveryInformationModal';
import { getPartnerType } from '../util/helpers';
import { LoadingButton } from '@catena-x/portal-shared-components';
import { Refresh } from '@mui/icons-material';
import { Demand } from '@models/types/data/demand';
import { DemandCategoryModal } from './DemandCategoryModal';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { useDemand } from '../hooks/useDemand';
import { useReportedDemand } from '../hooks/useReportedDemand';
import { Production } from '@models/types/data/production';
import { PlannedProductionModal } from './PlannedProductionModal';
import { useProduction } from '../hooks/useProduction';
import { useReportedProduction } from '../hooks/useReportedProduction';

import { requestReportedStocks } from '@services/stocks-service';
import { useDelivery } from '../hooks/useDelivery';
import { requestReportedDeliveries } from '@services/delivery-service';
import { requestReportedProductions } from '@services/productions-service';
import { requestReportedDemands } from '@services/demands-service';
import { ModalMode } from '@models/types/data/modal-mode';

const NUMBER_OF_DAYS = 28;

type DashboardState = {
    selectedMaterial: MaterialDescriptor | null;
    selectedSite: Site | null;
    selectedPartnerSites: Site[] | null;
    deliveryDialogOptions: { open: boolean; mode: ModalMode, direction: 'incoming' | 'outgoing', site: Site | null };
    demandDialogOptions: { open: boolean; mode: ModalMode };
    productionDialogOptions: { open: boolean; mode: ModalMode };
    delivery: Delivery | null;
    demand: Partial<Demand> | null;
    production: Partial<Production> | null;
    isRefreshing: boolean;
};

type DashboardAction = {
    type: keyof DashboardState;
    payload: DashboardState[keyof DashboardState];
};

const reducer = (state: DashboardState, action: DashboardAction): DashboardState => {
    return { ...state, [action.type]: action.payload };
};

const initialState: DashboardState = {
    selectedMaterial: null,
    selectedSite: null,
    selectedPartnerSites: null,
    deliveryDialogOptions: { open: false, mode: 'edit', direction: 'incoming', site: null },
    demandDialogOptions: { open: false, mode: 'edit' },
    productionDialogOptions: { open: false, mode: 'edit' },
    delivery: null,
    demand: null,
    production: null,
    isRefreshing: false,
};

export const Dashboard = ({ type }: { type: 'customer' | 'supplier' }) => {
    const [state, dispatch] = useReducer(reducer, initialState);
    const { stocks } = useStocks(type === 'customer' ? 'material' : 'product');
    const { partnerStocks } = usePartnerStocks(
        type === 'customer' ? 'material' : 'product',
        state.selectedMaterial?.ownMaterialNumber ?? null
    );
    const { demands, refreshDemand } = useDemand(state.selectedMaterial?.ownMaterialNumber ?? null, state.selectedSite?.bpns ?? null);
    const { reportedDemands } = useReportedDemand(state.selectedMaterial?.ownMaterialNumber ?? null);
    const { productions, refreshProduction } = useProduction(
        state.selectedMaterial?.ownMaterialNumber ?? null,
        state.selectedSite?.bpns ?? null
    );
    const { reportedProductions } = useReportedProduction(state.selectedMaterial?.ownMaterialNumber ?? null);
    const { deliveries, refreshDelivery } = useDelivery(
        state.selectedMaterial?.ownMaterialNumber ?? null,
        state.selectedSite?.bpns ?? null
    );

    const handleRefresh = () => {
        dispatch({ type: 'isRefreshing', payload: true });
        Promise.all([
            requestReportedStocks(type === 'customer' ? 'material' : 'product', state.selectedMaterial?.ownMaterialNumber ?? null),
            requestReportedDeliveries(state.selectedMaterial?.ownMaterialNumber ?? null),
            type === 'customer'
                ? requestReportedProductions(state.selectedMaterial?.ownMaterialNumber ?? null)
                : requestReportedDemands(state.selectedMaterial?.ownMaterialNumber ?? null)
        ]).finally(() => dispatch({ type: 'isRefreshing', payload: false }));
    };
    const openDeliveryDialog = useCallback(
        (d: Partial<Delivery>, mode: ModalMode, direction: 'incoming' | 'outgoing' = 'outgoing', site: Site | null) => {
            d.ownMaterialNumber = state.selectedMaterial?.ownMaterialNumber ?? '';
            dispatch({ type: 'delivery', payload: d });
            dispatch({ type: 'deliveryDialogOptions', payload: { open: true, mode, direction, site } });
        },
        [state.selectedMaterial?.ownMaterialNumber]
    );
    const openDemandDialog = (d: Partial<Demand>, mode: ModalMode) => {
        d.measurementUnit ??= 'unit:piece';
        d.demandCategoryCode ??= DEMAND_CATEGORY[0]?.key;
        d.ownMaterialNumber = state.selectedMaterial?.ownMaterialNumber ?? '';
        dispatch({ type: 'demand', payload: d });
        dispatch({ type: 'demandDialogOptions', payload: { open: true, mode } });
    };
    const openProductionDialog = (p: Partial<Production>, mode: ModalMode) => {
        p.material ??= {
            materialFlag: true,
            productFlag: false,
            materialNumberSupplier: state.selectedMaterial?.ownMaterialNumber ?? '',
            materialNumberCustomer: null,
            materialNumberCx: null,
            name: state.selectedMaterial?.description ?? '',
        };
        p.measurementUnit ??= 'unit:piece';
        dispatch({ type: 'production', payload: p });
        dispatch({ type: 'productionDialogOptions', payload: { open: true, mode } });
    };
    const handleMaterialSelect = useCallback((material: MaterialDescriptor | null) => {
        dispatch({ type: 'selectedMaterial', payload: material });
        dispatch({ type: 'selectedSite', payload: null });
        dispatch({ type: 'selectedPartnerSites', payload: null });
    }, []);
    return (
        <>
            <Stack spacing={3} alignItems={'center'} useFlexGap>
                <DashboardFilters
                    type={type}
                    material={state.selectedMaterial}
                    site={state.selectedSite}
                    partnerSites={state.selectedPartnerSites}
                    onMaterialChange={handleMaterialSelect}
                    onSiteChange={(site) => dispatch({ type: 'selectedSite', payload: site })}
                    onPartnerSitesChange={(sites) => dispatch({ type: 'selectedPartnerSites', payload: sites })}
                />
                <Box width="100%" marginTop="1rem">
                    <Typography variant="h5" component="h2">
                        Production Information
                        {state.selectedMaterial && state.selectedSite && <> for {state.selectedMaterial.description} ({state.selectedMaterial.ownMaterialNumber})</>}
                    </Typography>
                    {state.selectedSite && state.selectedMaterial ? (
                        type === 'supplier' ? (
                            <ProductionTable
                                readOnly={false}
                                numberOfDays={NUMBER_OF_DAYS}
                                stocks={stocks ?? []}
                                site={state.selectedSite}
                                onDeliveryClick={(delivery, mode) => openDeliveryDialog(delivery, mode, 'outgoing', state.selectedSite)}
                                onProductionClick={openProductionDialog}
                                productions={productions ?? []}
                                deliveries={deliveries ?? []}
                            />
                        ) : (
                            <DemandTable
                                numberOfDays={NUMBER_OF_DAYS}
                                stocks={stocks}
                                site={state.selectedSite}
                                onDeliveryClick={(delivery, mode) => openDeliveryDialog(delivery, mode, 'incoming', state.selectedSite)}
                                onDemandClick={openDemandDialog}
                                demands={demands}
                                deliveries={deliveries ?? []}
                            />
                        )
                    ) : (
                        <Typography variant="body1">Select a Site to show production data</Typography>
                    )}
                </Box>
                {state.selectedSite && (
                    <Stack width="100%">
                        <Box display="flex" justifyContent="space-between">
                            <Typography variant="h5" component="h2">
                                {`${capitalize(getPartnerType(type))} Information ${
                                    state.selectedMaterial ? `for ${state.selectedMaterial.description} (${state.selectedMaterial.ownMaterialNumber})` : ''
                                }`}
                            </Typography>
                            {state.selectedPartnerSites?.length &&
                                (state.isRefreshing ? (
                                    <LoadingButton
                                        label="Refresh"
                                        loadIndicator="refreshing..."
                                        loading={state.isRefreshing}
                                        variant="contained"
                                        onButtonClick={handleRefresh}
                                        sx={{ width: '10rem' }}
                                    />
                                ) : (
                                    <Button
                                        variant="contained"
                                        onClick={handleRefresh}
                                        sx={{ display: 'flex', alignItems: 'center', gap: '0.5rem', width: '10rem' }}
                                    >
                                        <Refresh></Refresh> Refresh
                                    </Button>
                                ))}
                        </Box>
                        <Stack spacing={4}>
                            {state.selectedPartnerSites ? (
                                state.selectedPartnerSites.map((ps) =>
                                    type === 'supplier' ? (
                                        <DemandTable
                                            key={ps.bpns}
                                            numberOfDays={NUMBER_OF_DAYS}
                                            stocks={partnerStocks}
                                            site={ps}
                                            onDeliveryClick={(delivery, mode) => openDeliveryDialog(delivery, mode, 'incoming', ps)}
                                            onDemandClick={openDemandDialog}
                                            demands={reportedDemands?.filter((d) => d.demandLocationBpns === ps.bpns) ?? []}
                                            deliveries={deliveries ?? []}
                                            readOnly
                                        />
                                    ) : (
                                        <ProductionTable
                                            key={ps.bpns}
                                            numberOfDays={NUMBER_OF_DAYS}
                                            stocks={partnerStocks ?? []}
                                            site={ps}
                                            onDeliveryClick={(delivery, mode) => openDeliveryDialog(delivery, mode, 'outgoing', ps)}
                                            onProductionClick={openProductionDialog}
                                            productions={reportedProductions?.filter((p) => p.productionSiteBpns === ps.bpns) ?? []}
                                            deliveries={deliveries ?? []}
                                            readOnly
                                        />
                                    )
                                )
                            ) : (
                                <Typography variant="body1">{`Select a ${getPartnerType(
                                    type
                                )} site to show their stock information`}</Typography>
                            )}
                        </Stack>
                    </Stack>
                )}
            </Stack>
            <DemandCategoryModal
                {...state.demandDialogOptions}
                onClose={() => dispatch({ type: 'demandDialogOptions', payload: { open: false, mode: state.demandDialogOptions.mode } })}
                onSave={refreshDemand}
                demand={state.demand}
                demands={(state.demandDialogOptions.mode === 'view' ? reportedDemands : demands) ?? []}
            />
            <PlannedProductionModal
                {...state.productionDialogOptions}
                onClose={() => dispatch({ type: 'productionDialogOptions', payload: { open: false, mode: state.productionDialogOptions.mode } })}
                onSave={refreshProduction}
                production={state.production}
                productions={(state.productionDialogOptions.mode === 'view' ? reportedProductions : productions) ?? []}
            />
            <DeliveryInformationModal
                {...state.deliveryDialogOptions}
                onClose={() =>
                    dispatch({ type: 'deliveryDialogOptions', payload: { ...state.deliveryDialogOptions, open: false, } })
                }
                onSave={refreshDelivery}
                delivery={state.delivery}
                deliveries={deliveries ?? []}
            />
        </>
    );
}
