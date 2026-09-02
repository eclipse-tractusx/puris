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
import { CollapsibleDisruptionPanel } from '@features/notifications/components/CollapsibleNotification';
import { DataExchangeRequest } from '@models/types/data/data-exchange-request';
import { DataExchangeApproval } from '@models/types/data/data-exchange-approval';
import { DataExchangeRequestInformationModal } from '@features/data-exchange/components/DataExchangeRequestModal';
import { getDataExchangeApproval, getDataExchangeRequest } from '@services/data-exchange-service';
import { DataExchangeRequestListModal } from '@features/data-exchange/components/DataExchangeRequestListModal';


export const DemandCapacityNotificationView = () => {
    const [demandCapacityNotification, setDemandCapacityNotification] = useState<DemandCapacityNotification[]>([]);
    const [dataExchangeRequests, setDataExchangeRequests] = useState<DataExchangeRequest[]>([]);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [dataRequestModalOpen, setDataRequestModalOpen] = useState<boolean>(false);
    const [dataApprovalMode, setDataApprovalMode] = useState<boolean>(false);
    const [isEditMode, setIsEditMode] = useState<boolean>(false);
    const [confirmModalOpen, setConfirmModalOpen] = useState<boolean>(false);
    const [selectedNotification, setSelectedNotification] = useState<DemandCapacityNotification | null>(null);
    const [selectedRequest, setSelectedRequest] = useState<DataExchangeRequest | null>(null);
    const [selectedApproval, setSelectedApproval] = useState<DataExchangeApproval | null>(null);
    const [filterPartners, setFilterPartners] = useState<Partner[] | null>(null);
    const [requestListContext, setRequestListContext] = useState<{ notification: DemandCapacityNotification; requests: DataExchangeRequest[]; } | null>(null);
    const [forwardData, setForwardData] = useState<{
        relatedNotificationIds?: string[];
        sourceDisruptionId: string;
        effect: EffectType,
        leadingRootCause: LeadingRootCauseType,
    } | undefined>(undefined);
    const { partners } = useAllPartners();

    const { setTitle } = useTitle();
    const activeRequestNotification = selectedNotification ?? demandCapacityNotification.find(n => n.notificationId === selectedRequest?.notificationId) ?? null;

    const findNotification = (notificationId: string) => demandCapacityNotification.find((n) => n.notificationId === notificationId) ?? null;

    const openRequest = (request: DataExchangeRequest) => {
        setSelectedRequest(request);
        setSelectedNotification(findNotification(request.notificationId));
        setDataApprovalMode(false);
        setSelectedApproval(null);
        setDataRequestModalOpen(true);
    };

    const openApproval = (request: DataExchangeRequest) => {
        setSelectedRequest(request);
        setSelectedNotification(findNotification(request.notificationId));
        setDataApprovalMode(true);
        setSelectedApproval(request.dataExchangeApproval);
        setDataRequestModalOpen(true);
    };

    const openCreateApproval = (request: DataExchangeRequest) => {
        setSelectedRequest(request);
        setSelectedNotification(findNotification(request.notificationId));
        setDataApprovalMode(true);
        setSelectedApproval(null);
        setDataRequestModalOpen(true);
    };

    const openCreateRequest = (notification: DemandCapacityNotification) => {
        setSelectedNotification(notification);
        setSelectedRequest(null);
        setDataApprovalMode(false);
        setSelectedApproval(null);
        setDataRequestModalOpen(true);
    };

    useEffect(() => {
        setTitle('Notifications');
    }, [setTitle]);

    const fetchNotificationsAndRequests = useCallback(async () => {
        try {
            const [incoming, outgoing] = await Promise.all([
                getDemandAndCapacityNotification(true),
                getDemandAndCapacityNotification(false),
            ]);
            setDemandCapacityNotification([...incoming, ...outgoing]);

            const [incomingRequest, outgoingRequest, incomingApproval, outgoingApproval] = await Promise.all([
                getDataExchangeRequest(false),
                getDataExchangeRequest(true),
                getDataExchangeApproval(false),
                getDataExchangeApproval(true),
            ]);

            // match outgoing requests to incoming approvals
            const outgoingRequestWithApproval = outgoingRequest.map((request: DataExchangeRequest) => ({
                ...request,
                dataExchangeApproval: incomingApproval.find(
                    (approval: DataExchangeApproval) => approval.dataExchangeRequestId === request.requestId
                ) ?? null,
            }));

            // match incoming requests to outgoing approvals
            const incomingRequestWithApproval = incomingRequest.map((request: DataExchangeRequest) => ({
                ...request,
                dataExchangeApproval: outgoingApproval.find(
                    (approval: DataExchangeApproval) => approval.dataExchangeRequestId === request.requestId
                ) ?? null,
            }));

            setDataExchangeRequests([...incomingRequestWithApproval, ...outgoingRequestWithApproval]);
        } catch (error) {
            console.error(error);
        }
    }, []);

    useEffect(() => {
        fetchNotificationsAndRequests();
    }, [fetchNotificationsAndRequests]);

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
            .filter(notification => notification.reported === true)
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

                {Object.keys(openGroups).length > 0 ? (
                    Object.entries(openGroups).map(([sourceDisruptionId, notifications]) => (
                        <Box key={sourceDisruptionId} width="100%" display="flex" flexDirection="column" paddingBottom="1">
                            <CollapsibleDisruptionPanel
                                key={sourceDisruptionId}
                                disruptionId={sourceDisruptionId}
                                notifications={notifications}
                                dataExchangeRequests={dataExchangeRequests.filter(r => notifications.some(n => n.notificationId === r.notificationId))}
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
                                onViewRequestClicked={openRequest}
                                onViewApprovalClicked={openApproval}
                                onCreateApprovalClicked={openCreateApproval}
                                onCreateRequestClicked={openCreateRequest}
                                onViewRequestListClicked={(notification, requests) => setRequestListContext({ notification, requests })}
                            />
                        </Box>
                    ))
                ) : (
                    <Typography color="text.secondary">There are currently no ongoing disruptions.</Typography>
                )}

                <Stack width='100%'>
                    <Typography variant="h6">Resolved</Typography>
                </Stack>

                {Object.keys(resolvedGroups).length > 0 ? (
                    Object.entries(resolvedGroups).map(([sourceDisruptionId, notifications]) => (
                        <Box key={sourceDisruptionId} width="100%" display="flex" flexDirection="column" paddingBottom="1">
                            <CollapsibleDisruptionPanel
                                key={sourceDisruptionId}
                                disruptionId={sourceDisruptionId}
                                notifications={notifications}
                                dataExchangeRequests={dataExchangeRequests.filter(r => notifications.some(n => n.notificationId === r.notificationId))}
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
                                onViewRequestClicked={openRequest}
                                onViewApprovalClicked={openApproval}
                                onViewRequestListClicked={(notification, requests) => setRequestListContext({ notification, requests })}
                            />
                        </Box>
                    ))
                ) : (
                    <Typography variant="h6">No previously resolved disruptions found.</Typography>
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
                onSave={fetchNotificationsAndRequests}

            />

            {activeRequestNotification && (
                <DataExchangeRequestInformationModal
                    open={dataRequestModalOpen}
                    dataApprovalMode={dataApprovalMode}
                    dataExchangeApproval={selectedApproval}
                    demandCapacityNotification={activeRequestNotification}
                    dataExchangeRequest={selectedRequest}
                    relatedNotificationsIds={demandCapacityNotification}
                    partners={filterPartners ?? partners}
                    onClose={() => {
                        setDataRequestModalOpen(false);
                        setFilterPartners(null);
                        setDataApprovalMode(false);
                        setSelectedApproval(null);
                        setSelectedRequest(null);
                    }}
                    onSave={fetchNotificationsAndRequests}
                />
            )}
            <DataExchangeRequestListModal
                open={!!requestListContext}
                demandCapacityNotification={requestListContext?.notification ?? null}
                dataExchangeRequests={requestListContext?.requests ?? []}
                partners={partners}
                onClose={() => setRequestListContext(null)}
                onViewRequestClicked={openRequest}
                onCreateApprovalClicked={openCreateApproval}
                onViewApprovalClicked={openApproval}
                onCreateRequestClicked={openCreateRequest}
            />

            <DemandCapacityNotificationResolutionModal
                open={confirmModalOpen}
                demandCapacityNotification={selectedNotification}
                onClose={() =>
                    setConfirmModalOpen(false)
                }
                onSave={fetchNotificationsAndRequests}

            />
        </>
    );
};