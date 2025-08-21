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

import { Close } from '@mui/icons-material';
import { Box, Button, Dialog, DialogTitle, FormLabel, Grid, Stack, Typography, useTheme } from '@mui/material';
import { useEffect, useState } from 'react';
import { putDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { DemandCapacityNotification, StatusType } from '@models/types/data/demand-capacity-notification';
import { Check, ReportProblem } from '@mui/icons-material';
import { useNotifications } from '@contexts/notificationContext';
import { Textarea } from '@catena-x/portal-shared-components';

type DemandCapacityNotificationResolutionModalProps = {
    open: boolean;
    demandCapacityNotification: DemandCapacityNotification | null;
    onClose: () => void;
    onSave: () => void;
};

export const DemandCapacityNotificationResolutionModal = ({
    open,
    demandCapacityNotification,
    onClose,
    onSave,
}: DemandCapacityNotificationResolutionModalProps) => {

    const theme = useTheme();
    const { notify } = useNotifications();

    const [resolutionMessage, setResolutionMessage] = useState('');
    const [formError, setFormError] = useState(false);

    useEffect(() => {
        if (open) {
            setResolutionMessage('');
            setFormError(false);
        }
    }, [open]);

    const handleSaveClick = () => {
        if (resolutionMessage === '' || !resolutionMessage?.trim()) {
            setFormError(true);
            return;
        }

        setFormError(false);

        const updatedNotification = {
            ...demandCapacityNotification,
            resolvingMeasureDescription: resolutionMessage,
            status: 'resolved' as StatusType,
            affectedSitesBpnsRecipient: [],
            affectedSitesBpnsSender: [],
            affectedMaterialNumbers: [],
            text: null
        };

        putDemandAndCapacityNotification(updatedNotification)
            .then(() => {
                onSave();
                notify({
                        title: 'Notification Resolved',
                        description: 'Notification has been resolved',
                        severity: 'success',
                    },
                );
            })
            .catch((error) => {
                notify({
                        title: 'Error resolving notification',
                        description: error.error,
                        severity: 'error',
                    },
                );
            })
            .finally(handleClose);
    };

    const handleClose = () => {
        setFormError(false);
        setResolutionMessage('');
        onClose();
    };
    return (
        <>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle variant="h3" textAlign="center">
                    Resolve Notification
                </DialogTitle>
                <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
                    {demandCapacityNotification && (
                        <Grid container spacing={1} padding=".25rem">
                            <>
                                <Grid item xs={12}>
                                    <FormLabel>Resolution message*</FormLabel>
                                    <Textarea
                                        minRows="5"
                                        id="resolvingMeasureDescription"
                                        value={resolutionMessage}
                                        onChange={(event) => setResolutionMessage(event.target.value)}
                                        error={formError && !resolutionMessage?.trim()}
                                        className={formError && !resolutionMessage?.trim() ? 'error-textarea' : ''}
                                        placeholder="Your message"
                                    />
                                </Grid>
                                <Typography  variant="body3" sx={{color: theme.palette.warning.main, py: 1}} ><ReportProblem></ReportProblem> Notification cannot be resolved without resolution message. Once resolved, notification will no longer be editable. Are you sure you want to continue?</Typography>
                            </>
                        </Grid>
                    ) }
                    <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="1rem">
                        <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
                            <Close></Close> Close
                        </Button>
                        {demandCapacityNotification ? (
                            <Button
                                variant="contained"
                                sx={{ display: 'flex', gap: '.25rem' }}
                                onClick={() => handleSaveClick()}
                            >
                                <Check></Check> Resolve
                            </Button>
                        ) : null}
                    </Box>
                </Stack>
            </Dialog>
        </>
    );
};