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

import { Stack } from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function MaterialListView() {
    const navigate = useNavigate();
    const [searchTerm, setSearchTerm] = useState('');
    const { materials } = useMaterials('material');
    const { materials: products } = useMaterials('product');
    const materialsAndProducts = materials?.concat(products ?? []).filter((material) => 
        material.description.toLowerCase().includes(searchTerm.toLowerCase()) || material.ownMaterialNumber.toLowerCase().includes(searchTerm.toLowerCase())) ?? [];
    return (
        <Stack spacing={3}>
            <ConfidentialBanner />
            <Stack
                direction="row"
                spacing={2}
                sx={{ padding: '.5rem 1rem', backgroundColor: 'white', borderRadius: '1rem', boxShadow: '0 1px 4px rgba(0, 0, 0, 0.1)' }}
            >
                <SearchInput placeholder="Search for materials" onSearch={(query) => setSearchTerm(query)} />
            </Stack>
            <MaterialList materials={materialsAndProducts} onRowClick={(material) => navigate(`/materials/${material.direction}/${material.ownMaterialNumber}`)} />
        </Stack>
    );
}
