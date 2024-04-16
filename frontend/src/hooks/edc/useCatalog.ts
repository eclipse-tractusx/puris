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
import { RawCatalogData } from '@models/types/edc/catalog';
import { useFetch } from '../useFetch';
import { isErrorResponse } from '@util/helpers';

export const useCatalog = (edcUrl: string | null) => {
    const {
        data,
        error,
        isLoading: isLoadingCatalog,
    } = useFetch<RawCatalogData>(edcUrl ? config.app.BACKEND_BASE_URL + 'edc/catalog?dspUrl=' + encodeURIComponent(edcUrl) : undefined);
    const catalog = data && !isErrorResponse(data)
        ? (data['dcat:dataset']?.map((item) => {
              return {
                  assetId: item['@id'],
                  assetType: item['asset:prop:type'],
                  assetVersion: item['https://w3id.org/catenax/ontology/common#version'],
                  permission: item['odrl:hasPolicy'] && item['odrl:hasPolicy']['odrl:permission'],
                  prohibitions: item['odrl:hasPolicy'] && item['odrl:hasPolicy']['odrl:prohibitions'],
                  obligations: item['odrl:hasPolicy'] && item['odrl:hasPolicy']['odrl:obligations'],
              };
          }) ?? null)
        : null;
    const catalogError = error ?? (data && isErrorResponse(data) ? data : null);
    return {
        catalog,
        catalogError,
        isLoadingCatalog,
    };
};
