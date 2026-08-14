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

import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { useCallback, useEffect, useState } from 'react';

export const useDemandCapacityNotifications = () => {
    const [notifications, setNotifications] = useState<DemandCapacityNotification[]>([]);
    const [isLoadingNotifications, setIsLoadingNotifications] = useState(true);

    const refreshNotifications = useCallback(async () => {
        setIsLoadingNotifications(true);
        try {
            const [incoming, outgoing] = await Promise.all([
                getDemandAndCapacityNotification(true),
                getDemandAndCapacityNotification(false),
            ]);
            setNotifications([...incoming, ...outgoing]);
        } catch (error) {
            console.error(error);
        } finally {
            setIsLoadingNotifications(false);
        }
    }, []);

    useEffect(() => {
        refreshNotifications();
    }, [refreshNotifications]);

    return { notifications, isLoadingNotifications, refreshNotifications };
};
