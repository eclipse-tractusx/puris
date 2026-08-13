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

import { useState } from 'react';
import { Box, Typography } from '@mui/material';
import { SupplyChainTreeNodeData } from '@util/supply-chain-relations';
import { Material } from '@models/types/data/stock';
import { getUnitOfMeasurement } from '@util/helpers';
import { TextToClipboard } from '@components/ui/TextToClipboard';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { SupplyChainRowHeader } from './SupplyChainRowHeader';
import { SupplyChainNodeExpandedSummary } from './SupplyChainNodeExpandedSummary';

type SupplyChainTreeNodeProps = {
    node: SupplyChainTreeNodeData;
    materialsByNumber: Map<string, Material>;
};

export function SupplyChainTreeNode({ node, materialsByNumber }: SupplyChainTreeNodeProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const material = materialsByNumber.get(node.ownMaterialNumber);
    const { partners, isLoadingPartners } = usePartners('material', node.ownMaterialNumber);

    return (
        <Box data-testid="supply-chain-tree-node">
            <Box id={`supply-chain-node-${node.ownMaterialNumber}`}>
                <SupplyChainRowHeader
                    depth={node.depth}
                    isExpanded={isExpanded}
                    onToggle={() => setIsExpanded((expanded) => !expanded)}
                    toggleTestId="supply-chain-tree-node-toggle"
                    beforeTitle={
                        partners &&
                        partners.length > 0 && (
                            <Typography variant="body1" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                {partners.map((partner, index) => (
                                    <Box component="span" key={partner.bpnl} sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                        {index > 0 && ','}
                                        {partner.name} (<TextToClipboard text={partner.bpnl} variant="light" />)
                                    </Box>
                                ))}
                                {' /'}
                            </Typography>
                        )
                    }
                    title={material?.name ?? node.ownMaterialNumber}
                    numberNode={<TextToClipboard text={node.ownMaterialNumber} variant="light" />}
                    quantity={node.quantity}
                    unit={getUnitOfMeasurement(node.measurementUnit)}
                />
            </Box>
            {isExpanded && (
                <SupplyChainNodeExpandedSummary
                    ownMaterialNumber={node.ownMaterialNumber}
                    depth={node.depth}
                    partners={partners}
                    isLoadingPartners={isLoadingPartners}
                />
            )}
            {isExpanded &&
                node.children.map((child) => (
                    <SupplyChainTreeNode key={`${child.depth}-${child.ownMaterialNumber}`} node={child} materialsByNumber={materialsByNumber} />
                ))}
        </Box>
    );
}
