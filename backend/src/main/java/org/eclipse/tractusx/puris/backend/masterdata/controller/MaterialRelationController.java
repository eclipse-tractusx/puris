/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

import java.util.List;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialRelationDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("material-relations")
@Slf4j
public class MaterialRelationController {
    
    @Autowired
    private MaterialRelationService materialRelationService;

    @Autowired
    private MaterialService materialService;
    
    @Autowired
    private Validator validator;
    
    private final ModelMapper modelMapper = new ModelMapper();
    
    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PostMapping
    @Operation(summary = "Creates a Material Relation -- ADMIN ONLY", 
               description = "Creates a new MaterialRelation entity with the data given in the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created a new MaterialRelation entity."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "500", description = "Internal Server error.")
    })
    public ResponseEntity<?> createMaterialRelation(@RequestBody MaterialRelationDto materialRelationDto) {
        if (!validator.validate(materialRelationDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.badRequest().body("MaterialRelation is invalid");
        }

        Material parentMaterial = materialService.findByOwnMaterialNumber(materialRelationDto.getParentMaterialNumber());
        Material childMaterial = materialService.findByOwnMaterialNumber(materialRelationDto.getChildMaterialNumber());
        if (parentMaterial == null) {
            log.warn("Parent material with ownMaterialNumber {} not found.", materialRelationDto.getParentMaterialNumber());
            return ResponseEntity.badRequest().body("Parent material not found");
        }
        if (childMaterial == null) {
            log.warn("Child material with ownMaterialNumber {} not found.", materialRelationDto.getChildMaterialNumber());
            return ResponseEntity.badRequest().body("Child material not found");
        }
        if (!parentMaterial.isProductFlag()) {
            log.warn("Parent material with ownMaterialNumber {} is not a product.", materialRelationDto.getParentMaterialNumber());
            return ResponseEntity.badRequest().body("Parent material is not a product");
        }
        if (!childMaterial.isMaterialFlag()) {
            log.warn("Child material with ownMaterialNumber {} is not a material.", materialRelationDto.getChildMaterialNumber());
            return ResponseEntity.badRequest().body("Child material is not a material");
        }
        try {
            MaterialRelation entity = modelMapper.map(materialRelationDto, MaterialRelation.class);
            MaterialRelation createdRelation = materialRelationService.create(entity);
            MaterialRelationDto responseDto = modelMapper.map(createdRelation, MaterialRelationDto.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (KeyAlreadyExistsException e) {
            log.error("MaterialRelation for the given parent and child material already exists", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("MaterialRelation already exists");
        } catch (IllegalArgumentException e) {
            log.error("Error creating MaterialRelation", e);
            return ResponseEntity.badRequest().body("MaterialRelation is invalid");
        } catch (Exception e) {
            log.error("Unexpected error creating MaterialRelation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error: An error occurred while creating the material relation. Check the server logs for details");
        }
    }
    
    @GetMapping()
    @Operation(summary = "Gets all material relations", 
               description = "Returns a list of all MaterialRelation entities.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all MaterialRelation entities.")
    })
    public ResponseEntity<List<MaterialRelationDto>> getAllMaterialRelations() {
        try {
            List<MaterialRelationDto> dtos = materialRelationService.findAll()
                .stream()
                .map(relation -> modelMapper.map(relation, MaterialRelationDto.class))
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving all material relations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
        }
    }
}
