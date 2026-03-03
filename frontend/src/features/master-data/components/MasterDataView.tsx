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
import { Site } from '@models/types/edc/site';
import { PartnerCreationModal } from './PartnerModal';
import { Add } from '@mui/icons-material';
import { MaterialPartnerRelation } from '@models/types/data/material-partner-relation';
import { getAllMaterialPartnerRelations, postMaterialPartnerRelation } from '@services/material-partner-relation-service';
import { MaterialPartnerRelationModal } from './MaterialpartnerRelationModal';
import { BPNS } from '@models/types/edc/bpn';

const getDirectionLabel = (row: Material): string => {
    if (row.materialFlag && row.productFlag) return 'Bidirectional';
    if (row.materialFlag) return 'Inbound';
    if (row.productFlag) return 'Outbound';
    return 'Unknown';
};

const renderSitesBpnsCell = (sitesBpns?: BPNS[]) => {
    if (!sitesBpns || sitesBpns.length === 0) {
        return ( <Box display="flex" alignItems="center" justifyContent="center" width="100%" height="100%">-</Box> );
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
        {sitesBpns.map((siteBpn) => (
            <Box key={siteBpn} sx={{ lineHeight: 1.2, mb: 0.5, '&:last-of-type': { mb: 0 } }}>
                {siteBpn}
            </Box>
        ))}
        </Box>
    );
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
                            <Box key={site.bpns} sx={{ lineHeight: 1.2, mb: 0.5, '&:last-of-type': { mb: 0 } }}>
                                {site.name}
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
    const [mprModalOpen, setMprModalOpen] = useState<boolean>(false);
    const [materials, setMaterials] = useState<Material[]>([]);
    const [partners, setPartners] = useState<Partner[]>([]);
    const [mprs, setMprs] = useState<MaterialPartnerRelation[]>([]);
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

    const fetchMprs = useCallback(async () => {
        try {
            setMprs(await getAllMaterialPartnerRelations());
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

    const handleSaveMpr = async (mpr: Partial<MaterialPartnerRelation>) => {
        await postMaterialPartnerRelation(mpr);
        await fetchMprs();
    };

    useEffect(() => {
        setTitle('Materials');
        fetchMaterials();
        fetchPartners();
        fetchMprs();
    }, [setTitle, fetchMaterials, fetchPartners, fetchMprs]);

    return (
        <>
            <Stack spacing={2}>
                <ConfidentialBanner />
                <Typography variant="h3" component="h1">Master data</Typography>
                <Stack width='100%' direction="row" justifyContent="end" alignItems="center">
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

                <Stack width='100%' direction="row" justifyContent="end" alignItems="center">
                    {<Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setPartnerModalOpen(true);
                    }}>
                        <Add></Add> New Partner
                    </Button>}
                </Stack>

                <Table
                    title="Partners"
                    columns={createParnerColumns()}
                    rows={partners ?? []}
                    getRowId={(row) => row.uuid}
                    noRowsMsg='No partners found'
                />

                <Stack width='100%' direction="row" justifyContent="end" alignItems="center">
                    {<Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setMprModalOpen(true);
                    }}>
                        <Add></Add> New Relation
                    </Button>}
                </Stack>

                <Table
                    title="Material Partner Relations"
                    columns={[
                        { headerName: 'Material Number', field: 'ownMaterialNumber', flex: 1 },
                        { headerName: 'Partner BPNL', field: 'partnerBpnl', flex: 1 },
                        { headerName: 'Partner Material Number', field: 'partnerMaterialNumber', flex: 1 },
                        {
                            headerName: 'Supplies Material',
                            field: 'partnerSuppliesMaterial',
                            flex: 1,
                            valueGetter: (params) => (params.row.partnerSuppliesMaterial ? 'Yes' : 'No'),
                        },
                        {
                            headerName: 'Buys Material',
                            field: 'partnerBuysMaterial',
                            flex: 1,
                            valueGetter: (params) => (params.row.partnerBuysMaterial ? 'Yes' : 'No'),
                        },
                        {
                            field: 'ownDemandingSiteBpnss',
                            headerName: 'Demanding Sites',
                            flex: 2,
                            sortable: false,
                            renderCell: (data: { row: MaterialPartnerRelation }) => renderSitesBpnsCell(data.row.ownDemandingSiteBpnss),
                        },
                        {
                            field: 'ownProducingSiteBpnss',
                            headerName: 'Producing Sites',
                            flex: 2,
                            sortable: false,
                            renderCell: (data: { row: MaterialPartnerRelation }) => renderSitesBpnsCell(data.row.ownProducingSiteBpnss),
                        },
                    ]}
                    rows={mprs ?? []}
                    getRowId={(row) => row.ownMaterialNumber + '-' + row.partnerBpnl}
                    noRowsMsg='No material partner relations found.'
                />
            </Stack>

            <MaterialInformationModal
                open={materialModalOpen}
                material={null}
                onClose={() => setMaterialModalOpen(false)}
                onSave={handleSaveMaterial}
            />

            <PartnerCreationModal
                open={partnerModalOpen}
                onClose={() => setPartnerModalOpen(false)}
                onSave={handleSavePartner}
            />

            <MaterialPartnerRelationModal
                open={mprModalOpen}
                onClose={() => setMprModalOpen(false)}
                onSave={handleSaveMpr}
                materials={materials}
                partners={partners}
                mprs={mprs}
            />
        </>
    );
};