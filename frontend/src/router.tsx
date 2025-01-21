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

import { createBrowserRouter, redirect } from 'react-router-dom';
import { Layout } from '@components/layout/Layout';
import { NotFoundView } from '@views/errors/NotFoundView';
import { CatalogView } from '@views/CatalogView';
import { NegotiationView } from '@views/NegotiationView';
import { TransferView } from '@views/TransferView';
import { StockView } from '@views/StockView';
import { DashboardView } from '@views/DashboardView';
import { AboutLicenseView } from '@views/AboutLicenseView';
import { UnauthorizedView } from '@views/errors/UnauthorizedView';
import { ErrorView } from '@views/errors/ErrorView';
import { RouteGuard } from '@components/RouteGuard';
import { UserGuideView } from '@views/UserGuideView';
import { DemandCapacityNotificationView } from '@views/DemandCapacityNotificationView';
import { MaterialListView } from '@views/MaterialListView';
import { MaterialDetailView } from '@views/MaterialDetailView';

export const router = createBrowserRouter([
    {
        element: <Layout />,
        errorElement: <ErrorView />,
        children: [
            {
                element: <RouteGuard roles={['PURIS_ADMIN', 'PURIS_USER']} />,
                errorElement: <ErrorView />,
                children: [
                    {
                        path: 'stocks',
                        element: <StockView />,
                    },
                    {
                        path: 'dashboard',
                        element: <DashboardView />,
                    },
                    {
                        path: 'notifications',
                        element: <DemandCapacityNotificationView />,
                    },
                    {
                        path: 'materials',
                        element: <MaterialListView />,
                    },
                    {
                        path: 'materials/:direction/:materialNumber',
                        element: <MaterialDetailView />,
                    }
                ],
            },
            {
                element: <RouteGuard roles={['PURIS_ADMIN']} />,
                errorElement: <ErrorView />,
                children: [
                    {
                        path: 'catalog',
                        element: <CatalogView />,
                    },
                    {
                        path: 'negotiations',
                        element: <NegotiationView />,
                    },
                    {
                        path: 'transfers',
                        element: <TransferView />,
                    },
                ],
            },
            {
                path: '/',
                loader: () => redirect('/materials'),
            },
            {
                path: 'user-guide',
                element: <UserGuideView />,
            },
            {
                path: 'aboutLicense',
                element: <AboutLicenseView />,
            },
            {
                path: 'unauthorized',
                element: <UnauthorizedView />,
            },
            {
                path: '*',
                element: <NotFoundView />,
            },
        ],
    },
]);
