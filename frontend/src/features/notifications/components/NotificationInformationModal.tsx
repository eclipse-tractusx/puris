/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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
import { Input, Textarea } from '@catena-x/portal-shared-components';
import { DateTime } from '@components/ui/DateTime';
import { Close, Send, ReportProblem } from '@mui/icons-material';
import { Autocomplete, Box, Button, Dialog, DialogTitle, FormLabel, Grid, InputLabel, Stack, Typography, useTheme } from '@mui/material';
import { useEffect, useState } from 'react';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { postDemandAndCapacityNotification, putDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { EFFECTS } from '@models/constants/effects';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { STATUS } from '@models/constants/status';
import { DemandCapacityNotification, EffectType, LeadingRootCauseType, StatusType } from '@models/types/data/demand-capacity-notification';
import { Site } from '@models/types/edc/site';
import { useSites } from '@features/stock-view/hooks/useSites';
import { usePartnerMaterials } from '@hooks/usePartnerMaterials';
import { Partner } from '@models/types/edc/partner';
import { useNotifications } from '@contexts/notificationContext';

const isValidDemandCapacityNotification = (notification: Partial<DemandCapacityNotification>) =>
    notification.partnerBpnl &&
    notification.effect &&
    notification.status &&
    notification.startDateOfEffect &&
    notification.text &&
    (!notification.expectedEndDateOfEffect || notification.startDateOfEffect < notification.expectedEndDateOfEffect);

type DemandCapacityNotificationInformationModalProps = {
    open: boolean;
    demandCapacityNotification: DemandCapacityNotification | null;
    partners: Partner[] | null;
    isEditMode: boolean;
    forwardData?: {
        relatedNotificationIds?: string[];
        sourceDisruptionId: string;
        effect: EffectType,
        leadingRootCause: LeadingRootCauseType,
    };
    onClose: () => void;
    onSave: () => void;
};

type DemandCapacityNotificationViewProps = {
    demandCapacityNotification: DemandCapacityNotification;
    partners: Partner[] | null;
};

const DemandCapacityNotificationView = ({ demandCapacityNotification, partners }: DemandCapacityNotificationViewProps) => {
    return (
        <Grid container spacing={3} padding=".25rem">
            <Grid display="grid" item xs={6}>
                <FormLabel>Partner</FormLabel>
                <Typography variant="body2">{partners?.find((p) => p.bpnl === demandCapacityNotification.partnerBpnl)?.name}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Leading Root Cause</FormLabel>
                <Typography variant="body2">
                    {LEADING_ROOT_CAUSE.find((dt) => dt.key === demandCapacityNotification.leadingRootCause)?.value}
                </Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Status</FormLabel>
                <Typography variant="body2">{STATUS.find((dt) => dt.key === demandCapacityNotification.status)?.value}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Effect</FormLabel>
                <Typography variant="body2">{EFFECTS.find((dt) => dt.key === demandCapacityNotification.effect)?.value}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Start Date of Effect</FormLabel>
                <Typography variant="body2">{new Date(demandCapacityNotification.startDateOfEffect).toLocaleString()}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Expected End Date of Effect</FormLabel>
                <Typography variant="body2">{demandCapacityNotification.expectedEndDateOfEffect ? new Date(demandCapacityNotification.expectedEndDateOfEffect).toLocaleString() : ''}</Typography>
            </Grid>
            <Grid display="grid" item xs={6}>
                <FormLabel>Last Updated</FormLabel>
                <Typography variant="body2">{demandCapacityNotification.contentChangedAt ? new Date(demandCapacityNotification.contentChangedAt).toLocaleString() : ''}</Typography>
            </Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Affected Sites Sender</FormLabel>
                <Typography variant="body2">
                    {demandCapacityNotification.affectedSitesBpnsSender && demandCapacityNotification.affectedSitesBpnsSender.length > 0
                        ? demandCapacityNotification.affectedSitesBpnsSender.join(', ')
                        : 'None'}
                </Typography>
            </Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Affected Sites Recipient</FormLabel>
                <Typography variant="body2">
                    {demandCapacityNotification.affectedSitesBpnsRecipient &&
                    demandCapacityNotification.affectedSitesBpnsRecipient.length > 0
                        ? demandCapacityNotification.affectedSitesBpnsRecipient.join(', ')
                        : 'None'}
                </Typography>
            </Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Affected Material Numbers</FormLabel>
                <Typography variant="body2">
                    {demandCapacityNotification.affectedMaterialNumbers && demandCapacityNotification.affectedMaterialNumbers.length > 0
                        ? demandCapacityNotification.affectedMaterialNumbers.join(', ')
                        : 'None'}
                </Typography>
            </Grid>
            <Grid display="grid" item xs={12}>
                <FormLabel>Text</FormLabel>
                <Typography variant="body2">{demandCapacityNotification.text}</Typography>
            </Grid>
            {demandCapacityNotification.resolvingMeasureDescription && (
                <Grid display="grid" item xs={12}>
                    <FormLabel>Resolution message</FormLabel>
                    <Typography variant="body2">{demandCapacityNotification.resolvingMeasureDescription}</Typography>
                </Grid>
            )}
        </Grid>
    );
};

export const DemandCapacityNotificationInformationModal = ({
    open,
    demandCapacityNotification,
    partners,
    isEditMode,
    forwardData,
    onClose,
    onSave,
}: DemandCapacityNotificationInformationModalProps) => {
    const [temporaryDemandCapacityNotification, setTemporaryDemandCapacityNotification] = useState<Partial<DemandCapacityNotification>>({});
    const { partnerMaterials } = usePartnerMaterials(temporaryDemandCapacityNotification.partnerBpnl ?? null);

    const theme = useTheme();
    const { notify } = useNotifications();
    const [formError, setFormError] = useState(false);

    const { sites } = useSites();

    useEffect(() => {
        if (open) {
            if (demandCapacityNotification) {
                setTemporaryDemandCapacityNotification(demandCapacityNotification);
            } else {
                const initialData: Partial<DemandCapacityNotification> = {
                    affectedMaterialNumbers: [],
                    affectedSitesBpnsRecipient: [],
                    status: 'open' as StatusType
                };

                if (forwardData) {
                    initialData.sourceDisruptionId = forwardData.sourceDisruptionId;
                    initialData.effect = forwardData.effect;
                    initialData.leadingRootCause = forwardData.leadingRootCause;

                    if (forwardData.relatedNotificationIds) {
                        initialData.relatedNotificationIds = forwardData.relatedNotificationIds;
                    }
                }

                setTemporaryDemandCapacityNotification(initialData);
            }
        }
    }, [open, demandCapacityNotification, forwardData]);

    const handleSaveClick = () => {
        if (!isValidDemandCapacityNotification(temporaryDemandCapacityNotification)) {
            setFormError(true);
            return;
        }
        setFormError(false);
        const request = isEditMode && demandCapacityNotification?.uuid
            ? putDemandAndCapacityNotification(temporaryDemandCapacityNotification)
            : postDemandAndCapacityNotification(temporaryDemandCapacityNotification);

        request
            .then(() => {
                onSave();
                notify({
                    title: isEditMode ? 'Notification Updated' : 'Notification Added',
                    description: `Notification has been ${isEditMode ? 'updated' : 'added'}`,
                    severity: 'success',
                });
            })
            .catch((error) => {
                notify({
                    title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                    description: error.status === 409 ? 'DemandCapacityNotification conflicting with an existing one' : error.error,
                    severity: 'error',
                });
            })
            .finally(handleClose);
    };

    const handleClose = () => {
        setFormError(false);
        setTemporaryDemandCapacityNotification({});
        onClose();
    };
    return (
        <>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle variant="h3" textAlign="center">
                    Demand Capacity Notification Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    {!demandCapacityNotification || isEditMode ? (
                        <Grid container spacing={1} padding=".25rem">
                            <>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        sx={{ margin: '0' }}
                                        id="partner"
                                        options={partners ?? []}
                                        getOptionLabel={(option) => option?.name ?? ''}
                                        label="Partner*"
                                        placeholder="Select a Partner"
                                        error={formError && !temporaryDemandCapacityNotification?.partnerBpnl}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                partnerBpnl: value?.bpnl ?? undefined,
                                            })
                                        }
                                        value={partners?.find((p) => p.bpnl === temporaryDemandCapacityNotification.partnerBpnl) ?? null}
                                        isOptionEqualToValue={(option, value) => option?.bpnl === value?.bpnl}
                                        disabled={isEditMode && demandCapacityNotification !== null}
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="leadingRootCause"
                                        options={LEADING_ROOT_CAUSE}
                                        getOptionLabel={(option) => option.value ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.key === value.key}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                leadingRootCause: value?.key ?? undefined,
                                            })
                                        }
                                        value={
                                            LEADING_ROOT_CAUSE.find(
                                                (dt) => dt.key === temporaryDemandCapacityNotification.leadingRootCause
                                            ) ?? null
                                        }
                                        label="Leading cause*"
                                        placeholder="Select the leading cause"
                                        error={formError && !temporaryDemandCapacityNotification?.leadingRootCause}
                                        disabled={ isEditMode && (demandCapacityNotification !== null || !!forwardData?.sourceDisruptionId) }
                                    />
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="status"
                                        options={STATUS}
                                        getOptionLabel={(option) => option.value ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.key === value.key}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                status: value?.key ?? undefined,
                                            })
                                        }
                                        value={STATUS.find((dt) => dt.key === temporaryDemandCapacityNotification.status) ?? null}
                                        label="Status*"
                                        placeholder="Select the status"
                                        error={formError && !temporaryDemandCapacityNotification?.status}
                                        disabled={true}
                                    ></LabelledAutoComplete>
                                </Grid>
                                <Grid item xs={6}>
                                    <LabelledAutoComplete
                                        id="effect"
                                        options={EFFECTS}
                                        getOptionLabel={(option) => option.value ?? ''}
                                        isOptionEqualToValue={(option, value) => option?.key === value.key}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                effect: value?.key ?? undefined,
                                            })
                                        }
                                        value={EFFECTS.find((dt) => dt.key === temporaryDemandCapacityNotification.effect) ?? null}
                                        label="Effect*"
                                        placeholder="Select the effect"
                                        error={formError && !temporaryDemandCapacityNotification?.effect}
                                        disabled={ isEditMode && (demandCapacityNotification !== null || !!forwardData?.sourceDisruptionId) }
                                    />
                                </Grid>
                                <Grid item xs={6} display="flex" alignItems="end">
                                    <DateTime
                                        label="Start Date of Effect*"
                                        placeholder="Pick Effect start date"
                                        locale="de"
                                        error={
                                            formError &&
                                            (!temporaryDemandCapacityNotification.startDateOfEffect ||
                                                temporaryDemandCapacityNotification.startDateOfEffect > new Date() ||
                                                (!!temporaryDemandCapacityNotification.expectedEndDateOfEffect &&
                                                    temporaryDemandCapacityNotification.startDateOfEffect >
                                                        temporaryDemandCapacityNotification.expectedEndDateOfEffect))
                                        }
                                        value={temporaryDemandCapacityNotification?.startDateOfEffect ?? null}
                                        onValueChange={(date) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                startDateOfEffect: date ?? undefined,
                                            })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={6} display="flex" alignItems="end">
                                    <DateTime
                                        label="Expected End Date Of Effect"
                                        placeholder="Pick Expected End Date Of Effect"
                                        locale="de"
                                        error={
                                            formError &&
                                            !!temporaryDemandCapacityNotification?.expectedEndDateOfEffect && (
                                                temporaryDemandCapacityNotification.expectedEndDateOfEffect < new Date() ||
                                                (!!temporaryDemandCapacityNotification.startDateOfEffect &&
                                                    temporaryDemandCapacityNotification.expectedEndDateOfEffect <
                                                        temporaryDemandCapacityNotification.startDateOfEffect)
                                            )
                                        }
                                        value={temporaryDemandCapacityNotification?.expectedEndDateOfEffect ?? null}
                                        onValueChange={(date) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                expectedEndDateOfEffect: date ?? undefined,
                                            })
                                        }
                                    />
                                </Grid>
                                <Grid item xs={12}>
                                    <InputLabel>Affected Sites Sender</InputLabel>
                                    <Autocomplete
                                        id="own-sites"
                                        value={temporaryDemandCapacityNotification.affectedSitesBpnsSender ?? []}
                                        options={sites?.map((site) => site.bpns) ?? []}
                                        getOptionLabel={(option) => `${sites?.find((site) => site.bpns === option)?.name} (${option})`}
                                        isOptionEqualToValue={(option, value) => option === value}
                                        renderInput={(params) => (
                                            <Input {...params} hiddenLabel placeholder={`Select Sender Affected Sites`} />
                                        )}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                affectedSitesBpnsSender: value ?? [],
                                            })
                                        }
                                        multiple={true}
                                    />
                                </Grid>
                                <Grid item xs={12}>
                                    <InputLabel>Affected Material Numbers</InputLabel>
                                    <Autocomplete
                                        id="affected-material-numbers"
                                        value={temporaryDemandCapacityNotification.affectedMaterialNumbers ?? []}
                                        options={partnerMaterials?.map((partnerMaterial) => partnerMaterial.ownMaterialNumber) ?? []}
                                        getOptionLabel={(option) =>
                                            `${
                                                partnerMaterials?.find((material) => material.ownMaterialNumber === option)?.name
                                            } (${option})`
                                        }
                                        isOptionEqualToValue={(option, value) => option === value}
                                        renderInput={(params) => (
                                            <Input {...params} hiddenLabel placeholder={`Select Affected Material Numbers`} />
                                        )}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                affectedMaterialNumbers: value ?? [],
                                            })
                                        }
                                        disabled={!temporaryDemandCapacityNotification?.partnerBpnl}
                                        multiple={true}
                                    />
                                </Grid>
                                <Grid item xs={12}>
                                    <InputLabel>Affected Sites Recipient</InputLabel>
                                    <Autocomplete
                                        id="partner-site"
                                        value={temporaryDemandCapacityNotification.affectedSitesBpnsRecipient ?? []}
                                        options={
                                            partners
                                                ?.find((partner) => partner.bpnl === temporaryDemandCapacityNotification?.partnerBpnl)
                                                ?.sites.map((site) => site.bpns) ?? []
                                        }
                                        disabled={!temporaryDemandCapacityNotification?.partnerBpnl}
                                        getOptionLabel={(option) =>
                                            `${
                                                partners
                                                    ?.reduce((acc: Site[], p) => [...acc, ...p.sites], [])
                                                    .find((site) => site.bpns === option)?.name
                                            } (${option})`
                                        }
                                        isOptionEqualToValue={(option, value) => option === value}
                                        renderInput={(params) => (
                                            <Input {...params} hiddenLabel placeholder={`Select Affected Sites of Partner`} />
                                        )}
                                        onChange={(_, value) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                affectedSitesBpnsRecipient: value ?? [],
                                            })
                                        }
                                        multiple={true}
                                    />
                                </Grid>
                                <Grid item xs={12}>
                                    <FormLabel>Text*</FormLabel>
                                    <Textarea
                                        minRows="5"
                                        id="text"
                                        value={temporaryDemandCapacityNotification?.text ?? ''}
                                        onChange={(event) =>
                                            setTemporaryDemandCapacityNotification({
                                                ...temporaryDemandCapacityNotification,
                                                text: event.target.value,
                                            })
                                        }
                                        error={formError && !temporaryDemandCapacityNotification?.text}
                                        className={formError && !temporaryDemandCapacityNotification?.text ? 'error-textarea' : ''}
                                    />
                                </Grid>
                                <Typography  variant="body3" sx={{color: theme.palette.warning.main, py: 1}} ><ReportProblem></ReportProblem> These notes will be shared with the selected partner. Please do not include sensitive data of third parties.</Typography>
                            </>
                        </Grid>
                    ) : (
                        <DemandCapacityNotificationView
                            demandCapacityNotification={demandCapacityNotification}
                            partners={partners}
                        ></DemandCapacityNotificationView>
                    )}
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {isEditMode || !demandCapacityNotification ? (
                            <Button
                                variant="contained"
                                sx={{ display: 'flex', gap: '.25rem' }}
                                onClick={handleSaveClick}
                            >
                                <Send /> Save
                            </Button>
                        ) : null}
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};