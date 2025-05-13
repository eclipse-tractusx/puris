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

import { Table } from '@catena-x/portal-shared-components';
import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { capitalize } from '@mui/material';

type MaterialListProps = {
    materials: MaterialDescriptor[];
    onRowClick: (row: MaterialDescriptor) => void;
};

export function MaterialList({ materials, onRowClick }: MaterialListProps) {
    
    return (
        <Table
            title="Materials"
            columns={[
                { headerName: 'Material Number', field: 'ownMaterialNumber', flex: 1.5 },
                { headerName: 'Name', field: 'description', flex: 2 },
                { headerName: 'Days of Supply', field: 'daysOfSupply', flex: 1, valueGetter: (params) => params.row.daysOfSupply.toFixed(2),},
                { headerName: 'Updated', field: 'lastUpdatedOn', flex: 1, valueGetter: (params) => new Date(params.row.lastUpdatedOn).toLocaleString() },
                { headerName: 'Direction', flex: 1, field: 'direction', valueGetter: (params) => capitalize(params.row.direction) },
            ]}
            rows={materials ?? []}
            getRowId={(row) => row.ownMaterialNumber + row.direction}
            onRowClick={(row) => onRowClick(row.row)}
            noRowsMsg='No materials found'
        />
    );
}
