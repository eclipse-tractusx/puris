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

import { Box, Button, Stack, Typography } from '@mui/material';
import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { useCallback, useEffect, useState } from 'react';
import { DemandCapacityNotificationInformationModal } from '@features/notifications/components/NotificationInformationModal';
import { DemandCapacityNotification, EffectType, LeadingRootCauseType } from '@models/types/data/demand-capacity-notification';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';
import { useAllPartners } from '@hooks/useAllPartners';
import { Partner } from '@models/types/edc/partner';
import { DemandCapacityNotificationResolutionModal } from '@features/notifications/components/NotificationResolutionMessageModal';
import { CollapsibleDisruptionPanel } from '@features/notifications/CollapsableNotification';


export const DemandCapacityNotificationView = () => {
    const [demandCapacityNotification, setDemandCapacityNotification] = useState<DemandCapacityNotification[]>([]);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [isEditMode, setIsEditMode] = useState<boolean>(false);
    const [confirmModalOpen, setConfirmModalOpen] = useState<boolean>(false);
    const [selectedNotification, setSelectedNotification] = useState<DemandCapacityNotification | null>(null);
    const [filterPartners, setFilterPartners] = useState<Partner[] | null>(null);
    const [forwardData, setForwardData] = useState<{
        relatedNotificationIds?: string[];
        sourceDisruptionId: string;
        effect: EffectType,
        leadingRootCause: LeadingRootCauseType,
    } | undefined>(undefined);
    const { partners } = useAllPartners();

    const { setTitle } = useTitle();

    useEffect(() => {
        setTitle('Notifications');
    }, [setTitle]);

    const fetchAndLogNotification = useCallback(async () => {
        try {
            const incoming = (await getDemandAndCapacityNotification(true)).map((n: DemandCapacityNotification) => ({
                ...n,
                direction: 'incoming'
            }));
            const outgoing = (await getDemandAndCapacityNotification(false)).map((n: DemandCapacityNotification) => ({
                ...n,
                direction: 'outgoing',
            }));
            setDemandCapacityNotification([...incoming, ...outgoing]);
        } catch (error) {
            console.error(error);
        }
    }, []);

    useEffect(() => {
        fetchAndLogNotification();
    }, [fetchAndLogNotification]);

    const groupedNotifications = demandCapacityNotification.reduce((groups: Record<string, DemandCapacityNotification[]>, notification) => {
        if (!groups[notification.sourceDisruptionId]) groups[notification.sourceDisruptionId] = [];
        groups[notification.sourceDisruptionId].push(notification);
        return groups;
    }, {});

    const openGroups: typeof groupedNotifications = {};
    const resolvedGroups: typeof groupedNotifications = {};

    Object.entries(groupedNotifications).forEach(([id, group]) => {
        if (group.every((n) => n.status === 'resolved')) {
            resolvedGroups[id] = group;
        } else {
            openGroups[id] = group;
        }
    });

    const handleCreateNotificationFromDisruption = (
        disruptionId: string, 
        notifications: DemandCapacityNotification[]
    ) => {
        const relatedNotificationIds = notifications
            .filter(notification => notification.direction === 'incoming')
            .map(notification => notification.notificationId);
        setFilterPartners(
            partners?.filter(partner => !(notifications.map(notification => notification.partnerBpnl)).includes(partner.bpnl)) ?? null
        );

        setSelectedNotification(null);
        setModalOpen(true);
        setIsEditMode(true);
        
        setForwardData({
            sourceDisruptionId: disruptionId,
            relatedNotificationIds: relatedNotificationIds,
            effect: notifications[0].effect,
            leadingRootCause: notifications[0].leadingRootCause
        });
    };

    return (
        <>
            <Stack spacing={2} alignItems='center' width='100%' height='100%'>
                <ConfidentialBanner />
                <Stack width='100%' direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">Open</Typography>
                    <Button variant="contained" sx={{display: 'flex', gap: '.5rem'}} onClick={() => {
                        setSelectedNotification(null);
                        setModalOpen(true);
                        setIsEditMode(true);
                    }}>
                        New Notification
                    </Button>
                </Stack>

                {Object.keys(openGroups).length > 0 && (
                    <>
                        {Object.entries(openGroups).map(([sourceDisruptionId, notifications]) => (
                            <Box key={sourceDisruptionId} width="100%" display="flex" flexDirection="column" paddingBottom="1">
                                <CollapsibleDisruptionPanel
                                    key={sourceDisruptionId}
                                    disruptionId={sourceDisruptionId}
                                    notifications={notifications}
                                    partners={partners}
                                    isResolved={false}
                                    onForwardClick={handleCreateNotificationFromDisruption}
                                    onRowSelected={(notification) => {
                                        setModalOpen(true);
                                        setSelectedNotification(notification);
                                        setIsEditMode(false);
                                    }}
                                    onEditClicked={(notification) => {
                                        setModalOpen(true);
                                        setSelectedNotification(notification);
                                        setIsEditMode(true);
                                    }}
                                    onCheckClicked={(notification) => {
                                        setSelectedNotification(notification);
                                        setConfirmModalOpen(true);
                                    }}
                                />
                            </Box>
                        ))}
                    </>
                )}

                {Object.keys(resolvedGroups).length > 0 && (
                    <>
                        <Stack width='100%'>
                            <Typography variant="h6">Resolved</Typography>
                        </Stack>

                        {Object.entries(resolvedGroups).map(([sourceDisruptionId, notifications]) => (
                            <Box key={sourceDisruptionId} width="100%" display="flex" flexDirection="column" paddingBottom="2rem">
                                <CollapsibleDisruptionPanel
                                    key={sourceDisruptionId}
                                    disruptionId={sourceDisruptionId}
                                    notifications={notifications}
                                    partners={partners}
                                    isResolved={true}
                                    onForwardClick={handleCreateNotificationFromDisruption}
                                    onRowSelected={(notification) => {
                                        setModalOpen(true);
                                        setSelectedNotification(notification);
                                        setIsEditMode(false);
                                    }}
                                    onEditClicked={(notification) => {
                                        setModalOpen(true);
                                        setSelectedNotification(notification);
                                        setIsEditMode(true);
                                    }}
                                    onCheckClicked={(notification) => {
                                        setSelectedNotification(notification);
                                        setConfirmModalOpen(true);
                                    }}
                                />
                            </Box>
                        ))}
                    </>
                )}
            </Stack>

            <DemandCapacityNotificationInformationModal
                open={modalOpen}
                isEditMode={isEditMode}
                demandCapacityNotification={selectedNotification}
                partners={filterPartners ?? partners}
                forwardData={forwardData}
                onClose={() => {
                    setModalOpen(false);
                    setIsEditMode(false);
                    setFilterPartners(null);
                    setForwardData(undefined);
                }}
                onSave={fetchAndLogNotification}

            />

            <DemandCapacityNotificationResolutionModal
                open={confirmModalOpen}
                demandCapacityNotification={selectedNotification}
                onClose={() =>
                    setConfirmModalOpen(false)
                }
                onSave={fetchAndLogNotification}

            />
        </>
    );
};
