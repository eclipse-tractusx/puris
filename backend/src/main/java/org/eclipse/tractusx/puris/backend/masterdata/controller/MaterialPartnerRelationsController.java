/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelationDto;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("materialpartnerrelations")
@Slf4j
public class MaterialPartnerRelationsController {


    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    private final Pattern materialPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    private final Pattern bpnsPattern = PatternStore.BPNS_PATTERN;

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @GetMapping
    @Operation(summary = "Get all Material Partner Relations -- ADMIN ONLY", description =
        "Returns a list of all MaterialPartnerRelations as DTOs.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all MaterialPartnerRelations."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<List<MaterialPartnerRelationDto>> getAllMaterialPartnerRelations() {
        try {
            List<MaterialPartnerRelation> allRelations = mprService.findAll();
            List<MaterialPartnerRelationDto> dtos = allRelations.stream()
                .map(mpr -> new MaterialPartnerRelationDto(
                    mpr.getMaterial().getOwnMaterialNumber(),
                    mpr.getPartner().getBpnl(),
                    mpr.getPartnerMaterialNumber(),
                    mpr.isPartnerSuppliesMaterial(),
                    mpr.isPartnerBuysMaterial(),
                    mpr.getOwnProducingSites().stream().map(Site::getBpns).collect(Collectors.toList()),
                    mpr.getOwnDemandingSites().stream().map(Site::getBpns).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving all material partner relations", e);
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
    }

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PostMapping
    @Operation(summary = "Creates a Material Partner Relation -- ADMIN ONLY", description =
        "Creates a new MaterialPartnerRelation with the given parameter data. " +
        "Please note that this is only possible, if the designated Material " +
        "and Partner entities have already been created before this request. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a new MaterialPartnerRelationEntity."),
        @ApiResponse(responseCode = "400", description = "Material and/or Partner do not exist or invalid parameters"),
        @ApiResponse(responseCode = "409", description = "Relation for given Material and Partner does already exist."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<?> createMaterialPartnerRelation(@RequestBody MaterialPartnerRelationDto dto) {
        if (!materialPattern.matcher(dto.getOwnMaterialNumber()).matches() || !materialPattern.matcher(dto.getPartnerMaterialNumber()).matches()
            || !bpnlPattern.matcher(dto.getPartnerBpnl()).matches()) {
            log.warn("Rejected message parameters. ");
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        Material material = materialService.findByOwnMaterialNumber(dto.getOwnMaterialNumber());
        if (material == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner partner = partnerService.findByBpnl(dto.getPartnerBpnl());
        if (partner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner ownPartner = partnerService.getOwnPartnerEntity();
        SortedSet<Site> producingSites = new TreeSet<>();
        if (dto.getOwnProducingSiteBpnss() != null) {
            for (var siteBpns : dto.getOwnProducingSiteBpnss()) {
                if (!bpnsPattern.matcher(siteBpns).matches()) {
                    log.warn("Invalid producing site BPNS format.");
                    return new ResponseEntity<>(HttpStatusCode.valueOf(400));
                }
                Site site = ownPartner.getSites().stream()
                    .filter(s -> s.getBpns().contains(siteBpns))
                    .findFirst()
                    .orElse(null);
                if (site == null) {
                    log.warn("Invalid producing site. Own site with BPNS {} not found.", siteBpns);
                    return new ResponseEntity<>(HttpStatusCode.valueOf(400));
                }
                producingSites.add(site);
            }
        }
        SortedSet<Site> demandingSites = new TreeSet<>();
        if (dto.getOwnDemandingSiteBpnss() != null) {
            for (var siteBpns : dto.getOwnDemandingSiteBpnss()) {
                if (!bpnsPattern.matcher(siteBpns).matches()) {
                    log.warn("Invalid demanding site BPNS format.");
                    return new ResponseEntity<>(HttpStatusCode.valueOf(400));
                }
                Site site = ownPartner.getSites().stream()
                    .filter(s -> s.getBpns().contains(siteBpns))
                    .findFirst()
                    .orElse(null);
                if (site == null) {
                    log.warn("Invalid demanding site. Own site with BPNS {} not found.", siteBpns);
                    return new ResponseEntity<>(HttpStatusCode.valueOf(400));
                }
                demandingSites.add(site);
            }
        }
        if (mprService.find(material, partner) != null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(409));
        }
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, dto.getPartnerMaterialNumber(), dto.isPartnerSuppliesMaterial(), dto.isPartnerBuysMaterial(), producingSites, demandingSites);

        newMpr = mprService.create(newMpr);
        if (newMpr == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }

        return new ResponseEntity<>(newMpr, HttpStatusCode.valueOf(200));
    }

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PutMapping
    @Operation(summary = "Updates a Material Partner Relation -- ADMIN ONLY", description =
        "Updates an existing MaterialPartnerRelation. You have to specify the ownMaterialNumber and " +
        "the partnerBpnl. The other three parameters are genuinely optional. Provide them only if you want to change their values. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update was accepted."),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "404", description = "No existing entity was found."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<?> updateMaterialPartnerRelation(
        @Parameter(description = "The Material Number that is used in your own company to identify the Material, " +
            "encoded in base64") @RequestParam String ownMaterialNumber,
        @Parameter(description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR") @RequestParam() String partnerBpnl,
        @Parameter(description = "The Material Number that this Partner is using in his own company to identify the Material, "
            + "encoded in base64") @RequestParam(required = false) String partnerMaterialNumber,
        @Parameter(description = "The CatenaX Number that this Partner uses",
            example = "860fb504-b884-4009-9313-c6fb6cdc776b") @RequestParam(required = false) String partnerCXNumber,
        @Parameter(description = "The informal name that this Partner uses",
            example = "Semiconductor") @RequestParam(required = false) String nameAtManufacturer,
        @Parameter(description = "This boolean flag indicates whether this Partner is a potential supplier of the given Material.",
            example = "true") @RequestParam(required = false) Boolean partnerSupplies,
        @Parameter(description = "This boolean flag indicates whether this Partner is a potential customer of this Material.",
            example = "true") @RequestParam(required = false) Boolean partnerBuys) {
        try {
            ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
            partnerMaterialNumber = new String(Base64.getDecoder().decode(partnerMaterialNumber));
        } catch (Exception e) {
            log.error("parameters were not properly encoded in base64");
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        MaterialPartnerRelation existingRelation = null;
        if (!bpnlPattern.matcher(partnerBpnl).matches() || !materialPattern.matcher(ownMaterialNumber).matches() ||
            (partnerMaterialNumber != null && !materialPattern.matcher(partnerMaterialNumber).matches())) {
            log.warn("Rejected message parameters. ");
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner partner = partnerService.findByBpnl(partnerBpnl);

        Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (partner != null && material != null) {
            existingRelation = mprService.find(material, partner);
        }
        if (existingRelation == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        if (partnerSupplies != null) {
            existingRelation.setPartnerSuppliesMaterial(partnerSupplies);
        }
        if (partnerBuys != null) {
            existingRelation.setPartnerBuysMaterial(partnerBuys);
        }
        if (partnerMaterialNumber != null) {
            existingRelation.setPartnerMaterialNumber(partnerMaterialNumber);
        }
        if (nameAtManufacturer != null) {
            existingRelation.setNameAtManufacturer(nameAtManufacturer);
        }
        if (partnerCXNumber != null) {
            existingRelation.setPartnerCXNumber(partnerCXNumber);
        }
        existingRelation = mprService.update(existingRelation);
        if (existingRelation == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

}
