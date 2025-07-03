/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { config } from '@models/constants/config';
import { AssetType } from '@models/types/erp/assetType';
import { DirectionType } from '@models/types/erp/directionType';
import AuthenticationService from './authentication-service';

const PARAM_BPNL = 'partner-bpnl';
const PARAM_MATERIAL_NUMBER = 'own-materialnumber';
const PARAM_ASSET_TYPE = 'asset-type';
const PARAM_DIRECTION = 'direction';

export const scheduleErpUpdate = async (partnerBpnl: string | null, materialNumber: string | null, type: AssetType, direction: DirectionType): Promise<void> => {
    // assetType always ItemStock
    if (type != AssetType.ItemStock) {
        throw new Error("The ERP Adapter currently only implements ItemStock, you tried " + AssetType.ItemStock);
    }
    if (materialNumber != null) {
        materialNumber = btoa(materialNumber)
    }
    const endpoint = config.app.ENDPOINT_ERP_SCHEDULE_UPDATE;
    const res = await fetch(`${config.app.BACKEND_BASE_URL}${endpoint}?${PARAM_BPNL}=${partnerBpnl}&${PARAM_MATERIAL_NUMBER}=${materialNumber}&${PARAM_ASSET_TYPE}=${type}&${PARAM_DIRECTION}=${direction}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });
    if(res.status >= 400) {
        throw await res.json();
    }
    return;
}
