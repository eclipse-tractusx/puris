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
import { DialogActions, DialogContent, Textarea } from '@catena-x/portal-shared-components';
import { DateTime } from '@components/ui/DateTime';
import { Send, ReportProblem, Info, Close } from '@mui/icons-material';
import { Box, Button, Checkbox, Dialog, DialogTitle, Divider, FormLabel, Grid, InputLabel, Stack, Tooltip, Typography, useTheme } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { usePartnerMaterials } from '@hooks/usePartnerMaterials';
import { Partner } from '@models/types/edc/partner';
import { useNotifications } from '@contexts/notificationContext';
import { CriticalityEnumeration, DataExchangeRequest } from '@models/types/data/data-exchange-request';
import { postDataExchangeApproval, postDataExchangeRequest } from '@services/data-exchange-service';
import { CRITICALITY, RequestedType } from '@models/constants/criticality';
import { DataExchangeApproval } from '@models/types/data/data-exchange-approval';
import { EFFECTS } from '@models/constants/effects';
import { InfoButton } from '@components/ui/InfoButton';

type ReferencedNotificationCardProps = {
    notification: DemandCapacityNotification;
    partners: Partner[] | null;
};

const formatDate = (date?: string | Date | null) => date ? new Date(date).toLocaleDateString('en-GB') : null;

export const ReferencedNotificationCard = ({ notification, partners }: ReferencedNotificationCardProps) => {
    const rootCause = LEADING_ROOT_CAUSE.find((c) => c.key === notification.leadingRootCause)?.value ?? notification.leadingRootCause;
    const effect = EFFECTS.find((e) => e.key === notification.effect)?.value ?? notification.effect;
    const partnerName = partners?.find((p) => p.bpnl === notification.partnerBpnl)?.name ?? notification.partnerBpnl;
    const startDate = formatDate(notification.startDateOfEffect);
    const endDate = formatDate(notification.expectedEndDateOfEffect);

    return (
        <Stack gap={0.5}>
            <FormLabel>Reference Notification</FormLabel>
            <Stack direction="row" alignItems="center" justifyContent="space-between" gap={2} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, px: 1.5, py: 1 }} >
                <Box>
                    <Typography variant="body2" component="span" fontWeight="bold">{rootCause}</Typography>
                    <Typography variant="body2" component="span" color="text.secondary"> ({effect})</Typography>
                </Box>
                <Typography variant="body2">{partnerName}</Typography>
                <Typography variant="body2"> {startDate}{endDate ? ` - ${endDate}` : ''} </Typography>
            </Stack>
        </Stack>
    );
};

const startOfToday = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
};

const getDefaultDesiredStartDateTime = (demandCapacityNotification: DemandCapacityNotification): Date => {
    const now = new Date();
    const notificationStart = demandCapacityNotification.startDateOfEffect
        ? new Date(demandCapacityNotification.startDateOfEffect)
        : null;
    return notificationStart && notificationStart > now ? notificationStart : now;
};

const isDesiredStartDateTimeValid = (
    request: Partial<DataExchangeRequest>,
    demandCapacityNotification: DemandCapacityNotification,
) => {
    if (!request.desiredStartDateTime) return false;
    const start = new Date(request.desiredStartDateTime);
    if (start < startOfToday()) return false;
    if (start < new Date(demandCapacityNotification.startDateOfEffect)) return false;
    if (demandCapacityNotification.expectedEndDateOfEffect && start > new Date(demandCapacityNotification.expectedEndDateOfEffect)) return false;
    if (request.desiredEndDateTime && start >= new Date(request.desiredEndDateTime)) return false;
    return true;
};

const isDesiredEndDateTimeValid = (
    request: Partial<DataExchangeRequest>,
    demandCapacityNotification: DemandCapacityNotification,
) => {
    if (!request.desiredEndDateTime) return false;
    const end = new Date(request.desiredEndDateTime);
    if (end < new Date()) return false;
    if (end < new Date(demandCapacityNotification.startDateOfEffect)) return false;
    if (demandCapacityNotification.expectedEndDateOfEffect && end > new Date(demandCapacityNotification.expectedEndDateOfEffect)) return false;
    if (request.desiredStartDateTime && end <= new Date(request.desiredStartDateTime)) return false;
    return true;
};

