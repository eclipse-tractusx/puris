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

import { useTransfers } from '@hooks/edc/useTransfers';
import { Table } from '@catena-x/portal-shared-components';
import { Box } from '@mui/material';

export const TransferView = () => {
    const { transfers } = useTransfers();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Transfers</h1>
            <Box width="100%">
                <Table
                    title="Transfer history"
                    columns={[
                        { headerName: 'Transfer Id', field: '@id', width: 200 },
                        { headerName: 'Correlation Id', field: 'correlationId', width: 200 },
                        { headerName: 'State', field: 'state', width: 120 },
                        { headerName: 'Type', field: 'type', width: 120 },
                        { headerName: 'Asset Id', field: 'assetId', width: 200 },
                        { headerName: 'Contract Id', field: 'contractId', width: 200 },
                        { headerName: 'Connector Id', field: 'connectorId', width: 200 },
                    ]}
                    rows={transfers ?? []}
                    getRowId={(row) => row['@id']}
                    noRowsMsg='No transfers found'
                />
            </Box>
        </div>
    );
};
