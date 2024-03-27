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

package org.eclipse.tractusx.puris.backend.production.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.domain.model.PartnerProduction;
import org.eclipse.tractusx.puris.backend.production.logic.dto.ProductionDto;
import org.eclipse.tractusx.puris.backend.production.logic.service.PartnerProductionService;
import org.eclipse.tractusx.puris.backend.production.logic.service.ProductionService;
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
@RequestMapping("production")
@Slf4j
public class ProductionController {
    @Autowired
    private ProductionService productionService;

    @Autowired
    private PartnerProductionService partnerProductionService;

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
    @Operation(summary = "Get all planned productions for the given Material", description = "Get all planned productions for the given material number. Optionally the production site can be filtered by its bpns.")
    public List<ProductionDto> getAllProductions(String materialNumber, Optional<String> site) {
        if (site.isEmpty()) {
            return productionService.findAll().stream().filter(prod -> prod.getMaterial().getOwnMaterialNumber().equals(materialNumber)).map(this::convertToDto).collect(Collectors.toList());
        }
        return productionService.findAll().stream()
            .filter(prod -> prod.getMaterial().getOwnMaterialNumber().equals(materialNumber) && prod.getProductionSiteBpns().equals(site.get()))
            .map(this::convertToDto).collect(Collectors.toList());
    }

    @PostMapping()
    @ResponseBody
    @Operation(summary = "Creates a new planned production")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Planned Production was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "409", description = "Planned Production already exists."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionDto createProduction(@RequestBody ProductionDto productionDto) {
        if (!validator.validate(productionDto).isEmpty()) {
            log.warn("Rejected invalid message body");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (productionDto.getUuid() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Production with this UUID already exists.");
        }
        if (productionDto.getMaterial().getMaterialNumberSupplier() == null ||
                productionDto.getMaterial().getMaterialNumberSupplier().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Production Information misses material identification.");
        }
        if (productionDto.getPartner().getBpnl() == null || productionDto.getPartner().getBpnl().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Production Information misses partner identification.");
        }
        OwnProduction production = convertToEntity(productionDto);
        log.info("Production: " + production);
        if (!productionService.validate(production)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Production is invalid.");
        }
        List<OwnProduction> existingProductions = productionService.findAll();
        log.info("finding existing production");
        boolean productionExists = existingProductions.stream()
                .anyMatch(prod -> production.equals(prod));
        if (productionExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Production already exists. Use PUT instead.");
        }
        OwnProduction createdProduction = productionService.create(production);
        if (createdProduction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock could not be created.");
        }
        log.info("Created product-stock: " + createdProduction);

        return convertToDto(createdProduction);
    }

    @PostMapping("/range")
    @ResponseBody
    @Operation(summary = "Creates a range of planned productions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Planned Productions were created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "409", description = "Planned Productions already exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductionDto> createProductionRange(@RequestBody List<ProductionDto> productionDtos) {
        List<OwnProduction> productions = productionDtos.stream().map(dto -> {
            if (!validator.validate(dto).isEmpty()) {
                log.warn("Rejected invalid message body");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (dto.getMaterial().getMaterialNumberSupplier() == null ||
                    dto.getMaterial().getMaterialNumberSupplier().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Production Information misses material identification.");
            }
            if (dto.getPartner().getBpnl() == null || dto.getPartner().getBpnl().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Production Information misses partner identification.");
            }
            return convertToEntity(dto);
        }).collect(Collectors.toList());

        productions = productionService.createAll(productions);
        if (productions == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stocks could not be created.");
        }
        return productions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @PutMapping("{id}")
    @Operation(summary = "Updates a planned production by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Planned Productions was updated."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Planned Production does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.OK)
    public ProductionDto updateProduction(@PathVariable UUID id, @RequestBody ProductionDto dto) {
        OwnProduction updatedProduction = productionService.update(convertToEntity(dto));
        if (updatedProduction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Production does not exist.");
        }
        return convertToDto(updatedProduction);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletes a planned production by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Planned Productions was deleted."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Planned Production does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduction(@PathVariable UUID id) {
        OwnProduction production = productionService.findById(id);
        if (production == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Production does not exist.");
        }
        productionService.delete(id);
    }

    @GetMapping("partner")
    @ResponseBody
    @Operation(summary = "Get all productions of partners for a material", description = "Get all productions of partners for a material number. Optionally the partners can be filtered by their bpnl and the production site can be filtered by its bpns.")
    public List<ProductionDto> getAllProductionsForPartner(String materialNumber, Optional<String> bpnl,
            Optional<String> site) {
        if (bpnl.isEmpty()) {
            if (site.isEmpty()) {
                return partnerProductionService.findAll().stream()
                        .filter(prod -> prod.getMaterial().getOwnMaterialNumber().equals(materialNumber))
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
            }
            return partnerProductionService.findAll().stream()
                    .filter(prod -> prod.getMaterial().getOwnMaterialNumber().equals(materialNumber) && prod.getProductionSiteBpns().equals(site.get()))
                    .map(this::convertToDto).collect(Collectors.toList());
        }

        Partner partner = partnerService.findByBpnl(bpnl.get());
        if (partner == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner does not exist.");
        }
        return partnerProductionService.findAllByPartnerId(partner.getUuid()).stream()
                .filter(prod -> prod.getMaterial().getOwnMaterialNumber().equals(materialNumber)
                        && (site.isEmpty() || prod.getProductionSiteBpns().equals(site.get())))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ProductionDto convertToDto(OwnProduction entity) {
        ProductionDto dto = modelMapper.map(entity, ProductionDto.class);

        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());

        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
                entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());

        return dto;
    }

    private OwnProduction convertToEntity(ProductionDto dto) {
        OwnProduction entity = modelMapper.map(dto, OwnProduction.class);

        Material material = materialService.findByOwnMaterialNumber(dto.getMaterial().getMaterialNumberSupplier());
        entity.setMaterial(material);

        PartnerDto partnerDto = dto.getPartner();
        Partner existingPartner = partnerService.findByBpnl(partnerDto.getBpnl());

        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                    "Partner for bpnl %s could not be found",
                    partnerDto.getBpnl()));
        }
        entity.setPartner(existingPartner);
        return entity;
    }

    private ProductionDto convertToDto(PartnerProduction entity) {
        ProductionDto dto = modelMapper.map(entity, ProductionDto.class);

        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());

        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(), entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());

        return dto;
    }
}
