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
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.ddtr.logic.util.DtrRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.security.OAuth2ClientInterceptor;
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
    private OkHttpClient CLIENT;

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DtrRequestBodyBuilder dtrRequestBodyBuilder;

    @Autowired
    private DigitalTwinMappingService digitalTwinMappingService;

    @Autowired(required = false)
    public DtrAdapterService(@Nullable OAuth2ClientInterceptor oAuth2ClientInterceptor) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // add client interceptor if enabled by property
        if (oAuth2ClientInterceptor != null) {
            clientBuilder.addInterceptor(oAuth2ClientInterceptor);
        }

        this.CLIENT = clientBuilder.build();
    }

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

    /**
     * Updates an existing product-AAS with all corresponding customer partners.
     *
     * @param material The given Material
     * @param mprs     The list of all MaterialProductRelations that exist with customers of the given Material
     * @return The HTTP response code from the DTR, or null if none was received
     */
    public Integer updateProduct(Material material, List<MaterialPartnerRelation> mprs) {
        String twinId = digitalTwinMappingService.get(material).getProductTwinId();
        String idAsBase64 = Base64.getEncoder().encodeToString(twinId.getBytes(StandardCharsets.UTF_8));
        var body = dtrRequestBodyBuilder.createProductRegistrationRequestBody(material, twinId, mprs);
        try (var response = sendDtrPutRequest(body, List.of("api", "v3", "shell-descriptors", idAsBase64))) {
            return response.code();
        } catch (Exception e) {
            log.error("Failure in update for product twin " + material.getOwnMaterialNumber(), e);
        }
        return null;
    }

    /**
     * Call this method when a new Material with a product flag was created in your MaterialService - or if a product
     * flag was later added to an existing Material.
     * <p>
     * A new AAS will be registered for this Material at your dDTR.
     *
     * @param material The Material
     * @return The HTTP response code from the DTR, or null if none was received
     */
    public Integer registerProductAtDtr(Material material) {
        String twinId = digitalTwinMappingService.get(material).getProductTwinId();
        var body = dtrRequestBodyBuilder.createProductRegistrationRequestBody(material, twinId, List.of());
        try (var response = sendDtrPostRequest(body, List.of("api", "v3", "shell-descriptors"))) {
            return response.code();
        } catch (Exception e) {
            log.error("Failed to register product at DTR " + material.getOwnMaterialNumber(), e);
        }
        return null;
    }

    /**
     * Call this method when a MaterialPartnerRelation was created or updated it's flag signals that this partner is
     * a supplier for the referenced Material.
     *
     * @param supplierPartnerRelation The MaterialPartnerRelation indicating a supplier for a given Material.
     * @return The HTTP response code from the DTR, or null if none was received
     */
    public Integer registerMaterialAtDtr(MaterialPartnerRelation supplierPartnerRelation) {
        var body = dtrRequestBodyBuilder.createMaterialRegistrationRequestBody(supplierPartnerRelation);
        try (var response = sendDtrPostRequest(body, List.of("api", "v3", "shell-descriptors"))) {
            return response.code();
        } catch (Exception e) {
            log.error("Failed to register material at DTR " + supplierPartnerRelation.getMaterial().getOwnMaterialNumber(), e);
        }
        return null;
    }

    /**
     * Updates an existing material-AAS with the Information from the given MaterialPartnerRelation
     *
     * @param supplierPartnerRelation The MPR that indicates the material and the partner
     * @return The HTTP response code from the DTR, or null if none was received
     */
    public Integer updateMaterialAtDtr(MaterialPartnerRelation supplierPartnerRelation) {
        var body = dtrRequestBodyBuilder.createMaterialRegistrationRequestBody(supplierPartnerRelation);
        String idAsBase64 = Base64.getEncoder().encodeToString(supplierPartnerRelation.getPartnerCXNumber().getBytes(StandardCharsets.UTF_8));
        try (var response = sendDtrPutRequest(body, List.of("api", "v3", "shell-descriptors", idAsBase64))) {
            return response.code();
        } catch (Exception e) {
            log.error("Failed to register material at DTR " + supplierPartnerRelation.getMaterial().getOwnMaterialNumber(), e);
        }
        return null;
    }
}
