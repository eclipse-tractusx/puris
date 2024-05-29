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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.erpadapter.logic.service.ItemStockErpAdapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(description = "This endpoint accepts responses from the ERP adapter. Incoming messages are expected to " +
        "carry a SAMM of the previously requested type. \n\nPlease note that this version currently accepts multiple responses " +
        "addressing the same request-id for testing purposes. However, in the near future, this will be enforced strictly. " +
        "I.e. only the first response for a given request-id will be accepted. All later responses addressing the same request-id" +
        " will be rejected (status code 409)\n\n" +
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
        @Parameter(example = "2024-05-28T15:00:00")
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date responseTimestamp,
        @RequestBody JsonNode requestBody
        ) {
        boolean valid = BPNL_PATTERN.matcher(partnerBpnl).matches();
        valid = valid && NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(responseType).matches();
        valid = valid && NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(sammVersion).matches();
        if (!valid) {
            return ResponseEntity.badRequest().build();
        }
        Dto dto = new Dto(requestId, partnerBpnl, responseType, sammVersion, responseTimestamp, requestBody);
        int responseCode = 501;
        switch (responseType) {
            case "ItemStock" -> responseCode = itemStockErpAdapterService.receiveItemStockUpdate(dto);
            default -> {
                return ResponseEntity.status(responseCode).body("Unsupported response type: " + responseType);
            }
        }
        return ResponseEntity.status(responseCode).build();
    }

    public record Dto(UUID requestId, String partnerBpnl, String responseType, String sammVersion,
                      Date responseTimeStamp, JsonNode body){}
}
