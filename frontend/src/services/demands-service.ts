/*
Copyright (c) 2023,2024 Volkswagen AG
Copyright (c) 2023,2024 Contributors to the Eclipse Foundation

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
import { Demand } from '@models/types/data/demand';
import { UUID } from 'crypto';

export const postDemand = async (demand: Partial<Demand>) => {
  const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_DEMAND, {
    method: 'POST',
    body: JSON.stringify(demand),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
  return res.json();
}

export const deleteDemand = async (id: UUID) => {
  const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_DEMAND + `/${id}`, {
    method: 'DELETE',
    headers: {
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
}

export const requestReportedDemands = async (materialNumber: string | null) => {
  const res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DEMAND}/reported/refresh?ownMaterialNumber=${materialNumber}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
  return res.json();
}
