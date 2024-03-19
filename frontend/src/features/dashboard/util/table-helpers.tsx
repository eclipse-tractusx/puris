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

import { Info } from '@mui/icons-material';
import { Box, Button } from '@mui/material';

export const createDateColumnHeaders = (numberOfDays: number) => {
    return Object.keys(Array.from({ length: numberOfDays })).map((_, index) => {
        const date = new Date();
        date.setDate(date.getDate() + index);
        return {
            field: `${index}`,
            headerName: date.toLocaleDateString('en-US', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }),
            width: 180,
            renderCell: (data: { value?: number } & { row: {id: number | string }}) => {
                return (
                    <Box display='flex' textAlign='center' alignItems='center' justifyContent='center' width='100%' height='100%' color={data.value !== undefined && data.value < 0 ? 'red' : undefined}>
                        {(data.row.id === 'delivery' || data.row.id === 'shipment') && data.value !== 0 ? <Button variant='text'>{data.value} <Info sx={{fontSize: '1.25rem'}}></Info></Button> : data.value} 
                    </Box>
                );
            },
            type: 'number',
        };
    });
};
