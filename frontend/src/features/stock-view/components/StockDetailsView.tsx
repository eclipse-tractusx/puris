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

import { Stock, StockType } from '@models/types/data/stock';
import { postStocks, putStocks, refreshPartnerStocks } from '@services/stocks-service';

import { useMaterials } from '../hooks/useMaterials';
import { StockUpdateForm } from './StockUpdateForm';
import { PartnerStockTable } from './PartnerStockTable';
import { StockTable } from './StockTable';
import { useStocks } from '../hooks/useStocks';
import { usePartnerStocks } from '../hooks/usePartnerStocks';

type StockDetailsViewProps<T extends StockType> = {
    type: T;
};

export const StockDetailsView = <T extends StockType>({ type }: StockDetailsViewProps<T>) => {
    const { materials } = useMaterials(type);
    const { stocks, refreshStocks } = useStocks(type);
    const [selectedMaterial, setSelectedMaterial] = useState<Stock | null>(null);
    const { partnerStocks, refreshPartnerStocks: refresh } = usePartnerStocks(
        type,
        type === 'product' ? selectedMaterial?.material?.materialNumberSupplier : selectedMaterial?.material?.materialNumberCustomer
    );
    const [saving, setSaving] = useState<boolean>(false);
    const [refreshing, setRefreshing] = useState(false);

    const handleStockRefresh = () => {
        setRefreshing(true);
        refreshPartnerStocks(
            type,
            (type == 'product' ? selectedMaterial?.material?.materialNumberSupplier : selectedMaterial?.material?.materialNumberCustomer) ??
                null
        ).then(() => {
            refresh();
            setRefreshing(false);
        });
    };

    const saveStock = (stock: Stock) => {
        if (saving) return;
        stock.uuid ??=
            stocks?.find(
                (s) =>
                    s.stockLocationBpna === stock.stockLocationBpna &&
                    s.stockLocationBpns === stock.stockLocationBpns &&
                    (s.material.materialNumberCustomer === stock.material.materialNumberCustomer ||
                        s.material.materialNumberSupplier === stock.material.materialNumberSupplier) &&
                    s.partner?.uuid === stock.partner?.uuid
            )?.uuid ?? null;
        (stock.uuid == null ? postStocks(type, stock) : putStocks(type, stock)).then(() => {
            setSaving(false);
            refreshStocks();
        });
    };

    return (
        <div className="flex flex-col gap-10 pb-5">
            <div className="mx-auto">
                <StockUpdateForm items={materials} type={type} selectedItem={selectedMaterial} onSubmit={saveStock} isSaving={saving} />
            </div>
            <StockTable type={type} onSelection={setSelectedMaterial} stocks={stocks ?? []} />
            <PartnerStockTable
                type={type}
                materialName={selectedMaterial?.material?.name}
                partnerStocks={partnerStocks ?? []}
                onRefresh={handleStockRefresh}
                isRefreshing={refreshing}
            />
        </div>
    );
};
