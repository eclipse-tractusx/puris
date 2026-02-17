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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import java.util.Date;
import java.util.List;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaterialRelationService {
    
    @Autowired
    private MaterialRelationRepository materialRelationRepository;
    
    /**
     * Creates a new MaterialRelation entity.
     * 
     * @param materialRelation the MaterialRelation entity to create
     * @return the created MaterialRelation entity
     */
    public MaterialRelation create(MaterialRelation materialRelation) {
        MaterialRelation existingRelation = materialRelationRepository.findAll().stream()
                .filter(rel -> rel.getParentMaterialNumber().equals(materialRelation.getParentMaterialNumber())
                        && rel.getChildMaterialNumber().equals(materialRelation.getChildMaterialNumber()))
                .findFirst()
                .orElse(null);
        if (existingRelation != null) {
            log.warn("MaterialRelation between parent '{}' and child '{}' already exists.",
                    materialRelation.getParentMaterialNumber(), materialRelation.getChildMaterialNumber());
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
     * Retrieves all MaterialRelation entities.
     * 
     * @return a list of all MaterialRelation entities
     */
    public List<MaterialRelation> findAll() {
        return materialRelationRepository.findAll();
    }
}
