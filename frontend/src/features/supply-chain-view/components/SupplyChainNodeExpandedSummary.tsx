/*
Copyright (c) 2026 Volkswagen AG
Copyright (c) 2026 Contributors to the Eclipse Foundation

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

import { useMemo } from 'react';
import { Stack, Typography } from '@mui/material';
import { Partner } from '@models/types/edc/partner';
import { useReportedProduction } from '@features/material-details/hooks/useReportedProduction';
import { useReportedStocks } from '@features/stock-view/hooks/useReportedStocks';
import { useDelivery } from '@features/material-details/hooks/useDelivery';
import { ReportedSummaryPanel } from '@features/material-details/components/SummaryPanel';
import { createSummary } from '@features/material-details/util/summary-service';
import { useAggregatedMaterialData } from '@hooks/useAggregatedMaterialData';
import { AggregatedMaterialDataNodeSummary } from './AggregatedMaterialDataNodeSummary';

type SupplyChainNodeExpandedSummaryProps = {
    ownMaterialNumber: string;
    depth: number;
    partners: Partner[] | null | undefined;
    isLoadingPartners: boolean;
};

export function SupplyChainNodeExpandedSummary({ ownMaterialNumber, depth, partners, isLoadingPartners }: SupplyChainNodeExpandedSummaryProps) {
    const { reportedProductions } = useReportedProduction(ownMaterialNumber);
    const { deliveries } = useDelivery(ownMaterialNumber, null);
    const { reportedStocks } = useReportedStocks('material', ownMaterialNumber);
    const { aggregatedMaterialData } = useAggregatedMaterialData(ownMaterialNumber);

    const partnerSummaries = useMemo(
        () =>
            (partners ?? []).map((partner) => {
                const partnerBpnss = partner.sites.map((site) => site.bpns);
                const productions = reportedProductions?.filter((p) => p.partner.bpnl === partner.bpnl) ?? [];
                const partnerDeliveries = deliveries?.filter((d) => partnerBpnss.includes(d.originBpns)) ?? [];
                const partnerStocks = reportedStocks?.filter((s) => s.partner.bpnl === partner.bpnl) ?? [];
                return { partner, summary: createSummary('production', productions, partnerDeliveries, partnerStocks) };
            }),
        [partners, reportedProductions, deliveries, reportedStocks]
    );

    let partnerSection;
    if (isLoadingPartners) {
        partnerSection = (
            <Typography variant="body2" color="text.secondary" padding="0.5rem 1rem">
                Loading supplier data...
            </Typography>
        );
    } else if (partnerSummaries.length === 0) {
        partnerSection = (
            <Typography variant="body2" color="text.secondary" padding="0.5rem 1rem">
                No supplying partner found for this material.
            </Typography>
        );
    } else {
        partnerSection = partnerSummaries.map(({ partner, summary }) => (
            <Stack key={partner.bpnl}>
                <ReportedSummaryPanel summary={summary} materialNumber={ownMaterialNumber} partnerBpnl={partner.bpnl} includeDaysOfSupply />
            </Stack>
        ));
    }

    return (
        <>
            {partnerSection}
            {aggregatedMaterialData.map((data) => (
                <Stack key={data.uuid}>
                    {data.childMaterialData.map((node) => (
                        <AggregatedMaterialDataNodeSummary key={node.uuid} node={node} depth={depth + 1} />
                    ))}
                </Stack>
            ))}
        </>
    );
}
