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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

    @Operation(summary = "This endpoint receives the DemandAndCapacityNotification 2.0.0 requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("request")
    public ResponseEntity<DemandAndCapacityNotificationSamm> postDemandAndCapacityNotification(
        @RequestHeader("edc-bpn") String bpnl,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {@Content(examples = {
            @ExampleObject(sample)
        })})
        @RequestBody JsonNode body)
    {
        if (!bpnlPattern.matcher(bpnl).matches()) {
            log.warn("Rejecting request at DemandAndCapacityNotification request 2.0.0 endpoint. Invalid BPNL");
            return ResponseEntity.badRequest().build();
        }
        try {
            log.info("Received POST request for DemandAndCapacityNotification 2.0.0 with BPNL: " + bpnl);
            var notification = objectMapper.readValue(
                body.get("content").get("demandAndCapacityNotification").toString(),
                DemandAndCapacityNotificationSamm.class);
            demandAndCapacityNotificationRequestApiService.handleIncomingNotification(bpnl, notification);
        } catch (Exception e) {
            log.warn("Rejecting invalid request body at DemandAndCapacityNotification request 2.0.0 endpoint");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(null);
    }

    final static String sample = "{\n" +
        "  \"header\": {\n" +
        "    \"senderBpn\": \"BPNL7588787849VQ\",\n" +
        "    \"context\": \"CX-DemandAndCapacityNotification:2.0\",\n" +
        "    \"messageId\": \"3b4edc05-e214-47a1-b0c2-1d831cdd9ba9\",\n" +
        "    \"receiverBpn\": \"BPNL6666787765VQ\",\n" +
        "    \"sentDateTime\": \"2023-06-19T21:24:00+07:00\",\n" +
        "    \"version\": \"3.0.0\"\n" +
        "  },\n" +
        "  \"content\": {\n" +
        "    \"demandAndCapacityNotification\": {\n" +
        "      \"affectedSitesSender\": [\n" +
        "        \"BPNS7588787849VQ\"\n" +
        "      ],\n" +
        "      \"affectedSitesRecipient\": [\n" +
        "        \"BPNS6666787765VQ\"\n" +
        "      ],\n" +
        "      \"materialNumberSupplier\": [\n" +
        "        \"MNR-8101-ID146955.001\"\n" +
        "      ],\n" +
        "      \"contentChangedAt\": \"2023-12-13T15:00:00+01:00\",\n" +
        "      \"startDateOfEffect\": \"2023-12-13T15:00:00+01:00\",\n" +
        "      \"relatedNotificationId\": \"urn:uuid:d05cef4a-b692-45bf-87cc-eda2d84e4c04\",\n" +
        "      \"materialNumberCustomer\": [\n" +
        "        \"MNR-7307-AU340474.002\"\n" +
        "      ],\n" +
        "      \"leadingRootCause\": \"strike\",\n" +
        "      \"materialGlobalAssetId\": [\n" +
        "        \"urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df\"\n" +
        "      ],\n" +
        "      \"effect\": \"demand-reduction\",\n" +
        "      \"notificationId\": \"urn:uuid:d9452f24-3bf3-4134-b3eb-68858f1b2362\",\n" +
        "      \"text\": \"Capacity reduction due to ongoing strike.\",\n" +
        "      \"expectedEndDateOfEffect\": \"2023-12-17T08:00:00+01:00\",\n" +
        "      \"sourceNotificationId\": \"urn:uuid:c69cb3e4-16ad-43c3-82b9-0deac75ecf9e\",\n" +
        "      \"status\": \"resolved\"\n" +
        "    }\n" +
        "  }\n" +
        "}";
}
