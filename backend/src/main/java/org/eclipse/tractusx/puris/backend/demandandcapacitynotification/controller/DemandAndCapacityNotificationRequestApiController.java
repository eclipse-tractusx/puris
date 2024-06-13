/*
 * Copyright (c) 2023, 2024 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.DemandAndCapacityNotificationSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.DemandAndCapacityNotifcationRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

@RestController
@RequestMapping("demand-and-capacity-notification")
@Slf4j
/**
 * This class offers the endpoint for requesting the DemandAndCapacityNotification Submodel 2.0.0
 */
public class DemandAndCapacityNotificationRequestApiController {

    @Autowired
    private DemandAndCapacityNotifcationRequestApiService demandAndCapacityNotificationRequestApiService;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    @Autowired
    private ObjectMapper objectMapper;

    @Operation(summary = "This endpoint receives the DemandAndCapacityNotification Submodel 2.0.0 requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("request/{materialnumbercx}/")
    public ResponseEntity<DemandAndCapacityNotificationSamm> postDemandAndCapacityNotification(
        @RequestHeader("edc-bpn") String bpnl,
        @RequestBody String body)
    {
        if (!bpnlPattern.matcher(bpnl).matches()) {
            log.warn("Rejecting request at DemandAndCapacityNotification Submodel request 2.0.0 endpoint. Invalid BPNL");
            return ResponseEntity.badRequest().build();
        }
        try {
            var data = objectMapper.readTree(body);
            log.info("Received POST request for DemandAndCapacityNotification Submodel 2.0.0 with BPNL: " + bpnl);
            var notification = objectMapper.readValue(
                data.get("content").get("demandAndCapacityNotification").toString(),
                DemandAndCapacityNotificationSamm.class);
            demandAndCapacityNotificationRequestApiService.handleNotificationSubmodelRequest(bpnl, notification);
        } catch (Exception e) {
            log.warn("Rejecting invalid request body at DemandAndCapacityNotification Submodel request 2.0.0 endpoint");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(null);
    }
}
