/*
Copyright (c) 2026 Volkswagen AG

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

import { Box, IconButton, Stack, Typography } from '@mui/material';
import { ChevronRightOutlined, ExpandMoreOutlined } from '@mui/icons-material';
import { InfoButton } from '@components/ui/InfoButton';
import { TextToClipboard } from '@components/ui/TextToClipboard';
import { AggregatedMaterialDataNode } from '@models/types/data/aggregated-material-data';
import { Partner } from '@models/types/edc/partner';
import { getUnitOfMeasurement } from '@util/helpers';

type SupplyChainRowHeaderProps = {
    node: AggregatedMaterialDataNode;
    depth: number;
    isExpanded: boolean;
    onToggle: () => void;
    partner?: Partner;
};

export function SupplyChainRowHeader({ node, depth, isExpanded, onToggle, partner }: SupplyChainRowHeaderProps) {
    return (
        <Stack
            direction="row"
            alignItems="center"
            gap={1}
            sx={{
                backgroundColor: 'primary.light',
                color: 'primary.contrastText',
                padding: '0.375rem 0.5rem',
            }}
        >
            <IconButton size="small" onClick={onToggle} sx={{ color: 'inherit' }} data-testid="aggregated-material-data-node-toggle">
                {isExpanded ? <ExpandMoreOutlined fontSize="small" /> : <ChevronRightOutlined fontSize="small" />}
            </IconButton>
            {depth > 1 && (
                <Typography variant="body3" sx={{ opacity: 0.7 }} aria-hidden>
                    {'└─'.repeat(depth - 1)}
                </Typography>
            )}
            {partner && (
                <Typography variant="body1" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        {partner.name} (<TextToClipboard text={partner.bpnl} variant="light" />)
                    </Box>
                    {' /'}
                </Typography>
            )}
            <Typography variant="body1">
                {node.externalMaterialName ??
                    node.externalMaterialNumber ?? (
                        <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                            Unknown component
                            <InfoButton text="There was an error when fetching the material data for this node." />
                        </Box>
                    )}
            </Typography>
            {node.externalMaterialNumber && (
                <Typography variant="body3" sx={{ opacity: 0.7 }}>
                    (<TextToClipboard text={node.externalMaterialNumber} variant="light" />)
                </Typography>
            )}
            <Typography variant="body2">
                {node.quantity} {getUnitOfMeasurement(node.measurementUnit)}
            </Typography>
        </Stack>
    );
}
