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

import { Material } from '@models/types/data/stock';
import { DirectionType } from '@models/types/erp/directionType';
import { Add, ChevronLeftOutlined, Refresh, Schedule } from '@mui/icons-material';
import { Box, Button, capitalize, Stack, Typography } from '@mui/material';
import { useDataModal } from '@contexts/dataModalContext';
import { Link } from 'react-router-dom';
import { LoadingButton } from '@components/ui/LoadingButton';
import { TextToClipboard } from '@components/ui/TextToClipboard';

type MaterialDetailsHeaderProps = {
    material: Material;
    direction: DirectionType;
    isRefreshing: boolean;
    isSchedulingUpdate: boolean;
    onRefresh: () => void;
    onScheduleUpdate: () => void;
};

export function MaterialDetailsHeader({ material, direction, isRefreshing, isSchedulingUpdate, onRefresh, onScheduleUpdate }: MaterialDetailsHeaderProps) {
    const { openDialog } = useDataModal();
    return (
        <>
            <Stack direction="row" alignItems="center" spacing={1} width="100%">
                <Link to="/materials" data-testid="back-button"> <Box padding="0.25rem" display="flex" alignItems="center"> <ChevronLeftOutlined /> </Box> </Link>
                <Typography variant="h3" component="h1" marginRight="auto !important">
                    {direction === DirectionType.Outbound ? 'Production Information' : 'Demand Information'} for {material?.name} (<TextToClipboard text={material?.ownMaterialNumber ?? ""} />, {capitalize(direction.toLowerCase())})
                </Typography>
                <Stack gap="0.5rem" sx={{flexDirection: { xs: 'column', xl: 'row'}}}>
                    <Stack direction="row" gap="0.5rem">
                        {direction === DirectionType.Outbound ? (
                            <Button 
                                sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={() => openDialog('production', {}, [], 'create')}
                                data-testid="add-production-button"
                            >
                                <Add></Add> Add Production
                            </Button>
                        ) : (
                            <Button 
                                sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={() => openDialog('demand', {}, [], 'create')}
                                data-testid="add-demand-button"
                            >
                                <Add></Add> Add Demand
                            </Button>
                        )}
                        <Button
                            sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            onClick={() =>
                                openDialog(
                                    'delivery',
                                    { departureType: 'estimated-departure', arrivalType: 'estimated-arrival' },
                                    [],
                                    'create',
                                    direction,
                                    null
                                )
                            }
                            data-testid="add-delivery-button"
                        >
                            <Add></Add> Add Delivery
                        </Button>
                        <Button
                            sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            onClick={() =>
                                openDialog(
                                    'stock',
                                    { },
                                    [],
                                    'create',
                                    direction,
                                    null
                                )
                            }
                            data-testid="add-stock-button"
                        >
                            <Add></Add> Add Stock
                        </Button>
                    </Stack>
                    <Stack direction="row" gap="0.5rem" justifyContent="end">
                        <LoadingButton 
                            Icon={Schedule}
                            isLoading={isSchedulingUpdate}
                            onClick={onScheduleUpdate}
                            data-testid="schedule-erp-button"
                        >
                            Schedule ERP Update
                        </LoadingButton>
                        <LoadingButton
                            Icon={Refresh}
                            isLoading={isRefreshing}
                            onClick={onRefresh}
                            data-testid="refresh-partner-data-button"
                        >
                            Refresh
                        </LoadingButton>
                    </Stack>
                </Stack>
            </Stack>
        </>
    );
}
