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

import { Tab, TabPanel, Tabs } from '@catena-x/portal-shared-components';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { Dashboard } from '@features/dashboard/components/Dashboard';
import { Box, Stack, Typography } from '@mui/material';
import { useState } from 'react';

export const DashboardView = () => {
    const [selectedTab, setSelectedTab] = useState<number>(0);
    return (
        <Stack spacing={2} alignItems='center' width='100%' height='100%'>
            <ConfidentialBanner />
            <Typography variant='h4' component='h1' fontWeight={600}>Dashboard</Typography>
            <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)}>
                <Tab label="Customer"></Tab>
                <Tab label="Supplier"></Tab>
            </Tabs>
            <Box width='100%' display='flex' marginTop='0 !important' paddingBottom='2rem'>
                <TabPanel value={selectedTab} index={0}>
                    <Dashboard type='customer'/>
                </TabPanel>
                <TabPanel value={selectedTab} index={1}>
                    <Dashboard type='supplier'/>
                </TabPanel>
            </Box>
        </Stack>
    );
};
