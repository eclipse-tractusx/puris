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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.DeliveryResponsibilityEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.adapter.DeliveryInformationSammMapper;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.DeliveryInformation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
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
import java.util.function.Predicate;

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
        MaterialPartnerRelation mpr = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumberCx);

        if (material == null) {
            // Could not identify partner cx number. I.e. we do not have that partner's
            // CX id in one of our MaterialPartnerRelation entities. Try to fix this by
            // looking for MPR's, where that partner is a supplier and where we don't have
            // a partnerCXId yet. Of course this can only work if there was previously an MPR
            // created, but for some unforeseen reason, the initial PartTypeRetrieval didn't succeed.
            // Sidenote: This still means that the ShellDescriptor has not been created and someone tries to access our
            // api without using the href from DTR
            if (mpr == null) {
                log.warn("Could not find " + materialNumberCx + " from partner " + partner.getBpnl());
                log.warn("ATTENTION: PARTNER WITH BPNL {} ACCESSED THE SERVICE FOR A MATERIAL WITHOUT SHELL DESCRIPTOR " +
                    "IN DTR. THIS MIGHT BE A SECURITY ISSUE!", partner.getBpnl());
                mprService.triggerPartTypeRetrievalTask(partner);
                // check if cx id is now given
                mpr = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumberCx);
                if (mpr == null) {
                    log.error("No material partner relation for BPNL '{}' and material global asset id '{}'." +
                            "Abort answering delivery request.",
                        partner.getBpnl(),
                        materialNumberCx
                    );
                    return null;
                }
            }
            material = mpr.getMaterial();
        }

        if (material == null) {
            log.error("Unknown Material " + materialNumberCx);
            return null;
        }

        // if the material number cx has been defined by us, we're returning information as an supplier
        // that means our partner is acting as customer
        // search mpr again by partner and material to have one mpr at hand independent of partner role
        boolean partnerIsCustomer = material.getMaterialNumberCx().equals(materialNumberCx);
        mpr = mprService.find(material, partner);
        if (mpr == null ||
            (partnerIsCustomer && !mpr.isPartnerBuysMaterial()) ||
            (!partnerIsCustomer && !mpr.isPartnerSuppliesMaterial())
        ) {
            // only send an answer if partner is registered as supplier
            log.warn(
                "Partner acts as role '{}' but tried to access data for material number cx '{}' for the " +
                    "opposite role '{}'. Returning no data at all.",
                partnerIsCustomer ? "Customer" : "Supplier",
                materialNumberCx,
                partnerIsCustomer ? "Supplier" : "Customer"
            );
            return null;
        }

        List<OwnDelivery> currentDeliveries = ownDeliveryService.findAllByFilters(
            Optional.of(material.getOwnMaterialNumber()),
            Optional.empty(),
            Optional.of(partner.getBpnl()),
            Optional.empty(),
            Optional.empty());

        log.debug(
            "Found '{}' deliveries for material number cx '{}' for partner with bpnl '{}' asking in role '{}'.",
            currentDeliveries.size(),
            materialNumberCx,
            bpnl,
            partnerIsCustomer ? "Customer" : "Supplier"
        );
        return sammMapper.ownDeliveryToSamm(currentDeliveries, partner, material);
    }

    public RefreshResult doReportedDeliveryRequest(Partner partner, Material material) {
        List<RefreshError> errors = new ArrayList<>();
        try {
            var mpr = mprService.find(material, partner);
            if (mpr.getPartnerCXNumber() == null) {
                mprService.triggerPartTypeRetrievalTask(partner);
                mpr = mprService.find(material, partner);
            }
            var direction = material.isMaterialFlag() ? DirectionCharacteristic.OUTBOUND : DirectionCharacteristic.INBOUND;
            var data = edcAdapterService.doSubmodelRequest(AssetType.DELIVERY_SUBMODEL, mpr, direction, 1);
            var samm = objectMapper.treeToValue(data, DeliveryInformation.class);
            var deliveries = sammMapper.sammToReportedDeliveries(samm, partner);
            for (var delivery : deliveries) {
                var deliveryPartner = delivery.getPartner();
                var deliveryMaterial = delivery.getMaterial();
                if (!partner.equals(deliveryPartner) || !material.equals(deliveryMaterial)) {
                    errors.add(new RefreshError(List.of("Received inconsistent data from " + partner.getBpnl())));
                    continue;
                }

                List<String> validationErrors = reportedDeliveryService.validateWithDetails(delivery);
                if (!validationErrors.isEmpty()) {
                    errors.add(new RefreshError(validationErrors));
                }
            }

            if (!errors.isEmpty()) {
                log.warn("Validation errors found for ReportedDelivery request from partner {} for material {}: {}", 
                        partner.getBpnl(), material.getOwnMaterialNumber(), errors);
                return new RefreshResult("Validation failed for reported deliveries", errors);
            }
   
            // delete older data:
            var oldDeliveries = reportedDeliveryService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.empty(), Optional.of(partner.getBpnl()), Optional.empty(), Optional.empty());
            for (var oldDelivery : oldDeliveries) {
                reportedDeliveryService.delete(oldDelivery.getUuid());
            }
            for (var newDelivery : deliveries) {
                reportedDeliveryService.create(newDelivery);
            }
            log.info("Successfully updated ReportedDelivery for {} and partner {}", 
                        material.getOwnMaterialNumber(), partner.getBpnl());
            materialService.updateTimestamp(material.getOwnMaterialNumber());
            return new RefreshResult("Successfully processed all reported deliveries", errors);
        } catch (Exception e) {
            log.error("Error in Reported Deliveries Request for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
            errors.add(new RefreshError(List.of("System error: " + e.getMessage())));
            return new RefreshResult("System error occurred during processing", errors);
        }
    }

    /**
     * filters OwnDelivery entities to be returned to a partner based on his role
     * <p>
     * Based on accuracy the following rules apply:
     * <li>use role derived from cx id and incoterm responsibility</li>
     * <li>use mpr role, if only one is present and no incoterms are given</li>
     *
     * @param partnerIsCustomer to use as role in combination with incoterms (derived from material number cx)
     * @param mpr               to check the supplies / orders relation in case of missing incoterms
     * @return Predicate<OwnDelivery> returning true if incoterms and role match or no incoterm but only one role on mpr is given, else false
     */
    public static Predicate<OwnDelivery> partnerRoleDirectionPredicate(boolean partnerIsCustomer, MaterialPartnerRelation mpr) {
        return delivery -> {
            // return all, if no incoterm is set but partner only acts in one role
            if (delivery.getIncoterm() == null) {
                // unlikely that we have a mpr without any role set but then we also don't return something
                if (mpr.isPartnerBuysMaterial() && mpr.isPartnerSuppliesMaterial()) {
                    return false;
                } else return mpr.isPartnerBuysMaterial() || mpr.isPartnerSuppliesMaterial();
            }

            // if we have incoterms set, filter more sophisticatedly based on responsbility of incoterms and role
            // derived from material number cx
            DeliveryResponsibilityEnumeration responsibility = delivery.getIncoterm().getResponsibility();
            if (partnerIsCustomer) {
                return responsibility == DeliveryResponsibilityEnumeration.SUPPLIER ||
                    responsibility == DeliveryResponsibilityEnumeration.PARTIAL;
            } else {
                return responsibility == DeliveryResponsibilityEnumeration.CUSTOMER ||
                    responsibility == DeliveryResponsibilityEnumeration.PARTIAL;
            }
        };
    }
}
