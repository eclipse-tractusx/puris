/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.masterdata.controller;

import java.util.regex.Pattern;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.SingleLevelBomAsPlannedRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * This class offers the endpoint for requesting the SingleLevelBomAsPlanned
 * Submodel 1.0.0
 */
@RestController
@RequestMapping("single-level-bom-as-planned")
public class SingleLevelBomAsPlannedRequestApiController {

    static Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    static Pattern materialNumberPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @Autowired
    private SingleLevelBomAsPlannedRequestApiService singleLevelBomAsPlannedRequestApiService;

    @RequestMapping(value = "/**")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ResponseEntity<String> handleNotImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(description = "Endpoint that delivers SingleLevelBomAsPlanned of own products. "
            + "This endpoint is meant for self-access only via EDC. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters. ", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access forbidden - self-access only. ", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found for given parameters. ", content = @Content),
        @ApiResponse(responseCode = "501", description = "Unsupported representation requested. ", content = @Content)
    })
    @GetMapping("/request/{materialnumber}/submodel/{representation}")
    public ResponseEntity<SingleLevelBomAsPlanned> getSingleLevelBomAsPlannedMapping(@RequestHeader("edc-bpn") String bpnl,
            @Parameter(description = "The CatenaX material number (UUID) of the material in question") @PathVariable String materialnumber,
            @Parameter(description = "Must be set to '$value'") @PathVariable String representation) {
        if (!bpnlPattern.matcher(bpnl).matches() || !materialNumberPattern.matcher(materialnumber).matches()) {
            return ResponseEntity.badRequest().build();
        }

        if (!"$value".equals(representation)) {
            return ResponseEntity.status(501).build();
        }

        var samm = singleLevelBomAsPlannedRequestApiService.handleSingleLevelBomAsPlannedSubmodelRequest(bpnl, materialnumber);
        if (samm == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(samm);
    }
}
