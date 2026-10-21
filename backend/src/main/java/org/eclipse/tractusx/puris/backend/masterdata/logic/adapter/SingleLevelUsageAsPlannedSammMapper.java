/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated 
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Anthropic Claude (claude-opus-5-5)
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.ExternalMaterialRelationRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ValidityPeriodEntity;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.ParentData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.SingleLevelUsageAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SingleLevelUsageAsPlannedSammMapper {
    @Autowired
    private ExternalMaterialRelationRepository externalMaterialRelationRepository;

    @Autowired
    private MaterialPartnerRelationService mprService;

    /**
     * Converts the usage of an own product (outbound material) to a SingleLevelUsageAsPlanned SAMM.
     *
     * @param product the own product
     * @return the SAMM representation, or null if the material is not a product
     */
    public SingleLevelUsageAsPlanned productToSingleLevelUsageAsPlannedSamm(Material product) {
        if (!product.isProductFlag()) {
            log.warn("Material {} is not a product, cannot generate SingleLevelUsageAsPlanned", product.getOwnMaterialNumber());
            return null;
        }
        Set<ParentData> parentItems = externalMaterialRelationRepository.findAllByMaterial(product).stream()
            .map(this::toParentData)
            .collect(Collectors.toSet());
        List<String> customers = mprService.findAllCustomersForOwnMaterialNumber(product.getOwnMaterialNumber()).stream()
            .map(Partner::getBpnl)
            .toList();
        return new SingleLevelUsageAsPlanned(product.getMaterialNumberCx(), parentItems, customers);
    }

    private ParentData toParentData(ExternalMaterialRelation relation) {
        ValidityPeriodEntity validityPeriod = null;
        if (relation.getValidityPeriod() != null) {
            validityPeriod = new ValidityPeriodEntity(relation.getValidityPeriod().getValidFrom(),
                relation.getValidityPeriod().getValidTo());
        }
        return new ParentData(
            new ItemQuantityEntity(relation.getQuantity(), relation.getMeasurementUnit()),
            relation.getCreatedOn(),
            relation.getPartner().getBpnl(),
            relation.getParentGlobalAssetId(),
            relation.getLastModifiedOn(),
            validityPeriod);
    }
}
