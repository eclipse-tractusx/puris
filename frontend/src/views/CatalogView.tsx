/*
Copyright (c) 2022,2024 Volkswagen AG
Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022,2024 Contributors to the Eclipse Foundation

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

import { LoadingButton, Table } from '@catena-x/portal-shared-components';
import { useCatalog } from '@hooks/edc/useCatalog';
import { useRef, useState } from 'react';
import { CatalogOperation, CatalogPermission } from '@models/types/edc/catalog';
import { Box, Stack } from '@mui/material';
import { Partner } from '@models/types/edc/partner';
import { useAllPartners } from '@hooks/useAllPartners'; 
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { getCatalogOperator } from '@util/helpers';

const PermissionList = ({ permission }: { permission: CatalogPermission }) => {
    return (
        <Stack>
            {permission['odrl:constraint'] &&
                'odrl:and' in permission['odrl:constraint'] &&
                permission['odrl:constraint']['odrl:and'].map((constraint) => {
                    const permissionString = `${constraint['odrl:leftOperand']} ${getCatalogOperator(constraint['odrl:operator']['@id'])} ${
                        constraint['odrl:rightOperand']
                    }`;
                    return <div title={permissionString}>{permissionString}</div>;
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
                { headerName: 'Asset ID', field: 'assetId', width: 400 },
                {
                    headerName: 'Asset Type',
                    field: 'assetType',
                    width: 150,
                    renderCell: (row) => <div title={row.row.assetType}>{row.row.assetType.split('#')[1]}</div>,
                },
                { headerName: 'Version', field: 'assetVersion', width: 70 },
                {
                    headerName: 'Action',
                    field: 'permission.action',
                    width: 70,
                    renderCell: (row) => {
                        const actionString = row.row.permission['odrl:action']['odrl:type'];
                        const action = actionString.split('/')[actionString.split('/').length - 1];
                        return <div title={actionString}> {action} </div>;
                    },
                },
                {
                    headerName: 'Usage Policies',
                    field: 'permission',
                    width: 500,
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
    return (
        <div className="flex flex-col items-center gap-4 w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700">View Partner Catalog</h1>
            <div className="flex w-[40rem] items-end gap-5">
                <LabelledAutoComplete
                    id="partner"
                    value={partner}
                    options={partners ?? []}
                    getOptionLabel={(option) => option?.name ?? ''}
                    label="Partner*"
                    placeholder="Select a Partner"
                    onChange={(_, newValue) => (partnerRef.current = newValue)}
                    isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                    className="flex-grow"
                />
                <div className="mb-3 flex-shrink-0">
                    <LoadingButton
                        label="Get Catalog"
                        loadIndicator="Loading..."
                        loading={isLoadingCatalog}
                        onButtonClick={() => setPartner(partnerRef?.current)}
                    />
                </div>
            </div>
            {!isLoadingCatalog ? (catalog && catalog.length > 0 ? (
                <Box width="100%">
                    <CatalogList title={`Catalog for ${partner?.name}`} catalog={catalog} />
                </Box>
            ) : catalogError != null ? (
                <div className="text-red-500 py-5">There was an error retrieving the Catalog from {partner?.name}</div>
            ) : (
                partner != null && <div className="py-5"> {`No Catalog available for ${partner?.name}`} </div>
            )) : <div>Loading Catalog... </div>}
        </div>
    );
};
