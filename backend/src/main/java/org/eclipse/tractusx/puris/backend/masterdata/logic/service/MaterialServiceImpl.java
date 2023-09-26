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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    MaterialRepository materialRepository;

    @Override
    public Material create(Material material) {
        var searchResult = materialRepository.findById(material.getOwnMaterialNumber());
        if (searchResult.isEmpty()) {
            return materialRepository.save(material);
        }
        log.error("Could not create material " + material.getOwnMaterialNumber() + " because it already exists");
        return null;
    }

    @Override
    public Material update(Material material) {
        Optional<Material> existingMaterial =
                materialRepository.findById(material.getOwnMaterialNumber());
        if (existingMaterial.isPresent()) {
            return existingMaterial.get();
        }
        log.error("Could not update material " + material.getOwnMaterialNumber() + " because it didn't exist before");
        return null;
    }

    @Override
    public List<Material> findAllMaterials() {
        return materialRepository.findAllByMaterialFlagTrue();
    }

    @Override
    public List<Material> findAllProducts() {
        return materialRepository.findAllByProductFlagTrue();
    }

    @Override
    public Material findByOwnMaterialNumber(String ownMaterialNumber) {
        var searchResult = materialRepository.findById(ownMaterialNumber);
        if (searchResult.isPresent()) {
            return searchResult.get();
        }
        return null;
    }

    @Override
    public Material findByMaterialNumberCx(String materialNumberCx) {
        List<Material> foundMaterial = materialRepository.findByMaterialNumberCx(materialNumberCx);
        if (foundMaterial.isEmpty()) {
            return null;
        }
        if (foundMaterial.size() > 1) {
            log.warn("Found more than one result for materialNumberCx " + materialNumberCx);
        }
        return foundMaterial.get(0);

    }

    @Override
    public List<Material> findAll() {
        return materialRepository.findAll();
    }

}
