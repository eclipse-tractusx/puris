/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.demand.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.DemandDto;
import org.eclipse.tractusx.puris.backend.demand.logic.services.DemandRequestApiService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.ReportedDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("demand")
public class DemandController {
    @Autowired
    private OwnDemandService ownDemandService;

    @Autowired
    private ReportedDemandService reportedDemandService;

    @Autowired
    private DemandRequestApiService demandRequestApiService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Validator validator;

    private final Pattern materialPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @Autowired
    private ExecutorService executorService;

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all own demands for the given Material", description = "Get all own demands for the given material number. Optionally the demanding site can be filtered by its bpns.")
    public List<DemandDto> getAllDemands(@Parameter(description = "encoded in base64") String ownMaterialNumber, Optional<String> site) {
        ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        return ownDemandService.findAllByFilters(Optional.of(ownMaterialNumber), Optional.empty(), site, Optional.empty())
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @PostMapping()
    @ResponseBody
    @Operation(summary = "Creates a new demand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Demand was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Demand already exists. Use PUT instead.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DemandDto createDemand(@RequestBody DemandDto demandDto) {
        if (!validator.validate(demandDto).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Demand Information");
        }

        if (demandDto.getOwnMaterialNumber().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Demand Information misses material identification.");
        }

        if (demandDto.getPartnerBpnl() == null || demandDto.getPartnerBpnl().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Demand Information misses partner identification.");
        }

        try {
            var dto = convertToDto(ownDemandService.create(convertToEntity(demandDto)));
            materialService.updateTimestamp(demandDto.getOwnMaterialNumber());
            return dto;
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Demand already exists. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demand is invalid.");
        }
    }

    @PutMapping()
    @Operation(summary = "Updates a demand by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Demand was updated."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Demand does not exist.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    @ResponseStatus(HttpStatus.OK)
    public DemandDto updateDemand(@RequestBody DemandDto dto) {
        OwnDemand updatedDemand = ownDemandService.update(convertToEntity(dto));
        if (updatedDemand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand does not exist.");
        }
        materialService.updateTimestamp(dto.getOwnMaterialNumber());
        return convertToDto(updatedDemand);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletes a demand by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Demand was deleted."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Demand does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDemand(@PathVariable UUID id) {
        OwnDemand demand = ownDemandService.findById(id);
        if (demand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand does not exist.");
        }
        ownDemandService.delete(id);
        materialService.updateTimestamp(demand.getMaterial().getOwnMaterialNumber());
    }

    @GetMapping("reported")
    @ResponseBody
    @Operation(
        summary = "Get all demands of partners for a material", 
        description = "Get all demands of partners for a material number. Optionally the partners can be filtered by their bpnl and the demanding site can be filtered by its bpns."
    )
    public List<DemandDto> getAllDemandsForPartner(@Parameter(description = "encoded in base64") String ownMaterialNumber, Optional<String> bpnl,
            Optional<String> site) {
        if (ownMaterialNumber != null) {
            ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        }
        return reportedDemandService.findAllByFilters(Optional.of(ownMaterialNumber), bpnl, site, Optional.empty())
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @GetMapping("reported/refresh")
    @ResponseBody
    @Operation(
        summary = "Refreshes all reported demands", 
        description = "Refreshes all reported demands from the demand request API."
    )
    public ResponseEntity<List<PartnerDto>> refreshReportedProductions(
        @RequestParam @Parameter(description = "encoded in base64") String ownMaterialNumber) {
        ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        List<Partner> allCustomerPartnerEntities = mprService.findAllCustomersForOwnMaterialNumber(ownMaterialNumber);
        for (Partner customerPartner : allCustomerPartnerEntities) {
            executorService.submit(() ->
            demandRequestApiService.doReportedDemandRequest(customerPartner, materialEntity));
        }

        return ResponseEntity.ok(allCustomerPartnerEntities.stream()
            .map(partner -> modelMapper.map(partner, PartnerDto.class))
            .toList());
    }

    private DemandDto convertToDto(OwnDemand entity) {
        DemandDto dto = modelMapper.map(entity, DemandDto.class);
        dto.setOwnMaterialNumber(entity.getMaterial().getOwnMaterialNumber());
        dto.setPartnerBpnl(entity.getPartner().getBpnl());

        return dto;
    }

    private OwnDemand convertToEntity(DemandDto dto) {
        OwnDemand entity = modelMapper.map(dto, OwnDemand.class);
        Material material = materialService.findByOwnMaterialNumber(dto.getOwnMaterialNumber());
        entity.setMaterial(material);

        Partner existingPartner = partnerService.findByBpnl(dto.getPartnerBpnl());

        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                    "Partner for bpnl %s could not be found",
                    dto.getPartnerBpnl()));
        }
        entity.setPartner(existingPartner);
        return entity;
    }

    private DemandDto convertToDto(ReportedDemand entity) {
        DemandDto dto = modelMapper.map(entity, DemandDto.class);

        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
                entity.getPartner().getUuid());
        dto.setOwnMaterialNumber(materialPartnerRelation.getPartnerMaterialNumber());
        dto.setPartnerBpnl(entity.getPartner().getBpnl());

        return dto;
    }
}

