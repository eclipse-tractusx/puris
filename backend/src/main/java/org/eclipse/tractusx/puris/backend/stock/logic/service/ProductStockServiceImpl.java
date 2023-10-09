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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ProductStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductStockServiceImpl implements ProductStockService {

    @Autowired
    ProductStockRepository productStockRepository;

    @Autowired
    MaterialRepository materialRepository;

    @Autowired
    MaterialPartnerRelationService mprService;

    @Autowired
    PartnerService partnerService;


    @Override
    public ProductStock create(ProductStock productStock) {

        // validate, if material is missing
        if (productStock.getMaterial() == null || productStock.getMaterial().getOwnMaterialNumber() == null){
            log.error("Can't create product stock due to missing material or material uuid");
            return null;
        }
        Optional<Material> existingMaterial = materialRepository.findById(productStock.getMaterial().getOwnMaterialNumber());

        if (!existingMaterial.isPresent()) {
            log.error(String.format("Material for uuid %s not found", productStock.getMaterial().getOwnMaterialNumber()));
            return null;
        }

        // validate if partner allocation is missing
        if (productStock.getAllocatedToCustomerPartner() == null){
            log.error("Can't create product stock due to missing allocation to a partner");
            return null;
        }

        if (!mprService.partnerOrdersProduct(productStock.getMaterial(), productStock.getAllocatedToCustomerPartner())) {
            log.error("Partner is not registered als customer for product " + productStock.getMaterial().getOwnMaterialNumber());
            return null;
        }

        return productStockRepository.save(productStock);
    }

    @Override
    public List<ProductStock> findAll() {
        return productStockRepository.findAll();
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
    public List<ProductStock> findAllByMaterialNumberCustomer(String materialNumberCustomer, Partner customerPartner) {
        List<Material> materialsList = mprService.findAllByPartnerMaterialNumber(materialNumberCustomer);
        ArrayList<ProductStock> output = new ArrayList<>();
        for (var material : materialsList) {
            output.addAll(productStockRepository.
                findAllByMaterial_OwnMaterialNumberAndType(material.getOwnMaterialNumber(), DT_StockTypeEnum.PRODUCT)
                .stream()
                .filter(productStock -> productStock.getAllocatedToCustomerPartner().getUuid().equals(customerPartner.getUuid()))
                .collect(Collectors.toList()));
        }
        return output;

    }

    @Override
    public List<ProductStock> findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
            String materialNumberCustomer,
            String customerBpnl) {
        Partner customerPartner  = partnerService.findByBpnl(customerBpnl);
        if (customerPartner == null) {
            return List.of();
        }
        return findAllByMaterialNumberCustomer(materialNumberCustomer, customerPartner);
    }

    @Override
    public ProductStock update(ProductStock productStock) {

        Optional<ProductStock> existingStock = productStockRepository.findById(productStock.getUuid());

        if (existingStock.isPresent() && existingStock.get().getType() == DT_StockTypeEnum.PRODUCT) {
            productStock = productStockRepository.save(productStock);
            return productStock;
        } else
            return null;
    }
}
