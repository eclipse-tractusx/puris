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

import lombok.AllArgsConstructor;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.PartnerProductStockRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@AllArgsConstructor
public class PartnerProductStockServiceImpl implements PartnerProductStockService {

    private PartnerProductStockRepository partnerProductStockRepository;

    private MaterialPartnerRelationService mprService;

    @Override
    public PartnerProductStock create(PartnerProductStock partnerProductStock) {
        return partnerProductStockRepository.save(partnerProductStock);
    }

    @Override
    public List<PartnerProductStock> findAll() {
        return partnerProductStockRepository.findAllByType(DT_StockTypeEnum.PRODUCT);
    }

    @Override
    public List<PartnerProductStock> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return partnerProductStockRepository.findAllByMaterial_OwnMaterialNumberAndType(ownMaterialNumber, DT_StockTypeEnum.PRODUCT);
    }

    @Override
    public PartnerProductStock update(PartnerProductStock partnerProductStock) {

        Optional<PartnerProductStock> existingStock = partnerProductStockRepository.findById(partnerProductStock.getUuid());

        if (existingStock.isPresent() && existingStock.get().getType() == DT_StockTypeEnum.PRODUCT) {
            return partnerProductStockRepository.save(partnerProductStock);
        } else
            return null;
    }

    @Override

    public List<PartnerProductStock> findAllByOwnMaterialNumberAndPartnerUuid(String ownMaterialNumber, UUID partnerUuid) {
        return partnerProductStockRepository.findAllByMaterial_OwnMaterialNumberAndTypeAndSupplierPartner_Uuid(
            ownMaterialNumber, DT_StockTypeEnum.PRODUCT, partnerUuid);
    }

    @Override
    public List<PartnerProductStock> findAllByPartnerMaterialNumber(Partner partner, String partnerMaterialNumber) {

        var materialsList  = mprService.findAllByPartnerMaterialNumber(partnerMaterialNumber);
        return partnerProductStockRepository
            .findAllBySupplierPartner_Uuid(partner.getUuid())
            .stream()
            .filter(pps -> materialsList.contains(pps.getMaterial()))
            .collect(Collectors.toList());
    }

    @Override
    public List<PartnerProductStock> findAllByPartnerAndMaterialAndLocationAndMeasurementUnit(Partner partner, Material material,
                                                                                              String locationId, LocationIdTypeEnum locationIdType, MeasurementUnit measurementUnit) {
        return partnerProductStockRepository.
            findAllBySupplierPartnerAndMaterialAndLocationIdAndLocationIdTypeAndMeasurementUnit(partner, material, locationId,
                locationIdType, measurementUnit);
    }
}
