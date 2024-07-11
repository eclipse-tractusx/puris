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

package org.eclipse.tractusx.puris.backend.erpadapter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.erpadapter.logic.service.ErpAdapterTriggerService;
import org.eclipse.tractusx.puris.backend.erpadapter.logic.service.ItemStockErpAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.eclipse.tractusx.puris.backend.common.util.PatternStore.BPNL_PATTERN;
import static org.eclipse.tractusx.puris.backend.common.util.PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

@RestController
@RequestMapping("erp-adapter")
@Slf4j
public class ErpAdapterController {

    @Autowired
    private ItemStockErpAdapterService itemStockErpAdapterService;

    @Autowired
    private ErpAdapterTriggerService erpAdapterTriggerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Operation(description = "This endpoint is used to trigger scheduled updates from the ErpAdapter. This is useful " +
        "if you are expecting a specific request from a partner in the near future and want to make a best-effort attempt to ensure " +
        "that your PURIS backend has already obtained current data to respond to that expected request, when it arrives. \n\n" +
        "Please note, that calling this endpoint has no significant effect, if a request with the exact same specifics is already " +
        "currently in place. In that case, a call to this endpoint will only extend the period, after which the scheduled request will " +
        "be assumed to be irrelevant (see the puris.erpadapter.timelimit property and its documentation for details in this regard). ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "accepted"),
        @ApiResponse(responseCode = "400", description = "bad request")
    })
    @PostMapping("/trigger")
    public ResponseEntity<?> scheduleErpUpdate(
        @RequestParam("partner-bpnl") String bpnl,
        @RequestParam("own-materialnumber") String materialNumber,
        @RequestParam("asset-type") AssetType assetType,
        @RequestParam(required = false, value = "direction") DirectionCharacteristic directionCharacteristic
    ) {
        boolean valid = BPNL_PATTERN.matcher(bpnl).matches()
            && NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(materialNumber).matches();
        if (valid && mprService.find(bpnl, materialNumber) != null) {
            erpAdapterTriggerService.notifyPartnerRequest(bpnl, materialNumber, assetType, directionCharacteristic);
            return ResponseEntity.status(201).build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }



    @Operation(description = "This endpoint accepts responses from the ERP adapter. Incoming messages are expected to " +
        "carry a SAMM of the previously requested type. \n\n" +
        "Currently supported: \n\n" +
        "| response-type | samm-version |\n" +
        "|---------------|--------------|\n" +
        "| ItemStock     | 2.0          |"
        )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "accepted"),
        @ApiResponse(responseCode = "400", description = "bad request"),
        @ApiResponse(responseCode = "404", description = "unknown request-id"),
        @ApiResponse(responseCode = "409", description = "repeated answer for request-id"),
        @ApiResponse(responseCode = "500", description = "internal server error"),
        @ApiResponse(responseCode = "501", description = "unsupported response-type")
    })
    @PutMapping
    public ResponseEntity<?> putMethod(
        @RequestParam("request-id") UUID requestId,
        @RequestParam("bpnl") String partnerBpnl,
        @RequestParam("response-type") String responseType,
        @RequestParam("samm-version") String sammVersion,
        @RequestParam(value = "response-timestamp")
        @Parameter(example = "1719295545654", description = "Represented as the number of milliseconds since January 1, 1970, 00:00:00 GMT")
        long responseTimestamp,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {@Content(examples = {
            @ExampleObject(itemStock20Sample)
        })})
        @RequestBody JsonNode requestBody
        ) {
        boolean valid = BPNL_PATTERN.matcher(partnerBpnl).matches()
                     && NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(responseType).matches()
                     && NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(sammVersion).matches();
        if (!valid) {
            return ResponseEntity.badRequest().build();
        }
        Dto dto = new Dto(requestId, partnerBpnl, responseType, sammVersion, new Date(responseTimestamp), requestBody);
        AssetType assetType = Arrays.stream(AssetType.values()).filter(type -> type.ERP_KEYWORD.equals(responseType)).findFirst().orElse(null);
        int responseCode = 501;
        switch (assetType) {
            case ITEM_STOCK_SUBMODEL -> responseCode = itemStockErpAdapterService.receiveItemStockUpdate(dto);
            case null, default -> {
                return ResponseEntity.status(responseCode).body("Unsupported response type: " + responseType);
            }
        }
        return ResponseEntity.status(responseCode).build();
    }

    public record Dto(UUID requestId, String partnerBpnl, String responseType, String sammVersion,
                      Date responseTimeStamp, JsonNode body){}

    private final static String itemStock20Sample = "{\n" +
        "    \"materialGlobalAssetId\": null,\n" +
        "    \"positions\": [\n" +
        "        {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-01\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": false,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 22.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-03\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": false,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 66.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "                {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-02\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": true,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 44.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        }\n" +
        "    ],\n" +
        "    \"direction\": \"INBOUND\"\n" +
        "}";
}
