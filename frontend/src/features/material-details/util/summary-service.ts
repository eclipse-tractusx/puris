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

import { Delivery } from '@models/types/data/delivery';
import { Demand } from '@models/types/data/demand';
import { Production } from '@models/types/data/production';
import { Stock } from '@models/types/data/stock';
import { groupBy, Prettify } from '@util/helpers';

type SummaryTypeMap = {
    production: Production;
    demand: Demand;
};

export type SummaryType = Prettify<keyof SummaryTypeMap>;

export type DailySummary<TType extends SummaryType> = {
    primaryValues: SummaryTypeMap[TType][];
    primaryValueTotal: number;
    deliveries: Delivery[];
    deliveryTotal: number;
    stocks: Stock[];
    stockTotal: number;
};

export type Summary<TType extends SummaryType> = {
    type: TType;
    dailySummaries: Record<string, DailySummary<TType>>;
};

export type ProductionSummary = Summary<'production'>;
export type DemandSummary = Summary<'demand'>;

function getDateValue<TType extends SummaryType>(type: TType, entity: SummaryTypeMap[TType]): Date {
    return new Date(
        type === 'production'
            ? (entity as SummaryTypeMap['production']).estimatedTimeOfCompletion
            : (entity as SummaryTypeMap['demand']).day
    );
}

/**
 * Creates a material details summary for a given role and timespan. The summary includes the daily sums of primary values, deliveries, and stocks as well as 
 * the individual corresponding positions.
 * 
 * @param type the type of summary to create (production for supplier, demand for customer)
 * @param primaryValues the list of demands or productions for customers and suppliers respectively
 * @param deliveries the list of incoming deliveries and outgoing shipments for customers and suppliers respectively
 * @param stocks the list of stocks for the material
 * @param timespan the timespan in days to create the summary for
 * @returns 
 */
export function createSummary<TType extends SummaryType>(
    type: TType,
    primaryValues: SummaryTypeMap[TType][],
    deliveries: Delivery[],
    stocks: Stock[],
    timespan: number = 28
) {
    const groupedProductions = groupBy(primaryValues, (item) => getDateValue(type, item).toLocaleDateString());
    const groupedDeliveries = groupBy(deliveries, (item) =>
        new Date(type === 'production' ? item.dateOfDeparture : item.dateOfArrival).toLocaleDateString()
    );
    const summary: Summary<TType> = {
        type: type,
        dailySummaries: {},
    };
    const dates = [...new Array(timespan).keys()].map((index) => {
        const today = new Date();
        today.setDate(today.getDate() + index);
        return today.toLocaleDateString();
    });
    for (let i = 0; i <= timespan; i++) {
        const dateString = dates[i];
        const previousDateString = i !== 0 ? dates[i - 1] : '';
        let stockDelta = i !== 0 ? +summary.dailySummaries[previousDateString].primaryValueTotal - +summary.dailySummaries[previousDateString].deliveryTotal : 0;
        if (type === 'demand') {
            stockDelta *= -1;
        }
        const dailySummary: DailySummary<TType> = {
            primaryValues: groupedProductions[dateString] ?? [],
            primaryValueTotal: (groupedProductions[dateString] ?? []).reduce((sum, p) => sum + p.quantity, 0),
            deliveries: groupedDeliveries[dateString] ?? [],
            deliveryTotal: (groupedDeliveries[dateString] ?? []).reduce((sum, d) => sum + d.quantity, 0),
            stocks: i === 0 ? stocks ?? [] : [],
            stockTotal:
                i === 0 ? stocks.reduce((sum, s) => sum + s.quantity, 0) : summary.dailySummaries[previousDateString].stockTotal + stockDelta,
        };
        summary.dailySummaries[dateString] = dailySummary;
    }
    return summary;
}
