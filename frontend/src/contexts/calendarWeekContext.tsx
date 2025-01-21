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

import { createContext, ReactNode, useContext, useState } from 'react';
import { Expandable } from '@features/material-details/models/expandable';
import { CalendarWeek, getCalendarWeek, incrementCalendarWeek } from '@util/date-helpers';

function initializeCalendarWeeks(): Expandable<CalendarWeek>[] {
    const today = new Date();
    const numberOfWeeks = today.getUTCDay() === 1 ? 4 : 5;
    const weeks: Expandable<CalendarWeek>[] = [];
    const currentCalendarWeek = getCalendarWeek(today);
    for (let i = 0; i < numberOfWeeks; i++) {
        weeks.push({
            ...incrementCalendarWeek(currentCalendarWeek, i),
            isExpanded: i === 0,
        });
    }
    return weeks;
}

type CalendarWeekContext = {
    calendarWeeks: Expandable<CalendarWeek>[];
    expandWeek: (state: boolean, index: number) => void;
};

const calendarWeekContext = createContext<CalendarWeekContext | null>(null);

type CalendarWeekProviderProps = {
    children: ReactNode;
};

export const CalendarWeekProvider = ({ children }: CalendarWeekProviderProps) => {
    const [calendarWeeks, setCalendarWeeks] = useState<Expandable<CalendarWeek>[]>(() => initializeCalendarWeeks());
    const expandWeek = (state: boolean, index: number) => {
        setCalendarWeeks((prev) => prev.map((cw, i) => (i === index ? { ...cw, isExpanded: state } : cw)));
    };
    return (
        <>
            <calendarWeekContext.Provider value={{ calendarWeeks, expandWeek }}>{children}</calendarWeekContext.Provider>
        </>
    );
};

export function useCalendarWeeks() {
    const context = useContext(calendarWeekContext);
    if (context === null) {
        throw new Error('useCalendarWeeks must be used within a CalendarWeekProvider');
    }
    return context;
}
