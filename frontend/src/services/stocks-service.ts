/*
Copyright (c) 2023 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2023 Contributors to the Eclipse Foundation

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
import { Stock, StockType } from '@models/types/data/stock';
import { scheduleErpUpdate } from '@services/erp-service'
import { AssetType } from "@models/types/erp/assetType.ts";
import { DirectionType } from "@models/types/erp/directionType.ts";
import AuthenticationService from './authentication-service';

export const postStocks = async (type: StockType, stock: Partial<Stock>, mode: string) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_PRODUCT_STOCKS : config.app.ENDPOINT_MATERIAL_STOCKS;
  const res = await fetch(config.app.BACKEND_BASE_URL + endpoint, {
     method: mode == 'create' ? 'POST' : 'PUT',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${AuthenticationService.getToken()}`
    },
  });
  if(res.status >= 400) {
      throw await res.json();
  }
  return res.json();
}

export const putStocks = async (type: StockType, stock: Stock) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_PRODUCT_STOCKS : config.app.ENDPOINT_MATERIAL_STOCKS;
  const res = await fetch(config.app.BACKEND_BASE_URL + endpoint, {
    method: 'PUT',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${AuthenticationService.getToken()}`
    },
  });
  if(res.status >= 400) {
      throw await res.json();
  }
  return res.json();
}

export const deleteStocks = async (type: StockType, id: string) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_PRODUCT_STOCKS : config.app.ENDPOINT_MATERIAL_STOCKS;
  const res = await fetch(`${config.app.BACKEND_BASE_URL}${endpoint}/${id}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${AuthenticationService.getToken()}`
    },
  });
  if(res.status >= 400) {
      throw await res.json();
  }
  return ;
}

export const requestReportedStocks = async (type: StockType, materialNumber: string | null) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS : config.app.ENDPOINT_UPDATE_REPORTED_MATERIAL_STOCKS;
  if (materialNumber != null) {
    materialNumber = btoa(materialNumber);
  }
  const res = await fetch(`${config.app.BACKEND_BASE_URL}${endpoint}${materialNumber}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${AuthenticationService.getToken()}`
    },
  });
  if(res.status >= 400) {
      throw await res.json();
  }
  return res.json();
}

export const scheduleErpUpdateStocks = async (type: StockType, partnerBpnl: string | null, materialNumber: string | null): Promise<void> => {
    // assetType always ItemStock
    const assetType = AssetType.ItemStock;
    // infer product = OUTBOUND, material = INBOUND
    const direction = type === 'product' ? DirectionType.Outbound : DirectionType.Inbound;

    return scheduleErpUpdate(partnerBpnl, materialNumber, assetType, direction);
}
