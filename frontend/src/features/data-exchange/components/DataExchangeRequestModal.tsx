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
import { Textarea } from '@catena-x/portal-shared-components';
import { DateTime } from '@components/ui/DateTime';
import { Send, ReportProblem, Info, Close } from '@mui/icons-material';
import { Box, Button, Checkbox, Dialog, DialogTitle, Divider, FormLabel, Grid, InputLabel, Stack, Tooltip, Typography, useTheme } from '@mui/material';
import { useEffect, useState } from 'react';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { usePartnerMaterials } from '@hooks/usePartnerMaterials';
import { Partner } from '@models/types/edc/partner';
import { useNotifications } from '@contexts/notificationContext';
import { CriticalityEnumeration, DataExchangeRequest, RequestedTypeEnumeration } from '@models/types/data/data-exchange-request';
import { postDataExchangeApproval, postDataExchangeRequest } from '@services/data-exchange-service';
import { CRITICALITY } from '@models/constants/criticality';
import { DataExchangeApproval } from '@models/types/data/data-exchange-approval';

const isValidDataExchangeRequest = (request: Partial<DataExchangeRequest>) =>
    request.notificationId &&
    request.criticality &&
    request.desiredStartDateTime &&
    request.desiredEndDateTime &&
    request.requestedTypes && request.requestedTypes?.length > 0 &&
    request.text &&
    (!request.desiredEndDateTime || request.desiredStartDateTime < request.desiredEndDateTime);

type DataExchangeRequestModalProps = {
    open: boolean;
    demandCapacityNotification: DemandCapacityNotification;
    dataExchangeRequest: DataExchangeRequest | null;
    partners: Partner[] | null;
    isEditMode: boolean;
    dataApprovalMode: boolean;
    dataExchangeApproval: DataExchangeApproval | null;
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
    approvedTypes: RequestedTypeEnumeration[];
    onApprovedTypesChange: (checked: boolean) => void;
};

