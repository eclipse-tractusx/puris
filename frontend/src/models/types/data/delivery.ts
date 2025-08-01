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
import { BPNA, BPNL, BPNS } from '../edc/bpn';
import { UnitOfMeasurementKey } from './uom';
import { OrderReference } from './order-reference';

export type ArrivalType = 'estimated-arrival' | 'actual-arrival';
export type DepartureType = 'estimated-departure' | 'actual-departure';

export type Delivery = {
  uuid?: UUID;
  ownMaterialNumber: string;
  quantity: number;
  measurementUnit: UnitOfMeasurementKey;
  trackingNumber: string;
  incoterm: string;
  partnerBpnl: BPNL;
  destinationBpns: BPNS;
  destinationBpna?: BPNA;
  originBpns: BPNS;
  originBpna?: BPNA;
  dateOfDeparture: Date;
  dateOfArrival: Date;
  departureType: DepartureType;
  arrivalType: ArrivalType;
  lastUpdatedOnDateTime: Date;
  reported: boolean;
} & OrderReference;
