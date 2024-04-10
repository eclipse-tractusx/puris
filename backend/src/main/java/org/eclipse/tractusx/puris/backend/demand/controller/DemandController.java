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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.DemandDto;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.ReportedDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("demand")
@Slf4j
public class DemandController {
    @Autowired
    private OwnDemandService ownDemandService;

    @Autowired
    private ReportedDemandService reportedDemandService;

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

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all own demands for the given Material", description = "Get all own demands for the given material number. Optionally the demanding site can be filtered by its bpns.")
    public List<DemandDto> getAllDemands(String materialNumber, Optional<String> site) {
        return ownDemandService.findAllByFilters(Optional.of(materialNumber), Optional.empty(), site)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @PostMapping()
    @ResponseBody
    @Operation(summary = "Creates a new demand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Demand was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "409", description = "Demand already exists. Use PUT instead."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DemandDto createDemand(@RequestBody DemandDto demandDto) {
        if (!validator.validate(demandDto).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Demand Information: \n" + demandDto.toString());
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
            return convertToDto(ownDemandService.create(convertToEntity(demandDto)));
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Demand already exists. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demand is invalid.");
        }
    }

    /* @PostMapping("/range")
    @ResponseBody
    @Operation(summary = "Creates a range of planned demands")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Planned Demands were created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "409", description = "Planned Demands already exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public List<DemandDto> createDemandRange(@RequestBody List<DemandDto> dtos) {
        List<OwnDemand> demands = dtos.stream().map(dto -> {
            if (!validator.validate(dto).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejected invalid message body");
            }
            if (dto.getMaterial().getMaterialNumberSupplier() == null ||
                    dto.getMaterial().getMaterialNumberSupplier().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Demand Information misses material identification.");
            }
            if (dto.getPartner().getBpnl() == null || dto.getPartner().getBpnl().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Demand Information misses partner identification.");
            }
            return convertToEntity(dto);
        }).toList();
        try {
            return ownDemandService.createAll(demands).stream().map(this::convertToDto).collect(Collectors.toList());
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more demands already exist. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more demands are invalid.");
        }      
    } */

    @PutMapping()
    @Operation(summary = "Updates a demand by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Demand was updated."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Demand does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.OK)
    public DemandDto updateDemand(@RequestBody DemandDto dto) {
        OwnDemand updatedDemand = ownDemandService.update(convertToEntity(dto));
        if (updatedDemand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand does not exist.");
        }
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
    }

    @GetMapping("reported")
    @ResponseBody
    @Operation(
        summary = "Get all demands of partners for a material", 
        description = "Get all demands of partners for a material number. Optionally the partners can be filtered by their bpnl and the demanding site can be filtered by its bpns."
    )
    public List<DemandDto> getAllDemandsForPartner(String materialNumber, Optional<String> bpnl,
            Optional<String> site) {
        return reportedDemandService.findAllByFilters(Optional.of(materialNumber), bpnl, site)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private DemandDto convertToDto(OwnDemand entity) {
        DemandDto dto = modelMapper.map(entity, DemandDto.class);
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
                entity.getPartner().getUuid());
        dto.setOwnMaterialNumber(materialPartnerRelation.getPartnerMaterialNumber());
        dto.setPartnerBpnl(entity.getPartner().getBpnl());

        return dto;
    }

    private OwnDemand convertToEntity(DemandDto dto) {
        OwnDemand entity = modelMapper.map(dto, OwnDemand.class);

        Material material = materialService.findByOwnMaterialNumber(dto.getOwnMaterialNumber());
        log.info("Found material: " + material.toString());
        entity.setMaterial(material);

        Partner existingPartner = partnerService.findByBpnl(dto.getPartnerBpnl());

        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                    "Partner for bpnl %s could not be found",
                    dto.getPartnerBpnl()));
        }
        log.info("Found partner: " + existingPartner.toString());
        entity.setPartner(existingPartner);
        log.info("Dto: " + dto.toString());
        log.info("Entity: " + entity.toString());
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