const AffectedMaterialsSection = ({ materialNumbers, partnerMaterials }: { materialNumbers?: string[]; partnerMaterials: PartnerMaterials; }) => (
    <Grid display="grid" item xs={12}>
        <FormLabel>Customer Affected Materials:</FormLabel>
        <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5, mt: 0.5, maxHeight: 180, overflowY: 'auto' }}>
            <Grid container>
                {materialNumbers?.map((materialNumber, index) => {
                    const name = partnerMaterials?.find((m) => m.ownMaterialNumber === materialNumber)?.description;
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
    approvedTypes,
    onApprovedTypesChange,
}: DataExchangeRequestViewProps) => {
    const isCreatingApproval = dataApprovalMode && !dataExchangeApproval;
    const isNTierChecked = dataApprovalMode
        ? (dataExchangeApproval ? dataExchangeApproval.approvedTypes.includes('n-tier') : approvedTypes.includes('n-tier'))
        : dataExchangeRequest.requestedTypes.includes('n-tier');

    return (
        <Grid container spacing={3} padding=".25rem">
            <Grid display="grid" item xs={6}>
                <FormLabel>Notification ID</FormLabel>
                <Typography variant="body2">{demandCapacityNotification.notificationId}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Leading Cause</FormLabel>
                <Typography variant="body2">{LEADING_ROOT_CAUSE.find((dt) => dt.key === demandCapacityNotification.leadingRootCause)?.value}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Affected Partner</FormLabel>
                <Typography variant="body2">{partners?.find((p) => p.bpnl === demandCapacityNotification.partnerBpnl)?.name}</Typography>
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
                <FormLabel>Desired Start Time and Date</FormLabel>
                <Typography variant="body2">{dataExchangeRequest.desiredStartDateTime ? new Date(dataExchangeRequest.desiredStartDateTime).toLocaleString() : ''}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Desired End Time and Date</FormLabel>
                <Typography variant="body2">{dataExchangeRequest.desiredEndDateTime ? new Date(dataExchangeRequest.desiredEndDateTime).toLocaleString() : ''}</Typography>
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
                        checked={isNTierChecked}
                        disabled={!isCreatingApproval}
                        onChange={isCreatingApproval ? (_, checked) => onApprovedTypesChange(checked) : undefined}
                        data-testid="requested-types-n-tier"
                    />
                    <InputLabel htmlFor="requested-types-n-tier"> Exchange anonymous data with relevant participants (N-Tier) </InputLabel>
                </Stack>
            </Grid>
        </Grid>
    );
};

export const DataExchangeRequestInformationModal = ({
    open,
    demandCapacityNotification,
    dataExchangeRequest,
    partners,
    isEditMode,
    dataApprovalMode,
    dataExchangeApproval,
    onClose,
    onSave,
}: DataExchangeRequestModalProps) => {
    const [temporaryDataExchangeRequest, setTemporaryDataExchangeRequest] = useState<Partial<DataExchangeRequest>>({});
    const [temporaryApprovedTypes, setTemporaryApprovedTypes] = useState<RequestedTypeEnumeration[]>([]);
    const { partnerMaterials } = usePartnerMaterials(demandCapacityNotification.partnerBpnl);

    const theme = useTheme();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);

    useEffect(() => {
        if (open) {
            if (dataExchangeRequest) {
                setTemporaryDataExchangeRequest(dataExchangeRequest);
            } else {
                const initialData: Partial<DataExchangeRequest> = {
                    requestedTypes: [],
                    criticality: 'low' as CriticalityEnumeration,
                    notificationId: demandCapacityNotification.notificationId
                };

                setTemporaryDataExchangeRequest(initialData);
            }
            setTemporaryApprovedTypes(dataExchangeApproval?.approvedTypes ?? []);
        }
    }, [open, dataExchangeRequest, dataExchangeApproval]);

    const handleSaveClick = () => {
        if (!isValidDataExchangeRequest(temporaryDataExchangeRequest) ||
            !temporaryDataExchangeRequest.text?.trim()) {
            setFormError(true);
            return;
        }
        setFormError(false);
        postDataExchangeRequest(temporaryDataExchangeRequest)
            .then(() => {
                onSave();
                notify({
                    title: 'Data Exchange Request Added',
                    description: 'Data exchange request has been added',
                    severity: 'success',
                });
            })
            .catch((error) => {
                console.error("Error occurred while saving data exchange request: ", error);
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error',
                    description: error.status === 409 ? 'Data exchange request conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            })
            .finally(handleClose);
    };

    const handleApprovedTypesChange = (checked: boolean) => {
        setTemporaryApprovedTypes(checked ? ['n-tier'] : []);
    };

    const handleApproveClick = () => {
        if (!dataExchangeRequest) {
            return;
        }
        const approvalToSave: Partial<DataExchangeApproval> = {
            approvedTypes: temporaryApprovedTypes,
            dataExchangeRequestId: dataExchangeRequest.requestId,
            isFinalized: true
        };
        postDataExchangeApproval(dataExchangeRequest.uuid, approvalToSave)
            .then(() => {
                onSave();
                notify({
                    title: 'Data Exchange Approval Added',
                    description: 'Data exchange approval has been added',
                    severity: 'success',
                });
            })
            .catch((error) => {
                console.error("Error occurred while saving data exchange approval: ", error);
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error',
                    description: error.status === 409 ? 'Data exchange approval conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            })
            .finally(handleClose);
    };

    const handleClose = () => {
        setFormError(false);
        setTemporaryDataExchangeRequest({});
        setTemporaryApprovedTypes([]);
        onClose();
    };
    return (
        <>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle variant="h3" textAlign="center">
                    {dataApprovalMode ? 'Data Exchange Approval' : isEditMode ? 'Create Data Exchange Request' : 'Data Exchange'}
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    {!dataApprovalMode && (!dataExchangeRequest || isEditMode) ? (
                        <Grid container spacing={3} padding=".25rem">
                            <Grid display="grid" item xs={12}>
                                <FormLabel>Partner</FormLabel>
                                <Typography variant="body2">{partners?.find((p) => p.bpnl === demandCapacityNotification.partnerBpnl)?.name}</Typography>
                            </Grid>
                            <Grid display="grid" item xs={6}>
                                <FormLabel>Notification ID</FormLabel>
                                <Typography variant="body2">{demandCapacityNotification.notificationId}</Typography>
                            </Grid>
                            <Grid display="grid" item xs={6}>
                                <FormLabel>Leading Cause</FormLabel>
                                <Typography variant="body2">{LEADING_ROOT_CAUSE.find((dt) => dt.key === demandCapacityNotification.leadingRootCause)?.value}</Typography>
                            </Grid>
                            {demandCapacityNotification.affectedMaterialNumbers && demandCapacityNotification.affectedMaterialNumbers.length > 0 && (
                                <AffectedMaterialsSection
                                    materialNumbers={demandCapacityNotification.affectedMaterialNumbers}
                                    partnerMaterials={partnerMaterials}
                                />    
                            )}
                            <Grid item xs={6}>
                                <FormLabel>Request*:</FormLabel>
                                <Stack direction="row" alignItems="center">
                                    <Checkbox
                                        id="requestedTypes-n-tier"
                                        checked={temporaryDataExchangeRequest?.requestedTypes?.includes('n-tier') ?? false}
                                        onChange={(_, checked) =>
                                            setTemporaryDataExchangeRequest({
                                                ...temporaryDataExchangeRequest,
                                                requestedTypes: checked
                                                    ? ['n-tier']
                                                    : [],
                                            })
                                        }
                                    />
                                    <InputLabel error={formError && !temporaryDataExchangeRequest?.requestedTypes?.length} htmlFor="requestedTypes-n-tier"> Anonymous data from relevant participants (N-Tier) </InputLabel>
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
                                    error={
                                        formError &&
                                        (!temporaryDataExchangeRequest.desiredStartDateTime ||
                                            (!!temporaryDataExchangeRequest.desiredEndDateTime &&
                                                temporaryDataExchangeRequest.desiredStartDateTime >
                                                temporaryDataExchangeRequest.desiredEndDateTime))
                                    }
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
                                    error={
                                        formError &&
                                        !!temporaryDataExchangeRequest?.desiredEndDateTime && (
                                            temporaryDataExchangeRequest.desiredEndDateTime < new Date() ||
                                            (!!temporaryDataExchangeRequest.desiredStartDateTime &&
                                                temporaryDataExchangeRequest.desiredEndDateTime <
                                                temporaryDataExchangeRequest.desiredStartDateTime)
                                        )
                                    }
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
                                <FormLabel>Message (available only to direct partners)</FormLabel>
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
                                <Typography variant="body3" sx={{ color: theme.palette.error.main, py: 1 }} ><ReportProblem></ReportProblem> These notes will be released to all selected partners and may include sensitive data. Proceed with caution!</Typography>
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
                            approvedTypes={temporaryApprovedTypes}
                            onApprovedTypesChange={handleApprovedTypesChange}
                        ></DataExchangeRequestView>
                    ) : null}
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {dataApprovalMode ? (
                            dataExchangeApproval ? (
                                <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} disabled>
                                    Approved
                                </Button>
                            ) : (
                                <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleApproveClick}>
                                    <Send /> Approve and close
                                </Button>
                            )
                        ) : isEditMode || !dataExchangeRequest ? (
                            <Button variant="contained" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleSaveClick}
                            >
                                <Send /> Send
                            </Button>
                        ) : null}
                    </Box>
                </Stack>
            </Dialog >
        </>
    );
};