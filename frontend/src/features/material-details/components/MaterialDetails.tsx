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

import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { CalendarWeekProvider } from '@contexts/calendarWeekContext';
import { Box, capitalize, Stack, Typography } from '@mui/material';
import { MaterialDetailsHeader } from './MaterialDetailsHeader';
import { SummaryPanel } from './SummaryPanel';
import { CollapsibleSummary } from './CollapsibleSummary';
import { DataCategory, useDataModal } from '@contexts/dataModalContext';
import { ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { groupBy } from '@util/helpers';
import { DirectionType } from '@models/types/erp/directionType';
import { createSummary } from '../util/summary-service';
import { Partner } from '@models/types/edc/partner';
import { NotFoundView } from '@views/errors/NotFoundView';
import { Material } from '@models/types/data/stock';
import { BPNS } from '@models/types/edc/bpn';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useProduction } from '../hooks/useProduction';
import { useDemand } from '../hooks/useDemand';
import { useDelivery } from '../hooks/useDelivery';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { Expandable } from '../models/expandable';
import { useReportedProduction } from '../hooks/useReportedProduction';
import { useReportedDemand } from '../hooks/useReportedDemand';
import { useReportedStocks } from '@features/stock-view/hooks/useReportedStocks';

type SummaryContainerProps = { children: ReactNode; };

function SummaryContainer({ children }: SummaryContainerProps) {
    return (
        <Box
            sx={{
                backgroundColor: 'white',
                borderRadius: '.5rem',
                overflow: 'hidden',
                boxShadow: 'rgba(0,0,0,0.1) 0px 1px 3px 0px',
            }}
        >
            <Stack sx={{ overflowX: 'auto', position: 'relative' }}>{children}</Stack>
        </Box>
    );
}

type MaterialDetailsProps = { material: Material; direction: DirectionType; };

