/*
 * Copyright (c) 2026 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ChildData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ItemQuantity;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ValidityPeriodEntity;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for mapping Material BOM structure to SingleLevelBomAsPlanned SAMM
 * model.
 */
@Service
@Slf4j
public class SingleLevelBomAsPlannedSammMapper {

    @Autowired
    private MaterialRelationService materialRelationService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialPartnerRelationService materialPartnerRelationService;

    /**
     * Convert a Material's BOM structure to a SingleLevelBomAsPlanned SAMM.
     *
     * @param material the parent material whose BOM structure to convert
     * @return the SAMM representation of the single-level BOM as planned
     */
    public SingleLevelBomAsPlanned materialToSamm(Material material) {

        SingleLevelBomAsPlanned samm = new SingleLevelBomAsPlanned();
        samm.setCatenaXId(material.getMaterialNumberCx());

        if (!material.isProductFlag()) {
            log.debug("Material {} is not marked as product, returning empty BOM",
                    material.getOwnMaterialNumber());
            samm.setChildItems(new HashSet<>());
            return samm;
        }

        Set<ChildData> childItems = new HashSet<>();

        List<MaterialRelation> childRelations = materialRelationService.findAll().stream()
                .filter(rel -> rel.getParentOwnMaterialNumber().equals(material.getOwnMaterialNumber()))
                .collect(Collectors.toList());

        for (MaterialRelation materialRelation : childRelations) {
            String childMaterialNumber = materialRelation.getChildOwnMaterialNumber();
            Material childMaterial = materialService.findByOwnMaterialNumber(childMaterialNumber);

            if (childMaterial == null) {
                log.warn("Child material {} not found in database, skipping", childMaterialNumber);
                continue;
            }

            if (childMaterial.getMaterialNumberCx() == null || childMaterial.getMaterialNumberCx().isEmpty()) {
                log.warn("Child material {} has no CatenaX ID, skipping", childMaterialNumber);
                continue;
            }

            List<MaterialPartnerRelation> supplierRelations = materialPartnerRelationService
                    .findAllByOwnMaterialNumber(childMaterial.getOwnMaterialNumber()).stream()
                    .filter(MaterialPartnerRelation::isPartnerSuppliesMaterial)
                    .collect(Collectors.toList());

            if (supplierRelations.isEmpty()) {
                log.warn("No supplier MPR found for child material {}, skipping", childMaterialNumber);
                continue;
            }

            // Create one ChildData per supplier of this component
            for (MaterialPartnerRelation mpr : supplierRelations) {
                ChildData childData = createChildData(childMaterial, materialRelation, mpr.getPartner().getBpnl());
                childItems.add(childData);
            }
        }

        samm.setChildItems(childItems);
        return samm;
    }

    /**
     * Create a ChildData entry from a Material, MaterialRelation and supplier BPNL.
     *
     * @param childMaterial    the child material
     * @param materialRelation the MaterialRelation containing quantity and validity info
     * @param supplierBpnl     the BPNL of the partner that supplies the child component
     * @return the ChildData representation
     */
    private ChildData createChildData(Material childMaterial, MaterialRelation materialRelation, String supplierBpnl) {
        ChildData childData = new ChildData();

        Date createdOn = materialRelation.getCreatedOn() != null
                ? materialRelation.getCreatedOn()
                : new Date();
        childData.setCreatedOn(createdOn);

        double quantityValue = materialRelation.getQuantity();
        String unit = materialRelation.getMeasurementUnit().getValue();
        ItemQuantity quantity = new ItemQuantity(quantityValue, unit);
        childData.setQuantity(quantity);

        Date lastModifiedOn = materialRelation.getLastModifiedOn() != null
                ? materialRelation.getLastModifiedOn()
                : createdOn;
        childData.setLastModifiedOn(lastModifiedOn);

        ValidityPeriodEntity validityPeriod = null;
        if (materialRelation.getValidFrom() != null || materialRelation.getValidTo() != null) {
            validityPeriod = new ValidityPeriodEntity(
                    materialRelation.getValidFrom(),
                    materialRelation.getValidTo());
        }
        childData.setValidityPeriod(validityPeriod);

        childData.setBusinessPartner(supplierBpnl);
        childData.setCatenaXId(childMaterial.getMaterialNumberCx());

        return childData;
    }
}
