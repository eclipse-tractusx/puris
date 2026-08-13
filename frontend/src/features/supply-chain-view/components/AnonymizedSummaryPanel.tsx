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
import { Box, Grid, Stack, Typography } from '@mui/material';
import { InfoButton } from '@components/ui/InfoButton';
import { useCalendarWeeks } from '@contexts/calendarWeekContext';
import { Expandable } from '@features/material-details/models/expandable';
import { CalendarWeek, incrementDate } from '@util/date-helpers';
import { AnonymizedSummary } from '../util/anonymized-summary';

type AnonymizedSummaryPanelProps = {
    summary: AnonymizedSummary;
};

export function AnonymizedSummaryPanel({ summary }: AnonymizedSummaryPanelProps) {
    const { calendarWeeks } = useCalendarWeeks();
    return (
        <Stack direction="row" data-testid="anonymized-summary-panel">
            <Stack
                flex={1}
                minWidth="12rem"
                sx={{ position: 'sticky', left: 0, backgroundColor: 'white', zIndex: 100, borderRight: '1px solid #e5e5e5' }}
            >
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Planned Production
                    <InfoButton text="The anonymized planned production quantity reported for this component on the given date." />
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Outgoing Shipments
                    <InfoButton text="The anonymized quantity of outgoing shipments departing this component's reporting site on the given date." />
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Item Stock
                    <InfoButton text="The anonymized projected item stock for this component, reflecting the stock at the end of the calendar week." />
                </Stack>
            </Stack>
            <Stack direction="row" width="100%">
                {calendarWeeks.map((cw) => (
                    <AnonymizedCalendarWeekSummary key={cw.week} cw={cw} summary={summary} isExpanded={cw.isExpanded} />
                ))}
            </Stack>
        </Stack>
    );
}

type AnonymizedCalendarWeekSummaryProps = {
    cw: Expandable<CalendarWeek>;
    summary: AnonymizedSummary;
    isExpanded: boolean;
};

function AnonymizedCalendarWeekSummary({ cw, summary, isExpanded }: AnonymizedCalendarWeekSummaryProps) {
    const weekDates = useMemo(() => Array.from(new Array(7).keys()).map((day) => incrementDate(cw.startDate, day)), [cw.startDate]);
    const productionTotal = weekDates.reduce((sum, date) => sum + (summary.dailySummaries[date.toLocaleDateString()]?.productionTotal ?? 0), 0);
    const deliveryTotal = weekDates.reduce((sum, date) => sum + (summary.dailySummaries[date.toLocaleDateString()]?.deliveryTotal ?? 0), 0);
    const stockTotal = summary.dailySummaries[incrementDate(weekDates[6], 1).toLocaleDateString()]?.stockTotal ?? 0;

    return (
        <Stack
            flex={isExpanded ? 50 : 10}
            sx={{ borderRight: '1px solid #e5e5e5', minWidth: isExpanded ? '34rem' : '9rem', backgroundColor: isExpanded ? 'white' : '#f5f5f5' }}
            data-testid={`anonymized-cw-summary-${cw.week}`}
        >
            <Grid container columns={8} flex={1} height="100%">
                {isExpanded &&
                    weekDates.map((date) => {
                        const dailyProduction = summary.dailySummaries[date.toLocaleDateString()]?.productionTotal ?? 0;
                        const dailyDelivery = summary.dailySummaries[date.toLocaleDateString()]?.deliveryTotal ?? 0;
                        const dailyStock = summary.dailySummaries[incrementDate(date, 1).toLocaleDateString()]?.stockTotal ?? 0;
                        return (
                            <Grid key={date.toLocaleDateString()} item xs={1} height="100%">
                                <Stack height="100%" data-testid={`anonymized-cw-${cw.week}-day-${date.getDay()}`}>
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                                        <Typography variant="body2">{dailyProduction}</Typography>
                                    </Box>
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                                        <Typography variant="body2">{dailyDelivery}</Typography>
                                    </Box>
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                                        <Typography variant="body2" color={dailyStock < 0 ? '#f44336bb' : 'inherit'}>
                                            {dailyStock}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </Grid>
                        );
                    })}
                <Grid item xs={isExpanded ? 1 : 8} height="100%">
                    <Stack height="100%" sx={{ borderLeft: isExpanded ? '1px solid #e5e5e5' : 'none' }}>
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                            <Typography variant="body2">{productionTotal}</Typography>
                        </Box>
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                            <Typography variant="body2">{deliveryTotal}</Typography>
                        </Box>
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center" padding=".75rem 0">
                            <Typography variant="body2" color={stockTotal < 0 ? '#f44336bb' : 'inherit'}>
                                {stockTotal}
                            </Typography>
                        </Box>
                    </Stack>
                </Grid>
            </Grid>
        </Stack>
    );
}
