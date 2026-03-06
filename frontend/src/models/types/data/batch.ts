/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
import { DirectionType } from '../erp/directionType';

export enum BatchRunStatus {
    IN_PROGRESS = "IN_PROGRESS",
    COMPLETED = "COMPLETED",
    COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS",
    FAILED = "FAILED"
}

export enum BatchRunEntryStatus {
    SUCCESS = "SUCCESS",
    ERROR = "ERROR",
    SKIPPED = "SKIPPED"
}

export enum InformationType {
    STOCK = 'STOCK',
    DEMAND = 'DEMAND',
    PRODUCTION = 'PRODUCTION',
    DAYS_OF_SUPPLY = 'DAYS_OF_SUPPLY',
    DELIVERY = 'DELIVERY',
}

export interface BatchRunDto {
    id: string;
    startTime: string;
    endTime?: string;
    status: BatchRunStatus;
    durationInSeconds: number;
    totalEntries: number;
    totalErrorCount: number;
}

export interface BatchRunEntryDto {
    id: string;
    ownMaterialNumber: string;
    partnerName: string;
    direction: DirectionType;
    informationType: InformationType;
    status: BatchRunEntryStatus;
    errorMessage?: string;
}

export default {};
