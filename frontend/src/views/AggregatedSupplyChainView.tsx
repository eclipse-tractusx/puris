/*
Copyright (c) 2026 Volkswagen AG

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

import { useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { Box } from '@mui/material';
import { NotFoundView } from './errors/NotFoundView';
import { DataModalProvider } from '@contexts/dataModalContext';
import { useMaterial } from '@hooks/useMaterial';
import { useTitle } from '@contexts/titleProvider';
import { AggregatedSupplyChainViewContent } from '@features/supply-chain-view/components/AggregatedSupplyChainViewContent';

export function AggregatedSupplyChainView() {
    const { materialNumber } = useParams();
    const { material, isLoading } = useMaterial(materialNumber ?? '');
    const { setTitle } = useTitle();

    useEffect(() => {
        if (isLoading) {
            setTitle('Loading...');
            return;
        }
        setTitle(`${material?.name} (Supply chain overview)`);
    }, [isLoading, material?.name, setTitle]);

    if (isLoading) return <Box>Loading...</Box>;
    if (!materialNumber || !material || !material.productFlag) return <NotFoundView />;

    return (
        <DataModalProvider material={material}>
            <AggregatedSupplyChainViewContent material={material} />
        </DataModalProvider>
    );
}
