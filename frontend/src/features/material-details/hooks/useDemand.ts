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

import { useFetch } from '@hooks/useFetch'
import { config } from '@models/constants/config'
import { Demand } from '@models/types/data/demand';
import { BPNS } from '@models/types/edc/bpn';

export const useDemand = (materialNumber: string | null, site: BPNS | null) => {
  if (materialNumber != null) {
    materialNumber = btoa(materialNumber);
  }
  const params = new URLSearchParams();
  let url: string | undefined = undefined;
  if (materialNumber) {
    params.set('ownMaterialNumber', materialNumber);
    if (site) {
      params.set('site', site)
    }
    url = `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DEMAND}?${params.toString()}`;
  }
  const {data: demands, error: demandsError, isLoading: isLoadingDemands, refresh: refreshDemand } = useFetch<Demand[]>(url);
  return {
    demands,
    demandsError,
    isLoadingDemands,
    refreshDemand,
  };
}
