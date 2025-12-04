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

import { config } from '@models/constants/config';
import AuthenticationService from './authentication-service';
import { Material } from '@models/types/data/stock';

export const getAllMaterials = async () => {
    
    const res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_ALL_MATERIALS}/all`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });
    if (res.status >= 400) {
        const error = await res.json();
        throw error;
    }
    return res.json();
}

export const postMaterial = async (material: Partial<Material>) => {
    const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_ALL_MATERIALS, {
        method: 'POST',
        body: JSON.stringify(material),
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });
    if (res.status >= 400) {
        const error = await res.json();
        throw error;
    }

    return res.json();
}

export const putMaterial = async (material: Partial<Material>) => {
    const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_ALL_MATERIALS, {
        method: 'PUT',
        body: JSON.stringify(material),
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });
    if (res.status >= 400) {
        const error = await res.json();
        throw error;
    }
    return res.json();
}