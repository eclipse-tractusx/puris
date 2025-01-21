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

import { ReactNode, useState } from 'react';
import { Button, Stack, useTheme } from '@mui/material';
import { SummaryPanel } from './SummaryPanel';
import { ChevronRightOutlined, SubdirectoryArrowRightOutlined } from '@mui/icons-material';
import { Summary, SummaryType } from '../util/summary-service';

type CollapsibleSummaryProps<TType extends SummaryType> = {
    variant?: 'default' | 'sub';
    summary: Summary<TType>;
    renderTitle: () => ReactNode;
    children?: ReactNode;
};

export function CollapsibleSummary<TType extends SummaryType>({
    summary,
    renderTitle,
    children,
    variant = 'default',
}: CollapsibleSummaryProps<TType>) {
    const theme = useTheme();
    const [isExpanded, setIsExpanded] = useState(false);
    return (
        <>
            <Button
                variant="text"
                sx={{
                    flexGrow: 1,
                    padding: 0,
                    borderRadius: 0,
                    textTransform: 'none',
                    mindWidth: '100%',
                    position: 'sticky',
                    left: 0,
                    display: 'flex',
                }}
                onClick={() => setIsExpanded((prev) => !prev)}
            >
                <Stack
                    direction="row"
                    alignItems="center"
                    spacing={0.5}
                    sx={{
                        minHeight: '2rem',
                        width: '100%',
                        paddingLeft: '.5rem',
                        verticalAlign: 'middle',
                        backgroundColor: variant === 'default' ? theme.palette.primary.main : theme.palette.primary.light,
                        color: theme.palette.primary.contrastText,
                    }}
                >
                    {<ChevronRightOutlined sx={{ rotate: isExpanded ? '90deg' : '0deg', transition: 'rotate 300ms ease-in-out' }} />}
                    {variant === 'sub' && <SubdirectoryArrowRightOutlined />}
                    {renderTitle()}
                </Stack>
            </Button>
            <SummaryPanel sx={{ display: isExpanded ? 'flex' : 'none' }} summary={summary} />
            {isExpanded ? children : null}
        </>
    );
}
