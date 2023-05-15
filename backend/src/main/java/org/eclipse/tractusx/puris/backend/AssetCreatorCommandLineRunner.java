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
package org.eclipse.tractusx.puris.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.CreateAssetDto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EDCRequestBodyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AssetCreatorCommandLineRunner implements CommandLineRunner {

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Value("${request.serverendpoint}")
    private String requestApiBaseUrl;

    @Value("${response.serverendpoint}")
    private String responseApiBaseUrl;

    private ObjectMapper objectMapper;

    public AssetCreatorCommandLineRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {

        registerResponseAndRequestApiAsset();

    }

    private void registerResponseAndRequestApiAsset() {

        // Create Request Api Asset
        CreateAssetDto createRequestApiAssetDto =
                EDCRequestBodyBuilder.buildCreateAssetDtoForApi(DT_ApiMethodEnum.REQUEST,
                        requestApiBaseUrl);

        CreateAssetDto createResponseApiAssetDto =
                EDCRequestBodyBuilder.buildCreateAssetDtoForApi(DT_ApiMethodEnum.RESPONSE,
                        responseApiBaseUrl);

        try {
            edcAdapterService.publishAssetAtEDC(createResponseApiAssetDto);
            edcAdapterService.publishAssetAtEDC(createRequestApiAssetDto);
            log.info("Published sample RequestAndResponseAssetData");
        } catch (Exception e) {
            log.error("FAILED TO REGISTER REQUEST/RESPONSE ASSETS");
            log.error(e.getMessage());
        }

    }


}
