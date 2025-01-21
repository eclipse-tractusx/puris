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

package org.eclipse.tractusx.puris.backend.production.logic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.logic.adapter.PlannedProductionSammMapper;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.PlannedProductionOutput;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
/**
 * This class is a Service that handles requests for Planned Production Output
 */
public class ProductionRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private OwnProductionService ownProductionService;
    @Autowired
    private ReportedProductionService reportedProductionService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private PlannedProductionSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public PlannedProductionOutput handleProductionSubmodelRequest(String bpnl, String materialNumberCx) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            return null;
        }
        Material material = materialService.findByMaterialNumberCx(materialNumberCx);
        if (material == null) {
            return null;
        }
        if (!mprService.find(material, partner).isPartnerBuysMaterial()) {
            // only send an answer if partner is registered as customer
            return null;
        }
        var currentProduction = ownProductionService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()), Optional.empty(), Optional.empty());
        return sammMapper.ownProductionToSamm(currentProduction, partner, material);
    }

    public void doReportedProductionRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            var data = edcAdapterService.doSubmodelRequest(AssetType.PRODUCTION_SUBMODEL, mpr, DirectionCharacteristic.OUTBOUND, 1);
            var samm = objectMapper.treeToValue(data, PlannedProductionOutput.class);
            var productions = sammMapper.sammToReportedProduction(samm, partner);
            for (var production : productions) {
                var productionPartner = production.getPartner();
                var productionMaterial = production.getMaterial();
                if (!partner.equals(productionPartner) || !material.equals(productionMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl());
                    return;
                }
            }
            // delete older data:
            var oldProductions = reportedProductionService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()), Optional.empty(), Optional.empty());
            for (var oldProduction : oldProductions) {
                reportedProductionService.delete(oldProduction.getUuid());
            }
            for (var newProduction : productions) {
                reportedProductionService.create(newProduction);
            }
            log.info("Updated ReportedProduction for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());

            materialService.updateTimestamp(material.getOwnMaterialNumber());
        } catch (Exception e) {
            log.error("Error in ReportedProductionRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }
}
