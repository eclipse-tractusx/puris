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
import { Box, Chip, Stack, Typography } from '@mui/material';
import { ChevronLeftOutlined } from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { Material } from '@models/types/data/stock';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { CalendarWeekProvider } from '@contexts/calendarWeekContext';
import { TextToClipboard } from '@components/ui/TextToClipboard';
import { SummaryContainer } from '@features/material-details/components/MaterialDetails';
import { OwnSummaryPanel } from '@features/material-details/components/SummaryPanel';
import { createSummary } from '@features/material-details/util/summary-service';
import { useProduction } from '@features/material-details/hooks/useProduction';
import { useDelivery } from '@features/material-details/hooks/useDelivery';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useAllMaterials } from '@hooks/useAllMaterials';
import { useAggregatedMaterialData } from '@hooks/useAggregatedMaterialData';
import { useMaterialNumbersMapping } from '@hooks/useMaterialNumbersMapping';
import { AggregatedMaterialDataNodeSummary } from './AggregatedMaterialDataNodeSummary';

type AggregatedSupplyChainViewContentProps = {
    material: Material;
};

export function AggregatedSupplyChainViewContent({ material }: AggregatedSupplyChainViewContentProps) {
    const ownMaterialNumber = material.ownMaterialNumber ?? '';
    const { productions } = useProduction(ownMaterialNumber, null);
    const { deliveries } = useDelivery(ownMaterialNumber, null);
    const { stocks } = useStocks('product', ownMaterialNumber);
    const { sites } = useSites();
    const outgoingShipments = useMemo(
        () => deliveries?.filter((d) => sites?.some((site) => site.bpns === d.originBpns)),
        [deliveries, sites]
    );
    const summary = useMemo(
        () => createSummary('production', productions ?? [], outgoingShipments ?? [], stocks ?? []),
        [productions, outgoingShipments, stocks]
    );

    const { materials } = useAllMaterials();
    const { aggregatedMaterialData } = useAggregatedMaterialData(ownMaterialNumber);
    const mappingByOwnMaterialNumber = useMaterialNumbersMapping(materials);
    const materialBySupplierNumber = useMemo(() => {
        const map = new Map<string, string>();
        mappingByOwnMaterialNumber.forEach((partnerNumbers, matchedOwnMaterialNumber) => {
            Object.values(partnerNumbers).forEach((partnerMaterialNumber) => map.set(partnerMaterialNumber, matchedOwnMaterialNumber));
        });
        return map;
    }, [mappingByOwnMaterialNumber]);

    return (
        <CalendarWeekProvider>
            <Stack spacing={2}>
                <ConfidentialBanner />
                <Stack direction="row" alignItems="center" spacing={1} width="100%">
                    <Link to={`/materials/outbound/${ownMaterialNumber}`} data-testid="back-button">
                        <Box padding="0.25rem" display="flex" alignItems="center">
                            <ChevronLeftOutlined />
                        </Box>
                    </Link>
                    <Typography variant="h3" component="h1">
                        Supply Chain Overview for {material.name} (<TextToClipboard text={ownMaterialNumber} />, Outbound)
                    </Typography>
                    <Chip label="Aggregated Supply Chain View" size="small" data-testid="aggregated-supply-chain-view-indicator" />
                </Stack>
                <SummaryContainer>
                    <OwnSummaryPanel
                        title="Supply Chain Overview"
                        summary={summary}
                        materialNumber={ownMaterialNumber}
                        showHeader
                    />
                    {aggregatedMaterialData.length === 0 ? (
                        <Typography padding="1rem" color="text.secondary">
                            No aggregated data available for this material.
                        </Typography>
                    ) : (
                        aggregatedMaterialData.map((data) => (
                            <Stack key={data.uuid}>
                                {data.childMaterialData.map((node) => (
                                    <AggregatedMaterialDataNodeSummary
                                        key={node.uuid}
                                        node={node}
                                        depth={1}
                                        materialBySupplierNumber={materialBySupplierNumber}
                                    />
                                ))}
                            </Stack>
                        ))
                    )}
                </SummaryContainer>
            </Stack>
        </CalendarWeekProvider>
    );
}
