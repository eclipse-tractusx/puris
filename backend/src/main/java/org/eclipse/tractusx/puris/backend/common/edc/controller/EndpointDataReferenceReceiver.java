/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.common.edc.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * This class contains the endpoint for receiving the authCodes from
 * the counterparty's dataplane.
 */
@RestController
@Slf4j
public class EndpointDataReferenceReceiver {

    @Autowired
    private EndpointDataReferenceService edrService;

    private final Pattern basicPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;
    private final Pattern edcUrlPattern = PatternStore.URL_PATTERN;

    /**
     * This endpoint awaits incoming EDR Tokens from external
     * partners during a consumer pull transfer.
     *
     * @param body
     * @return Status code 200 if request body was found, otherwise 400
     */
    @PostMapping("/edrendpoint")
    @Operation(summary = "Endpoint for receiving the authCodes from the counterparty's connector",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {@ExampleObject(name = "EDR Token", value = sample)})))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Invalid message body")
    })
    @CrossOrigin
    private ResponseEntity<String> authCodeReceivingEndpoint(@RequestBody JsonNode body) {
        log.debug("Received edr data:\n" + body.toPrettyString());
        String transferId = body.get("id").asText();
        boolean valid = (transferId != null) && basicPattern.matcher(transferId).matches();

        String authKey = body.get("authKey").asText();
        valid = valid && (authKey != null) && basicPattern.matcher(authKey).matches();

        String authCode = body.get("authCode").asText();
        valid = valid && (authCode != null) && basicPattern.matcher(authCode).matches();

        String endpoint = body.get("endpoint").asText();
        valid = valid && (endpoint != null) && edcUrlPattern.matcher(endpoint).matches();

        if (!valid) {
            log.warn("EDR endpoint received invalid message:\n" + body.toPrettyString());
            return ResponseEntity.status(400).build();
        }

        edrService.save(transferId, new EDR_Dto(authKey, authCode, endpoint));
        log.debug("EDR endpoint stored authCode for " + transferId);
        return ResponseEntity.status(200).build();
    }

    private final static String sample = "{\"id\":\"3b603c3e-0f1a-4989-90b7-a4b024496d04\",\"at\":1699446240954,\"payload\":{\"transferProcessId\":\"e5c59912-b88a-4c42-9766-9fa593b72603\",\"callbackAddresses\":[{\"uri\":\"http://host.docker.internal:4000\",\"events\":[\"contract.negotiation\",\"transfer.process\"],\"transactional\":false,\"authKey\":null,\"authCodeId\":null}],\"dataAddress\":{\"properties\":{\"https://w3id.org/edc/v0.0.1/ns/type\":\"EDR\",\"https://w3id.org/edc/v0.0.1/ns/endpoint\":\"http://supplier-data-plane:9285/api/public/\",\"https://w3id.org/edc/v0.0.1/ns/authCode\":\"eyJhbGciOiJSUzI1NiJ9.eyJleHAiOjE2OTk0NDY4NDAsImRhZCI6ImMvMnFYVThaemVRTnJ5WDloUjhFZytONXpaWjhaUnc5dHpDOWRPcDlQNnpQQUhxZE9XN0pyN3JIWk94R0k0dFBXMmUxZVZ6elRJNlA5dUZXREpVVWtvRjJpWm93aXp6UEhmbGF4YTdsY0JIcWNISThhZWlEcm5MYiswM1ZXemRtMEZqLzNYYVdUa3VGYzM4VGtWS1lMaEViOVRNRUV4UGhqbEM5WFplamc0ZWJHNUZ6QldDQXZ2bmlMaWpFMjA4ZDhOcE80M0w5cG4xWXBpUThkZ2IrYjhkcEsyMDZObDNzTFhyS1hsU09ZaERHR2tLM2dSYkxBRHpiRjl3RWlCd3Z6SjFvSzFXbllzMVJwTVNOY1ZPc1ZseDJ6YS9mT1J6M29NYnh4TGdsSzMxU1c3NjVNWlVyWWlLK3JDOFhFaHNNa2JMSlNIcXhKYlFWYnZtL3dic1FyQWoxbUVsajhjbk9FY1p2NUhJOHJoUElyaTQyeU1hbXpWSXhXWW9hWU5PV0x4WHk1SUhZc3ZKcUJMc1cwaWs4eDlOZDhTcDduUGhqempLYjlQeFVVbCthS3BZQWVJaE9XZnNGT2pSMFFHM3lYcmJDalM0S1ZlaHhZbW1MZ0ZIczhyeWNsd1h2VUJzRkk4bzVLZUVwSll2U2RPSjlTQ0dNODB5a1lNczFxK0dTRVZmN2VrTUZRU2pjeGRSMElncUtFRk92OE5Ra3ZXZHAxbVpXc25IbnZNY2E0MFYxTm5nQWFVRndtRDhEeGVyc3k4R0IiLCJjaWQiOiJNUT09OlptVjBZMmhCYzNObGRBPT06T0dWbE4yWmhZelV0WlRjek15MDBPR0prTFRneFl6a3RaV1kzTkRsbFlXSTVNMll6In0.z9Nm_csmyHGBPGdEGgiyUV7pLWes0KE2IK82BHtCOS8XBerJrGb_wqNCgcgph6Zx7j84FwaVSH190FQ98FhJORgVCQ8u187hz1iPjXne9GEclR5Xr9_fSb9ZNK8VNTJvCdevJO5uT7Jkkc_-2U8DKUDDOj_Wqby8uStoSSs0P0idQ4pAazFYTy_Dbl0ltJsz6xc3YxwXk3yk0P1Ys5zYN0ueBznUMEJ6-YXpafAS5kn_iN8zU3It3Q2AgS0ER_M9AzeBHXZmST2MkaXXo3s_kuVxCZEtGRWkv8gmI3XZ5dprJ6x6keQSZ2ApSrxtmhswq2hPcqSQXF1gIFTTSSzg8A\",\"https://w3id.org/edc/v0.0.1/ns/id\":\"e5c59912-b88a-4c42-9766-9fa593b72603\",\"https://w3id.org/edc/v0.0.1/ns/authKey\":\"Authorization\"}}},\"type\":\"TransferProcessStarted\"}";

}
