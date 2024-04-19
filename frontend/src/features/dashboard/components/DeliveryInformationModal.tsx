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

import { Delivery } from '@models/types/data/delivery';
import { Button, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';

const GridItem = ({ label, value }: { label: string; value: string }) => (
    <Grid item xs={6}>
        <Stack>
            <Typography variant="caption1" fontWeight={500}>
                {label}:
            </Typography>
            <Typography variant="body3" paddingLeft=".5rem">
                {value}
            </Typography>
        </Stack>
    </Grid>
);

type DeliveryInformationModalProps = {
    open: boolean;
    onClose: () => void;
    delivery: Delivery | null;
};

export const DeliveryInformationModal = ({ open, onClose, delivery }: DeliveryInformationModalProps) => {
    return (
        <Dialog open={open && delivery !== null} onClose={onClose} title="Delivery Information">
            <DialogTitle fontWeight={600} textAlign='center'>Delivery Information</DialogTitle>
            <Stack padding="0 2rem 2rem">
                <Grid container spacing={2} width="32rem" padding='.25rem'>
                    <GridItem label="Incoterm" value="EXW" />
                    <GridItem label="Tracking Number" value="1Z9829WDE02128" />
                    <GridItem label="Origin" value={delivery?.origin?.bpns ?? ''} />
                    <GridItem label="Destination" value={delivery?.destination?.bpns ?? ''} />
                    <GridItem label="ETD" value={delivery?.etd ?? ''} />
                    <GridItem label="ETA" value={new Date().toLocaleDateString(undefined, { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })} />
                    <GridItem label="Delivery Quantity" value={`${delivery?.quantity} pieces`} />
                </Grid>
                <Button variant="contained" sx={{ marginTop: '2rem' }} onClick={onClose}>
                    Close
                </Button>
            </Stack>
        </Dialog>
    );
};
