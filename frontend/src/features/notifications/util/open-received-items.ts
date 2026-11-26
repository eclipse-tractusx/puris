/*
Copyright (c) 2026 Volkswagen AG

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

import { getDataExchangeStatus } from '@features/data-exchange/util/data-exchange-status';
import { DataExchangeRequest } from '@models/types/data/data-exchange-request';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';

/**
 * Counts the items received from partners that are still open, using the same status rules as
 * the notification view:
 * - reported notifications that are not resolved
 * - reported data exchange requests we have not answered yet (status "Pending")
 * - approvals reported for our own requests that are not finalized yet
 * Requests stop counting once their notification is resolved or the request has expired.
 */
export const countOpenReceivedItems = (
    reportedNotifications: DemandCapacityNotification[],
    allNotifications: DemandCapacityNotification[],
    reportedRequests: DataExchangeRequest[],
    ownRequests: DataExchangeRequest[]
) => {
    const statusOf = (request: DataExchangeRequest) => {
        const notification = allNotifications.find((n) => n.notificationId === request.notificationId);
        return notification ? getDataExchangeStatus(request, notification, request.dataExchangeApproval).label : null;
    };
    const openNotifications = reportedNotifications.filter((notification) => notification.status === 'open').length;
    const unansweredRequests = reportedRequests.filter((request) => statusOf(request) === 'Pending').length;
    const unfinalizedApprovals = ownRequests.filter((request) => statusOf(request) === 'Approved, not finalized').length;
    return openNotifications + unansweredRequests + unfinalizedApprovals;
};
