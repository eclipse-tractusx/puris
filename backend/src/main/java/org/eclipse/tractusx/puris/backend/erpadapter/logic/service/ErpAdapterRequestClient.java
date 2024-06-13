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

package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErpAdapterRequestClient {

    private final OkHttpClient client = new OkHttpClient();

    private final ObjectMapper mapper = new ObjectMapper();

    private final VariablesService variablesService;

    public Integer sendRequest(ErpAdapterRequest erpAdapterRequest){
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getErpAdapterUrl()).newBuilder();
        urlBuilder.addQueryParameter("bpnl", erpAdapterRequest.getPartnerBpnl());
        urlBuilder.addQueryParameter("request-type", erpAdapterRequest.getRequestType());
        urlBuilder.addQueryParameter("request-id", erpAdapterRequest.getId().toString());
        urlBuilder.addQueryParameter("samm-version", erpAdapterRequest.getSammVersion());
        urlBuilder.addQueryParameter("request-timestamp", String.valueOf(erpAdapterRequest.getRequestDate().getTime()));

        ObjectNode requestBody = mapper.createObjectNode();

        requestBody.put("material", erpAdapterRequest.getOwnMaterialNumber());
        requestBody.put("direction", erpAdapterRequest.getDirectionCharacteristic().toString());
        requestBody.put("responseUrl", variablesService.getErpResponseUrl());

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .post(body)
            .url(urlBuilder.build())
            .header(variablesService.getErpAdapterAuthKey(), variablesService.getErpAdapterAuthSecret())
            .header("Content-Type", "application/json")
            .build();
        try (var response = client.newCall(request).execute()) {
            return response.code();
        } catch (IOException e) {
            log.error("Error while sending ErpAdapterRequest", e);
            return null;
        }

    }
}
