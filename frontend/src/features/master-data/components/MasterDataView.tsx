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

import { Button, Stack, Typography } from '@mui/material';
import { Table } from '@catena-x/portal-shared-components';
import { useTitle } from '@contexts/titleProvider';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useCallback, useEffect, useState } from 'react';
import { MaterialInformationModal } from './MaterialModal';
import { Material } from '@models/types/data/stock';
import { getAllMaterials, postMaterial } from '@services/materials-service';

export const MasterDataView = () => {
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [materialz, setMaterialz] = useState<Material[]>([]);
    const { setTitle } = useTitle();

    const fetchMaterials = useCallback(async () => {
            try {
                setMaterialz(await getAllMaterials());
            } catch (error) {
                console.error(error);
            }
        }, []);

    const handleSaveMaterial = async (material: Partial<Material>) => {
        await postMaterial(material);
        await fetchMaterials();
    };

    useEffect(() => {
        setTitle('Materials');
        fetchMaterials();
    }, [setTitle, fetchMaterials]);

    const getDirectionLabel = (row: Material): string => {
        if (row.materialFlag && row.productFlag) return 'Bidirectional';
        if (row.materialFlag) return 'Outbound';
        if (row.productFlag) return 'Inbound';
        return 'Unknown';
    };

    return (
        <>
            <Stack spacing={3}>
                <ConfidentialBanner />
                <Stack width='100%' direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">Master data</Typography>
                    {<Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setModalOpen(true);
                    }}>
                        New Material
                    </Button> }
                </Stack>

                <Table
                    title="Materials"
                    columns={[
                        { headerName: 'Material Number', field: 'ownMaterialNumber', flex: 1 },
                        { headerName: 'Name', field: 'name', flex: 1.5 },
                        { headerName: 'Global Asset Id', field: 'materialNumberCx', flex: 1},
                        { headerName: 'Direction', field: 'direction', flex: 1, valueGetter: (params) => getDirectionLabel(params.row) },
                    ]}
                    rows={materialz ?? []}
                    getRowId={(row) => row.ownMaterialNumber}
                    noRowsMsg='No materials found'
                />
            </Stack>

            <MaterialInformationModal
                open={modalOpen}
                material={null}
                onClose={() => setModalOpen(false)}
                onSave={handleSaveMaterial}
            />
        </>
    );
};