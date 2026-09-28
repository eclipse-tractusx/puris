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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ValidityPeriodEntity;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.ParentData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.SingleLevelUsageAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SingleLevelUsageAsPlannedSammMapper {
    @Autowired
    private MaterialRelationService materialRelationService;
 
    @Autowired
    private MaterialService materialService;
 
    @Autowired
    private VariablesService variablesService;

    /**
     * Convert the usage of an inbound material to a SingleLevelUsageAsPlanned SAMM.
     *
     * @param supplierMpr the MaterialPartnerRelation of the supplier of the material
     * @return the SAMM representation of the single-level usage as planned
     */
    public SingleLevelUsageAsPlanned materialPartnerRelationToSingleLevelUsageAsPlannedSamm(
            MaterialPartnerRelation supplierMpr) {
 
        Material childMaterial = supplierMpr.getMaterial();
 
        if (!supplierMpr.isPartnerSuppliesMaterial()) {
            log.warn("Partner {} does not supply material {}, cannot generate SingleLevelUsageAsPlanned", supplierMpr.getPartner().getBpnl(), childMaterial.getOwnMaterialNumber());
            return null;
        }
 
        if (supplierMpr.getPartnerCXNumber() == null) {
            log.warn("No CX number of supplier {} known for material {}, cannot generate SingleLevelUsageAsPlanned", supplierMpr.getPartner().getBpnl(), childMaterial.getOwnMaterialNumber());
            return null;
        }
 
        String ownBpnl = variablesService.getOwnBpnl();
        Set<ParentData> parentItems = new HashSet<>();
 
        List<MaterialRelation> parentRelations = materialRelationService.findAllParents(childMaterial.getOwnMaterialNumber());
 
        for (MaterialRelation materialRelation : parentRelations) {
            String parentMaterialNumber = materialRelation.getParentOwnMaterialNumber();
            Material parentMaterial = materialService.findByOwnMaterialNumber(parentMaterialNumber);
 
            if (parentMaterial == null) {
                log.warn("Parent material {} not found in database", parentMaterialNumber);
                continue;
            }
 
            if (!parentMaterial.isProductFlag() || parentMaterial.getMaterialNumberCx() == null) {
                log.warn("Parent material {} is not a product with a CX number", parentMaterialNumber);
                continue;
            }
 
            parentItems.add(createParentData(materialRelation, parentMaterial, ownBpnl));
        }

        return new SingleLevelUsageAsPlanned(supplierMpr.getPartnerCXNumber(), parentItems, List.of(ownBpnl));
    }
 
    /**
     * Create a ParentData entry from a MaterialRelation and the parent material.
     *
     * @param materialRelation the MaterialRelation containing quantity and validity info
     * @param parentMaterial   the own product the child gets used in
     * @param ownBpnl          the BPNL of the own company
     * @return the ParentData representation
     */
    private ParentData createParentData(MaterialRelation materialRelation, Material parentMaterial, String ownBpnl) {
        Date createdOn = materialRelation.getCreatedOn() != null ? materialRelation.getCreatedOn() : new Date();
 
        Date lastModifiedOn = materialRelation.getLastModifiedOn() != null ? materialRelation.getLastModifiedOn() : createdOn;
 
        ItemQuantityEntity quantity = new ItemQuantityEntity(materialRelation.getQuantity(), materialRelation.getMeasurementUnit());
 
        ValidityPeriodEntity validityPeriod = null;
        if (materialRelation.getValidFrom() != null || materialRelation.getValidTo() != null) {
            validityPeriod = new ValidityPeriodEntity(materialRelation.getValidFrom(), materialRelation.getValidTo());
        }
 
        return new ParentData(quantity, createdOn, ownBpnl, parentMaterial.getMaterialNumberCx(), lastModifiedOn, validityPeriod);
    }
}