export const getDataExchangeStatus = (dataExchangeRequest: DataExchangeRequest, demandCapacityNotification: DemandCapacityNotification, dataExchangeApproval: DataExchangeApproval | null): { label: string; explanation: string } => {
    if (demandCapacityNotification.status === 'resolved') {
        return { label: 'Terminated', explanation: 'The notification has been resolved' };
    }
    if (dataExchangeRequest.desiredEndDateTime && new Date(dataExchangeRequest.desiredEndDateTime) < new Date()) {
        return { label: 'Expired', explanation: `The request has ended on ${new Date(dataExchangeRequest.desiredEndDateTime).toLocaleString()}`};
    }
    if (dataExchangeApproval) {
        if (dataExchangeApproval.isFinalized === true) {
            return { label: 'Approved, finalized', explanation: 'The request has been approved and finalized' };
        } else if (dataExchangeApproval.isFinalized === false) {
            return { label: 'Approved, not finalized', explanation: 'The request has been approved, but not finalized' };    
        }
        return { label: 'Approved', explanation: 'The request has been approved' };
    }
    return { label: 'Pending', explanation: 'Data exchange request created, waiting for approval' };
};

const isValidDataExchangeRequest = (request: Partial<DataExchangeRequest>, demandCapacityNotification: DemandCapacityNotification) =>
    request.notificationId &&
    request.criticality &&
    request.desiredStartDateTime &&
    request.desiredEndDateTime &&
    request.requestedTypes && request.requestedTypes?.length > 0 &&
    request.text &&
    isDesiredStartDateTimeValid(request, demandCapacityNotification) &&
    isDesiredEndDateTimeValid(request, demandCapacityNotification);

export type ForwardTarget = {
    notification: DemandCapacityNotification;
    start: Date;
    end: Date;
};

const maxDate = (a: Date, b: Date) => (a < b ? b : a);
const minDate = (a: Date, b: Date) => (a < b ? a : b);

export const resolveForwardTargets = (dataExchangeRequest: DataExchangeRequest, demandCapacityNotification: DemandCapacityNotification, candidates: DemandCapacityNotification[]): ForwardTarget[] => {
    const relatedIds = demandCapacityNotification.relatedNotificationIds;
    if (!relatedIds?.length || !dataExchangeRequest.desiredStartDateTime || !dataExchangeRequest.desiredEndDateTime) {
        return [];
    }
    const requesterBpnl = demandCapacityNotification.partnerBpnl;
    const requestedStart = new Date(dataExchangeRequest.desiredStartDateTime);
    const requestedEnd = new Date(dataExchangeRequest.desiredEndDateTime);
 
    return candidates.filter((candidate) => candidate.reported && relatedIds.includes(candidate.notificationId) && candidate.partnerBpnl !== requesterBpnl)
        .map((notification) => ({
            notification,
            start: maxDate(requestedStart, new Date(notification.startDateOfEffect)),
            end: notification.expectedEndDateOfEffect ? minDate(requestedEnd, new Date(notification.expectedEndDateOfEffect)) : requestedEnd,
        }))
        .filter(({ start, end }) => start < end);
};

type DataExchangeRequestModalProps = {
    open: boolean;
    demandCapacityNotification: DemandCapacityNotification;
    dataExchangeRequest: DataExchangeRequest | null;
    partners: Partner[] | null;
    dataApprovalMode: boolean;
    dataExchangeApproval: DataExchangeApproval | null;
    relatedNotificationsIds?: DemandCapacityNotification[];
    onClose: () => void;
    onSave: () => void;
};

type PartnerMaterials = ReturnType<typeof usePartnerMaterials>['partnerMaterials'];

