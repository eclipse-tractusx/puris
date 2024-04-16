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

import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { UnitOfMeasurementKey } from '@models/types/data/uom';

export const getUnitOfMeasurement = (unitOfMeasurementKey: UnitOfMeasurementKey) =>
    UNITS_OF_MEASUREMENT.find((uom) => uom.key === unitOfMeasurementKey)?.value;

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
 * 
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
