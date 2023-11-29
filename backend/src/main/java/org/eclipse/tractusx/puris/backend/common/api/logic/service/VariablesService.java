/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.common.api.logic.service;

import lombok.Getter;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Getter
@Service
public class VariablesService {

    @Value("${puris.apiversion}")
    private String purisApiVersion;

    @Value("${puris.demonstrator.role}")
    private String purisDemonstratorRole;

    @Value("${request.apiassetid}")
    private String requestApiAssetId;

    @Value("${response.apiassetid}")
    private String responseApiAssetId;

    @Value("${own.bpnl}")
    private String ownBpnl;

    @Value("${own.name}")
    private String ownName;
    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;
    @Value("${own.bpns}")
    private String ownDefaultBpns;
    @Value("${own.streetandnumber}")
    private String ownDefaultStreetAndNumber;
    @Value("${own.site.name}")
    private String ownDefaultSiteName;
    @Value("${own.bpna}")
    private String ownDefaultBpna;
    @Value("${own.zipcodeandcity}")
    private String ownDefaultZipCodeAndCity;
    @Value("${own.country}")
    private String ownDefaultCountry;
    @Value("${puris.api.key}")
    private String apiKey;


    /**
     * Returns the asset-id as defined in the properties file for the given api method
     * under request.apiassetid or response.apiassetid respectively.
     * @param method
     * @return the asset-id
     */
    public String getApiAssetId(DT_ApiMethodEnum method) {
        if(responseApiAssetId == null || requestApiAssetId == null) {
            throw new RuntimeException("You must define request.apiassetid and response.apiassetid in properties file");
        }
        switch (method) {
            case REQUEST: return requestApiAssetId;
            case RESPONSE: return responseApiAssetId;
            default: throw new RuntimeException("Unknown Api Method: " + method);
        }
    }
    
}