type DataExchangeRequestViewProps = {
    dataExchangeRequest: DataExchangeRequest;
    demandCapacityNotification: DemandCapacityNotification;
    dataExchangeApproval: DataExchangeApproval | null;
    partners: Partner[] | null;
    partnerMaterials: PartnerMaterials;
    dataApprovalMode: boolean;
    forwardTargets: ForwardTarget[];
    forward: boolean;
    onForwardChange: (forward: boolean) => void;
};

const AffectedMaterialsSection = ({ materialNumbers, partnerMaterials }: { materialNumbers?: string[]; partnerMaterials: PartnerMaterials; }) => (
    <Grid display="grid" item xs={12}>
        <FormLabel>Affected Materials:</FormLabel>
        <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5, mt: 0.5, maxHeight: 180, overflowY: 'auto' }}>
            <Grid container>
                {materialNumbers?.map((materialNumber, index) => {
                    const name = partnerMaterials?.find((m) => m.ownMaterialNumber === materialNumber)?.name;
                    return (
                        <Grid item xs={4} key={index}>
                            <Tooltip
                                title={<Typography variant="body2">Material Name: {name ?? materialNumber}</Typography>}
                                placement="bottom-start"
                                arrow
                            >
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, py: 0.5, px: 0.5, borderRadius: 0.5 }}>
                                    <Info sx={{ fontSize: 16, color: 'text.primary', flexShrink: 0 }} />
                                    <Typography variant="body2" noWrap>{materialNumber}</Typography>
                                </Box>
                            </Tooltip>
                        </Grid>
                    );
                })}
            </Grid>
        </Box>
    </Grid>
);

const DataExchangeRequestView = ({
    dataExchangeRequest,
    demandCapacityNotification,
    partners,
    partnerMaterials,
    dataExchangeApproval,
    dataApprovalMode,
    forwardTargets,
    forward,
    onForwardChange
}: DataExchangeRequestViewProps) => {
    const isCreatingApproval = dataApprovalMode && !dataExchangeApproval;
    const status = getDataExchangeStatus(dataExchangeRequest, demandCapacityNotification, dataExchangeApproval);

    return (
        <Grid container spacing={3} padding=".25rem">
            <Grid item xs={12}>
                <ReferencedNotificationCard notification={demandCapacityNotification} partners={partners} />
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Criticality</FormLabel>
                <Typography variant="body2" color={dataExchangeRequest.criticality === 'high' ? 'error' : 'text.primary'}>
                    {CRITICALITY.find((dt) => dt.key === dataExchangeRequest.criticality)?.value}
                </Typography>
            </Grid>
            {demandCapacityNotification.affectedMaterialNumbers && demandCapacityNotification.affectedMaterialNumbers.length > 0 && (
                <>
                <Grid display="grid" item xs={12}><Divider flexItem /></Grid>
                <AffectedMaterialsSection
                    materialNumbers={demandCapacityNotification.affectedMaterialNumbers}
                    partnerMaterials={partnerMaterials}
                />
                </>
            )}
            <Grid display="grid" item xs={12}><Divider flexItem /></Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Desired Start and End Time and Date</FormLabel>
                <Typography variant="body2">{dataExchangeRequest.desiredStartDateTime ? new Date(dataExchangeRequest.desiredStartDateTime).toLocaleString() : ''} - {dataExchangeRequest.desiredEndDateTime ? new Date(dataExchangeRequest.desiredEndDateTime).toLocaleString() : ''}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Data Exchange Status</FormLabel>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} paddingBlock=".75rem">
                    <Typography variant="body2">{status.label}</Typography><InfoButton text={status.explanation}></InfoButton>
                </Stack>
            </Grid>
            <Grid display="grid" item xs={12}><Divider flexItem /></Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Message</FormLabel>
                <Typography variant="body2">{dataExchangeRequest.text}</Typography>
            </Grid>
            <Grid display="grid" item xs={12}><Divider flexItem /></Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Agree to</FormLabel>
                <Stack direction="row" alignItems="center">
                    <Checkbox
                        id="requested-types-n-tier"
                        checked={true}
                        disabled={!isCreatingApproval}
                        data-testid="requested-types-n-tier"
                    />
                    <InputLabel htmlFor="requested-types-n-tier"> Exchange anonymous data with relevant participants across multiple tiers </InputLabel>
                </Stack>
            </Grid>
            {isCreatingApproval && forwardTargets.length > 0 && (
                <>
                    <Grid display="grid" item xs={12}><Divider flexItem /></Grid>
                    <Grid display="grid" item xs={12}>
                        <FormLabel>Forward</FormLabel>
                        <Stack direction="row" alignItems="center">
                            <Checkbox id="forward-request" checked={forward} onChange={(_, checked) => onForwardChange(checked)} data-testid="forward-request" />
                            <InputLabel htmlFor="forward-request"> Forward this request to {forwardTargets.length} partner{forwardTargets.length > 1 ? 's' : ''}</InputLabel>
                        </Stack>
                    </Grid>
                </>
            )}
        </Grid>
    );
};

