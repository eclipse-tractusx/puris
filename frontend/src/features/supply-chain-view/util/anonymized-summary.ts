/*
Copyright (c) 2026 Volkswagen AG
Copyright (c) 2026 Contributors to the Eclipse Foundation

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

import { AnonymizedDelivery, AnonymizedProduction, AnonymizedStock } from '@models/types/data/aggregated-material-data';
import { groupBy } from '@util/helpers';

export type AnonymizedDailySummary = {
    productionTotal: number;
    deliveryTotal: number;
    stockTotal: number;
};

export type AnonymizedSummary = {
    dailySummaries: Record<string, AnonymizedDailySummary>;
};

function getProductionDate(production: AnonymizedProduction): Date {
    return new Date(production.estimatedTimeOfCompletion);
}

function getDeliveryDate(delivery: AnonymizedDelivery): Date {
    return new Date(delivery.dateOfDeparture);
}

/**
 * Creates a daily summary of anonymized planned production, deliveries and item stock for a tier-2+ BOM node,
 * mirroring the shape produced by createSummary(), but derived purely from anonymized aggregated data models.
 */
export function createAnonymizedSummary(
    productions: AnonymizedProduction[],
    deliveries: AnonymizedDelivery[],
    stocks: AnonymizedStock[],
    timespan: number = 28
): AnonymizedSummary {
    const groupedProductions = groupBy(productions, (production) => getProductionDate(production).toLocaleDateString());
    const groupedDeliveries = groupBy(deliveries, (delivery) => getDeliveryDate(delivery).toLocaleDateString());
    const stockTotalToday = stocks.reduce((sum, stock) => sum + stock.quantity, 0);

    const dates = [...new Array(timespan + 1).keys()].map((index) => {
        const today = new Date();
        today.setDate(today.getDate() + index);
        return today.toLocaleDateString();
    });

    const dailySummaries: Record<string, AnonymizedDailySummary> = {};
    for (let i = 0; i <= timespan; i++) {
        const dateString = dates[i];
        const previousDateString = i !== 0 ? dates[i - 1] : '';
        const productionTotal = (groupedProductions[dateString] ?? []).reduce((sum, production) => sum + production.quantity, 0);
        const deliveryTotal = (groupedDeliveries[dateString] ?? []).reduce((sum, delivery) => sum + delivery.quantity, 0);
        const stockTotal =
            i === 0
                ? stockTotalToday
                : dailySummaries[previousDateString].stockTotal +
                  dailySummaries[previousDateString].productionTotal -
                  dailySummaries[previousDateString].deliveryTotal;
        dailySummaries[dateString] = { productionTotal, deliveryTotal, stockTotal };
    }
    return { dailySummaries };
}
