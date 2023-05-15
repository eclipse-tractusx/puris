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

import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ProductStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductStockServiceImpl implements ProductStockService {

    @Autowired
    ProductStockRepository productStockRepository;

    @Override
    public ProductStock create(ProductStock productStock) {
        return productStockRepository.save(productStock);
    }

    @Override
    public List<ProductStock> findAll() {
        return productStockRepository.findAllByType(DT_StockTypeEnum.PRODUCT);
    }

    @Override
    public ProductStock findByUuid(UUID productStockUuid) {
        Optional<ProductStock> foundProductStock =
                productStockRepository.findById(productStockUuid);

        if (!foundProductStock.isPresent()) {
            return null;
        }
        return foundProductStock.get();
    }

    @Override
    public List<ProductStock> findAllByMaterialNumberCustomer(String materialNumberCustomer) {
        return productStockRepository.findAllByMaterial_MaterialNumberCustomerAndType(
                materialNumberCustomer,
                DT_StockTypeEnum.MATERIAL);
    }

    @Override
    public List<ProductStock> findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
            String materialNumberCustomer,
            String customerBpnl) {
        return productStockRepository
                .findAllByMaterial_MaterialNumberCustomerAndTypeAndAllocatedToCustomerPartner_Bpnl(
                        materialNumberCustomer,
                        DT_StockTypeEnum.PRODUCT,
                        customerBpnl);
    }

    @Override
    public ProductStock update(ProductStock productStock) {

        Optional<ProductStock> existingStock = productStockRepository.findById(productStock.getUuid());

        if (existingStock.isPresent() && existingStock.get().getType() == DT_StockTypeEnum.PRODUCT) {
            return existingStock.get();
        } else
            return null;
    }
}
