/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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
import { DateType, Datepicker } from '@catena-x/portal-shared-components';
import Box from '@mui/material/Box';
import { useEffect, useRef, useState } from 'react';

const isValidTime = (time?: string) => {
    if (!time) {
        return false;
    }
    const splits = time.split(':');
    if (splits.length !== 2) {
        return false;
    }
    const [hours, minutes] = splits;
    return parseInt(hours) >= 0 && parseInt(hours) <= 23 && parseInt(minutes) >= 0 && parseInt(minutes) <= 59;
};

type DateTimeProps = {
    label: string;
    placeholder: string;
    locale: 'en' | 'de';
    error: boolean;
    value: Date | null;
    onValueChange: (date: Date | null) => void;
};

export const DateTime = ({ error, value, onValueChange, ...props }: DateTimeProps) => {
    const [date, setDate] = useState<Date | null>(value ? new Date(value) : null);
    const timeRef = useRef<HTMLInputElement>(null);
    const handleTimeChange = () => {
        const time = timeRef.current?.value;
        if (time && date) {
            const [hours, minutes] = time.split(':');
            const newDate = new Date(date);
            newDate.setHours(parseInt(hours));
            newDate.setMinutes(parseInt(minutes));
            onValueChange(newDate);
        } else {
            onValueChange(null);
        }
    };
    const handleDateChange = (newDate: DateType) => {
        setDate(newDate);
        if (newDate && timeRef.current) {
            onValueChange(new Date(newDate));
        } else {
            onValueChange(null);
        }
    };

    useEffect(() => {
        if (value) {
            const d = new Date(value);
            setDate(d);
            const hours = d.getHours().toString().padStart(2, '0');
            const minutes = d.getMinutes().toString().padStart(2, '0');
            timeRef.current!.value = `${hours}:${minutes}`;
        }
    }, [value]);
    return (
        <Box display="flex" gap=".25rem" width="100%" marginTop="auto">
            <Box display="flex" flexGrow="1" sx={{ '& .MuiFormControl-root, & .MuiBox-root': { minWidth: '100% !important' } }}>
                <Datepicker
                    {...props}
                    error={error}
                    value={date?.toISOString().split('T')[0]}
                    readOnly={false}
                    onChangeItem={(event) => handleDateChange(event)}
                />
            </Box>
            <input
                ref={timeRef}
                className={`${error && !isValidTime(timeRef.current?.value) ? 'error' : ''}`}
                type="time"
                id={"time"+props.label.toLowerCase()}
                name={"etoc"+props.label.toLowerCase()}
                onChange={handleTimeChange}
            />
        </Box>
    );
};
