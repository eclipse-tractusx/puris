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
import { Partner } from '@models/types/edc/partner';

export const getAllPartners = async () => {
    
    const res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_ALL_PARTNERS}/all`, {
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

export const postPartner = async (partner: Partial<Partner>) => {
    const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_ALL_PARTNERS, {
        method: 'POST',
        body: JSON.stringify(partner),
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });
    if (res.status >= 400) {
        const errorText = await res.text();
        throw new Error(errorText);
    }

    return res.json();
}