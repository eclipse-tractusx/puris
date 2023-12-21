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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.MaterialStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service
public class MaterialStockServiceImpl implements MaterialStockService {

    @Autowired
    MaterialStockRepository materialStockRepository;

    @Autowired
    MaterialRepository materialRepository;

    @Autowired
    MaterialPartnerRelationService mprService;

    @Override
    public MaterialStock create(MaterialStock materialStock) {
        // avoid unintentional overwriting of an existing ProductStock
        materialStock.setUuid(null);

        // validate, if material is missing
        if (materialStock.getMaterial() == null || materialStock.getMaterial().getOwnMaterialNumber() == null){
            log.error("Can't create material stock due to missing material or material uuid");
            return null;
        }
        Optional<Material> existingMaterial = materialRepository.findById(materialStock.getMaterial().getOwnMaterialNumber());

        if (existingMaterial.isEmpty()) {
            log.error(String.format("Material %s not found", materialStock.getMaterial().getOwnMaterialNumber()));
            return null;
        }

        return materialStockRepository.save(materialStock);
    }

    @Override
    public List<MaterialStock> findAll() {
        return materialStockRepository.findAllByType(DT_StockTypeEnum.MATERIAL);
    }

    @Override
    public MaterialStock findByUuid(UUID materialStockUuid) {

        Optional<MaterialStock> foundMaterialStock = materialStockRepository.findById(materialStockUuid);

        return foundMaterialStock.orElse(null);
    }

    @Override
    public List<MaterialStock> findAllByPartnerMaterialNumber(String partnerMaterialNumber) {
        var materials = mprService.findAllByPartnerMaterialNumber(partnerMaterialNumber);
        ArrayList<MaterialStock> output = new ArrayList<>();
        for (var material : materials) {
            output.addAll(
            materialStockRepository.findAllByMaterial_OwnMaterialNumberAndType(material.getOwnMaterialNumber(),
                DT_StockTypeEnum.MATERIAL));
        }
        return output;
    }

    @Override
    public List<MaterialStock> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return materialStockRepository.findAllByMaterial_OwnMaterialNumberAndType(ownMaterialNumber, DT_StockTypeEnum.MATERIAL);
    }

    @Override
    public MaterialStock update(MaterialStock materialStock) {
        Optional<MaterialStock> existingStock = materialStockRepository.findById(materialStock.getUuid());
        if (existingStock.isPresent() && existingStock.get().getType() == DT_StockTypeEnum.MATERIAL) {
            materialStock = materialStockRepository.save(materialStock);
            return materialStock;
        } else {
            if (existingStock.isPresent()) {
                log.error(("update of materialStock " + materialStock.getUuid() + " failed because" +
                    " existing stock is not of type MATERIAL"));
                return null;
            }
        }
        log.error("update of materialStock " + materialStock.getUuid() + " failed because there " +
            "is no existing stock stored to the database");
        return null;
    }
}
