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
import { useNegotiations } from '@hooks/edc/useNegotiations';
import { Table } from '@catena-x/portal-shared-components';
import { Box } from '@mui/material';

export const NegotiationView = () => {
    const { negotiations } = useNegotiations();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Negotiation</h1>
            <Box width="100%">
                <Table
                    title="Negotiation history"
                    columns={[
                        { headerName: 'Negotiation Id', field: '@id', width: 120 },
                        { headerName: 'Type', field: 'type', width: 150 },
                        { headerName: 'State', field: 'state', width: 150 },
                        { headerName: 'CounterParty', field: 'counterPartyId', width: 200 },
                        { headerName: 'Counterparty EDC URL', field: 'counterPartyAddress', width: 350 },
                        {
                            headerName: 'Timestamp',
                            field: 'createdAt',
                            width: 180,
                            valueFormatter: (params) => new Date(params.value).toLocaleString(),
                        },
                    ]}
                    rows={negotiations ?? []}
                    getRowId={(row) => row['@id']}
                />
            </Box>
        </div>
    );
};
