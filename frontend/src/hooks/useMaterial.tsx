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

import { Material } from '@models/types/data/stock';
import { useFetch } from './useFetch';
import { config } from '@models/constants/config';

export function useMaterial(materialNumber: string) {
    const params = new URLSearchParams();
    params.set('ownMaterialNumber', btoa(materialNumber));
    const { data, error, isLoading, refresh } = useFetch<Material>(config.app.BACKEND_BASE_URL + 'materials?' + params.toString());
    return {
        material: data,
        error,
        isLoading,
        refresh,
    };
}
