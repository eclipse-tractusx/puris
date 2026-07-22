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
import { useState } from 'react';
import { Box, Button, IconButton, Stack, Tooltip, Typography, useTheme } from '@mui/material';
import { AddBoxOutlined, CallMade, CallReceived, Check, ChevronRightOutlined, Edit, FactCheck, Visibility } from '@mui/icons-material';
import { DemandCapacityNotification, EffectType } from '@models/types/data/demand-capacity-notification';
import { Partner } from '@models/types/edc/partner';
import { Table } from '@catena-x/portal-shared-components';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { EFFECTS } from '@models/constants/effects';
import { STATUS } from '@models/constants/status';
import { DataExchangeRequest } from '@models/types/data/data-exchange-request';
import { InfoButton } from '@components/ui/InfoButton';

type CollapsibleDemandNotificationProps = {
    disruptionId: string;
    notifications: DemandCapacityNotification[];
    dataExchangeRequests: DataExchangeRequest[];
    partners: Partner[] | null;
    isResolved: boolean,
    onForwardClick: (id: string, notifications: DemandCapacityNotification[]) => void;
    onRowSelected: (notification: DemandCapacityNotification) => void;
    onEditClicked: (notification: DemandCapacityNotification) => void;
    onCheckClicked: (notification: DemandCapacityNotification) => void;
    onViewRequestClicked?: (request: DataExchangeRequest) => void;
    onCreateApprovalClicked?: (request: DataExchangeRequest) => void;
    onViewApprovalClicked?: (request: DataExchangeRequest) => void;
    onCreateRequestClicked?: (notification: DemandCapacityNotification) => void;
};

type ExchangeDirection = 'incoming' | 'outgoing';

type ExchangeStatusDescriptor =
    | { kind: 'info'; text: string }
    | { kind: 'status'; label: string; direction?: ExchangeDirection; onClick?: () => void; request?: DataExchangeRequest; };

const getExchangeStatus = (
    notification: DemandCapacityNotification,
    request: DataExchangeRequest | undefined,
    callbacks: Pick<NotificationTableProps, 'onCreateRequestClicked' | 'onViewRequestClicked' | 'onCreateApprovalClicked' | 'onViewApprovalClicked'>,
): ExchangeStatusDescriptor => {
    const { onCreateRequestClicked, onViewRequestClicked, onCreateApprovalClicked, onViewApprovalClicked } = callbacks;

    if (!isDemandEffect(notification.effect)) {
        return { kind: 'info', text: 'Requesting data exchange is currently not supported for the specified effect of the notification.' };
    }
    if (!request) {
        return { kind: 'status', label: 'Not Requested', onClick: notification.reported ? () => onCreateRequestClicked?.(notification) : undefined };
    }

    const direction: ExchangeDirection = notification.reported ? 'outgoing' : 'incoming';
    const status = (label: string, onClick: () => void) => ({ kind: 'status' as const, label, direction, request, onClick });

    if (notification.status === 'resolved') return status('Terminated', () => onViewApprovalClicked?.(request));
    if (!request.dataExchangeApproval) return direction === 'outgoing'
        ? status('Request Pending', () => onViewRequestClicked?.(request))
        : status('Approval Pending', () => onCreateApprovalClicked?.(request));
    if (request.desiredEndDateTime && new Date(request.desiredEndDateTime) < new Date()) return status('Expired', () => onViewApprovalClicked?.(request));
    return status('Approved', () => onViewApprovalClicked?.(request));
};
const isDemandEffect = (effect: EffectType): boolean => effect === 'capacity-reduction' || effect === 'capacity-increase';

const DIRECTION_META: Record<ExchangeDirection, { icon: React.ReactElement; label: string }> = {
    outgoing: { icon: <CallMade fontSize="inherit" />, label: 'Outgoing request' },
    incoming: { icon: <CallReceived fontSize="inherit" />, label: 'Incoming request' },
};

