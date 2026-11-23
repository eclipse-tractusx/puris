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

import { DataExchangeApproval } from '@models/types/data/data-exchange-approval';
import { DataExchangeRequest } from '@models/types/data/data-exchange-request';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';

export const getDataExchangeStatus = (dataExchangeRequest: DataExchangeRequest, demandCapacityNotification: DemandCapacityNotification, dataExchangeApproval: DataExchangeApproval | null): { label: string; explanation: string } => {
    if (demandCapacityNotification.status === 'resolved') {
        return { label: 'Terminated', explanation: 'The notification has been resolved' };
    }
    if (dataExchangeRequest.desiredEndDateTime && new Date(dataExchangeRequest.desiredEndDateTime) < new Date()) {
        return { label: 'Expired', explanation: `The request has ended on ${new Date(dataExchangeRequest.desiredEndDateTime).toLocaleString()}`};
    }
    if (dataExchangeApproval) {
        if (dataExchangeApproval.isFinalized === true) {
            return { label: 'Approved, finalized', explanation: 'The request has been approved and finalized' };
        } else if (dataExchangeApproval.isFinalized === false) {
           return { label: 'Approved, not finalized', explanation: 'The request has been approved, but one or more forwarded requests are pending approval.' };
        }
        return { label: 'Approved', explanation: 'The request has been approved' };
    }
    return { label: 'Pending', explanation: 'Data exchange request created, waiting for approval' };
};
