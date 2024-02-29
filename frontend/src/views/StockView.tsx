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

import { useState } from 'react';
import { Tab, TabPanel, Tabs } from '@catena-x/portal-shared-components';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { StockDetailsView } from '@features/stock-view/components/StockDetailsView';

export const StockView = () => {
    const [selectedTab, setSelectedTab] = useState<number>(0);
    return (
        <>
            <ConfidentialBanner />
            <div className="flex flex-col items-center w-full h-full p-5">
                <h1 className="text-3xl font-semibold mb-5">View and manage stocks</h1>
                <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)}>
                    <Tab label="Material Stocks"></Tab>
                    <Tab label="Product Stocks"></Tab>
                </Tabs>
                <div className="flex w-full">
                    <TabPanel value={selectedTab} index={0}>
                        <StockDetailsView type="material" />
                    </TabPanel>
                    <TabPanel value={selectedTab} index={1}>
                        <StockDetailsView type="product" />
                    </TabPanel>
                </div>
            </div>
        </>
    );
};
