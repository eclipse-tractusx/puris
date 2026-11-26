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

import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@hooks/useAuth';
import { useLiveUpdate } from '@hooks/useLiveUpdate';
import { LIVE_UPDATE_TOPICS } from '@models/constants/live-update-topics';
import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { getDataExchangeRequestsWithApprovals } from '@services/data-exchange-service';
import { countOpenReceivedItems } from '../util/open-received-items';

const ALL_TOPICS = [LIVE_UPDATE_TOPICS.notifications, LIVE_UPDATE_TOPICS.dataExchangeRequests, LIVE_UPDATE_TOPICS.dataExchangeApprovals];

/**
 * Number of open notifications, data exchange requests and approvals received from partners.
 * Recounted whenever one of them changes.
 */
export const useOpenReceivedItemCount = () => {
    const { isAuthenticated } = useAuth();
    const [count, setCount] = useState(0);

    const refreshCount = useCallback(async () => {
        try {
            const [reportedNotifications, ownNotifications, { own, reported }] = await Promise.all([
                getDemandAndCapacityNotification(true),
                getDemandAndCapacityNotification(false),
                getDataExchangeRequestsWithApprovals(),
            ]);
            setCount(countOpenReceivedItems(reportedNotifications, [...reportedNotifications, ...ownNotifications], reported, own));
        } catch (error) {
            // keep the last known count, the next update or reload corrects it
            console.error(error);
        }
    }, []);

    useEffect(() => {
        if (isAuthenticated) refreshCount();
    }, [isAuthenticated, refreshCount]);

    useLiveUpdate(ALL_TOPICS, refreshCount);

    return count;
};
