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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/

import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { useNotifications } from '@contexts/notificationContext';
import { Material } from '@models/types/data/stock';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { IrsChainOpeningGrant } from '@models/types/irs/irs-grant';
import { Close, Send } from '@mui/icons-material';
import { Box, Button, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { postIrsJob } from '@services/irs-service';

const formatInstant = (value: string | null | undefined) => (value ? new Date(value).toLocaleString() : '-');

type Option = {
    id: string;
    label: string;
};

type IrsJobCreationModalProps = {
    open: boolean;
    rootGrants: IrsChainOpeningGrant[];
    materials: Material[];
    onClose: () => void;
    onSave: () => void;
};

export const IrsJobCreationModal = ({ open, rootGrants, materials, onClose, onSave }: IrsJobCreationModalProps) => {
    const [globalAssetId, setGlobalAssetId] = useState<string | null>(null);
    const [notificationId, setNotificationId] = useState<string | null>(null);
    const [reportedNotifications, setReportedNotifications] = useState<DemandCapacityNotification[]>([]);
    const [formError, setFormError] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const { notify } = useNotifications();

    const grants = useMemo(() => rootGrants.filter((grant) => grant.syncStatus !== 'DELETED'), [rootGrants]);
    const grantHasNotification = (grant: IrsChainOpeningGrant, id: string) => !!grant.reportedNotificationIds?.includes(id);

    const materialOptions = useMemo<Option[]>(() => {
        const globalAssetIds = new Set(
            grants.filter((grant) => !notificationId || grantHasNotification(grant, notificationId)).map((grant) => grant.globalAssetId)
        );
        return [...globalAssetIds].map((id) => {
            const material = materials.find((m) => m.materialNumberCx === id);
            return { id, label: material ? `${material.name} (${material.ownMaterialNumber})` : id };
        });
    }, [grants, materials, notificationId]);

    const notificationOptions = useMemo<Option[]>(() => {
        const notificationIds = new Set(
            grants
                .filter((grant) => !globalAssetId || grant.globalAssetId === globalAssetId)
                .flatMap((grant) => grant.reportedNotificationIds ?? [])
        );
        return [...notificationIds].map((id) => {
            const notification = reportedNotifications.find((n) => n.notificationId === id);
            return {
                id,
                label: notification ? `${notification.text || notification.effect} (${notification.partnerBpnl})` : id,
            };
        });
    }, [grants, reportedNotifications, globalAssetId]);

    const selectedGrant = useMemo(
        () =>
            globalAssetId && notificationId
                ? grants.find((grant) => grant.globalAssetId === globalAssetId && grantHasNotification(grant, notificationId)) ?? null
                : null,
        [grants, globalAssetId, notificationId]
    );

    const selectedNotification = reportedNotifications.find((n) => n.notificationId === notificationId);

    useEffect(() => {
        if (!open) return;
        setGlobalAssetId(null);
        setNotificationId(null);
        getDemandAndCapacityNotification(true)
            .then((notifications: DemandCapacityNotification[]) => setReportedNotifications(notifications))
            .catch(() => setReportedNotifications([]));
    }, [open]);

    const handleMaterialChange = (value: Option | null) => {
        const id = value?.id ?? null;
        setGlobalAssetId(id);
        if (id && notificationId && !grants.some((grant) => grant.globalAssetId === id && grantHasNotification(grant, notificationId))) {
            setNotificationId(null);
        }
    };

    const handleNotificationChange = (value: Option | null) => {
        const id = value?.id ?? null;
        setNotificationId(id);
        if (id && globalAssetId && !grants.some((grant) => grant.globalAssetId === globalAssetId && grantHasNotification(grant, id))) {
            setGlobalAssetId(null);
        }
    };

    const handleClose = () => {
        setFormError(false);
        setGlobalAssetId(null);
        setNotificationId(null);
        onClose();
    };

    const handleSaveClick = async () => {
        if (!selectedGrant) {
            setFormError(true);
            return;
        }
        setFormError(false);
        setIsSaving(true);
        try {
            await postIrsJob({ rootGrantUuid: selectedGrant.uuid });
            notify({
                title: 'IRS Job created',
                description: 'The job creation request has been queued for the IRS',
                severity: 'success',
            });
            onSave();
            handleClose();
        } catch (error: any) {
            const isConflict = error?.status === 409;
            notify({
                title: isConflict ? 'IRS Job already queued' : 'Error creating IRS Job',
                description: isConflict
                    ? 'A job for this material and source disruption is already queued for the IRS'
                    : error?.message,
                severity: 'error',
            });
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle variant="h3" textAlign="center">
                New Job
            </DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                <Grid container spacing={1} padding=".25rem">
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="irs-job-material"
                            options={materialOptions}
                            disabled={grants.length === 0}
                            getOptionLabel={(option) => option.label}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            onChange={(_, value) => handleMaterialChange(value)}
                            value={materialOptions.find((option) => option.id === globalAssetId) ?? null}
                            label="Material*"
                            placeholder={grants.length === 0 ? 'No root grants available' : 'Select material'}
                            error={formError && !globalAssetId}
                            data-testid="irs-job-modal-material"
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <LabelledAutoComplete
                            id="irs-job-notification"
                            options={notificationOptions}
                            disabled={grants.length === 0}
                            getOptionLabel={(option) => option.label}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            onChange={(_, value) => handleNotificationChange(value)}
                            value={notificationOptions.find((option) => option.id === notificationId) ?? null}
                            label="Notification*"
                            placeholder={grants.length === 0 ? 'No root grants available' : 'Select notification'}
                            error={formError && !notificationId}
                            data-testid="irs-job-modal-notification"
                        />
                    </Grid>
                    {selectedGrant && (
                        <>
                            <GrantDetail label="Global Asset Id" value={selectedGrant.globalAssetId} />
                            <GrantDetail label="Source Disruption Id" value={selectedGrant.sourceDisruptionId} />
                            <GrantDetail label="Valid From" value={formatInstant(selectedGrant.validFrom)} />
                            <GrantDetail label="Valid To" value={formatInstant(selectedGrant.validTo)} />
                            <GrantDetail label="Allowed BPNLs" value={selectedGrant.allowedBpnlSet?.join(', ') || '-'} />
                            <GrantDetail
                                label="Cause"
                                value={
                                    LEADING_ROOT_CAUSE.find((cause) => cause.key === selectedNotification?.leadingRootCause)?.value ?? '-'
                                }
                            />
                        </>
                    )}
                </Grid>
                <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                    <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                        <Close /> Close
                    </Button>
                    <Button
                        variant="contained"
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleSaveClick}
                        disabled={isSaving}
                        data-testid="irs-job-modal-save"
                    >
                        <Send /> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};

const GrantDetail = ({ label, value }: { label: string; value: string }) => (
    <Grid item xs={6}>
        <Typography variant="body3" fontWeight={600}>
            {label}
        </Typography>
        <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
            {value}
        </Typography>
    </Grid>
);
