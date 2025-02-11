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
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';
import { useEffect } from 'react';

export const TransferView = () => {
    const { transfers } = useTransfers();
    const { setTitle } = useTitle();

    useEffect(() => {
        setTitle('Transfers');
    }, [setTitle])
    return (
        <Box width="100%" sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConfidentialBanner />
            <Table
                title="Transfer history"
                columns={[
                    { headerName: 'Transfer Id', field: '@id', flex: 1 },
                    { headerName: 'Correlation Id', field: 'correlationId', flex: 1 },
                    { headerName: 'State', field: 'state', flex: 0.75 },
                    { headerName: 'Type', field: 'type', flex: 0.75 },
                    { headerName: 'Asset Id', field: 'assetId', flex: 1 },
                    { headerName: 'Contract Id', field: 'contractId', flex: 1 },
                    { headerName: 'Connector Id', field: 'connectorId', flex: 1 },
                ]}
                rows={transfers ?? []}
                getRowId={(row) => row['@id']}
                noRowsMsg="No transfers found"
            />
        </Box>
    );
};