const ExchangeStatusCell: React.FC<{ status: ExchangeStatusDescriptor }> = ({ status }) => {
    if (status.kind === 'info') {
        return (
            <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                - <InfoButton text={status.text} />
            </Stack>
        );
    }

    const meta = status.direction && DIRECTION_META[status.direction];
    const label = (
        <Stack direction="row" alignItems="center" gap={0.5}>
            {meta && (
                <Tooltip title={meta.label} arrow>
                    <Box component="span" role="img" aria-label={meta.label} sx={{ display: 'inline-flex', color: 'text.secondary' }}>
                        {meta.icon}
                    </Box>
                </Tooltip>
            )}
            <Typography>{status.label}</Typography>
        </Stack>
    );

    if (!status.onClick) return label;

    const actionIcon = !status.request
        ? <AddBoxOutlined />
        : !status.request.dataExchangeApproval && status.direction === 'incoming'
            ? <FactCheck />
            : <Visibility />;

    return (
        <Button variant="text" sx={{ textTransform: 'none' }} onClick={(e) => { e.stopPropagation(); status.onClick?.(); }} startIcon={actionIcon}>
            {label}
        </Button>
    );
};

export function CollapsibleDisruptionPanel({
        disruptionId,
        notifications,
        dataExchangeRequests,
        partners,
        isResolved,
        onForwardClick,
        onRowSelected,
        onEditClicked,
        onCheckClicked,
        onViewRequestClicked,
        onCreateApprovalClicked,
        onViewApprovalClicked,
        onCreateRequestClicked
    }: CollapsibleDemandNotificationProps) {
    const theme = useTheme();
    const [isExpanded, setIsExpanded] = useState(false);

    const incomingCount = notifications.filter(n => n.reported === true).length;
    const outgoingCount = notifications.filter(n => n.reported === false).length;
    const resolvedCount = notifications.filter(n => n.status === 'resolved').length;

    const pendingOutgoingCount = dataExchangeRequests.filter((request) =>
        !request.dataExchangeApproval && notifications.some((n) => n.notificationId === request.notificationId && n.reported === true)
    ).length;

    const pendingIncomingCount = dataExchangeRequests.filter((request) =>
        !request.dataExchangeApproval && notifications.some((n) => n.notificationId === request.notificationId && n.reported === false)
    ).length;
    
    return (
        <>
            <Box style={{ position: 'relative' }}>
                <Button
                    variant="text"
                    sx={{
                        flexGrow: 1,
                        padding: 0,
                        textTransform: 'none',
                        minWidth: '100%',
                        position: 'sticky',
                        left: 0,
                        display: 'flex',
                    }}
                    onClick={() => setIsExpanded((prev) => !prev)}
                    data-testid={`collapsible-notification-button-${disruptionId}`}
                >
                    <Stack
                        direction="row"
                        alignItems="center"
                        spacing={0.5}
                        sx={{
                            borderRadius: isExpanded ? '0.75rem 0.75rem 0 0' : '0.75rem',
                            minHeight: '2.5rem',
                            width: '100%',
                            paddingLeft: '.5rem',
                            backgroundColor: isResolved ? theme.palette.primary.main : theme.palette.primary.dark,
                            color: theme.palette.primary.contrastText,
                        }}
                    >
                        <Box sx={{ display: 'flex', flex: 1, alignItems: 'center', gap: '.5rem' }}>
                            <ChevronRightOutlined sx={{ rotate: isExpanded ? '90deg' : '0deg', transition: 'rotate 300ms ease-in-out'}} />
                            <Typography variant="body2"><b>{LEADING_ROOT_CAUSE.find((cause) => cause.key === notifications[0].leadingRootCause)?.value}</b></Typography>
                            <Typography variant="body2" color="#ccc">({EFFECTS.find((effect) => effect.key === notifications[0].effect)?.value})</Typography>
                        </Box>
                        <Box sx={{ display: 'flex', flex: 1, pr: 2, justifyContent: !isResolved ? 'flex-start' : 'flex-end', textAlign: 'center', gap: '1rem'}}>
                            {incomingCount > 0 && (
                                <Typography variant="body2"><b>Incoming:</b> {incomingCount}</Typography>
                            )}
                            <Typography variant="body2"><b>Outgoing:</b> {outgoingCount}</Typography>
                            {!isResolved && (
                                <Typography variant="body2"><b>Resolved:</b> {resolvedCount}</Typography>
                            )}
                        </Box>
                    </Stack>
                </Button>

                {!isResolved && (
                    <Box sx={{ position: 'absolute', top: '50%', right: '1rem', transform: 'translateY(-50%)', display: 'flex', alignItems: 'center', gap: 1.5, zIndex: 1 }} >
                        {isDemandEffect(notifications[0].effect) && pendingOutgoingCount > 0 && (
                            <Tooltip title={`Pending Outgoing Data Exchange Requests: ${pendingOutgoingCount}`} arrow>
                                <Stack direction="row" alignItems="center" gap={0.5} sx={{ backgroundColor: '#fff', color: theme.palette.error.main, borderRadius: '1rem', px: 1, py: 0.25 }} aria-label={`${pendingOutgoingCount} pending outgoing data exchange requests`}>
                                    <CallMade fontSize="small" />
                                    <Typography variant="body2" fontWeight="bold">{pendingOutgoingCount}</Typography>
                                </Stack>
                            </Tooltip>
                        )}
                        {isDemandEffect(notifications[0].effect) && pendingIncomingCount > 0 && (
                            <Tooltip title={`Pending Incoming Data Exchange Requests (unanswered approvals): ${pendingIncomingCount}`} arrow>
                                <Stack direction="row" alignItems="center" gap={0.5} sx={{ backgroundColor: '#fff', color: theme.palette.error.main, borderRadius: '1rem', px: 1, py: 0.25 }} aria-label={`${pendingIncomingCount} pending incoming data exchange requests, unanswered approvals`} >
                                    <CallReceived fontSize="small" />
                                    <Typography variant="body2" fontWeight="bold">{pendingIncomingCount}</Typography>
                                </Stack>
                            </Tooltip>
                        )}
                        <Button variant="contained" onClick={() => onForwardClick(disruptionId, notifications)}>
                            {notifications.some((n) => !n.reported && (!n.relatedNotificationIds || n.relatedNotificationIds.length === 0)) ? 'New Notification' : 'Forward'}
                        </Button>
                    </Box>
                )}
            </Box>

            {isExpanded && (
                <DemandCapacityNotificationTable
                    notifications={notifications}
                    dataExchangeRequests={dataExchangeRequests}
                    partners={partners}
                    onRowSelected={onRowSelected}
                    onEditClicked={onEditClicked}
                    onCheckClicked={onCheckClicked}
                    onViewRequestClicked={onViewRequestClicked}
                    onCreateApprovalClicked={onCreateApprovalClicked}
                    onViewApprovalClicked={onViewApprovalClicked}
                    onCreateRequestClicked={onCreateRequestClicked}
                    showActionsColumn={!isResolved}
                    incomingCount={incomingCount}
                />
            )}
        </>
    );
}

