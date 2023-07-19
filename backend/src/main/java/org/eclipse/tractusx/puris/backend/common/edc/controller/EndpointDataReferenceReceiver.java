/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.edc.controller;

import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

/**
 * This class contains the endpoint for receiving the authCodes from
 * the counterparty's dataplane. 
 */
@RestController
@Slf4j
public class EndpointDataReferenceReceiver {

    @Autowired
    private EndpointDataReferenceService edcService;

    /**
     * This endpoint awaits incoming authCodes from external
     * partners during a consumer pull transfer. 
     * @param body
     * @return
     */
    @PostMapping("/edrendpoint")
    private ResponseEntity<String> authCodeReceivingEndpoint(@RequestBody JsonNode body) {
        log.debug("Received edr data:\n" + body.toPrettyString());
        String transferId = body.get("id").asText(); 
        String authKey = body.get("authKey").asText();
        String authCode = body.get("authCode").asText();
        String endpoint = body.get("endpoint").asText();
        if (transferId == null || authCode == null) {
            log.warn("authCodes endpoint received invalid message:\n" + body.toPrettyString());
            return ResponseEntity.status(400).build();
        }
        edcService.save(transferId, new EDR_Dto(authKey, authCode, endpoint));
        log.debug("authCodes endpoint stored authCode for " + transferId);
        return ResponseEntity.status(200).build();
    }

}