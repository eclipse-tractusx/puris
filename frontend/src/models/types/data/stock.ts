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

import { UUID } from 'crypto';
import { BPNA, BPNS } from '../edc/bpn';
import { Partner } from '../edc/partner';
import { UnitOfMeasurementKey } from './uom';
import { OrderReference } from './order-reference';

export type Material = {
    uuid?: UUID | null;
    materialFlag: boolean;
    productFlag: boolean;
    ownMaterialNumber: string | null;
    materialNumberCustomer: string | null;
    materialNumberSupplier: string | null;
    materialNumberCx: string | null;
    name: string;
};

export type Stock = {
    uuid?: UUID | null;
    material: Material
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    stockLocationBpns: BPNS;
    stockLocationBpna: BPNA;
    lastUpdatedOn: string;
    partner: Partner;
    isBlocked: boolean;
} & OrderReference;

export type StockType = 'material' | 'product';
