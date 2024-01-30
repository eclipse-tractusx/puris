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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialPartnerRelationService mprService;

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

    @Override
    public Material findFromCustomerPerspective(String materialNumberCx, String customerMatNbr, String supplierMatNbr, Partner partner) {
        Material material = null;
        if (materialNumberCx != null) {
            // Use Material Number CX
            material = findByMaterialNumberCx(materialNumberCx);
            if (material != null) {
                if (customerMatNbr != null && !material.equals(findByOwnMaterialNumber(customerMatNbr))) {
                    log.warn("Mismatch between " + material + " and " + customerMatNbr);
                }
                if (partner != null && !mprService.partnerSuppliesMaterial(material, partner)) {
                    log.warn("Partner " + partner + " does not supply material " + material);
                }
            }

        }
        if (material == null && customerMatNbr != null) {
            // If previous effort yielded not result, use Customer Material Number
            material = findByOwnMaterialNumber(customerMatNbr);
            if (material != null && materialNumberCx != null) {
                log.warn("Unknown Material Number CX " + materialNumberCx + " for Material " + material);
            }
            if (material != null && partner != null && !mprService.partnerSuppliesMaterial(material, partner)) {
                log.warn("Partner " + partner + " does not supply material " + material);
            }
        }
        if (material == null && supplierMatNbr != null) {
            // If previous effort yielded not result, use Supplier Material Number
            List<Material> materialList;
            if (partner != null) {
                materialList = mprService.findAllBySupplierPartnerAndPartnerMaterialNumber(partner, supplierMatNbr)
                    .stream()
                    .map(MaterialPartnerRelation::getMaterial)
                    .toList();
            } else {
                materialList = mprService.findAllBySupplierPartnerMaterialNumber(supplierMatNbr)
                    .stream()
                    .map(MaterialPartnerRelation::getMaterial)
                    .toList();
            }

            if (!materialList.isEmpty()) {
                material = materialList.get(0);
            }
            if (materialList.size() > 1) {
                log.warn("Ambiguous results for supplier partner Material Number " + supplierMatNbr + ", arbitrarily choosing " + material);
            }
            if (material != null) {
                if (materialNumberCx != null) {
                    log.warn("Unknown Material Number CX " + materialNumberCx + " for Material " + material);
                }
                if (customerMatNbr != null) {
                    log.warn("Unknown Customer Material Number " + customerMatNbr + " for Material " + material);
                }
            }
        }

        return material;
    }

    @Override
    public Material findFromSupplierPerspective(String materialNumberCx, String customerMatNbr, String supplierMatNbr, Partner partner) {
        Material material = null;
        if (materialNumberCx != null) {
            // Use Material Number CX
            material = findByMaterialNumberCx(materialNumberCx);
            if (material != null) {
                if (supplierMatNbr != null && !material.equals(findByOwnMaterialNumber(supplierMatNbr))) {
                    log.warn("Mismatch between " + material + " and " + supplierMatNbr);
                }
                if(partner != null && ! mprService.partnerOrdersProduct(material, partner)) {
                    log.warn("Partner " + partner + " does not order material " + material);
                }
            }
        }
        if (material == null && customerMatNbr != null) {
            // If previous effort yielded not result, use Customer Material Number
            List<Material> materialList;
            if (partner != null) {
                materialList = mprService.findAllByCustomerPartnerAndPartnerMaterialNumber(partner, customerMatNbr)
                    .stream()
                    .map(MaterialPartnerRelation::getMaterial)
                    .toList();
            } else {
                materialList = mprService.findAllByCustomerPartnerMaterialNumber(customerMatNbr)
                    .stream()
                    .map(MaterialPartnerRelation::getMaterial)
                    .toList();
            }

            if (!materialList.isEmpty()) {
                material = materialList.get(0);
            }
            if (materialList.size() > 1) {
                log.warn("Ambiguous results for customer partner Material Number " + customerMatNbr + ", arbitrarily choosing " + material);
            }
            if (material != null) {
                if (materialNumberCx != null) {
                    log.warn("Unknown Material Number CX " + materialNumberCx + " for Material " + material);
                }
                if (supplierMatNbr != null && !material.equals(findByOwnMaterialNumber(supplierMatNbr))) {
                    log.warn("Mismatch between OwnMaterialNumber " + supplierMatNbr + " and " + material);
                }
            }
        }
        if (material == null && supplierMatNbr != null) {
            // If previous effort yielded not result, use Supplier Material Number
            material = findByOwnMaterialNumber(supplierMatNbr);
            if (material != null) {
                if (materialNumberCx != null) {
                    log.warn("Unknown Material Number CX " + materialNumberCx + " for Material " + material);
                }
                if (customerMatNbr != null) {
                    log.warn("Unknown customer Material Number " + customerMatNbr + " for Material " + material);
                }
                if(partner != null && ! mprService.partnerOrdersProduct(material, partner)) {
                    log.warn("Partner " + partner + " does not order material " + material);
                }
            }
        }
        return material;
    }

}