export function MaterialDetails({ material, direction }: MaterialDetailsProps) {
    const { addOnSaveListener, removeOnSaveListener } = useDataModal();
    const { sites, isLoadingSites } = useSites();
    const { productions, isLoadingProductions, refreshProduction } = useProduction(material.ownMaterialNumber ?? null, null);
    const { demands, isLoadingDemands, refreshDemand } = useDemand(material.ownMaterialNumber ?? null, null);
    const { deliveries, isLoadingDeliveries, refreshDelivery } = useDelivery(material.ownMaterialNumber ?? null, null);
    const { stocks, isLoadingStocks, refreshStocks } = useStocks(direction === 'INBOUND' ? 'material' : 'product');
    const { partners, isLoadingPartners } = usePartners(direction === 'INBOUND' ? 'product' : 'material', material.ownMaterialNumber);
    const [expandablePartners, setExpandablePartners] = useState<Expandable<Partner>[]>([]);
    const { reportedProductions, isLoadingReportedProductions } = useReportedProduction(material.ownMaterialNumber ?? null);
    const { reportedDemands, isLoadingReportedDemands } = useReportedDemand(material.ownMaterialNumber ?? null);
    const { reportedStocks, isLoadingReportedStocks } = useReportedStocks(direction === 'INBOUND' ? 'product' : 'material', material.ownMaterialNumber ?? null);

    const refresh = useCallback(
        (categoriesToRefresh: DataCategory[]) => {
            categoriesToRefresh.forEach((category) => {
                switch (category) {
                    case 'production':
                        refreshProduction();
                        break;
                    case 'demand':
                        refreshDemand();
                        break;
                    case 'delivery':
                        refreshDelivery();
                        break;
                    case 'stock':
                        refreshStocks();
                        break;
                    default:
                        return;
                }
            });
        },
        [refreshDelivery, refreshDemand, refreshProduction, refreshStocks]
    );
    const isLoading =
        isLoadingProductions ||
        isLoadingDemands ||
        isLoadingDeliveries ||
        isLoadingStocks ||
        isLoadingPartners ||
        isLoadingSites ||
        isLoadingReportedProductions ||
        isLoadingReportedDemands ||
        isLoadingReportedStocks;
    const incomingDeliveries = deliveries?.filter((d) => sites?.some((site) => site.bpns === d.destinationBpns));
    const outgoingShipments = deliveries?.filter((d) => sites?.some((site) => site.bpns === d.originBpns));
    const groupedProductions = useMemo(() => groupBy(productions ?? [], (prod) => prod.productionSiteBpns), [productions]);
    const groupedDemands = useMemo(() => groupBy(demands ?? [], (dem) => dem.demandLocationBpns), [demands]);
    const groupedIncomingDeliveries = useMemo(() => groupBy(incomingDeliveries ?? [], (del) => del.destinationBpns), [incomingDeliveries]);
    const groupedOutgoingShipments = useMemo(() => groupBy(outgoingShipments ?? [], (del) => del.originBpns), [outgoingShipments]);
    const groupedStocks = useMemo(() => groupBy(stocks ?? [], (stock) => stock.stockLocationBpns), [stocks]);

    useEffect(() => {
        if (isLoadingPartners) return;
        setExpandablePartners(partners?.map((p) => ({ isExpanded: false, ...p })) ?? []);
    }, [isLoadingPartners, partners]);
    useEffect(() => {
        const callback = (category: DataCategory) => refresh([category]);
        addOnSaveListener(callback);
        return () => removeOnSaveListener(callback);
    }, [addOnSaveListener, refresh, removeOnSaveListener]);

    if (isLoading) { return <Typography variant="body1">Loading...</Typography>; }
    if (!material) { return <NotFoundView />; }

    const summary = direction === DirectionType.Outbound
            ? createSummary('production', productions ?? [], outgoingShipments ?? [], stocks ?? [])
            : createSummary('demand', demands ?? [], incomingDeliveries ?? [], stocks ?? []);

    const createSummaryByPartnerAndDirection = (partner: Partner, direction: DirectionType, partnerSite?: BPNS, ownSite?: BPNS) => {
        let partnerStocks = reportedStocks?.filter((s) => s.partner.bpnl === partner.bpnl);
        let partnerBpnss = partner.sites.map((s) => s.bpns);
        if (partnerSite) {
            partnerBpnss = partnerBpnss.filter((bpns) => bpns === partnerSite);
            partnerStocks = partnerStocks?.filter((stock) => stock.stockLocationBpns === partnerSite);
        }
        if (direction === DirectionType.Outbound) {
            const demands = reportedDemands?.filter((d) =>
                d.partnerBpnl === partner.bpnl && (!partnerSite || d.demandLocationBpns === partnerSite) && (!ownSite || d.supplierLocationBpns === ownSite)
            );
            const deliveries = outgoingShipments?.filter((d) => partnerBpnss.includes(d.destinationBpns) && (!ownSite || d.originBpns === ownSite));
            return createSummary('demand', demands ?? [], deliveries ?? [], partnerStocks ?? []);
        } else {
            const productions = reportedProductions?.filter((p) => p.partner.bpnl === partner.bpnl && (!partnerSite || p.productionSiteBpns === partnerSite));
            const shipments = incomingDeliveries?.filter((d) => partnerBpnss.includes(d.originBpns) && (!ownSite || d.destinationBpns === ownSite));
            return createSummary('production', productions ?? [], shipments ?? [], partnerStocks ?? []);
        }
    };
    return (
        <CalendarWeekProvider>
            <Stack spacing={2}>
                <ConfidentialBanner />
                <MaterialDetailsHeader material={material} direction={direction} />
                <Stack spacing={5}>
                    <SummaryContainer>
                        <SummaryPanel title={`${capitalize(summary.type ?? '')} Summary`} summary={summary} showHeader />
                        {expandablePartners.map((partner) => (
                            <CollapsibleSummary
                                key={partner.bpnl}
                                summary={createSummaryByPartnerAndDirection(partner, direction)}
                                renderTitle={() => (
                                    <>
                                        <Typography variant="body1">{partner.name}</Typography>
                                        <Typography variant="body3" color="#ccc">({partner.bpnl})</Typography>
                                    </>
                                )}
                            >
                                {partner.sites.map((site) => (
                                    <CollapsibleSummary
                                        key={site.bpns}
                                        summary={createSummaryByPartnerAndDirection(partner, direction, site.bpns)}
                                        variant="sub"
                                        renderTitle={() => (<>
                                                                <Typography variant="body2" color="#ccc"> {partner.name}/</Typography>
                                                                <Typography variant="body1">{site.name}</Typography>
                                                                <Typography variant="body3" color="#ccc">({site.bpns})</Typography>
                                                            </>)}
                                    />
                                ))}
                            </CollapsibleSummary>
                        ))}
                    </SummaryContainer>
                    {sites?.map((site) => (
                        <SummaryContainer key={site.bpns}>
                            <SummaryPanel
                                title={site.name}
                                summary={direction === DirectionType.Outbound
                                            ? createSummary(
                                                'production',
                                                groupedProductions[site.bpns] ?? [],
                                                groupedOutgoingShipments[site.bpns] ?? [],
                                                groupedStocks[site.bpns] ?? []
                                            )
                                            : createSummary(
                                                'demand',
                                                groupedDemands[site.bpns] ?? [],
                                                groupedIncomingDeliveries[site.bpns] ?? [],
                                                groupedStocks[site.bpns] ?? []
                                            )
                                }
                                showHeader
                            ></SummaryPanel>
                            {expandablePartners.map((partner) => (
                                <CollapsibleSummary
                                    key={partner.bpnl}
                                    summary={createSummaryByPartnerAndDirection(partner, direction, undefined, site.bpns)}
                                    renderTitle={() => (
                                        <>
                                            <Typography variant="body1">{partner.name}</Typography>
                                            <Typography variant="body3" color="#ccc"> ({partner.bpnl}) </Typography>
                                        </>
                                    )}
                                >
                                    {partner.sites.map((partnerSite) => (
                                        <CollapsibleSummary
                                            key={partnerSite.bpns}
                                            summary={createSummaryByPartnerAndDirection(partner, direction, partnerSite.bpns, site.bpns)}
                                            variant="sub"
                                            renderTitle={() => (
                                                <>
                                                    <Typography variant="body2" color="#ccc">{partner.name}/</Typography>
                                                    <Typography variant="body1">{partnerSite.name}</Typography>
                                                    <Typography variant="body3" color="#ccc">({partnerSite.bpns})</Typography>
                                                </>
                                            )}
                                        />
                                    ))}
                                </CollapsibleSummary>
                            ))}
                        </SummaryContainer>
                    ))}
                </Stack>
            </Stack>
        </CalendarWeekProvider>
    );
}
