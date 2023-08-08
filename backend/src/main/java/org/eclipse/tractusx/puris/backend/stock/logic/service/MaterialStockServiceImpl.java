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

import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.MaterialStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MaterialStockServiceImpl implements MaterialStockService {

    @Autowired
    MaterialStockRepository materialStockRepository;

    @Override
    public MaterialStock create(MaterialStock materialStock) {
        return materialStockRepository.save(materialStock);
    }

    @Override
    public List<MaterialStock> findAll() {
        return materialStockRepository.findAllByType(DT_StockTypeEnum.MATERIAL);
    }

    @Override
    public MaterialStock findByUuid(UUID materialStockUuid) {

        Optional<MaterialStock> foundMaterialStock = materialStockRepository.findById(materialStockUuid);

        if (!foundMaterialStock.isPresent()) {
            return null;
        }
        return foundMaterialStock.get();
    }

    @Override
    public List<MaterialStock> findAllByMaterialNumberCustomer(String materialNumberCustomer) {
        return materialStockRepository.findAllByMaterial_MaterialNumberCustomerAndType(materialNumberCustomer, DT_StockTypeEnum.MATERIAL);
    }

    @Override
    public MaterialStock update(MaterialStock materialStock) {

        Optional<MaterialStock> existingStock = materialStockRepository.findById(materialStock.getUuid());

        if (existingStock.isPresent() && existingStock.get().getType() == DT_StockTypeEnum.MATERIAL) {
            return existingStock.get();
        } else return null;//TODO error handling: Updated MaterialStock is not material
    }
}
