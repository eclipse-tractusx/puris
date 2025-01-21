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

import { INCOTERMS } from '@models/constants/incoterms';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { OrderReference } from '@models/types/data/order-reference';
import { UnitOfMeasurementKey } from '@models/types/data/uom';

export const getUnitOfMeasurement = (unitOfMeasurementKey: UnitOfMeasurementKey) =>
    UNITS_OF_MEASUREMENT.find((uom) => uom.key === unitOfMeasurementKey)?.value;

export const getIncoterm = (incoterm: string) => {
    return INCOTERMS.find((i) => i.key === incoterm)?.value;
};

export const getCatalogOperator = (operatorId: string) => {
    switch (operatorId) {
        case 'odrl:eq':
            return 'equals';
        default:
            return operatorId;
    }
};

/* Type predicates */

/***
 * Type predicate to check if a value is an array
 * Unlike Array.isArray, this predicate asserts the members of the array to be unknown rather than any
 */
export const isArray = (value: unknown): value is unknown[] => Array.isArray(value);

type ErrorResponse = {
    message: string;
    type: string;
    path: string;
    invalidValue: string | null;
};

export const isErrorResponse = (response: unknown): response is ErrorResponse => {
    return (
        isArray(response) &&
        typeof response[0] === 'object' &&
        response[0] != null &&
        'message' in response[0] &&
        'type' in response[0] &&
        'path' in response[0] &&
        'invalidValue' in response[0] &&
        typeof response[0].message === 'string'
    );
};

export const isValidOrderReference = (ref: Partial<OrderReference>) =>
    (ref.customerOrderNumber && ref.customerOrderPositionNumber) ||
    (!ref.customerOrderNumber && !ref.customerOrderPositionNumber && !ref.supplierOrderNumber);

export function groupBy<TItem>(arr: TItem[], callback: (item: TItem, index: number, array: TItem[]) => PropertyKey) {
    return arr.reduce((acc: Record<PropertyKey, TItem[]> = {}, ...args) => {
        const key = callback(...args);
        acc[key] ??= [];
        acc[key].push(args[0]);
        return acc;
    }, {});
}

/**
 * This type utility unwraps all properties of a given complex type to improve usability.
 */
export type Prettify<T> = {
    [K in keyof T]: T[K];
  // eslint-disable-next-line @typescript-eslint/ban-types
  } & {};
