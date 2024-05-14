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

import { useFetch } from '@hooks/useFetch';
import { config } from '@models/constants/config';
import { Delivery } from '@models/types/data/delivery';
import { BPNS } from '@models/types/edc/bpn';

export const useDelivery = (materialNumber: string | null, site: BPNS | null) => {
    const {
        data: deliveries,
        error: deliveriesError,
        isLoading: isLoadingDeliverys,
        refresh: refreshDelivery,
    } = useFetch<Delivery[]>(
        materialNumber && site
            ? `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DELIVERY}?ownMaterialNumber=${materialNumber}&site=${site}`
            : undefined
    );
    return {
        deliveries,
        deliveriesError,
        isLoadingDeliverys,
        refreshDelivery,
    };
};
