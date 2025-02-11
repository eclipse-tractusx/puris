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

import { Input } from '@catena-x/portal-shared-components';
import { useDebounce } from '@hooks/useDebounce';
import { Search } from '@mui/icons-material';
import { Box } from '@mui/material';
import { useEffect } from 'react';


type SearchInputProps = {
    placeholder: string;
    onSearch: (searchTerm: string) => void;
};

export function SearchInput({ placeholder, onSearch }: SearchInputProps) {
    const { debouncedValue, debounce } = useDebounce<string>('');
    useEffect(() => {
        onSearch(debouncedValue);
    }, [debouncedValue, onSearch]);
    return (
        <Box position='relative'>
            <Search sx={{ position: 'absolute', top: '50%', left: '1rem', transform: 'translateY(-50%)', fill: 'rgba(0, 0, 0, 0.87)' }} />
            <Input
                className="search-input"
                placeholder={placeholder}
                sx={{ width: '32rem' }}
                onChange={(event) => debounce(event.target.value)}
            />
        </Box>
    );
}
