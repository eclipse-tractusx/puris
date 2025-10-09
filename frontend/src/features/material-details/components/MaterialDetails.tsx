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
import { OwnSummaryPanel } from './SummaryPanel';
import { CollapsibleSummary } from './CollapsibleSummary';
import { DataCategory, useMaterialDetails } from '../hooks/useMaterialDetails';
import { useNotifications } from '@contexts/notificationContext';
import { useDataModal } from '@contexts/dataModalContext';
import { ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { groupBy } from '@util/helpers';
import { DirectionType } from '@models/types/erp/directionType';
import { createSummary, PartnerSummary } from '../util/summary-service';
import { Partner } from '@models/types/edc/partner';
import { scheduleErpUpdateStocks } from '@services/stocks-service';
import { NotFoundView } from '@views/errors/NotFoundView';
import { Material } from '@models/types/data/stock';
import { BPNS } from '@models/types/edc/bpn';
import { IMessage, useSubscription } from 'react-stomp-hooks';
import { refreshPartnerData } from '@services/refresh-service';

type SummaryContainerProps = {
    children: ReactNode;
};

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

function makeErrorDownload(payloadText: string, materialNo: string) {
  let contents: string;
  try {
    contents = JSON.stringify(JSON.parse(payloadText), null, 2);
  } catch {
    contents = JSON.stringify(
      { material: materialNo, errors: [payloadText] },
      null,
      2
    );
  }

  const blob = new Blob([contents], { type: 'application/json' });
  const url = URL.createObjectURL(blob);

  const ts = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `material-${materialNo}-refresh-errors-${ts}.json`;

  return { url, filename };
}

function parseRefreshWsMessage(msg?: string): { ok: boolean; errors: string[] } {
  const text = (msg ?? '').trim();
  if (!text || text === 'SUCCESS') return { ok: true, errors: [] };

  try {
    const arr = JSON.parse(text) as unknown;
    if (Array.isArray(arr)) {
      const a = arr as any[];

      const hasMessage = a.some(e => e && typeof e === 'object' && 'message' in e);
      const errors = hasMessage
        ? a.flatMap(entry => {
            const msg = typeof entry?.message === 'string' ? entry.message.trim() : '';
            const prefix = msg ? `${msg}: ` : '';
            const errs = Array.isArray(entry?.errors) ? entry.errors : [];
            return errs.flatMap((e: any) => {
              if (Array.isArray(e?.errors)) {
                return e.errors.filter((s: any) => typeof s === 'string' && s.length > 0)
                               .map((s: string) => prefix + s);
              }
              return typeof e === 'string' && e.length > 0 ? [prefix + e] : [];
            });
          })
        : (a as { errors?: string[] }[])
            .flatMap(e => Array.isArray(e?.errors) ? e.errors! : [])
            .filter(s => typeof s === 'string' && s.length > 0);
      return { ok: errors.length === 0, errors: errors.length ? errors : [text] };
    }
    return { ok: false, errors: [text] };
  } catch {
    return { ok: false, errors: [text] };
  }
}

type MaterialDetailsProps = {
    material: Material;
    direction: DirectionType;
};

export function MaterialDetails({ material, direction }: MaterialDetailsProps) {
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [isSchedulingUpdate, setIsSchedulingUpdate] = useState(false);
    const { notify } = useNotifications();
    const { addOnSaveListener, removeOnSaveListener } = useDataModal();
    const {
        productions,
        demands,
        deliveries,
        stocks,
        expandablePartners,
        sites,
        reportedDemands,
        reportedProductions,
        reportedStocks,
        refresh,
    } = useMaterialDetails(material.ownMaterialNumber ?? '', direction);
    const incomingDeliveries = useMemo(() => deliveries?.filter((d) => sites?.some((site) => site.bpns === d.destinationBpns)), [deliveries, sites]);
    const outgoingShipments = useMemo(() => deliveries?.filter((d) => sites?.some((site) => site.bpns === d.originBpns)), [deliveries, sites]);
    const groupedProductions = useMemo(() => groupBy(productions ?? [], (prod) => prod.productionSiteBpns), [productions]);
    const groupedDemands = useMemo(() => groupBy(demands ?? [], (dem) => dem.demandLocationBpns), [demands]);
    const groupedIncomingDeliveries = useMemo(() => groupBy(incomingDeliveries ?? [], (del) => del.destinationBpns), [incomingDeliveries]);
    const groupedOutgoingShipments = useMemo(() => groupBy(outgoingShipments ?? [], (del) => del.originBpns), [outgoingShipments]);
    const groupedStocks = useMemo(() => groupBy(stocks ?? [], (stock) => stock.stockLocationBpns), [stocks]);

    const createSummaryByPartnerAndDirection = useCallback((partner: Partner, direction: DirectionType, partnerSite?: BPNS, ownSite?: BPNS) => {
        let partnerStocks = reportedStocks?.filter((s) => s.partner.bpnl === partner.bpnl);
        let partnerBpnss = partner.sites.map((s) => s.bpns);
        if (partnerSite) {
            partnerBpnss = partnerBpnss.filter((bpns) => bpns === partnerSite);
            partnerStocks = partnerStocks?.filter((stock) => stock.stockLocationBpns === partnerSite);
        }
        if (direction === DirectionType.Outbound) {
            const demands = reportedDemands?.filter(
                (d) =>
                    d.partnerBpnl === partner.bpnl &&
                (!partnerSite || d.demandLocationBpns === partnerSite) &&
                (!ownSite || d.supplierLocationBpns === ownSite)
            );
            const deliveries = outgoingShipments?.filter(
                (d) => partnerBpnss.includes(d.destinationBpns) && (!ownSite || d.originBpns === ownSite)
            );
            return createSummary('demand', demands ?? [], deliveries ?? [], partnerStocks ?? []);
        } else {
            const productions = reportedProductions?.filter(
                (p) => p.partner.bpnl === partner.bpnl && (!partnerSite || p.productionSiteBpns === partnerSite)
            );
            const shipments = incomingDeliveries?.filter(
                (d) => partnerBpnss.includes(d.originBpns) && (!ownSite || d.destinationBpns === ownSite)
            );
            return createSummary('production', productions ?? [], shipments ?? [], partnerStocks ?? []);
        }
    }, [incomingDeliveries, outgoingShipments, reportedDemands, reportedProductions, reportedStocks]);

    const partnerSummaries: PartnerSummary = useMemo(() => expandablePartners.reduce((summaries, partner) => {
        const siteSummaries = partner.sites.reduce((summaries, site) => ({
            ...summaries,
            [site.bpns]: createSummaryByPartnerAndDirection(partner, direction, site.bpns)
        }), {});
        return {
            ...summaries,
            [partner.bpnl]: {
                summary: createSummaryByPartnerAndDirection(partner, direction),
                siteSummaries
            }
        };
    }, {}), [createSummaryByPartnerAndDirection, direction, expandablePartners]);

    const handleRefreshMessage = async (message?: string) => {
        const raw = (message ?? '').trim();
        const { ok, errors } = parseRefreshWsMessage(raw);
        try {
            await refresh(['partner-data']);

            if (ok) {
                notify({
                    title: 'Partner data updated',
                    description: `The partner data for ${material.name} was updated as requested.`,
                    severity: 'success',
                });
            } else {
                const { url, filename } = makeErrorDownload(raw, material.ownMaterialNumber || '');
                notify({
                    title: 'Partner data updated with errors',
                   description: (
                        <span>
                        Found {errors.length} issue{errors.length === 1 ? '' : 's'}.{' '}
                        <a
                            href={url}
                            download={filename}
                            onClick={() => {
                                setTimeout(() => URL.revokeObjectURL(url), 3000);
                            }}
                        >
                            Download error details
                        </a>
                        </span>
                    ) as unknown as string,
                    severity: 'error',
                });
            }
        } finally {
            setIsRefreshing(false);
        }
    };
    useSubscription('/topic/material/' + material.ownMaterialNumber, (msg: IMessage) => handleRefreshMessage(msg?.body));
    
    useEffect(() => {
        const callback = (category: DataCategory) => refresh([category]);
        addOnSaveListener(callback);
        return () => removeOnSaveListener(callback);
    }, [addOnSaveListener, refresh, removeOnSaveListener]);
    
    if (!material) {
        return <NotFoundView />;
    }
    
    const summary =
    direction === DirectionType.Outbound
    ? createSummary('production', productions ?? [], outgoingShipments ?? [], stocks ?? [])
    : createSummary('demand', demands ?? [], incomingDeliveries ?? [], stocks ?? []);
    
    const handlePartnerDataRequest = () => {
        setIsRefreshing(true);
        refreshPartnerData(material.ownMaterialNumber)
            .then(() => {
                notify({
                    title: 'Partner data update requested.',
                    description: `An update to the partner data for ${material.name} was requested`,
                    severity: 'success'
                })
            })
            .catch((error: unknown) => {
                const msg =
                error !== null && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
                ? error.message
                : 'Unknown Error';
                notify({
                    title: 'Error requesting update',
                    description: msg,
                    severity: 'error',
                });
            });
    };
        
    const handleScheduleUpdate = () => {
        setIsSchedulingUpdate(true);
        Promise.all(
            expandablePartners.map((partner) =>
                scheduleErpUpdateStocks(
                    direction === DirectionType.Outbound ? 'product' : 'material',
                    partner.bpnl,
                    material.ownMaterialNumber
            )
        ))
        .then(() => {
            notify({
                title: 'Update requested',
                description: `Scheduled ERP data update of stocks for ${material?.ownMaterialNumber} in your role as ${
                        direction === DirectionType.Inbound ? 'Customer' : 'Supplier'
                    }. Please reload dialog later.`,
                    severity: 'success',
                });
        })
        .catch((error: unknown) => {
            const msg =
            error !== null && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
            ? error.message
            : 'Unknown Error';
            notify({
                title: 'Error scheduling ERP update',
                description: msg,
                severity: 'error',
            });
        })
        .finally(() => setIsSchedulingUpdate(false));
    };
        
        return (
            <CalendarWeekProvider>
            <Stack spacing={2}>
                <ConfidentialBanner />
                <MaterialDetailsHeader
                    material={material}
                    direction={direction}
                    isRefreshing={isRefreshing}
                    isSchedulingUpdate={isSchedulingUpdate}
                    onRefresh={handlePartnerDataRequest}
                    onScheduleUpdate={handleScheduleUpdate}
                />
                <Stack spacing={5}>
                    <SummaryContainer>
                        <OwnSummaryPanel title={`${capitalize(summary.type ?? '')} Summary`} summary={summary} materialNumber={material.ownMaterialNumber ?? ''} showHeader includeDaysOfSupply />
                        {expandablePartners.map((partner) => (
                            <CollapsibleSummary
                                key={partner.bpnl}
                                summary={partnerSummaries[partner.bpnl].summary}
                                materialNumber={material.ownMaterialNumber ?? ''}
                                partnerBpnl={partner.bpnl}
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
                                        summary={partnerSummaries[partner.bpnl].siteSummaries[site.bpns]}
                                        materialNumber={material.ownMaterialNumber ?? ''}
                                        partnerBpnl={partner.bpnl}
                                        site={site.bpns}
                                        variant="sub"
                                        renderTitle={() => (
                                            <>
                                                <Typography variant="body2" color="#ccc" data-testid="partner-site-owner">{partner.name}/</Typography>
                                                <Typography variant="body1" data-testid="partner-site-name">{site.name}</Typography>
                                                <Typography variant="body3" color="#ccc" data-testid="partner-site-bpns">({site.bpns})</Typography>
                                            </>
                                        )}
                                        includeDaysOfSupply
                                    />
                                ))}
                            </CollapsibleSummary>
                        ))}
                    </SummaryContainer>
                    {sites?.map((site) => (
                        <SummaryContainer key={site.bpns}>
                            <OwnSummaryPanel
                                title={site.name}
                                summary={
                                    direction === DirectionType.Outbound
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
                                materialNumber={material.ownMaterialNumber ?? ''}
                                site={site.bpns}
                                showHeader
                                includeDaysOfSupply
                            ></OwnSummaryPanel>
                            {expandablePartners.map((partner) => (
                                <CollapsibleSummary
                                    key={partner.bpnl}
                                    summary={createSummaryByPartnerAndDirection(partner, direction, undefined, site.bpns)}
                                    materialNumber={material.ownMaterialNumber ?? ''}
                                    site={site.bpns}
                                    partnerBpnl={partner.bpnl}
                                    renderTitle={() => (
                                        <>
                                            <Typography variant="body1">{partner.name}</Typography>
                                            <Typography variant="body3" color="#ccc">
                                                ({partner.bpnl})
                                            </Typography>
                                        </>
                                    )}
                                >
                                    {partner.sites.map((partnerSite) => (
                                        <CollapsibleSummary
                                            key={partnerSite.bpns}
                                            summary={createSummaryByPartnerAndDirection(partner, direction, partnerSite.bpns, site.bpns)}
                                            materialNumber={material.ownMaterialNumber ?? ''}
                                            site={site.bpns}
                                            partnerBpnl={partner.bpnl}
                                            variant="sub"
                                            renderTitle={() => (
                                                <>
                                                    <Typography variant="body2" color="#ccc">
                                                        {partner.name}/
                                                    </Typography>
                                                    <Typography variant="body1">{partnerSite.name}</Typography>
                                                    <Typography variant="body3" color="#ccc">
                                                        ({partnerSite.bpns})
                                                    </Typography>
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
