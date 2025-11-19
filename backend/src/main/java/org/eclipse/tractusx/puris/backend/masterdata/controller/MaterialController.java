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

import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialEntityDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRefreshService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("materials")
@Slf4j
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialRefreshService materialRefreshService;

    @Autowired
    private Validator validator;
    private final ModelMapper modelMapper = new ModelMapper();
    private final Pattern materialPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PostMapping
    @Operation(summary = "Creates a Material -- ADMIN ONLY", description = "Creates a new Material entity with the data given in the request body. As a bare minimum, " +
        "it must contain a new, unique ownMaterialNumber.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a new Material entity."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "409", description = "Material with the given ownMaterialNumber already exists."),
        @ApiResponse(responseCode = "500", description = "Internal Server error.")
    })
    public MaterialEntityDto createMaterial(@RequestBody MaterialEntityDto materialDto) {
        if(!validator.validate(materialDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Material.");
        }
        if (materialDto.getOwnMaterialNumber() == null || materialDto.getOwnMaterialNumber().isEmpty()) {
            // Cannot create material without ownMaterialNumber
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material information misses material identification.");
        }
        if (materialService.findByOwnMaterialNumber(materialDto.getOwnMaterialNumber()) != null) {
            // Cannot create material, ownMaterialNumber is already assigned
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material already exists. Use PUT instead.");
        }
        Material createdMaterial;
        try {
            Material entity = modelMapper.map(materialDto, Material.class);
            createdMaterial = materialService.create(entity);
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material already exists. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material is invalid.");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return modelMapper.map(createdMaterial, MaterialEntityDto.class);
    }

    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PutMapping
    @Operation(summary = "Updates a Material -- ADMIN ONLY", description = "Updates an existing Material entity with the data given in the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update was accepted."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "404", description = "No existing Material Entity found, no update was performed."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public MaterialEntityDto updateMaterial(@RequestBody MaterialEntityDto materialDto) {
        if(!validator.validate(materialDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Material.");
        }
        if (materialDto.getOwnMaterialNumber() == null || materialDto.getOwnMaterialNumber().isEmpty()) {
            // Cannot update material without ownMaterialNumber
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material information misses material identification.");
        }
        Material updatedMaterial;
        try {
            Material entity = modelMapper.map(materialDto, Material.class);
            updatedMaterial = materialService.update(entity);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while updating material", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

        return modelMapper.map(updatedMaterial, MaterialEntityDto.class);
    }

    @GetMapping
    @Operation(summary = "Gets a Material by ownMaterialNumber", description = "Returns the requested Material dto, specified by the given ownMaterialNumber.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns the requested Material."),
        @ApiResponse(responseCode = "400", description = "Invalid parameter", content = @Content),
        @ApiResponse(responseCode = "404", description = "Requested Material was not found.", content = @Content)
    })
    public ResponseEntity<MaterialEntityDto> getMaterial(@Parameter(name = "ownMaterialNumber",
        description = "The Material Number that is used in your own company to identify the Material, encoded in base64"
        ) @RequestParam String ownMaterialNumber) {
        ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        if(!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material foundMaterial = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (foundMaterial == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }

        MaterialEntityDto dto = modelMapper.map(foundMaterial, MaterialEntityDto.class);
        return new ResponseEntity<>(dto, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/all")
    @Operation(summary = "Gets all materials", description = "Returns a list of all Materials and Products.")
    public ResponseEntity<List<MaterialEntityDto>> listMaterials() {
        return new ResponseEntity<>(materialService.findAll().
            stream().map(x -> modelMapper.map(x, MaterialEntityDto.class)).collect(Collectors.toList()),
            HttpStatusCode.valueOf(200));
    }

    @GetMapping("/refresh")
    @Operation(summary = "Refreshes partner data for specified material", description = "Requests material data for the given material from all partners")
    public ResponseEntity<String> refreshMaterialData(@RequestParam @Parameter(description = "encoded in base64") String ownMaterialNumber) {
        ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        materialRefreshService.refreshPartnerData(ownMaterialNumber);
        return new ResponseEntity<>(ownMaterialNumber, HttpStatusCode.valueOf(200));
    }
}
