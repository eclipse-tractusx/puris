/*
Copyright (c) 2023 Volkswagen AG
Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2023 Contributors to the Eclipse Foundation

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

import { Tab, TabPanel, Tabs, Table } from '@catena-x/portal-shared-components';
import { Box, Button, Stack } from '@mui/material';
import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Send } from '@mui/icons-material';
import { DemandCapacityNotificationInformationModal } from '@features/notifications/components/NotificationInformationModal';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { EFFECTS } from '@models/constants/effects';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { STATUS } from '@models/constants/status';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';


export const DemandCapacityNotificationView = () => {
    const [selectedTab, setSelectedTab] = useState<number>(0);
    const [demandCapacityNotification, setDemandCapacityNotification] = useState<DemandCapacityNotification[]>([]);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [selectedNotification, setSelectedNotification] = useState<DemandCapacityNotification | null>(null);
    const { setTitle } = useTitle();

    const tabs = useMemo(() => ['Incoming', 'Outgoing'], []);
    
    useEffect(() => {
        setTitle(`${tabs[selectedTab]} Notifications`);
    }, [selectedTab, setTitle, tabs])
    
    const fetchAndLogNotification = useCallback(async () => {
        try {
            const result = await getDemandAndCapacityNotification(selectedTab === 0);
            setDemandCapacityNotification(result);
        } catch (error) {
            console.error(error);
        }
    }, [selectedTab]);

    useEffect(() => {
        fetchAndLogNotification();
    }, [selectedTab, fetchAndLogNotification]);

    const TabPanelContent = ({ notifications }: { notifications: DemandCapacityNotification[] }) => {
        return (
            <DemandCapacityNotificationTable onRowSelected={(notification) => {
                setModalOpen(true);
                setSelectedNotification(notification);
            }} notifications={notifications} />
        );
    }

    return (
        <>
            <Stack spacing={2} alignItems='center' width='100%' height='100%'>
                <ConfidentialBanner />
                <Stack width='100%' direction="row" justifyContent="space-between" alignItems="center">
                    <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)}>
                        {tabs.map((tab, index) => <Tab key={index} label={tab} />)}
                    </Tabs>
                    <Button variant="contained" sx={{display: 'flex', gap: '.5rem'}} onClick={() => {
                        setSelectedNotification(null);
                        setModalOpen(true)
                    }}>
                        <Send></Send> Send Notification
                    </Button>
                </Stack>
                <Box width='100%' display='flex' marginTop='0 !important' paddingBottom='2rem'>
                    {tabs.map((_, index) => (
                        <TabPanel key={index} value={selectedTab} index={index}>
                            <TabPanelContent notifications={demandCapacityNotification} />
                        </TabPanel>
                    ))}
                </Box>
            </Stack>

            <DemandCapacityNotificationInformationModal
                open={modalOpen}
                demandCapacityNotification={selectedNotification}
                onClose={() =>
                    setModalOpen(false)
                }
                onSave={fetchAndLogNotification}

            />
        </>
    );
};

type NotificationTableProps = {
    notifications: DemandCapacityNotification[],
    onRowSelected: (notification: DemandCapacityNotification) => void;
}

const DemandCapacityNotificationTable: React.FC<NotificationTableProps> = ({ notifications, onRowSelected }) => {
    return (
        <Box width="100%">
            <Table
                onRowClick={(value) => {
                    onRowSelected(value.row);
                }}
                noRowsMsg='No Notifications found'
                title="Demand and Capacity Notifications"
                columns={[
                    { headerName: 'Partner Bpnl', field: 'partnerBpnl', flex: 1 },
                    { headerName: 'Leading Root Cause', field: 'leadingRootCause', flex: 1, valueFormatter: (params) => LEADING_ROOT_CAUSE.find((cause) => cause.key === params.value)?.value },
                    { headerName: 'Effect', field: 'effect', flex: 1, valueFormatter: (params) => EFFECTS.find((effect) => effect.key === params.value)?.value, },
                    { headerName: ' Affected Material Numbers', field: 'affectedMaterialNumbers', flex: 1 },
                    { headerName: ' Affected Sites Sender', field: 'affectedSitesBpnsSender', flex: 1 },
                    { headerName: ' Affected Sites Recipient', field: 'affectedSitesBpnsRecipient', flex: 1 },
                    { headerName: 'Text', field: 'text', flex: 1.25 },
                    { headerName: 'Status', field: 'status', flex: 0.5, valueFormatter: (params) => STATUS.find((status) => status.key === params.value)?.value },

                ]}
                rows={notifications ?? []}
                getRowId={(row) => row.uuid}
            />
        </Box>
    );
}
