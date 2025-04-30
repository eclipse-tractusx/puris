/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.DeliveryInformation;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.DeliveryRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/**
 * This class offers the endpoint for requesting the PlannedProduction Submodel 2.0.0
 */
@RestController
@RequestMapping("delivery-information")
@Slf4j
public class DeliveryRequestApiController {

    @Autowired
    private DeliveryRequestApiService deliveryRequestApiSrvice;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    private final Pattern urnPattern = PatternStore.URN_OR_UUID_PATTERN;


    @Operation(summary = "This endpoint receives the Delivery Information Submodel 2.0.0 requests. " +
        "This endpoint is meant to be accessed by partners via EDC only. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content),
        @ApiResponse(responseCode = "501", description = "Unsupported representation", content = @Content)
    })
    @GetMapping("request/{materialNumberCx}/submodel/{representation}")
    public ResponseEntity<DeliveryInformation> getDeliveryMapping(
        @RequestHeader("edc-bpn") String bpnl,
        @PathVariable String materialNumberCx,
        @PathVariable String representation
    ) {
        if (!bpnlPattern.matcher(bpnl).matches() || !urnPattern.matcher(materialNumberCx).matches()) {
            log.warn("Rejecting request at Delivery Information Submodel request 2.0.0 endpoint");
            return ResponseEntity.badRequest().build();
        }

        if (!"$value".equals(representation)) {
            log.warn("Rejecting request at Delivery Information Submodel request 2.0.0 endpoint, missing '$value' in request");
            if (!PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN.matcher(representation).matches()) {
                representation = "<REPLACED_INVALID_REPRESENTATION>";
            }
            log.warn("Received " + representation + " from " + bpnl);
            return ResponseEntity.status(501).build();
        }

        log.info("Received request for " + materialNumberCx + " from " + bpnl);
        var samm = deliveryRequestApiSrvice.handleDeliverySubmodelRequest(bpnl, materialNumberCx);
        if (samm == null) {
            log.error("SAMM for delivery is null, return 500.");
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(samm);
    }
}
