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

import { useMemo, useState } from 'react';
import { Box, Typography } from '@mui/material';
import { AggregatedMaterialDataNode } from '@models/types/data/aggregated-material-data';
import { getUnitOfMeasurement } from '@util/helpers';
import { TextToClipboard } from '@components/ui/TextToClipboard';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { createAnonymizedSummary } from '../util/anonymized-summary';
import { AnonymizedSummaryPanel } from './AnonymizedSummaryPanel';
import { SupplyChainRowHeader } from './SupplyChainRowHeader';

type AggregatedMaterialDataNodeSummaryProps = {
    node: AggregatedMaterialDataNode;
    depth: number;
    materialBySupplierNumber: Map<string, string>;
};

export function AggregatedMaterialDataNodeSummary({
    node,
    depth,
    materialBySupplierNumber,
}: AggregatedMaterialDataNodeSummaryProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const hasBlockedStock = node.stocks.some((stock) => stock.blocked);
    const hasAnonymizedData = node.productions.length > 0 || node.deliveries.length > 0 || node.stocks.length > 0;
    const anonymizedSummary = useMemo(
        () => createAnonymizedSummary(node.productions, node.deliveries, node.stocks),
        [node.productions, node.deliveries, node.stocks]
    );

    // Only tier-1 nodes are supplied by a partner we directly know - deeper tiers are anonymized partners-of-partners.
    const matchedOwnMaterialNumber =
        depth === 1 && node.externalMaterialNumber ? materialBySupplierNumber.get(node.externalMaterialNumber) ?? null : null;
    const { partners } = usePartners('material', matchedOwnMaterialNumber);

    return (
        <Box data-testid="aggregated-material-data-node">
            <SupplyChainRowHeader
                depth={depth}
                isExpanded={isExpanded}
                onToggle={() => setIsExpanded((expanded) => !expanded)}
                toggleTestId="aggregated-material-data-node-toggle"
                beforeTitle={
                    partners &&
                    partners.length > 0 && (
                        <Typography variant="body1" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                    {partners[0].name} (<TextToClipboard text={partners[0].bpnl} variant="light" />)
                                </Box>
                            {' /'}
                        </Typography>
                    )
                }
                title={node.externalMaterialName ?? node.externalMaterialNumber ?? 'Unknown component'}
                numberNode={node.externalMaterialNumber && <TextToClipboard text={node.externalMaterialNumber} variant="light" />}
                quantity={node.quantity}
                unit={getUnitOfMeasurement(node.measurementUnit)}
                afterQuantity={
                    <Typography variant="body3" sx={{ opacity: 0.7 }}>
                        Blocked: {hasBlockedStock ? 'Yes' : 'No'}
                    </Typography>
                }
            />
            {isExpanded &&
                (hasAnonymizedData ? (
                    <AnonymizedSummaryPanel summary={anonymizedSummary} />
                ) : (
                    <Typography variant="body3" color="text.secondary" padding="0.5rem 1rem">
                        No anonymized data reported for this component.
                    </Typography>
                ))}
            {isExpanded &&
                node.childMaterialData.map((child) => (
                    <AggregatedMaterialDataNodeSummary
                        key={child.uuid}
                        node={child}
                        depth={depth + 1}
                        materialBySupplierNumber={materialBySupplierNumber}
                    />
                ))}
        </Box>
    );
}
