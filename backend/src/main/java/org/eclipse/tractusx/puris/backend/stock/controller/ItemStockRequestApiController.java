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

package org.eclipse.tractusx.puris.backend.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("item-stock")
@Slf4j
/**
 * This class offers endpoints for the ItemStock-request and
 * -status-request api assets.
 */
public class ItemStockRequestApiController {

    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    private final Pattern urnPattern = PatternStore.URN_OR_UUID_PATTERN;


    @Operation(summary = "This endpoint receives the ItemStock Submodel 2.0.0 requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error"),
        @ApiResponse(responseCode = "501", description = "Unsupported representation")
    })
    @GetMapping("request/{materialnumber}/{direction}/{representation}")
    public ResponseEntity<ItemStockSamm> getMappingItemStock2(@RequestHeader("edc-bpn") String bpnl,
                                                              @PathVariable String materialnumber,
                                                              @PathVariable DirectionCharacteristic direction,
                                                              @PathVariable String representation) {
        if (!bpnlPattern.matcher(bpnl).matches() || !urnPattern.matcher(materialnumber).matches() || direction == null) {
            log.warn("Rejecting request at ItemStock Submodel request 2.0.0 endpoint");
            return ResponseEntity.badRequest().build();
        }
        if (!"$value".equals(representation)) {
            log.warn("Rejecting request at ItemStock Submodel request 2.0.0 endpoint, missing '@value' in request");
            if (!PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(representation).matches()) {
                representation = "<REPLACED_INVALID_REPRESENTATION>";
            }
            log.warn("Received " + representation + " from " + bpnl + " with direction " + direction);
            return ResponseEntity.status(501).build();
        }
        log.info("Received request for " + materialnumber + " with " + direction + " from " + bpnl);
        var samm = itemStockRequestApiService.handleItemStockSubmodelRequest(bpnl, materialnumber, direction);
        if (samm == null) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(samm);
    }
}
