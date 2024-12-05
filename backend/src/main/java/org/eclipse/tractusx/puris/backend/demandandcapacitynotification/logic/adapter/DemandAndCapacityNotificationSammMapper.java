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
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.adapter;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.DemandAndCapacityNotificationSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DemandAndCapacityNotificationSammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private PartnerService partnerService;

    public DemandAndCapacityNotificationSamm ownNotificationToSamm(OwnDemandAndCapacityNotification notification) {
        List<String> affectedMaterialGlobalAssetIds = new ArrayList<>();
        List<String> affectedCustomerMaterialIds = new ArrayList<>();
        List<String> affectedSupplierMaterialIds = new ArrayList<>();
        var mprs = notification.getMaterials().stream().map(material -> mprService.find(material, notification.getPartner())).toList();
        switch (notification.getEffect()) {
            case DEMAND_INCREASE, DEMAND_REDUCTION -> {
                // we are customer, recipient is supplier
                affectedMaterialGlobalAssetIds.addAll(mprs.stream().map(MaterialPartnerRelation::getPartnerCXNumber).toList());
                affectedCustomerMaterialIds.addAll(mprs.stream().map(mpr -> mpr.getMaterial().getOwnMaterialNumber()).toList());
                affectedSupplierMaterialIds.addAll(mprs.stream().map(MaterialPartnerRelation::getPartnerMaterialNumber).toList());
            }
            case CAPACITY_INCREASE, CAPACITY_REDUCTION -> {
                // we are supplier, recipient is customer
                affectedMaterialGlobalAssetIds.addAll(mprs.stream().map(mpr -> mpr.getMaterial().getMaterialNumberCx()).toList());
                affectedSupplierMaterialIds.addAll(mprs.stream().map(mpr -> mpr.getMaterial().getOwnMaterialNumber()).toList());
                affectedCustomerMaterialIds.addAll(mprs.stream().map(MaterialPartnerRelation::getPartnerMaterialNumber).toList());
            }
            default -> throw new IllegalStateException("Unexpected value: " + notification.getEffect());
        }

        List<String> affectedSitesBpnsSender = notification.getAffectedSitesSender().stream().map(site -> site.getBpns()).collect(Collectors.toList());
        List<String> affectedSitesBpnsRecipient = notification.getAffectedSitesRecipient().stream().map(site -> site.getBpns()).collect(Collectors.toList());
        var builder = DemandAndCapacityNotificationSamm.builder();
        var samm = builder
                .notificationId(notification.getNotificationId().toString())
                .relatedNotificationId(notification.getRelatedNotificationId() != null ? notification.getRelatedNotificationId().toString() : null)
                .sourceNotificationId(notification.getSourceNotificationId() != null ? notification.getSourceNotificationId().toString() : null)
                .text(notification.getText())
                .leadingRootCause(notification.getLeadingRootCause())
                .effect(notification.getEffect())
                .status(notification.getStatus())
                .materialGlobalAssetId(affectedMaterialGlobalAssetIds)
                .materialNumberCustomer(affectedCustomerMaterialIds)
                .materialNumberSupplier(affectedSupplierMaterialIds)
                .affectedSitesSender(affectedSitesBpnsSender)
                .affectedSitesRecipient(affectedSitesBpnsRecipient)
                .startDateOfEffect(notification.getStartDateOfEffect())
                .expectedEndDateOfEffect(notification.getExpectedEndDateOfEffect())
                .contentChangedAt(notification.getContentChangedAt())
                .build();

        return samm;
    }

    public ReportedDemandAndCapacityNotification sammToReportedDemandAndCapacityNotification(DemandAndCapacityNotificationSamm samm, Partner partner) {
        var builder = ReportedDemandAndCapacityNotification.builder();
        HashSet<Material> materialsSet = new HashSet<>();
        switch (samm.getEffect()) {
            case DEMAND_INCREASE, DEMAND_REDUCTION -> {
                // sender of Samm is customer, we are supplier
                samm.getMaterialGlobalAssetId().forEach(assetId -> materialsSet.add(materialService.findByMaterialNumberCx(assetId)));
                samm.getMaterialNumberSupplier().forEach(matNbr -> materialsSet.add(materialService.findByOwnMaterialNumber(matNbr)));
                samm.getMaterialNumberCustomer()
                    .stream()
                    .flatMap(matNbr -> mprService.findAllByPartnerMaterialNumber(matNbr).stream())
                    .map(material -> mprService.find(material, partner))
                    .filter(mpr -> mpr.getPartner().equals(partner))
                    .forEach(mpr -> materialsSet.add(mpr.getMaterial()));
            }
            case CAPACITY_INCREASE, CAPACITY_REDUCTION -> {
                // sender of Samm is supplier, we are customer
                samm.getMaterialGlobalAssetId().forEach(assetId -> materialsSet.add(mprService.findByPartnerAndPartnerCXNumber(partner, assetId).getMaterial()));
                samm.getMaterialNumberCustomer().forEach(matNbr -> materialsSet.add(materialService.findByOwnMaterialNumber(matNbr)));
                samm.getMaterialNumberSupplier()
                    .stream()
                    .flatMap(matNbr -> mprService.findAllByPartnerMaterialNumber(matNbr).stream())
                    .map(material -> mprService.find(material, partner))
                    .filter(mpr -> mpr.getPartner().equals(partner))
                    .forEach(mpr -> materialsSet.add(mpr.getMaterial()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + samm.getEffect());
        }

        var affectedSitesSender = partner.getSites().stream()
            .filter(site -> samm.getAffectedSitesSender().contains(site.getBpns()))
            .collect(Collectors.toList());
        var affectedSitesBpnsRecipient = partnerService.getOwnPartnerEntity().getSites().stream()
            .filter(site -> samm.getAffectedSitesRecipient().contains(site.getBpns()))
            .collect(Collectors.toList());
        var notification = builder
                .notificationId(UUID.fromString(samm.getNotificationId()))
                .relatedNotificationId(samm.getRelatedNotificationId() != null ? UUID.fromString(samm.getRelatedNotificationId()) : null)
                .sourceNotificationId(samm.getSourceNotificationId() != null ? UUID.fromString(samm.getSourceNotificationId()) : null)
                .text(samm.getText())
                .leadingRootCause(samm.getLeadingRootCause())
                .effect(samm.getEffect())
                .status(samm.getStatus())
                .startDateOfEffect(samm.getStartDateOfEffect())
                .expectedEndDateOfEffect(samm.getExpectedEndDateOfEffect())
                .partner(partner)
                .materials(new ArrayList<>(materialsSet))
                .affectedSitesSender(affectedSitesSender)
                .affectedSitesRecipient(affectedSitesBpnsRecipient)
                .contentChangedAt(samm.getContentChangedAt())
                .build();
        return notification;
    }
}
