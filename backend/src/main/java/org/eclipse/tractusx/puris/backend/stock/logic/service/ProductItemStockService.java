/*
 * Copyright (c) 2023 Volkswagen AG
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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ProductItemStockRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductItemStockService extends ItemStockService<ProductItemStock> {

    private final ProductItemStockRepository repository;

    public ProductItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService,
                                   ProductItemStockRepository repository) {
        super(partnerService, mprService, repository);
        this.repository = repository;
    }

    public Optional<ProductItemStock> findByBusinessKey(
        Material material,
        Partner partner,
        String locationBpns,
        String locationBpna,
        String customerOrderId,
        String customerOrderPositionId,
        String supplierOrderId
    ) {
        return repository
            .findByMaterialOwnMaterialNumberAndPartnerUuidAndLocationBpnsAndLocationBpnaAndCustomerOrderIdAndCustomerOrderPositionIdAndSupplierOrderId(
                material.getOwnMaterialNumber(),
                partner.getUuid(),
                locationBpns,
                locationBpna,
                customerOrderId,
                customerOrderPositionId,
                supplierOrderId
            );
    }

    @Override
    public boolean validate(ProductItemStock productItemStock) {
        return basicValidation(productItemStock).isEmpty() && validateLocalStock(productItemStock).isEmpty()
            && validateProductItemStock(productItemStock).isEmpty();
    }

    public List<String> validateWithDetails(ProductItemStock productItemStock) {
        List<String> errors = new ArrayList<>();
        errors.addAll(basicValidation(productItemStock));
        errors.addAll(validateLocalStock(productItemStock));
        errors.addAll(validateProductItemStock(productItemStock));
        return errors;
    }
}
