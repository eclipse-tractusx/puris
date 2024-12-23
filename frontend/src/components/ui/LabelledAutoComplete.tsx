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

import { Input } from '@catena-x/portal-shared-components';
import { Autocomplete, InputLabel, Stack } from '@mui/material';
import { SyntheticEvent } from 'react';

type LabelledAutoCompleteProps<TValue> = {
    label: string;
    options: readonly TValue[];
    value: TValue | null;
    onChange: (event: SyntheticEvent<Element, Event>, value: TValue | null) => void;
    getOptionLabel?: (option: TValue) => string;
    isOptionEqualToValue?: (option: TValue, value: TValue) => boolean;
    placeholder: string;
    error?: boolean;
    [key: string]: unknown;
};

export const LabelledAutoComplete = <TValue,>({ label, placeholder, error, ...rest }: LabelledAutoCompleteProps<TValue>) => {
    return (
        <Stack spacing={1}>
            <InputLabel>{label}</InputLabel>
            <Autocomplete
                sx={{ width: '100%' }}
                {...rest}
                renderInput={(params) => <Input hiddenLabel {...params} placeholder={placeholder} error={error} />}
            />
        </Stack>
    );
};
