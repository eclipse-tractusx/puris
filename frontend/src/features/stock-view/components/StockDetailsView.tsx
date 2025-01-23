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

import { useState } from 'react';
import { PageSnackbar, PageSnackbarStack } from '@catena-x/portal-shared-components';

import { Stock, StockType } from '@models/types/data/stock';
import { postStocks, putStocks, requestReportedStocks } from '@services/stocks-service';

import { useMaterials } from '../hooks/useMaterials';
import { StockUpdateForm } from './StockUpdateForm';
import { PartnerStockTable } from './PartnerStockTable';
import { StockTable } from './StockTable';
import { useStocks } from '../hooks/useStocks';
import { useReportedStocks } from '../hooks/useReportedStocks';
import { compareStocks } from '@util/stock-helpers';
import { Notification } from '@models/types/data/notification';
import { Partner } from '@models/types/edc/partner';
import { Box, Stack } from '@mui/material';

type StockDetailsViewProps<T extends StockType> = {
    type: T;
};

export const StockDetailsView = <T extends StockType>({ type }: StockDetailsViewProps<T>) => {
    const { materials } = useMaterials(type);
    const { stocks, refreshStocks } = useStocks(type);
    const [selectedMaterial, setSelectedMaterial] = useState<Stock | null>(null);
    const { reportedStocks } = useReportedStocks(
        type,
        type === 'product'
            ? selectedMaterial?.material?.materialNumberSupplier ?? null
            : selectedMaterial?.material?.materialNumberCustomer ?? null
    );
    const [saving, setSaving] = useState<boolean>(false);
    const [, setRefreshing] = useState(false);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const [notifications, setNotifications] = useState<Notification[]>([]);

    const handleStockRefresh = () => {
        setRefreshing(true);
        requestReportedStocks(
            type,
            (type == 'product' ? selectedMaterial?.material?.materialNumberSupplier : selectedMaterial?.material?.materialNumberCustomer) ??
                null
        )
            .then((partners: Partner[]) => {
                setLastUpdated(new Date());
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: 'Update requested',
                        description: `Stock update has been requested for ${
                            partners.length < 3 ? partners.map((p) => p.name).join(', ') : `${partners.length} partners`
                        }`,
                        severity: 'success',
                    },
                ]);
            })
            .catch((error: unknown) => {
                const msg =
                    error !== null && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
                        ? error.message
                        : 'Unknown Error';
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: 'Error requesting update',
                        description: msg,
                        severity: 'error',
                    },
                ]);
            })
            .finally(() => setRefreshing(false));
    };

    const saveStock = (stock: Stock) => {
        if (saving) return;
        stock.uuid = stocks?.find((s) => compareStocks(s, stock))?.uuid ?? null;
        stock.customerOrderNumber ||= null;
        stock.customerOrderPositionNumber ||= null;
        stock.supplierOrderNumber ||= null;
        (stock.uuid == null ? postStocks(type, stock) : putStocks(type, stock))
            .then(() => {
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: `Stock ${stock.uuid == null ? 'Created' : 'Updated'}`,
                        description: 'The stock has been updated successfully',
                        severity: 'success',
                    },
                ]);
                refreshStocks();
            })
            .catch((error) => {
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: `Error ${stock.uuid == null ? 'Creating' : 'Updating'} Stock`,
                        description: error.message,
                        severity: 'error',
                    },
                ]);
            })
            .finally(() => setSaving(false));
    };

    return (
        <Stack spacing={2.5} paddingBottom={1.25}>
            <Box marginInline="auto">
                <StockUpdateForm items={materials ?? []} type={type} selectedItem={selectedMaterial} onSubmit={saveStock} />
            </Box>
            <Stack spacing={4} sx={{ backgroundColor: 'white', borderRadius: 2, padding: 2 }}>
                <StockTable type={type} onSelection={setSelectedMaterial} stocks={stocks ?? []} />
                <PartnerStockTable
                    type={type}
                    materialName={selectedMaterial?.material?.name}
                    partnerStocks={reportedStocks ?? []}
                    onRefresh={handleStockRefresh}
                    lastUpdated={lastUpdated}
                />
            </Stack>
            <PageSnackbarStack>
                {notifications.map((notification, index) => (
                    <PageSnackbar
                        key={index}
                        open={!!notification}
                        severity={notification?.severity}
                        title={notification?.title}
                        description={notification?.description}
                        autoClose={true}
                        onCloseNotification={() => setNotifications((ns) => ns.filter((_, i) => i !== index) ?? [])}
                    />
                ))}
            </PageSnackbarStack>
        </Stack>
    );
};
