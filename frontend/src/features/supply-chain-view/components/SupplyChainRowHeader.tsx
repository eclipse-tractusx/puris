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

import { ReactNode } from 'react';
import { IconButton, Stack, Typography } from '@mui/material';
import { ChevronRightOutlined, ExpandMoreOutlined } from '@mui/icons-material';
import { getSupplyChainTierLabel } from '@util/supply-chain-relations';

type SupplyChainRowHeaderProps = {
    depth: number;
    isExpanded: boolean;
    onToggle: () => void;
    toggleTestId: string;
    beforeTitle?: ReactNode;
    title: ReactNode;
    numberNode?: ReactNode;
    quantity: number;
    unit: string | undefined;
    afterQuantity?: ReactNode;
};

export function SupplyChainRowHeader({
    depth,
    isExpanded,
    onToggle,
    toggleTestId,
    beforeTitle,
    title,
    numberNode,
    quantity,
    unit,
    afterQuantity,
}: SupplyChainRowHeaderProps) {
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
            <IconButton size="small" onClick={onToggle} sx={{ color: 'inherit' }} data-testid={toggleTestId}>
                {isExpanded ? <ExpandMoreOutlined fontSize="small" /> : <ChevronRightOutlined fontSize="small" />}
            </IconButton>
            <Typography variant="body3" sx={{ opacity: 0.7 }}>
                {getSupplyChainTierLabel(depth)}
            </Typography>
            {beforeTitle}
            <Typography variant="body1">{title}</Typography>
            {numberNode && (
                <Typography variant="body3" sx={{ opacity: 0.7 }}>
                    ({numberNode})
                </Typography>
            )}
            <Typography variant="body2">
                {quantity} {unit}
            </Typography>
            {afterQuantity}
        </Stack>
    );
}
