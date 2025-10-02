/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.demand.logic.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.adapter.ShortTermMaterialDemandSammMapper;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.ShortTermMaterialDemand;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshError;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshResult;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
/**
 * This class is a Service that handles requests for Planned Production Output
 */
public class DemandRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private OwnDemandService ownDemandService;
    @Autowired
    private ReportedDemandService reportedDemandService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private ShortTermMaterialDemandSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public ShortTermMaterialDemand handleDemandSubmodelRequest(String bpnl, String materialNumberCx) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        Material material = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumberCx).getMaterial();

        if (material == null) {
            // Could not identify partner cx number. I.e. we do not have that partner's
            // CX id in one of our MaterialPartnerRelation entities. Try to fix this by
            // looking for MPR's, where that partner is a supplier and where we don't have
            // a partnerCXId yet. Of course this can only work if there was previously an MPR
            // created, but for some unforeseen reason, the initial PartTypeRetrieval didn't succeed.
            log.warn("Could not find " + materialNumberCx + " from partner " + partner.getBpnl());
            mprService.triggerPartTypeRetrievalTask(partner);
            material = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumberCx).getMaterial();
        }

        if (material == null) {
            log.error("Unknown Material");
            return null;
        }
        var mpr = mprService.find(material,partner);
        if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
            // only send an answer if partner is registered as supplier
            return null;
        }

        var currentDemands = ownDemandService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()), Optional.empty());
        return sammMapper.ownDemandToSamm(currentDemands, partner, material);
    }

    public RefreshResult doReportedDemandRequest(Partner partner, Material material) {
        List<RefreshError> errors = new ArrayList<>();
        try {
            var mpr = mprService.find(material, partner);
            if (mpr.getPartnerCXNumber() == null) {
                mprService.triggerPartTypeRetrievalTask(partner);
                mpr = mprService.find(material, partner);
            }
            var data = edcAdapterService.doSubmodelRequest(AssetType.DEMAND_SUBMODEL, mpr, DirectionCharacteristic.INBOUND, 1);
            var samm = objectMapper.treeToValue(data, ShortTermMaterialDemand.class);
            var demands = sammMapper.sammToReportedDemand(samm, partner);

            demands.get(demands.size() - 1).setQuantity(-20.0);
            
            for (var demand : demands) {
                var demandPartner = demand.getPartner();
                var demandMaterial = demand.getMaterial();
                if (!partner.equals(demandPartner) || !material.equals(demandMaterial)) {
                    errors.add(new RefreshError(List.of("Received inconsistent data: partner or material mismatch")));
                    continue;
                }

                List<String> validationErrors = reportedDemandService.validateWithDetails(demand);
                if (!validationErrors.isEmpty()) {
                    errors.add(new RefreshError(validationErrors));
                }
            }

            if (!errors.isEmpty()) {
                log.warn("Validation errors found for ReportedDemand request from partner {} for material {}: {}", 
                        partner.getBpnl(), material.getOwnMaterialNumber(), errors);
                return new RefreshResult("Validation failed for reported demands", errors);
            }

            // delete older data:
            var oldDemands = reportedDemandService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()), Optional.empty());
            for (var oldDemand : oldDemands) {
                reportedDemandService.delete(oldDemand.getUuid());
            }
            for (var newDemand : demands) {
                reportedDemandService.create(newDemand);
            }
            log.info("Successfully updated ReportedDemand for {} and partner {}", 
                material.getOwnMaterialNumber(), partner.getBpnl());
                materialService.updateTimestamp(material.getOwnMaterialNumber());
            return new RefreshResult("Successfully processed all reported demands", errors);
        } catch (Exception e) {
            log.error("Error in ReportedDemandRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
            errors.add(new RefreshError(List.of("System error: " + e.getMessage())));
            return new RefreshResult("Error in ReportedDemandRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), errors);
        }
    }
}
