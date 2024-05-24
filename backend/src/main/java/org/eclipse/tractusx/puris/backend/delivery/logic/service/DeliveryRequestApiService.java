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

package org.eclipse.tractusx.puris.backend.delivery.logic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.SubmodelType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.delivery.logic.adapter.DeliveryInformationSammMapper;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.DeliveryInformation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
/**
 * This class is a Service that handles requests for Delivery Information
 */
public class DeliveryRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private OwnDeliveryService ownDeliveryService;
    @Autowired
    private ReportedDeliveryService reportedDeliveryService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DeliveryInformationSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public DeliveryInformation handleDeliverySubmodelRequest(String bpnl, String materialNumberCx) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL " + bpnl);
            return null;
        }

        Material material = materialService.findByMaterialNumberCx(materialNumberCx);
        if (material == null) {
            // Could not identify partner cx number. I.e. we do not have that partner's
            // CX id in one of our MaterialPartnerRelation entities. Try to fix this by
            // looking for MPR's, where that partner is a supplier and where we don't have
            // a partnerCXId yet. Of course this can only work if there was previously an MPR
            // created, but for some unforeseen reason, the initial PartTypeRetrieval didn't succeed.
            log.warn("Could not find " + materialNumberCx + " from partner " + partner.getBpnl());
            mprService.triggerPartTypeRetrievalTask(partner);
            material = materialService.findByMaterialNumberCx(materialNumberCx);
        }

        if (material == null) {
            log.error("Unknown Material " + materialNumberCx);
            return null;
        }

        var mpr = mprService.find(material,partner);
        if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
            // only send an answer if partner is registered as supplier
            return null;
        }

        var currentDeliveries = ownDeliveryService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.empty(), Optional.of(partner.getBpnl()));
        return sammMapper.ownDeliveryToSamm(currentDeliveries, partner, material);
    }

    public void doReportedDeliveryRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            if (mpr.getPartnerCXNumber() == null) {
                mprService.triggerPartTypeRetrievalTask(partner);
                mpr = mprService.find(material, partner);
            }
            var direction = material.isMaterialFlag() ? DirectionCharacteristic.OUTBOUND : DirectionCharacteristic.INBOUND;
            var data = edcAdapterService.doSubmodelRequest(SubmodelType.DELIVERY, mpr, direction, 1);
            var samm = objectMapper.treeToValue(data, DeliveryInformation.class);
            var deliveries = sammMapper.sammToReportedDeliveries(samm, partner);
            for (var delivery : deliveries) {
                var deliveryPartner = delivery.getPartner();
                var deliveryMaterial = delivery.getMaterial();
                if (!partner.equals(deliveryPartner) || !material.equals(deliveryMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl() + "\n" + deliveries);
                    return;
                }
            }
            // delete older data:
            var oldDeliveries = reportedDeliveryService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.empty(), Optional.of(partner.getBpnl()));
            for (var oldDelivery : oldDeliveries) {
                reportedDeliveryService.delete(oldDelivery.getUuid());
            }
            for (var newDelivery : deliveries) {
                reportedDeliveryService.create(newDelivery);
            }
            log.info("Updated Reported Deliveries for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
        } catch (Exception e) {
            log.error("Error in Reported Deliveries Request for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }
}
