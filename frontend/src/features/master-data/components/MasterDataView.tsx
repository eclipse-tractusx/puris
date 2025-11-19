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

import { Box, Button, Stack, Typography } from '@mui/material';
import { Table } from '@catena-x/portal-shared-components';
import { useTitle } from '@contexts/titleProvider';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useCallback, useEffect, useState } from 'react';
import { MaterialInformationModal } from './MaterialModal';
import { Material } from '@models/types/data/stock';
import { getAllMaterials, postMaterial } from '@services/materials-service';
import { Partner } from '@models/types/edc/partner';
import { getAllPartners, postPartner } from '@services/partners-service';
import { Address } from '@models/types/edc/address';
import { Site } from '@models/types/edc/site';
import { PartnerCreateModal } from './PartnerModal';
import { Add } from '@mui/icons-material';

const getDirectionLabel = (row: Material): string => {
    if (row.materialFlag && row.productFlag) return 'Bidirectional';
    if (row.materialFlag) return 'Outbound';
    if (row.productFlag) return 'Inbound';
    return 'Unknown';
};

const createParnerColumns = () => {
    return [
        { headerName: 'Partner Name', field: 'name', flex: 1 },
        { headerName: 'BPNL', field: 'bpnl', flex: 1.5 },
        {
            field: 'addresses',
            headerName: 'Addresses',
            flex: 2,
            sortable: false,
            renderCell: (data: { row: Partner }) => {
                const { addresses } = data.row;
                if (!addresses || addresses.length === 0) {
                    return (
                        <Box display="flex" alignItems="center" justifyContent="center" width="100%" height="100%">-</Box>
                    );
                }

                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                        sx={{ whiteSpace: 'normal', wordBreak: 'break-word', py: 0.5 }}
                    >
                        {addresses.map((address) => (
                            <Box key={address.bpna} sx={{ lineHeight: 1.2, mb: 0.75, '&:last-of-type': { mb: 0 } }} >
                                <Box fontWeight={500}> {address.streetAndNumber}</Box>
                                <Box>{address.zipCodeAndCity}</Box>
                                <Box fontSize="0.75rem" sx={{ opacity: 0.7 }}>{address.country}</Box>
                            </Box>
                        ))}
                    </Box>
                );
            },
        },
        {
            field: 'sites',
            headerName: 'Sites',
            flex: 2,
            sortable: false,
            renderCell: (data: { row: Partner }) => {
                const { sites } = data.row;
                if (!sites || sites.length === 0) {
                    return (
                        <Box display="flex" alignItems="center" justifyContent="center" width="100%" height="100%">-</Box>
                    );
                }

                return (
                    <Box
                        display="flex"
                        flexDirection="column"
                        justifyContent="center"
                        width="100%"
                        height="100%"
                        sx={{ whiteSpace: 'normal', wordBreak: 'break-word', py: 0.5 }}
                    >
                        {sites.map((site: Site) => (
                            <Box
                                key={site.bpns}
                                sx={{ mb: 0.75, '&:last-of-type': { mb: 0 }}}
                            >
                                <Box display="flex" alignItems="center" gap={0.5}>
                                    <Box fontWeight={600}>{site.name || site.bpns}</Box>
                                    {site.name && (
                                        <Box fontSize="0.75rem" sx={{ opacity: 0.7 }}>{site.bpns}</Box>
                                    )}
                                </Box>
                                {site.addresses && site.addresses.length > 0 && (
                                    <Box sx={{ mt: 0.25, pl: 1 }}>
                                        {site.addresses.map((address: Address) => (
                                            <Box key={address.bpna} sx={{ lineHeight: 1.2, mb: 0.5 }}>
                                                <Box>{address.streetAndNumber}</Box>
                                                <Box>{address.zipCodeAndCity}</Box>
                                                <Box fontSize="0.75rem" sx={{ opacity: 0.7 }}>{address.country}</Box>
                                            </Box>
                                        ))}
                                    </Box>
                                )}
                            </Box>
                        ))}
                    </Box>
                );
            },
        }
    ] as const;
};

export const MasterDataView = () => {
    const [materialModalOpen, setMaterialModalOpen] = useState<boolean>(false);
    const [partnerModalOpen, setPartnerModalOpen] = useState<boolean>(false);
    const [materials, setMaterials] = useState<Material[]>([]);
    const [partners, setPartners] = useState<Partner[]>([]);
    const { setTitle } = useTitle();

    const fetchMaterials = useCallback(async () => {
        try {
            setMaterials(await getAllMaterials());
        } catch (error) {
            console.error(error);
        }
    }, []);

    const fetchPartners = useCallback(async () => {
        try {
            setPartners(await getAllPartners());
        } catch (error) {
            console.error(error);
        }
    }, []);

    const handleSaveMaterial = async (material: Partial<Material>) => {
        await postMaterial(material);
        await fetchMaterials();
    };

    const handleSavePartner = async (partner: Partial<Partner>) => {
        await postPartner(partner);
        await fetchPartners();
    };

    useEffect(() => {
        setTitle('Materials');
        fetchMaterials();
        fetchPartners();
    }, [setTitle, fetchMaterials, fetchPartners]);

    return (
        <>
            <Stack spacing={3}>
                <ConfidentialBanner />
                <Stack width='100%' direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">Master data Materials</Typography>
                    {<Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setMaterialModalOpen(true);
                    }}>
                        <Add></Add> New Material
                    </Button> }
                </Stack>

                <Table
                    title="Materials"
                    columns={[
                        { headerName: 'Material Number', field: 'ownMaterialNumber', flex: 1 },
                        { headerName: 'Name', field: 'name', flex: 1.5 },
                        { headerName: 'Global Asset Id', field: 'materialNumberCx', flex: 1 },
                        { headerName: 'Direction', field: 'direction', flex: 1, valueGetter: (params) => getDirectionLabel(params.row) },
                    ]}
                    rows={materials ?? []}
                    getRowId={(row) => row.ownMaterialNumber}
                    noRowsMsg='No materials found'
                />

                <Stack width='100%' direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">Master data Partners</Typography>
                    {<Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setPartnerModalOpen(true);
                    }}>
                        New Partner
                    </Button>}
                </Stack>

                <Table
                    autoHeight
                    title="Partners"
                    columns={createParnerColumns()}
                    rows={partners ?? []}
                    getRowId={(row) => row.uuid}
                    noRowsMsg='No partners found'
                />
            </Stack>

            <MaterialInformationModal
                open={materialModalOpen}
                material={null}
                onClose={() => setMaterialModalOpen(false)}
                onSave={handleSaveMaterial}
            />

            <PartnerCreateModal
                open={partnerModalOpen}
                onClose={() => setPartnerModalOpen(false)}
                onSave={handleSavePartner}
            />
        </>
    );
};