export const DataExchangeRequestInformationModal = ({
    open,
    demandCapacityNotification,
    dataExchangeRequest,
    partners,
    dataApprovalMode,
    dataExchangeApproval,
    relatedNotificationsIds = [],
    onClose,
    onSave,
}: DataExchangeRequestModalProps) => {
    const [temporaryDataExchangeRequest, setTemporaryDataExchangeRequest] = useState<Partial<DataExchangeRequest>>({});
    const [isConfirmApproveOpen, setIsConfirmApproveOpen] = useState(false);
    const [forwardRequest, setForwardRequest] = useState(false);
    const { partnerMaterials } = usePartnerMaterials(demandCapacityNotification.partnerBpnl);

    const theme = useTheme();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);

        const forwardTargets = useMemo(() => {
        if (!dataApprovalMode || !dataExchangeRequest || dataExchangeApproval) {
            return [];
        }
        return resolveForwardTargets(dataExchangeRequest, demandCapacityNotification, relatedNotificationsIds);
    }, [dataApprovalMode, dataExchangeRequest, dataExchangeApproval, demandCapacityNotification, relatedNotificationsIds]);
 
    const willForward = forwardRequest && forwardTargets.length > 0;

    useEffect(() => {
        if (open) {
            setForwardRequest(false);
            if (dataExchangeRequest) {
                setTemporaryDataExchangeRequest(dataExchangeRequest);
            } else {
                const initialData: Partial<DataExchangeRequest> = {
                    requestedTypes: [RequestedType.N_TIER],
                    criticality: 'low' as CriticalityEnumeration,
                    notificationId: demandCapacityNotification.notificationId,
                    desiredStartDateTime: getDefaultDesiredStartDateTime(demandCapacityNotification),
                    desiredEndDateTime: demandCapacityNotification.expectedEndDateOfEffect ? new Date(demandCapacityNotification?.expectedEndDateOfEffect) : new Date(),
                };
 
                setTemporaryDataExchangeRequest(initialData);
            }
        }
    }, [open, dataExchangeRequest, dataExchangeApproval, demandCapacityNotification]);

    const handleSaveClick = () => {
        if (!isValidDataExchangeRequest(temporaryDataExchangeRequest, demandCapacityNotification)) {
            setFormError(true);
            return;
        }
        setFormError(false);
        postDataExchangeRequest(temporaryDataExchangeRequest)
            .then(() => {
                onSave();
                notify({
                    title: 'Data Exchange Approval requested',
                    description: 'Data exchange approval for the specified notification has been requested',
                    severity: 'success',
                });
            })
            .catch((error) => {
                console.error("Error occurred while requesting data exchange approval: ", error);
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error',
                    description: error.status === 409 ? 'Data exchange approval has already been requested' : error.error,
                    severity: 'error',
                });
            })
            .finally(handleClose);
    };
 
    const handleApproveClick = () => {
        if (!dataExchangeRequest) {
            return;
        }
        const approvalToSave: Partial<DataExchangeApproval> = {
            approvedTypes: [RequestedType.N_TIER],
            dataExchangeRequestId: dataExchangeRequest.requestId,
            isFinalized: !willForward,
        };
        postDataExchangeApproval(dataExchangeRequest.uuid, approvalToSave, willForward)
            .then(() => {
                onSave();
                notify({
                    title: willForward ? 'Data Exchange approved and forwarded' : 'Data Exchange approved',
                    description: willForward
                        ? `The request has been approved and forwarded to partner${forwardTargets.length > 1 ? 's' : ''}.`
                        : 'The requested data exchange approval has been approved',
                    severity: 'success',
                });
            })
            .catch((error) => {
                console.error("Error occurred while approving data exchange: ", error);
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error',
                    description: error.status === 409 ? 'Data exchange has already been approved' : error.error,
                    severity: 'error',
                });
            })
            .finally(handleClose);
    };
    
    const handleApproveConfirm = () => {
        setIsConfirmApproveOpen(false);
        handleApproveClick();
    };
    const handleClose = () => {
        setFormError(false);
        setIsConfirmApproveOpen(false);
        setForwardRequest(false);
        setTemporaryDataExchangeRequest({});
        onClose();
    };
    return (
        <>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle variant="h3" textAlign="center">{dataApprovalMode ? 'Data Exchange Approval' : 'Data Exchange'}</DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    {!dataApprovalMode && (!dataExchangeRequest) ? (
                        <Grid container spacing={3} padding=".25rem">
                            <Grid item xs={12}>
                                <ReferencedNotificationCard notification={demandCapacityNotification} partners={partners} />
                            </Grid>
                            {demandCapacityNotification.affectedMaterialNumbers && demandCapacityNotification.affectedMaterialNumbers.length > 0 && (
                                <AffectedMaterialsSection
                                    materialNumbers={demandCapacityNotification.affectedMaterialNumbers}
                                    partnerMaterials={partnerMaterials}
                                />    
                            )}
                            <Grid item xs={6}>
                                <FormLabel>Request*</FormLabel>
                                <Stack direction="row" alignItems="center">
                                    <Checkbox
                                        id="requestedTypes-n-tier"
                                        checked={temporaryDataExchangeRequest?.requestedTypes?.includes(RequestedType.N_TIER) ?? false}
                                        disabled
                                    />
                                    <InputLabel htmlFor="requestedTypes-n-tier" sx={{ whiteSpace: 'normal', overflow: 'visible', textOverflow: 'unset' }}> Anonymous data from relevant participants accross multiple tiers </InputLabel>
                                </Stack>
                            </Grid>
                            <Grid item xs={6}>
                                <LabelledAutoComplete
                                    id="criticality"
                                    options={CRITICALITY}
                                    getOptionLabel={(option) => option.value ?? ''}
                                    isOptionEqualToValue={(option, value) => option?.key === value.key}
                                    onChange={(_, value) =>
                                        setTemporaryDataExchangeRequest({
                                            ...temporaryDataExchangeRequest,
                                            criticality: value?.key ?? undefined,
                                        })
                                    }
                                    value={CRITICALITY.find((dt) => dt.key === temporaryDataExchangeRequest.criticality) ?? null}
                                    label="Criticality*"
                                    placeholder="Set Criticality"
                                    error={formError && !temporaryDataExchangeRequest?.criticality}
                                ></LabelledAutoComplete>
                            </Grid>
                            <Grid item xs={6} display="flex" alignItems="end">
                                <DateTime
                                    label="Desired Start Time and Date*"
                                    placeholder="Pick Desired Start Time and Date"
                                    locale="de"
                                    error={formError && !isDesiredStartDateTimeValid(temporaryDataExchangeRequest, demandCapacityNotification)}
                                    value={temporaryDataExchangeRequest?.desiredStartDateTime ?? null}
                                    onValueChange={(date) =>
                                        setTemporaryDataExchangeRequest({
                                            ...temporaryDataExchangeRequest,
                                            desiredStartDateTime: date ?? undefined,
                                        })
                                    }
                                />
                            </Grid>
                            <Grid item xs={6} display="flex" alignItems="end">
                                <DateTime
                                    label="Desired End Time and Date*"
                                    placeholder="Pick Desired End Time and Date"
                                    locale="de"
                                    error={formError && !isDesiredEndDateTimeValid(temporaryDataExchangeRequest, demandCapacityNotification)}
                                    value={temporaryDataExchangeRequest?.desiredEndDateTime ?? null}
                                    onValueChange={(date) =>
                                        setTemporaryDataExchangeRequest({
                                            ...temporaryDataExchangeRequest,
                                            desiredEndDateTime: date ?? undefined,
                                        })
                                    }
                                />
                            </Grid>
                            <Grid item xs={12}>
                                <FormLabel>Message (available only to direct partners)*</FormLabel>
                                <Textarea
                                    minRows="5"
                                    id="text"
                                    value={temporaryDataExchangeRequest?.text ?? ''}
                                    onChange={(event) =>
                                        setTemporaryDataExchangeRequest({
                                            ...temporaryDataExchangeRequest,
                                            text: event.target.value,
                                        })
                                    }
                                    error={formError && !temporaryDataExchangeRequest?.text?.trim()}
                                    className={formError && !temporaryDataExchangeRequest?.text?.trim() ? 'error-textarea' : ''}
                                />
                                <Typography variant="body3" sx={{ color: theme.palette.error.main, py: 1 }} ><ReportProblem></ReportProblem> IMPORTANT: Please note that the requested data may be competitively sensitive and, according to appliable antitrust laws, must not be shared with competitors that are not involved. The message is only shared with the selected partner. Please consult your legal department, if necessary.</Typography>
                            </Grid>
                        </Grid>
                    ) : dataExchangeRequest ? (
                        <DataExchangeRequestView
                            dataExchangeApproval={dataExchangeApproval}
                            dataExchangeRequest={dataExchangeRequest}
                            demandCapacityNotification={demandCapacityNotification}
                            partners={partners}
                            partnerMaterials={partnerMaterials}
                            dataApprovalMode={dataApprovalMode}
                            forwardTargets={forwardTargets}
                            forward={forwardRequest}
                            onForwardChange={setForwardRequest}
                        ></DataExchangeRequestView>
                    ) : null}
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {dataApprovalMode ? (
                            dataExchangeApproval ? (
                                <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} disabled>
                                    {dataExchangeApproval.isFinalized === false ? 'Approved, not finalized' : 'Approved'}
                                </Button>
                            ) : (
                                <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} onClick={() => setIsConfirmApproveOpen(true)}>
                                    <Send /> {willForward ? 'Approve, forward and close' : 'Approve and close'}
                                </Button>
                            )
                        ) : !dataExchangeRequest ? (
                            <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleSaveClick}
                            >
                                <Send /> Send
                            </Button>
                        ) : null}
                    </Box>
                </Stack>
            </Dialog >
            <Dialog open={isConfirmApproveOpen} onClose={() => setIsConfirmApproveOpen(false)}>
                <DialogTitle variant="h3" textAlign="center">Confirm Approval</DialogTitle>
                <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 1, padding: "0.1rem 1rem" }}>
                    <Typography variant="body2">Do you really want to approve the data exchange request?</Typography>
                    {willForward && (
                        <Typography variant="body2">
                            The request is also forwarded to {forwardTargets.length} partner{forwardTargets.length > 1 ? 's' : ''}.
                        </Typography>
                    )}
                </DialogContent>
                <DialogActions sx={{ padding: 0 }}>
                    <Stack direction="row" justifyContent="center" width="100%" gap={1}> 
                        <Button variant="outlined" color="primary" onClick={() => setIsConfirmApproveOpen(false)}>Cancel</Button>
                        <Button variant="contained" onClick={handleApproveConfirm} autoFocus><Send sx={{ mr: 0.5 }} /> {willForward ? 'Approve and forward' : 'Approve'}</Button>
                    </Stack>
                </DialogActions>
            </Dialog>
        </>
    );
};