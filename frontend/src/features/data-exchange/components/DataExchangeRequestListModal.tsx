/*
Copyright (c) 2026 Volkswagen AG

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

import { InfoButton } from "@components/ui/InfoButton";
import { CRITICALITY } from "@models/constants/criticality";
import { DataExchangeRequest } from "@models/types/data/data-exchange-request";
import { DemandCapacityNotification } from "@models/types/data/demand-capacity-notification";
import { Partner } from "@models/types/edc/partner";
import { Visibility, FactCheck, Close } from "@mui/icons-material";
import { Box, Button, Dialog, DialogTitle, Stack, Tooltip, Typography } from "@mui/material";
import { Table } from "@catena-x/portal-shared-components";
import { getDataExchangeStatus } from "./DataExchangeRequestModal";

type DataExchangeRequestListModalProps = {
    open: boolean;
    demandCapacityNotification: DemandCapacityNotification | null;
    dataExchangeRequests: DataExchangeRequest[];
    partners: Partner[] | null;
    onClose: () => void;
    onViewRequestClicked?: (request: DataExchangeRequest) => void;
    onCreateApprovalClicked?: (request: DataExchangeRequest) => void;
    onViewApprovalClicked?: (request: DataExchangeRequest) => void;
    onCreateRequestClicked?: (notification: DemandCapacityNotification) => void;
};

const STATUS_COLOR: Record<string, string> = {
    'Approved': 'success.main',
    'Approved, finalized': 'success.main',
    'Approved, not finalized': 'warning.main',
    'Expired': 'text.disabled',
    'Terminated': 'text.disabled',
    'Pending': 'text.primary',
};

export const DataExchangeRequestListModal = ({
    open,
    demandCapacityNotification,
    dataExchangeRequests,
    onClose,
    onViewRequestClicked,
    onCreateApprovalClicked,
    onViewApprovalClicked,
}: DataExchangeRequestListModalProps) => {
    if (!demandCapacityNotification) {
        return null;
    }
    const isOutgoing = demandCapacityNotification.reported === true;

    const rows = [...dataExchangeRequests].sort((a, b) => {
        const openFirst = Number(!!a.dataExchangeApproval) - Number(!!b.dataExchangeApproval);
        if (openFirst !== 0) return openFirst;
        return new Date(a.desiredStartDateTime ?? 0).getTime() - new Date(b.desiredStartDateTime ?? 0).getTime();
    });

    const needsApproval = (request: DataExchangeRequest) => !isOutgoing && !request.dataExchangeApproval && demandCapacityNotification.status !== 'resolved';

    const handleRowClick = (request: DataExchangeRequest) => {
        onClose();
        if (needsApproval(request)) {
            onCreateApprovalClicked?.(request);
        } else if (!request.dataExchangeApproval && isOutgoing) {
            onViewRequestClicked?.(request);
        } else {
            onViewApprovalClicked?.(request);
        }
    };

    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle variant="h3" textAlign="center">Data Exchange Requests</DialogTitle>
            <Stack padding="0 2rem 2rem" gap={1.5} sx={{ width: '60rem' }}>
                <Box width="100%" className="hide-title">
                    <Table
                        onRowClick={(value) => handleRowClick(value.row)}
                        noRowsMsg="No data exchange requests found"
                        title="Data Exchange Requests"
                        columns={[
                            { headerName: 'Message', field: 'message', flex: 1.4,
                                renderCell: (params: { row: DataExchangeRequest }) => (
                                    <Stack display="flex" justifyContent="center" width="100%" height="100%">
                                        <Box>{params.row.text}</Box>
                                    </Stack>
                                ),
                            },
                            { headerName: 'Criticality', field: 'criticality',
                                renderCell: (params: { row: DataExchangeRequest }) => (
                                    <Typography variant="body2" color={params.row.criticality === 'high' ? 'error' : 'text.primary'}>
                                        {CRITICALITY.find((c) => c.key === params.row.criticality)?.value ?? params.row.criticality}
                                    </Typography>
                                ),
                            },
                            { headerName: 'Start date', field: 'desiredStartDateTime',
                                renderCell: (params: { row: DataExchangeRequest }) => {
                                    <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                                        <Box>{new Date(params.row.desiredStartDateTime).toLocaleDateString('en-GB')}</Box>
                                    </Stack>
                                },
                            },
                            { headerName: 'End date', field: 'desiredEndDateTime',
                                renderCell: (params: { row: DataExchangeRequest }) => {
                                    <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                                        <Box>{new Date(params.row.desiredEndDateTime).toLocaleDateString('en-GB')}</Box>
                                    </Stack>
                                },
                            },
                            { headerName: 'Status', field: 'status', flex: 1,
                                renderCell: (params: { row: DataExchangeRequest }) => {
                                    const status = getDataExchangeStatus(params.row, demandCapacityNotification, params.row.dataExchangeApproval ?? null);
                                    return (
                                        <Stack direction="row" alignItems="center" gap={0.75}>
                                            <Typography variant="body2" color={STATUS_COLOR[status.label] ?? 'text.primary'}>{status.label}</Typography>
                                            <InfoButton text={status.explanation} />
                                        </Stack>
                                    );
                                },
                            },
                            { headerName: 'Action', field: 'actions',
                                renderCell: (params: { row: DataExchangeRequest }) => (
                                    <Box display="flex" justifyContent="end" width="100%">
                                        <Tooltip title={needsApproval(params.row) ? 'Approve request' : 'View request'} arrow>
                                            <Box component="span" sx={{ display: 'inline-flex', color: needsApproval(params.row) ? 'primary.main' : 'text.secondary' }}>
                                                {needsApproval(params.row) ? <FactCheck /> : <Visibility />}
                                            </Box>
                                        </Tooltip>
                                    </Box>
                                ),
                            },
                        ]}
                        rows={rows}
                        getRowId={(row) => row.uuid ?? row.requestId}
                        getRowHeight={() => "auto"}
                    />
                </Box>

                <Box display="flex" gap="1rem" width="100%" justifyContent="end">
                    <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={onClose}>
                        <Close /> Close
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};