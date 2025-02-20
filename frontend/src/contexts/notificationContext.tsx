/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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

import { createContext, ReactNode, useContext, useState } from 'react';
import { Notification } from '@models/types/data/notification';
import { PageSnackbar, PageSnackbarStack } from '@catena-x/portal-shared-components';

type NotificationContext = {
    notify: (notification: Notification) => void;
};

const notificationContext = createContext<NotificationContext | null>(null);

type NotificationProviderProps = {
    children: ReactNode;
};

export const NotificationContextProvider = ({ children }: NotificationProviderProps) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const notify = (notification: Notification) => {
        setNotifications((ns) => [...ns, notification]);
    };
    return (
        <>
            <notificationContext.Provider value={{ notify }}>{children}</notificationContext.Provider>
            <PageSnackbarStack>
                {notifications.map((notification, index) => (
                    <PageSnackbar
                        key={index}
                        open={!!notification}
                        severity={notification?.severity}
                        title={notification?.title}
                        description={notification?.description}
                        autoClose={true}
                        onCloseNotification={() => setNotifications((ns) => ns.filter((_, i) => i !== index) ?? [])}
                    />
                ))}
            </PageSnackbarStack>
        </>
    );
};

export function useNotifications() {
    const context = useContext(notificationContext);
    if (context === null) {
        throw new Error('useNotifcations must be used within a NotificationContextProvider');
    }
    return context;
}
