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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialEntityDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("materials")
@Slf4j
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private Validator validator;
    private final ModelMapper modelMapper = new ModelMapper();
    private final Pattern materialPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @PostMapping
    @Operation(description = "Creates a new Material entity with the data given in the request body. As a bare minimum, " +
        "it must contain a new, unique ownMaterialNumber.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a new Material entity."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "409", description = "Material with the given ownMaterialNumber already exists."),
        @ApiResponse(responseCode = "500", description = "Internal Server error.")
    })
    public ResponseEntity<?> createMaterial(@RequestBody MaterialEntityDto materialDto) {
        if(!validator.validate(materialDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.status(400).build();
        }
        if (materialDto.getOwnMaterialNumber() == null || materialDto.getOwnMaterialNumber().isEmpty()) {
            // Cannot create material without ownMaterialNumber
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        if (materialService.findByOwnMaterialNumber(materialDto.getOwnMaterialNumber()) != null) {
            // Cannot create material, ownMaterialNumber is already assigned
            return new ResponseEntity<>(HttpStatusCode.valueOf(409));
        }
        Material createdMaterial;
        try {
            createdMaterial = modelMapper.map(materialDto, Material.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        createdMaterial = materialService.create(createdMaterial);
        if (createdMaterial == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping
    @Operation(description = "Updates an existing Material entity with the data given in the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update was accepted."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "404", description = "No existing Material Entity found, no update was performed."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ResponseEntity<?> updateMaterial(@RequestBody MaterialEntityDto materialDto) {
        if(!validator.validate(materialDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.status(400).build();
        }
        if (materialDto.getOwnMaterialNumber() == null || materialDto.getOwnMaterialNumber().isEmpty()) {
            // Cannot update material without ownMaterialNumber
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material existingMaterial = materialService.findByOwnMaterialNumber(materialDto.getOwnMaterialNumber());
        if (existingMaterial == null) {
            // Cannot update non-existent Material
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        Material updatedMaterial;
        try {
            updatedMaterial = modelMapper.map(materialDto, Material.class);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        updatedMaterial = materialService.update(updatedMaterial);
        if (updatedMaterial == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @GetMapping
    @Operation(description = "Returns the requested Material dto, specified by the given ownMaterialNumber.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns the requested Material."),
        @ApiResponse(responseCode = "400", description = "Invalid parameter"),
        @ApiResponse(responseCode = "404", description = "Requested Material was not found.")
    })
    public ResponseEntity<MaterialEntityDto> getMaterial(@Parameter(name = "ownMaterialNumber",
        description = "The Material Number that is used in your own company to identify the Material.",
        example = "MNR-7307-AU340474.002") @RequestParam String ownMaterialNumber) {
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
    @Operation(description = "Returns a list of all Materials and Products.")
    public ResponseEntity<List<MaterialEntityDto>> listMaterials() {
        return new ResponseEntity<>(materialService.findAll().
            stream().map(x -> modelMapper.map(x, MaterialEntityDto.class)).collect(Collectors.toList()),
            HttpStatusCode.valueOf(200));
    }
}
