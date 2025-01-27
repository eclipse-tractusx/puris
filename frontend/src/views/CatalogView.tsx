/*
Copyright (c) 2022 Volkswagen AG
Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022 Contributors to the Eclipse Foundation

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

import { Input, Table } from '@catena-x/portal-shared-components';
import { useCatalog } from '@hooks/edc/useCatalog';
import { useEffect, useRef, useState } from 'react';
import { CatalogOperation, CatalogPermission } from '@models/types/edc/catalog';
import { Box, Button, Stack, Typography, Autocomplete } from '@mui/material';
import { Partner } from '@models/types/edc/partner';
import { useAllPartners } from '@hooks/useAllPartners';
import { getCatalogOperator } from '@util/helpers';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';

const PermissionList = ({ permission }: { permission: CatalogPermission }) => {
    return (
        <Stack>
            {permission['odrl:constraint'] &&
                'odrl:and' in permission['odrl:constraint'] &&
                permission['odrl:constraint']['odrl:and'].map((constraint) => {
                    const permissionString = `${constraint['odrl:leftOperand']['@id']} ${getCatalogOperator(
                        constraint['odrl:operator']['@id']
                    )} ${constraint['odrl:rightOperand']}`;
                    return (
                        <div key={constraint['@type']} title={permissionString}>
                            {permissionString}
                        </div>
                    );
                })}
        </Stack>
    );
};

type CatalogItem = {
    assetId: string;
    assetType: string;
    assetVersion: string;
    permission: CatalogPermission;
    prohibitions: CatalogOperation[];
    obligations: CatalogOperation[];
};

type CatalogListProps = {
    title: string;
    catalog: CatalogItem[];
};

const CatalogList = ({ catalog, title }: CatalogListProps) => {
    return (
        <Table
            title={title}
            columns={[
                { headerName: 'Asset ID', field: 'assetId', flex: 2 },
                {
                    headerName: 'Asset Type',
                    field: 'assetType',
                    flex: 1,
                    renderCell: (row) => <div title={row.row.assetType}>{row.row.assetType.split('#')[1]}</div>,
                },
                { headerName: 'Version', field: 'assetVersion', flex: 0.5 },
                {
                    headerName: 'Action',
                    field: 'permission.action',
                    flex: 0.5,
                    renderCell: (row) => {
                        const actionString = row.row.permission['odrl:action']['@id'];
                        const action = actionString.split('/')[actionString.split('/').length - 1];
                        return <div title={actionString}> {action} </div>;
                    },
                },
                {
                    headerName: 'Usage Policies',
                    field: 'permission',
                    flex: 2,
                    renderCell: (row) => <PermissionList permission={row.row.permission} />,
                },
            ]}
            rows={catalog}
            getRowId={(row) => row.assetId}
        ></Table>
    );
};

export const CatalogView = () => {
    const { partners } = useAllPartners();
    const [partner, setPartner] = useState<Partner | null>(null);
    const { catalog, catalogError, isLoadingCatalog } = useCatalog(partner);
    const partnerRef = useRef<Partner | null>(null);
    const { setTitle } = useTitle();

    useEffect(() => {
        setTitle('Catalog');
    }, [setTitle])
    return (
        <Stack spacing={2}>
            <ConfidentialBanner />
            <Stack spacing={1} direction="row" alignItems="center" sx={{ borderRadius: 2, backgroundColor: 'white', p: '.5rem .5rem' }}>
                <Autocomplete
                    id="partner"
                    value={partner}
                    options={partners ?? []}
                    getOptionLabel={(option) => option?.name ?? ''}
                    sx={{ width: '32rem' }}
                    renderInput={(params) => <Input hiddenLabel {...params} placeholder="Select a Partner" />}
                    onChange={(_, newValue) => (partnerRef.current = newValue)}
                    isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                />
                <Button onClick={() => setPartner(partnerRef?.current)}>Get Catalog</Button>
            </Stack>
            {!isLoadingCatalog ? (
                catalog && catalog.length > 0 ? (
                    <Box width="100%">
                        <CatalogList title={`Catalog for ${partner?.name}`} catalog={catalog} />
                    </Box>
                ) : catalogError != null ? (
                    <Box display="flex" justifyContent="center" paddingTop="5rem">
                        <Typography variant="body1" color="red">
                            There was an error retrieving the Catalog from {partner?.name}
                        </Typography>
                    </Box>
                ) : (
                    partner != null && (
                        <Box display="flex" justifyContent="center" paddingTop="5rem">
                            <Typography variant="body1"> {`No Catalog available for ${partner?.name}`} </Typography>
                        </Box>
                    )
                )
            ) : (
                <Box display="flex" justifyContent="center" paddingTop="5rem">
                    <Typography variant="body1"> Loading Catalog... </Typography>
                </Box>
            )}
        </Stack>
    );
};
