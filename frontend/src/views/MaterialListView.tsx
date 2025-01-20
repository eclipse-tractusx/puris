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

import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { SearchInput } from '@components/ui/SearchInput';
import { MaterialList } from '@features/material-list/components/MaterialList';
import { useMaterials } from '@features/stock-view/hooks/useMaterials';

import { MenuItem, Select, Stack } from '@mui/material';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function MaterialListView() {
    const navigate = useNavigate();
    const [searchTerm, setSearchTerm] = useState('');
    const [direction, setDirection] = useState('');
    const { materials } = useMaterials('material', true);
    const { materials: products } = useMaterials('product', true);
    const materialsAndProducts = useMemo(
        () =>
            materials
                ?.concat(products ?? [])
                .filter(
                    (material) =>
                        (material.direction.includes(direction) && (material.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        material.ownMaterialNumber.toLowerCase().includes(searchTerm.toLowerCase())))
                ) ?? [],
        [materials, products, direction, searchTerm]
    );
    return (
        <Stack spacing={3}>
            <ConfidentialBanner />
            <Stack
                direction="row"
                spacing={2}
                justifyContent="space-between"
                sx={{ padding: '.5rem 1rem', backgroundColor: 'white', borderRadius: '1rem', boxShadow: '0 1px 4px rgba(0, 0, 0, 0.1)' }}
            >
                <SearchInput placeholder="Search for materials" onSearch={(query) => setSearchTerm(query)} />
                <Stack direction="row" spacing={2} alignItems="center">
                    <label htmlFor="direction-selector">Direction: </label>
                    <Select
                        id="direction-selector"
                        variant="filled"
                        defaultValue="all"
                        onChange={(event) => setDirection(event.target.value === 'all' ? '' : event.target.value)}
                        sx={{ minWidth: '12rem' }}
                    >
                        <MenuItem value="all">all</MenuItem>
                        <MenuItem value="inbound">inbound</MenuItem>
                        <MenuItem value="outbound">outbound</MenuItem>
                    </Select>
                </Stack>
            </Stack>
            <MaterialList
                materials={materialsAndProducts}
                onRowClick={(material) => navigate(`/materials/${material.direction}/${material.ownMaterialNumber}`)}
            />
        </Stack>
    );
}
