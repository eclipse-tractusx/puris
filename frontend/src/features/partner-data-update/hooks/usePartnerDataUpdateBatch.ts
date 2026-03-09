/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
import AuthenticationService from '@services/authentication-service';
import { BatchRunDto, BatchRunEntryDto } from '@models/types/data/batch';
import { Pagination } from '@models/types/data/pagination';

export const usePartnerDataUpdateBatch = (runId?: string, page?: number, size?: number, sort?: string) => {
  const base = config.app.BACKEND_BASE_URL;
  const endpoint = config.app.ENDPOINT_PARTNER_DATA_UPDATE_BATCH;
  const runsUrl = `${base}${endpoint}?page=${page ?? 0}&size=${size ?? 20}${sort ? `&sort=${encodeURIComponent(sort)}` : ''}`;
  const entriesUrl = runId ? `${base}${endpoint}/${runId}/entries?page=${page ?? 0}&size=${size ?? 20}` : undefined;

  const { data: runs, error: runsError, isLoading: isLoadingRuns, refresh: refreshRuns } = useFetch<Pagination<BatchRunDto>>(runsUrl);
  const { data: entries, error: entriesError, isLoading: isLoadingEntries, refresh: refreshEntries } = useFetch<Pagination<BatchRunEntryDto>>(entriesUrl);

  const triggerManualBatch = async (): Promise<boolean> => {
    const token = AuthenticationService.getToken();
    try {
      const res = await fetch(`${base}${endpoint}/run`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });
      return res.ok;
    } catch (e) {
      return false;
    }
  };

  return {
    runs,
    runsError,
    isLoadingRuns,
    refreshRuns,
    entries,
    entriesError,
    isLoadingEntries,
    refreshEntries,
    triggerManualBatch,
  };
};

export default usePartnerDataUpdateBatch;
