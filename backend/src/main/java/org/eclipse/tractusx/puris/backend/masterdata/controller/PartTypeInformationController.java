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

package org.eclipse.tractusx.puris.backend.masterdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.PartTypeInformationSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

@RestController
@RequestMapping("parttypeinformation")
@Slf4j
public class PartTypeInformationController {
    static Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    static Pattern materialNumberPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private PartTypeInformationSammMapper sammMapper;

    @Operation(description = "Endpoint that delivers PartTypeInformation of own products to customer partners. " +
        "'materialnumber' must be set to the ownMaterialNumber of " +
        "the party, that receives the request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters. "),
        @ApiResponse(responseCode = "401", description = "Access forbidden. "),
        @ApiResponse(responseCode = "404", description = "Product not found for given parameters. "),
        @ApiResponse(responseCode = "501", description = "Unsupported representation requested. ")
    })
    @GetMapping("/{materialnumber}/{representation}")
    public ResponseEntity<?> getMapping(@RequestHeader("edc-bpn") String bpnl,
                                        @Parameter(description = "The material number that the request receiving party uses for the material in question")
                                        @PathVariable String materialnumber,
                                        @Parameter(description = "Must be set to '$value'") @PathVariable String representation) {
        materialnumber = new String (Base64.getDecoder().decode(materialnumber.getBytes(StandardCharsets.UTF_8)));
        if (!bpnlPattern.matcher(bpnl).matches() || !materialNumberPattern.matcher(materialnumber).matches()) {
            return ResponseEntity.badRequest().build();
        }

        if (!"$value".equals(representation)) {
            return ResponseEntity.status(501).build();
        }
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            return ResponseEntity.status(401).build();
        }
        log.info(bpnl + " requests part type information on " + materialnumber);
        Material material = materialService.findByOwnMaterialNumber(materialnumber);
        if (material == null || !material.isProductFlag()) {
            return ResponseEntity.status(404).build();
        }
        var mpr = mprService.find(material, partner);
        if (mpr == null || !mpr.isPartnerBuysMaterial()) {
            return ResponseEntity.status(404).build();
        }
        var samm = sammMapper.productToSamm(material);
        return ResponseEntity.ok(samm);
    }
}
