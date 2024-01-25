/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.masterdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Address;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.AddressDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.SiteDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("partners")
@Slf4j
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private Validator validator;
    private final ModelMapper modelMapper = new ModelMapper();

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    @PostMapping
    @Operation(description = "Creates a new Partner entity with the data given in the request body. Please note that no " +
        "UUID can be assigned to a Partner that wasn't created before. So the request body must not contain a UUID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Partner created successfully."),
        @ApiResponse(responseCode = "400", description = "Request body was malformed, didn't meet the minimum constraints or wrongfully contained a UUID."),
        @ApiResponse(responseCode = "409", description = "The BPNL specified in the request body is already assigned. ")
    })
    public ResponseEntity<?> createPartner(@RequestBody PartnerDto partnerDto) {
        if(!validator.validate(partnerDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.status(400).build();
        }
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        // Any given UUID is wrong by default since we're creating a new Partner entity
        if (partnerDto.getUuid() != null || partnerDto.getBpnl() == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        // Check whether the given BPNL is already assigned
        Partner checkExistingPartner = partnerService.findByBpnl(partnerDto.getBpnl());
        if (checkExistingPartner != null) {
            // Cannot create Partner because BPNL is already assigned
            return new ResponseEntity<>(HttpStatusCode.valueOf(409));
        }

        Partner partnerEntity;
        try {
            partnerEntity = modelMapper.map(partnerDto, Partner.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        partnerEntity = partnerService.create(partnerEntity);
        if (partnerEntity == null) {
            // Creation failed due to unfulfilled constraints
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping("putAddress")
    @Operation(description = "Updates an existing Partner by adding a new Address. If that Partner already has " +
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
            return ResponseEntity.status(400).build();
        }
        if(!bpnlPattern.matcher(partnerBpnl).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner existingPartner = partnerService.findByBpnl(partnerBpnl);
        if (existingPartner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        Address newAddress;
        try {
            newAddress = modelMapper.map(address, Address.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
        // Remove operation in case there is an update of an existing address
        existingPartner.getAddresses().remove(newAddress);

        existingPartner.getAddresses().add(newAddress);
        existingPartner = partnerService.update(existingPartner);
        if (existingPartner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping("putSite")
    @Operation(description = "Updates an existing Partner by adding a new Site. If that Partner already has " +
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
            return ResponseEntity.status(400).build();
        }
        if(!bpnlPattern.matcher(partnerBpnl).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner existingPartner = partnerService.findByBpnl(partnerBpnl);
        if (existingPartner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        Site newSite;
        try {
            newSite = modelMapper.map(site, Site.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
        // Remove operation in case there is an update of an existing site
        existingPartner.getSites().remove(newSite);

        existingPartner.getSites().add(newSite);
        existingPartner = partnerService.update(existingPartner);
        if (existingPartner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @GetMapping
    @Operation(description = "Returns the requested PartnerDto.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found Partner, returning it in response body."),
        @ApiResponse(responseCode = "400", description = "Invalid parameter."),
        @ApiResponse(responseCode = "404", description = "Requested Partner not found."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
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
    @Operation(description = "Returns a list of all Partners. ")
    public ResponseEntity<List<PartnerDto>> listPartners() {
        return new ResponseEntity<>(partnerService.findAll().
            stream().map(partner -> modelMapper.map(partner, PartnerDto.class)).collect(Collectors.toList()),
            HttpStatusCode.valueOf(200));
    }

    @GetMapping("/ownSites")
    @Operation(description = "Returns all sites of the puris partner using the puris system.")
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

}
