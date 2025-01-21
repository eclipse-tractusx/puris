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

import { useParams } from 'react-router-dom';
import { NotFoundView } from './errors/NotFoundView';
import { DirectionType } from '@models/types/erp/directionType';
import { DataModalProvider } from '@contexts/dataModalContext';
import { MaterialDetails } from '@features/material-details/components/MaterialDetails';
import { useMaterial } from '@hooks/useMaterial';
import { Box } from '@mui/material';
import { useTitle } from '@contexts/titleProvider';
import { useEffect } from 'react';

export function MaterialDetailView() {
    const { materialNumber, direction } = useParams();
    const directionType: DirectionType = direction === 'inbound' ? DirectionType.Inbound : DirectionType.Outbound;
    const { material, isLoading } = useMaterial(materialNumber ?? '');
    const { setTitle } = useTitle();

    useEffect(() => {
        if (!isLoading && material) {
            setTitle(`${material.name} (${direction})`);
        }
    }, [direction, isLoading, material, setTitle])

    if (isLoading) {
        return <Box>Loading...</Box>
    }

    if (!['OUTBOUND', 'INBOUND'].includes(direction?.toUpperCase() ?? '') || !materialNumber || !material) {
        return <NotFoundView />;
    }
    return (
        <DataModalProvider material={material}>
            <MaterialDetails material={material} direction={directionType} />
        </DataModalProvider>
    );
}
