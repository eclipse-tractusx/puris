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

import { useDelivery } from '@features/material-details/hooks/useDelivery';
import { useDemand } from '@features/material-details/hooks/useDemand';
import { useProduction } from '@features/material-details/hooks/useProduction';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { Partner } from '@models/types/edc/partner';
import { DirectionType } from '@models/types/erp/directionType';
import { useCallback, useEffect, useState } from 'react';
import { Expandable } from '../models/expandable';
import { useReportedProduction } from './useReportedProduction';
import { useReportedDemand } from './useReportedDemand';
import { useReportedStocks } from '@features/stock-view/hooks/useReportedStocks';
import { Production } from '@models/types/data/production';
import { Demand } from '@models/types/data/demand';
import { Delivery } from '@models/types/data/delivery';
import { Stock } from '@models/types/data/stock';
import { useDaysOfSupply } from './useDaysOfSupply';
import { Supply } from '@models/types/data/supply';

export type DataCategory = 'production' | 'demand' | 'stock' | 'delivery' | 'supply' | 'partner-data';

export type DataCategoryTypeMap = {
    'production': Production;
    'demand': Demand;
    'delivery': Delivery;
    'stock': Stock;
    'supply': Supply; 
}

export function useMaterialDetails(materialNumber: string, direction: DirectionType) {
    const { sites, isLoadingSites } = useSites();
    const { productions, isLoadingProductions, refreshProduction } = useProduction(materialNumber ?? null, null);
    const { demands, isLoadingDemands, refreshDemand } = useDemand(materialNumber ?? null, null);
    const { deliveries, isLoadingDeliveries, refreshDelivery } = useDelivery(materialNumber ?? null, null);
    const { stocks, isLoadingStocks, refreshStocks } = useStocks(direction === 'INBOUND' ? 'material' : 'product', materialNumber);
    const { supplies, isLoadingSupply, refreshSupply } = useDaysOfSupply(materialNumber, direction);
    const { partners, isLoadingPartners } = usePartners(direction === 'INBOUND' ? 'material' : 'product', materialNumber);
    const [expandablePartners, setExpandablePartners] = useState<Expandable<Partner>[]>([]);
    const { reportedProductions, isLoadingReportedProductions, refreshReportedProduction } = useReportedProduction(materialNumber ?? null);
    const { reportedDemands, isLoadingReportedDemands, refreshReportedDemands } = useReportedDemand(materialNumber ?? null);
    const { reportedStocks, isLoadingReportedStocks, refreshReportedStocks } = useReportedStocks(
        direction === 'INBOUND' ? 'material' : 'product',
        materialNumber ?? null
    );

    const refresh = useCallback(async (categoriesToRefresh: DataCategory[]) => {
        categoriesToRefresh.forEach(category => {
            switch(category) {
                case 'production':
                    return refreshProduction();
                case 'demand':
                    return refreshDemand();
                case 'delivery':
                    return refreshDelivery();
                case 'stock':
                    return refreshStocks();
                case 'supply':
                    return refreshSupply();
                case 'partner-data':
                    return Promise.all([
                        refreshReportedDemands(),
                        refreshReportedProduction(),
                        refreshReportedStocks(),
                        refreshDelivery()
                    ])
                default:
                    return;
            }
        })
    }, [refreshDelivery, refreshDemand, refreshProduction, refreshReportedDemands, refreshReportedProduction, refreshReportedStocks, refreshStocks, refreshSupply]);

    const isLoading =
        isLoadingProductions ||
        isLoadingDemands ||
        isLoadingDeliveries ||
        isLoadingStocks ||
        isLoadingSupply ||
        isLoadingPartners ||
        isLoadingSites ||
        isLoadingReportedProductions ||
        isLoadingReportedDemands ||
        isLoadingReportedStocks;
    useEffect(() => {
        if (isLoadingPartners) return;
        setExpandablePartners(partners?.map((p) => ({ isExpanded: false, ...p })) ?? []);
    }, [isLoadingPartners, partners]);
    return {
        isLoading,
        productions,
        demands,
        deliveries,
        stocks,
        supplies,
        sites,
        expandablePartners,
        reportedProductions,
        reportedDemands,
        reportedStocks,
        refresh,
    };
}