type NotificationTableProps = {
    notifications: DemandCapacityNotification[],
    dataExchangeRequests: DataExchangeRequest[];
    partners: Partner[] | null,
    showActionsColumn?: boolean;
    incomingCount: number;
    onRowSelected: (notification: DemandCapacityNotification) => void;
    onEditClicked?: (notification: DemandCapacityNotification) => void;
    onCheckClicked?: (notification: DemandCapacityNotification) => void;
    onViewRequestClicked?: (request: DataExchangeRequest) => void;
    onCreateApprovalClicked?: (request: DataExchangeRequest) => void;
    onViewApprovalClicked?: (request: DataExchangeRequest) => void;
    onCreateRequestClicked?: (notification: DemandCapacityNotification) => void;
}

const DemandCapacityNotificationTable: React.FC<NotificationTableProps> = ({ notifications, dataExchangeRequests, partners, onRowSelected, onCheckClicked, onEditClicked, onViewRequestClicked, onCreateApprovalClicked, onViewApprovalClicked, onCreateRequestClicked, showActionsColumn = true, incomingCount}) => {
    return (
        <Box width="100%" className="hide-title">
            <Table
                onRowClick={(value) => {
                    onRowSelected(value.row);
                }}
                noRowsMsg='No Notifications found'
                title={`Title`}
                columns={[
                    ...(incomingCount > 1 ? [
                        {
                            headerName: 'Direction',
                            field: 'reported',
                            valueGetter: (params:  { row: DemandCapacityNotification }) => (params.row.reported ? 'Incoming' : 'Outgoing')
                        }
                    ] : []),
                    { headerName: 'Partner', field: 'partnerBpnl', flex: 1, valueFormatter: (params) => partners?.find((partner) => partner.bpnl === params.value)?.name || params.value },
                    ...(showActionsColumn ? [
                        { headerName: 'Material Numbers', field: 'affectedMaterialNumbers', flex: 1 },
                        { headerName: 'Sites Sender', field: 'affectedSitesBpnsSender', flex: 1 },
                        { headerName: 'Sites Recipient', field: 'affectedSitesBpnsRecipient', flex: 1 },
                    ] : []),
                    { headerName: 'Start date', field: 'startDateOfEffect', renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.startDateOfEffect).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.startDateOfEffect).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ),
                    },
                    { headerName: 'End date', field: 'expectedEndDateOfEffect', renderCell: (data: { row: DemandCapacityNotification }) =>
                        data.row.expectedEndDateOfEffect ? (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.expectedEndDateOfEffect).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.expectedEndDateOfEffect).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ) : null
                    },
                    { headerName: 'Last Updated', field: 'contentChangedAt', renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.contentChangedAt).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.contentChangedAt).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ),
                    },
                    { headerName: 'Status', field: 'status', valueFormatter: (params) => STATUS.find((status) => status.key === params.value)?.value },
                    { headerName: 'Exchange Status', field: 'exchangeStatus', flex: 1,
                        renderCell: (params) => {
                            const request = dataExchangeRequests.find((r) => r.notificationId === params.row.notificationId);
                            const status = getExchangeStatus(params.row, request, {
                                onCreateRequestClicked, onViewRequestClicked, onCreateApprovalClicked, onViewApprovalClicked,
                            });
                            return <ExchangeStatusCell status={status} />;
                        },
                    },
                    { headerName: 'Note', field: 'text', flex: 1.25, renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" justifyContent="center" width="100%" height="100%">
                            <Box>{data.row.text}</Box>
                            {data.row.resolvingMeasureDescription && (
                                <Box>Resolution: {data.row.resolvingMeasureDescription}</Box>
                            )}
                        </Stack>
                        ),
                    },
                    { headerName: 'Action', field: 'actions', renderCell: (params) => {
                        if (params.row.status === 'resolved' || params.row.reported === true) {
                            return null;
                        }
                        return (
                            <Box display="flex" gap={1} justifyContent="end" width="100%" >
                                <Tooltip title="Edit Notification">
                                    <IconButton
                                        color="primary"
                                        size="small"
                                        aria-label="edit"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            onEditClicked?.(params.row);
                                        }}
                                    >
                                        <Edit></Edit>
                                    </IconButton>
                                </Tooltip>
                                <Tooltip title="Resolve Notification">
                                    <IconButton
                                        color="primary"
                                        size="small"
                                        aria-label="confirm"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            onCheckClicked?.(params.row);
                                        }}
                                    >
                                        <Check ></Check >
                                    </IconButton>
                                </Tooltip>
                            </Box>
                        )
                    }}

                ]}
                rows={notifications ?? []}
                getRowId={(row) => row.uuid}
                getRowClassName={(params) => params.row.status === 'resolved' ? 'resolved-status' : ''}
                columnVisibilityModel={{
                    actions: showActionsColumn
                }}
            />
        </Box>
    );
}