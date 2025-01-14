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

import { config } from '@models/constants/config';
import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { useFetch } from '@hooks/useFetch';
import { StockType } from '@models/types/data/stock';

export const useMaterials = (type: StockType) => {
    const endpoint = type === 'material' ? config.app.ENDPOINT_STOCK_VIEW_MATERIALS : config.app.ENDPOINT_PRODUCTS;
    const {
        data,
        error: materialsError,
        isLoading: isLoadingMaterials,
    } = useFetch<MaterialDescriptor[]>(config.app.BACKEND_BASE_URL + endpoint);
    const materials = data?.map((material) => ({...material, direction: type === 'material' ? 'inbound' : 'outbound'}));
    return {
        materials,
        materialsError,
        isLoadingMaterials,
    };
}
