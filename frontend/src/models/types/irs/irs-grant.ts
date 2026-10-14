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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/

export type IrsGrantSyncStatus = 'NOT_SYNCED' | 'PENDING' | 'SYNCED' | 'DELETED' | 'OUT_OF_SYNC';

export type IrsChainOpeningGrant = {
    uuid: string;
    globalAssetId: string;
    sourceDisruptionId: string;
    requesterBpn: string;
    validFrom: string | null;
    validTo: string | null;
    syncStatus: IrsGrantSyncStatus;
    allowedBpnlSet: string[] | null;
    reportedNotificationIds: string[] | null;
};
