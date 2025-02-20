/*
Copyright (c) 2022 Volkswagen AG
Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022 Contributors to the Eclipse Foundation

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

import ReactDOM from 'react-dom/client';
import './index.css';
import { RouterProvider } from 'react-router-dom';
import { router } from './router.tsx';
import { AuthContextProvider } from '@contexts/authContext.tsx';
import { NotificationContextProvider } from '@contexts/notificationContext.tsx';
import { TitleContextProvider } from '@contexts/titleProvider.tsx';
import { ThemeProvider } from '@mui/material';
import theme from './theme.tsx';

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <AuthContextProvider>
        <ThemeProvider theme={theme}>
            <TitleContextProvider>
                <NotificationContextProvider>
                    <RouterProvider router={router} />
                </NotificationContextProvider>
            </TitleContextProvider>
        </ThemeProvider>
    </AuthContextProvider>
);
