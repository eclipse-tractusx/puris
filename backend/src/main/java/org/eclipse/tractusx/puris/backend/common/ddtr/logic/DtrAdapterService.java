/*
 * Copyright (c) 2024 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.common.ddtr.logic;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.ddtr.logic.util.DtrRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * A service that conducts HTTP-interactions with your decentralized Digital Twin Registry (dDTR).
 */
@Service
@Slf4j
public class DtrAdapterService {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DtrRequestBodyBuilder dtrRequestBodyBuilder;

    @Autowired
    private DigitalTwinMappingService digitalTwinMappingService;

    private Response sendDtrPostRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
            .post(body)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .build();
        return CLIENT.newCall(request).execute();
    }

    private Response sendDtrPutRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
            .put(body)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .build();
        return CLIENT.newCall(request).execute();
    }

    private Response sendDtrGetRequest(List<String> pathSegments, Map<String, String> headerData, Map<String, String> queryParams) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        if (queryParams != null) {
            for (var queryParam : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(queryParam.getKey(), queryParam.getValue());
            }
        }
        var requestBuilder = new Request.Builder().url(urlBuilder.build());
        if (headerData != null) {
            for (var header : headerData.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }
        return CLIENT.newCall(requestBuilder.build()).execute();
    }

    private String getAasForMaterial(String idAsBase64) {
        try (var response = sendDtrGetRequest(List.of("api", "v3.0", "shell-descriptors", idAsBase64), Map.of("Edc-Bpn", variablesService.getOwnBpnl()), Map.of())){
                var bodyString = response.body().string();
                log.info("Response Code " + response.code());
                if(response.isSuccessful()) {
                    return bodyString;
                }

        } catch (Exception e) {
            log.error("Failed to retrieve DTR from ");
        }
        return null;
    }

    /**
     * Call this method, when a MaterialPartnerRelation was created or updated via your MaterialPartnerRelationService
     * and it is about one of your products and a MaterialPartnerRelation involving a customer of this product.
     *
     * This method assumes that an entry for the respective Product was already created in your dDTR.
     *
     * @param materialPartnerRelation   The MaterialPartnerRelation
     * @return                          true, if the material's entry at the dDTR was successfully updated
     */
    public boolean updateProductForMaterialPartnerRelationWithCustomer(MaterialPartnerRelation materialPartnerRelation) {
        String twinId = digitalTwinMappingService.get(materialPartnerRelation.getMaterial()).getProductTwinId();
        String idAsBase64 = Base64.getEncoder().encodeToString(twinId.getBytes(StandardCharsets.UTF_8));
        var result = getAasForMaterial(idAsBase64);
        if (result == null) {
            return false;
        }
        try {
            var updatedBody = dtrRequestBodyBuilder.injectMaterialPartnerRelation(materialPartnerRelation, result);
            try (var response = sendDtrPutRequest(updatedBody, List.of("api", "v3.0", "shell-descriptors", idAsBase64))) {
                var bodyString = response.body().string();
                log.info("Response Code " + response.code());
                if(response.isSuccessful()) {
                    return true;
                }
                log.error("Failure in update for " + materialPartnerRelation + "\n" + bodyString);
            }
        } catch (Exception e) {
            log.error("Failure in update for " + materialPartnerRelation, e);
        }
        return false;
    }

    /**
     * Call this method when a new Material was created in your MaterialService, in order to
     * register this Material at your dDTR.
     * @param material  The Material
     * @return          true, if the registration was successful.
     */
    public boolean registerProductAtDtr(Material material) {
        String twinId = digitalTwinMappingService.get(material).getProductTwinId();
        var body = dtrRequestBodyBuilder.createProductRegistrationRequestBody(material, twinId);
        try (var response = sendDtrPostRequest(body, List.of("api", "v3.0", "shell-descriptors"))) {
            var bodyString = response.body().string();
            if (response.isSuccessful()) {
                return true;
            }
            log.error("Failed to register material at DTR " + material.getOwnMaterialNumber() + "\n" + bodyString);
            return false;
        } catch (Exception e) {
            log.error("Failed to register material at DTR " + material.getOwnMaterialNumber(), e);
            return false;
        }
    }
}
