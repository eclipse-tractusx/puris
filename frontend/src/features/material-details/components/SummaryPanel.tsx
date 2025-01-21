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

import { Stack, SxProps, Theme, Typography, useTheme } from '@mui/material';
import { CalendarWeekSummary } from '../../material-details/components/CalendarWeekSummary';
import { Summary, SummaryType } from '../util/summary-service';
import { useCalendarWeeks } from '@contexts/calendarWeekContext';
import { InfoButton } from '@components/ui/InfoButton';

type SummaryPanelProps<TType extends SummaryType> = {
    sx?: SxProps<Theme>
    title?: string;
    summary: Summary<TType>;
    showHeader?: boolean;
};

export function SummaryPanel<TType extends SummaryType>({ sx = {}, title, summary, showHeader = false }: SummaryPanelProps<TType>) {
    const theme = useTheme();
    const { calendarWeeks, expandWeek } = useCalendarWeeks();
    return (
        <Stack direction="row" sx={sx}>
            <Stack
                flex={1}
                minWidth="12rem"
                sx={{ position: 'sticky', left: 0, backgroundColor: 'white', zIndex: 100, borderRight: '1px solid #e5e5e5'}}
            >
                {showHeader && (
                    <>
                        <Stack
                            justifyContent="center"
                            paddingInlineStart=".5rem"
                            sx={{
                                backgroundColor: theme.palette.primary.dark,
                                color: theme.palette.primary.contrastText,
                                height: '2.25rem',
                                zIndex: 10,
                            }}
                        >
                            <Typography variant={title && title.length < 30 ? 'body1' : 'body2'} component="h3" textAlign={title && title.length < 30 ? 'start' : 'start'}>
                                {title}
                            </Typography>
                        </Stack>
                        <Stack sx={{ backgroundColor: '#f5f5f5', height: '2.5rem' }}></Stack>
                    </>
                )}
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    {summary.type === 'production' ? 'Planned Production' : 'Material Demand'}
                    <InfoButton text={summary.type === 'production' ? 'The planned production output for the material on the given date' : 'The estimated demand for the material on the given date.'}></InfoButton>
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    {summary.type === 'production' ? 'Outgoing Shipments' : 'Incoming Deliveries'}
                    <InfoButton text={`The total quantity of ${summary.type === 'production' ? 'outgoing shipments departing' : 'incoming deliveries arriving'} on the given date.`}></InfoButton>
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Projected Item Stock
                    <InfoButton text="The projected item stock for the material on a given date. The summary for projected item stock reflects the item stock at the end of the calendar week."></InfoButton>
                </Stack>
            </Stack>
            <Stack direction="row" width="100%">
                {calendarWeeks.map((cw, index) => (
                    <CalendarWeekSummary
                        key={cw.week}
                        summary={summary}
                        isExpanded={cw.isExpanded}
                        cw={cw}
                        onToggleExpanded={(state) => expandWeek(state, index)}
                        showHeader={showHeader}
                    ></CalendarWeekSummary>
                ))}
            </Stack>
        </Stack>
    );
}
