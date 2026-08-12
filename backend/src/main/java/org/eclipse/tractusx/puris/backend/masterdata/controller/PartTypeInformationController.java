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

package org.eclipse.tractusx.puris.backend.masterdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.PartTypeInformationSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.v1.PartTypeInformationLegacySAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.v2.PartTypeInformationSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

@RestController
@RequestMapping("parttypeinformation")
@Slf4j
public class PartTypeInformationController {
    /**
     * Url path segment of the PartTypeInformation 2.0.0 submodel. This is the version that is
     * used by default.
     */
    public static final String VERSION_2_0_0 = "2-0-0";
 
    /**
     * Url path segment of the PartTypeInformation 1.0.0 submodel. Only kept to stay
     * interoperable with partners that have not migrated yet.
     */
    public static final String VERSION_1_0_0 = "1-0-0";

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

    @RequestMapping(value = "/**")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ResponseEntity<String> handleNotImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(description = "Endpoint that delivers PartTypeInformation 2.0.0 of own products to customer partners. " +
        "'materialnumber' must be set to the ownMaterialNumber of the party, that receives the request. Please note that the " +
        "SAMMs delivered by this endpoint don't provide partClassification and partSitesInformationAsPlanned data. " +
        "This endpoint is meant to be accessed by partners via EDC only. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters. ", content = @Content),
        @ApiResponse(responseCode = "401", description = "Access forbidden. ", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found for given parameters. ", content = @Content),
        @ApiResponse(responseCode = "501", description = "Unsupported representation requested. ", content = @Content)
    })
    @GetMapping("/" + VERSION_2_0_0 + "/{materialnumber}/submodel/{representation}")
    public ResponseEntity<PartTypeInformationSAMM> getMapping(@RequestHeader("edc-bpn") String bpnl,
                                                              @Parameter(description = "The material number that the request receiving party uses for the material in question")
                                        @PathVariable String materialnumber,
                                                              @Parameter(description = "Must be set to '$value'") @PathVariable String representation) {
        var lookup = resolveProduct(bpnl, materialnumber, representation, VERSION_2_0_0);
        if (lookup.failed()) {
            return ResponseEntity.status(lookup.error()).build();
        }
        return ResponseEntity.ok(sammMapper.productToSamm(lookup.material()));
    }

    @Operation(description = "Endpoint that delivers PartTypeInformation 1.0.0 of own products to customer partners. " +
        "'materialnumber' must be set to the ownMaterialNumber of the party, that receives the request. Please note that the " +
        "SAMMs delivered by this endpoint don't provide partClassification and partSitesInformationAsPlanned data. " +
        "This endpoint is meant to be accessed by partners via EDC only. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters. ", content = @Content),
        @ApiResponse(responseCode = "401", description = "Access forbidden. ", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found for given parameters. ", content = @Content),
        @ApiResponse(responseCode = "501", description = "Unsupported representation requested. ", content = @Content)
    })
    @GetMapping("/" + VERSION_1_0_0 + "/{materialnumber}/submodel/{representation}")
    public ResponseEntity<PartTypeInformationLegacySAMM> getLegacyMapping(@RequestHeader("edc-bpn") String bpnl,
                                                              @Parameter(description = "The material number that the request receiving party uses for the material in question")
                                        @PathVariable String materialnumber,
                                                              @Parameter(description = "Must be set to '$value'") @PathVariable String representation) {
        var lookup = resolveProduct(bpnl, materialnumber, representation, VERSION_1_0_0);
        if (lookup.failed()) {
            return ResponseEntity.status(lookup.error()).build();
        }
        return ResponseEntity.ok(sammMapper.productToLegacySamm(lookup.material()));
    }

    /**
     * Validates the request parameters and looks up the requested product.
     *
     * @param bpnl              the bpnl of the requesting partner, taken from the edc-bpn header
     * @param materialnumber    the base64 encoded ownMaterialNumber of the requested product
     * @param representation    must be '$value'
     * @param version           the requested submodel version, for logging purposes only
     * @return                  the resolved product or the status to be returned to the partner
     */
    private ProductLookup resolveProduct(String bpnl, String materialnumber, String representation, String version) {
        materialnumber = new String(Base64.getDecoder().decode(materialnumber.getBytes(StandardCharsets.UTF_8)));
        if (!bpnlPattern.matcher(bpnl).matches() || !materialNumberPattern.matcher(materialnumber).matches()) {
            return ProductLookup.error(HttpStatus.BAD_REQUEST);
        }
 
        if (!"$value".equals(representation)) {
            return ProductLookup.error(HttpStatus.NOT_IMPLEMENTED);
        }
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            return ProductLookup.error(HttpStatus.UNAUTHORIZED);
        }
        log.info("{} requests part type information {} on {}", bpnl, version, materialnumber);
        Material material = materialService.findByOwnMaterialNumber(materialnumber);
        if (material == null || !material.isProductFlag()) {
            return ProductLookup.error(HttpStatus.NOT_FOUND);
        }
        var mpr = mprService.find(material, partner);
        if (mpr == null || !mpr.isPartnerBuysMaterial()) {
            return ProductLookup.error(HttpStatus.NOT_FOUND);
        }
        return ProductLookup.found(material);
    }

    private record ProductLookup(Material material, HttpStatus error) {
 
        private static ProductLookup error(HttpStatus status) {
            return new ProductLookup(null, status);
        }
 
        private static ProductLookup found(Material material) {
            return new ProductLookup(material, null);
        }
 
        private boolean failed() {
            return error != null;
        }
    }
}
