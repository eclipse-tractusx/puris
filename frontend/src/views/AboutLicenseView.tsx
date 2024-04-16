/*
Copyright (c) 2023,2024 Volkswagen AG
Copyright (c) 2023,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2023,2024 Contributors to the Eclipse Foundation

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

import { Link } from '@mui/material';
import aboutPage from '@assets/aboutPage.json';
import { Table } from '@catena-x/portal-shared-components';

const aboutPageColumns = [
    {
        field: 'header',
        headerName: '',
        flex: 3,
    },
    {
        field: 'body',
        headerName: '',
        renderCell: (data: { row: { body: string; link: string | null } }) => data.row['link'] ? (
            <Link href={data.row.link}>
                {data.row.body}
            </Link>
        ) : (
            data.row.body
        ),
        flex: 8,
    },
];

export const AboutLicenseView = () => {
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">About License</h1>
            <div className='w-[32rem]'>
                <Table
                    columnHeaderHeight={0}
                    title='License Information'
                    hideFooter={true}
                    columns={aboutPageColumns}
                    getRowId={(row) => row.header}
                    rows={aboutPage}
                />
            </div>
        </div>
    );
}
