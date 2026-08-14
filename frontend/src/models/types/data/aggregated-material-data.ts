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

import { UnitOfMeasurementKey } from './uom';
import { ArrivalType, DepartureType } from './delivery';

// Mirrors the flattened ReportedAnonymized* JPA entities returned by AggregatedMaterialDataNodeDto,
// not the nested DeliveryInformationAnonymized/ProductionInformationAnonymized DSP aspect models.
export type TransitEventType = DepartureType | ArrivalType;

export type AnonymizedDelivery = {
    uuid: string;
    aggregatedMaterialDataNodeId: string;
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    lastUpdatedOnDateTime: string;
    dateOfDeparture: string;
    dateOfArrival?: string;
    departureType: TransitEventType;
    arrivalType?: TransitEventType;
    originBpnsAnonymized: string;
    destinationBpnsAnonymized: string;
};

export type AnonymizedStock = {
    uuid: string;
    aggregatedMaterialDataNodeId: string;
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    stockLocationBpnsAnonymized: string;
    blocked: boolean;
    lastUpdatedOnDateTime: string;
};

export type AnonymizedProduction = {
    uuid: string;
    aggregatedMaterialDataNodeId: string;
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    productionSiteBpnsAnonymized: string;
    materialGlobalAssetIdAnonymized?: string;
    estimatedTimeOfCompletion: string;
    lastUpdatedOnDateTime: string;
};

export type AggregatedMaterialDataNode = {
    uuid: string;
    externalMaterialNumber: string;
    externalMaterialName: string;
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    productions: AnonymizedProduction[];
    deliveries: AnonymizedDelivery[];
    stocks: AnonymizedStock[];
    childMaterialData: AggregatedMaterialDataNode[];
};

export type AggregatedMaterialData = {
    uuid: string;
    ownMaterialNumber: string;
    childMaterialData: AggregatedMaterialDataNode[];
};
