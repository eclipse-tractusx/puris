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

import { MaterialPartnerRelation } from '@models/types/data/material-partner-relation';
import AuthenticationService from './authentication-service';
import { config } from '@models/constants/config';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    Authorization: `Bearer ${AuthenticationService.getToken()}`,
});

export const getAllMaterialPartnerRelations = async (): Promise<MaterialPartnerRelation[]> => {
    let res: Response;
    try {
        res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_MATERIAL_PARTNER_RELATIONS}`, {
            method: 'GET',
            headers: getHeaders(),
        });
    } catch (error) {
        throw {
            message: 'Failed to fetch: ' + (error instanceof Error ? error.message : String(error)),
        };
    }

    if (!res.ok) {
        const errorText = await res.text();
        throw {
            status: res.status,
            message: errorText,
        };
    }

    return res.json();
};

export const postMaterialPartnerRelation = async (mpr: Partial<MaterialPartnerRelation>): Promise<MaterialPartnerRelation> => {
    let res: Response;
    try {
        res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_MATERIAL_PARTNER_RELATIONS}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(mpr),
        });
    } catch (error) {
        throw {
            message: 'Failed to fetch: ' + (error instanceof Error ? error.message : String(error)),
        };
    }

    if (!res.ok) {
        const errorText = await res.text();
        throw {
            status: res.status,
            message: errorText,
        };
    }

    return res.json();
};
