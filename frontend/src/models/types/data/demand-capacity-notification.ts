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

import { BPNL } from "../edc/bpn";

export type LeadingRootCauseType = "strike" | "natural-disaster" | "production-incident" | "pandemic-or-epidemic" | "logistics-disruption" | "war" | "other";
export type EffectType = "demand-reduction" | "demand-increase" | "capacity-reduction" | "capacity-increase";
export type StatusType = "resolved" | "open";

export type DemandCapacityNotification = {
    uuid: string,
    notificationId: string,
    sourceDisruptionId: string,
    text: string,
    partnerBpnl: BPNL,
    affectedMaterialNumbers: string[],
    leadingRootCause: LeadingRootCauseType,
    effect: EffectType,
    status: StatusType,
    startDateOfEffect: Date,
    expectedEndDateOfEffect: Date,
    affectedSitesBpnsSender: string[], // own sites
    affectedSitesBpnsRecipient: string[], // partners sites
};
