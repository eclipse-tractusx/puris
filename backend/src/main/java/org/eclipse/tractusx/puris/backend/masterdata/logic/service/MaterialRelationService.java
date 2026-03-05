/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRelationRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaterialRelationService {
    
    private MaterialRelationRepository materialRelationRepository;

    protected final Function<MaterialRelation, Boolean> validator;

    public MaterialRelationService(MaterialRelationRepository repository) {
        this.materialRelationRepository = repository;
        this.validator = this::validate;
    }
    
    /**
     * Creates a new MaterialRelation entity.
     * 
     * @param materialRelation the MaterialRelation entity to create
     * @return the created MaterialRelation entity
     */
    public MaterialRelation create(MaterialRelation materialRelation) {
        if (!validator.apply(materialRelation)) {
            throw new IllegalArgumentException("Invalid material relation");
        }
        MaterialRelation existingRelation = materialRelationRepository.findAll().stream()
                .filter(rel -> rel.getParentOwnMaterialNumber().equals(materialRelation.getParentOwnMaterialNumber())
                        && rel.getChildOwnMaterialNumber().equals(materialRelation.getChildOwnMaterialNumber()))
                .findFirst()
                .orElse(null);
        if (existingRelation != null) {
            log.warn("MaterialRelation between parent '{}' and child '{}' already exists.",
                    materialRelation.getParentOwnMaterialNumber(), materialRelation.getChildOwnMaterialNumber());
            throw new KeyAlreadyExistsException();
        }
        Date now = new Date();
        materialRelation.setCreatedOn(now);
        materialRelation.setLastModifiedOn(now);
        
        MaterialRelation savedRelation = materialRelationRepository.save(materialRelation);
        log.info("Created MaterialRelation with UUID: {}", savedRelation.getUuid());
        return savedRelation;
    }

    /**
     * Updates an existing MaterialRelation entity
     * 
     * @param materialRelation the Materialrelation to update
     * @return the updated MaterialRelation entity
     */
    public MaterialRelation update(MaterialRelation materialRelation) {
        if (materialRelation.getUuid() == null) {
            throw new IllegalArgumentException("Missing uuid. Cannot identify entity.");
        }
        MaterialRelation existingRelation = materialRelationRepository.findById(materialRelation.getUuid()).orElse(null);
        if (existingRelation == null) {
            log.warn("MaterialRelation with uuid {} does not exist.", materialRelation.getUuid());
            throw new NoSuchElementException("Material relation does not exist.");
        }
        if (!validator.apply(materialRelation)) {
            throw new IllegalArgumentException("Invalid material relation");
        }
        if (!materialRelation.getChildOwnMaterialNumber().equals(existingRelation.getChildOwnMaterialNumber())) {
            log.warn("Cannot update childOwnMaterialNumber");
            throw new IllegalArgumentException("Changing the childOwnMaterialNumber is not allowed.");
        }
        if (!materialRelation.getParentOwnMaterialNumber().equals(existingRelation.getParentOwnMaterialNumber())) {
            log.warn("Cannot update parentOwnMaterialNumber");
            throw new IllegalArgumentException("Changing the parentOwnMaterialNumber is not allowed.");
        }
        if (!materialRelation.getMeasurementUnit().equals(existingRelation.getMeasurementUnit())) {
            log.warn("Cannot update measurementUnit");
            throw new IllegalArgumentException("Changing the measurementUnit is not allowed.");
        }
        existingRelation.setQuantity(materialRelation.getQuantity());
        existingRelation.setValidFrom(materialRelation.getValidFrom());
        existingRelation.setValidTo(materialRelation.getValidTo());
        existingRelation.setLastModifiedOn(new Date());
        MaterialRelation updatedRelation = materialRelationRepository.save(existingRelation);
        log.info("Updated MaterialRelation with UUID: {}", updatedRelation.getUuid());
        return updatedRelation;
    }
    
    /**
     * Retrieves all MaterialRelation entities.
     * 
     * @return a list of all MaterialRelation entities
     */
    public List<MaterialRelation> findAll() {
        return materialRelationRepository.findAll();
    }

    /**
     * Validates a given material relation
     * @param   materialRelation    the material relation entity to validate
     * @return                      a boolean value indication whether or not eh validation passes
     */
    public boolean validate(MaterialRelation materialRelation) {
        return 
            materialRelation.getParentOwnMaterialNumber() != null &&
            materialRelation.getChildOwnMaterialNumber() != null &&
            materialRelation.getQuantity() > 0 &&
            materialRelation.getMeasurementUnit() != null &&
            (
                materialRelation.getValidTo() == null || 
                (
                    materialRelation.getValidFrom() != null &&
                    materialRelation.getValidFrom().compareTo(materialRelation.getValidTo()) < 0
                )
            );
    }
}
