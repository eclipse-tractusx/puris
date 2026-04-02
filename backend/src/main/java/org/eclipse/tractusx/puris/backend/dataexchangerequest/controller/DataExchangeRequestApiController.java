/*
Copyright (c) 2026 Volkswagen AG

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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.controller;
import java.util.regex.Pattern;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.DataExchangeRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data-exchange-request")
@Slf4j
public class DataExchangeRequestApiController {
    @Autowired
    private DataExchangeRequestApiService dataExchangeRequestApiService;
    @Autowired
    private ObjectMapper objectMapper;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    
    @Operation(summary = "This endpoint receives the DataExchangeRequest 1.0.0 requests. " +
        "This endpoint is meant to be accessed by partners via EDC only. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok", content = @Content),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping("request")
    public ResponseEntity<?> postDataExchangeRequest(@RequestHeader("edc-bpn") String bpnl, @RequestBody JsonNode body)
    {
        if (!bpnlPattern.matcher(bpnl).matches()) {
            log.warn("Rejecting request at DataExchangeRequest request 1.0.0 endpoint. Invalid BPNL");
            return ResponseEntity.badRequest().build();
        }
        try {
            log.info("Received POST request for DataExchangeRequest");
            var request = objectMapper.readValue(
                body.get("content").toString(),
                DataExchangeRequestSamm.class);
            var result = dataExchangeRequestApiService.handleIncomingDataExchangeRequest(bpnl, request);
            if (result == null) {
                log.warn("Failed to create ReportedDataExchangeRequest from incoming request");
                return ResponseEntity.badRequest().build();
            }
            log.info("Created ReportedDataExchangeRequest from incoming request");
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            log.warn("Rejecting invalid request body at DataExchangeRequest request 1.0.0 endpoint");
            log.error("Error while processing incoming DataExchangeRequest", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
