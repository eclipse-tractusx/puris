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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Address;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.AddressDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.SiteDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

@RestController
@RequestMapping("partners")
@Slf4j
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private Validator validator;
    private final ModelMapper modelMapper = new ModelMapper();

    @Autowired
    private MaterialPartnerRelationService mpr;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PostMapping
    @Operation(summary = "Creates a new Partner -- ADMIN ONLY", description =
        "Creates a new Partner entity with the data given in the request body. Please note that no " +
        "UUID can be assigned to a Partner that wasn't created before. So the request body must not contain a UUID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Partner created successfully."),
        @ApiResponse(responseCode = "400", description = "Request body was malformed, didn't meet the minimum constraints or wrongfully contained a UUID."),
        @ApiResponse(responseCode = "409", description = "The BPNL specified in the request body is already assigned. ")
    })
    public ResponseEntity<?> createPartner(@RequestBody PartnerDto partnerDto) {
        if(!validator.validate(partnerDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.badRequest().body("Invalid Partner.");
        }
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        // Any given UUID is wrong by default since we're creating a new Partner entity
        if (partnerDto.getUuid() != null) {
            return ResponseEntity.badRequest().body("Partner information must not contain a UUID when creating a new partner.");
        }

        if (partnerDto.getBpnl() == null) {
            return ResponseEntity.badRequest().body("Partner information is missing BPNL.");
        }

        try {
            Partner partnerEntity = modelMapper.map(partnerDto, Partner.class);
            Partner createdPartner = partnerService.create(partnerEntity);

            PartnerDto createdPartnerDto = modelMapper.map(createdPartner, PartnerDto.class);
            return ResponseEntity.ok(createdPartnerDto);
        } catch (KeyAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Partner already exists. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Partner is invalid.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PutMapping("putAddress")
    @Operation(summary = "Adds a new Address to a Partner -- ADMIN ONLY", description =
        "Updates an existing Partner by adding a new Address. If that Partner already has " +
        "an Address with the BPNA given in the request body, that existing Address will be overwritten. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update accepted."),
        @ApiResponse(responseCode = "400", description = "Invalid Address data or invalid partnerBpnl."),
        @ApiResponse(responseCode = "404", description = "Partner not found."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<?> addAddress(
        @Parameter(description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR") @RequestParam() String partnerBpnl,
        @RequestBody AddressDto address) {
        if(!validator.validate(address).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.badRequest().body("Invalid Address.");
        }
        if(!bpnlPattern.matcher(partnerBpnl).matches()) {
            return ResponseEntity.badRequest().body("Invalid partnerBpnl.");
        }
        Partner existingPartner = partnerService.findByBpnl(partnerBpnl);
        if (existingPartner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Partner not found.");
        }
        Address newAddress;
        try {
            newAddress = modelMapper.map(address, Address.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Address is invalid.");
        }
        // Remove operation in case there is an update of an existing address
        existingPartner.getAddresses().remove(newAddress);

        existingPartner.getAddresses().add(newAddress);
        try {
            Partner updatedPartner = partnerService.update(existingPartner);
            PartnerDto updatedPartnerDto = modelMapper.map(updatedPartner, PartnerDto.class);
            return ResponseEntity.ok(updatedPartnerDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Partner is invalid.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PutMapping("putSite")
    @Operation(summary = "Adds a new Site to a Partner -- ADMIN ONLY", description =
        "Updates an existing Partner by adding a new Site. If that Partner already has " +
        "a Site with the BPNS given in the request body, that existing Site will be overwritten. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update accepted."),
        @ApiResponse(responseCode = "400", description = "Invalid Address data or invalid partnerBpnl."),
        @ApiResponse(responseCode = "404", description = "Partner not found."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<?> addSite(
        @Parameter(description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR") @RequestParam() String partnerBpnl,
        @RequestBody SiteDto site) {
        if(!validator.validate(site).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.badRequest().body("Invalid Site.");
        }
        if(!bpnlPattern.matcher(partnerBpnl).matches()) {
            return ResponseEntity.badRequest().body("Invalid partnerBpnl.");
        }
        Partner existingPartner = partnerService.findByBpnl(partnerBpnl);
        if (existingPartner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Partner not found.");
        }
        Site newSite;
        try {
            newSite = modelMapper.map(site, Site.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Site is invalid.");
        }
        // Remove operation in case there is an update of an existing site
        existingPartner.getSites().remove(newSite);
        existingPartner.getSites().add(newSite);
        try {
            Partner updatedPartner = partnerService.update(existingPartner);
            PartnerDto updatedPartnerDto = modelMapper.map(updatedPartner, PartnerDto.class);
            return ResponseEntity.ok(updatedPartnerDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Partner is invalid.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Gets a specific Partner -- ADMIN ONLY", description = "Returns the requested PartnerDto.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found Partner, returning it in response body."),
        @ApiResponse(responseCode = "400", description = "Invalid parameter.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Requested Partner not found.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    public ResponseEntity<PartnerDto> getPartner(
        @Parameter(description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR") @RequestParam() String partnerBpnl) {
        if(!bpnlPattern.matcher(partnerBpnl).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner partner = partnerService.findByBpnl(partnerBpnl);
        if (partner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        try {
            PartnerDto partnerDto = modelMapper.map(partner, PartnerDto.class);
            return new ResponseEntity<>(partnerDto, HttpStatusCode.valueOf(200));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Gets all Partners", description = "Returns a list of all Partners. ")
    public ResponseEntity<List<PartnerDto>> listPartners() {
        final String ownBpnl = variablesService.getOwnBpnl();
        return new ResponseEntity<>(partnerService.findAll().stream()
            .filter(partner -> !partner.getBpnl().equals(ownBpnl))
            .map(partner -> modelMapper.map(partner, PartnerDto.class))
            .collect(Collectors.toList()),
            HttpStatusCode.valueOf(200));
    }

    @GetMapping("/own")
    @Operation(summary = "Gets the own Partner entity", description = "Returns the own partnr entity.")
    public ResponseEntity<Partner> getOwnPartnerEntity() {
        return new ResponseEntity<>(partnerService.getOwnPartnerEntity(), HttpStatus.OK);
    }

    @GetMapping("/ownSites")
    @Operation(summary = "Gets the own sites", description = "Returns all sites of the puris partner using the puris system.")
    public ResponseEntity<List<SiteDto>> getOwnSites() {
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();

        if (ownPartnerEntity == null || ownPartnerEntity.getSites() == null ||
                ownPartnerEntity.getSites().isEmpty()) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }

        return new ResponseEntity<>(partnerService.getOwnPartnerEntity().
            getSites().
            stream().map(site -> modelMapper.map(site, SiteDto.class)).collect(Collectors.toList()),
            HttpStatus.OK);
    }

    @GetMapping("{partnerBpnl}/materials")
    @Operation(summary = "Gets all associated materials for Partner", description =
        "Returns all materials the specified partner is associated with.")
    public ResponseEntity<List<Material>> getMaterials(@PathVariable String partnerBpnl) {
    if (!bpnlPattern.matcher(partnerBpnl).matches()) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    Partner partner = partnerService.findByBpnl(partnerBpnl);
    if (partner == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(mpr.findAll().stream()
                                .filter(rel -> rel.getPartner().equals(partner))
                                .map(rel -> rel.getMaterial()).collect(Collectors.toList()), HttpStatus.OK);
    }

}
