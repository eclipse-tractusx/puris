/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.adapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.MaterialSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.OwnDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestSammMapper {
    @Autowired
    private OwnDemandAndCapacityNotificationService ownDemandAndCapacityNotificationService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private PartnerService partnerService;
 
    public DataExchangeRequestSamm ownDataExchangeRequestToSamm(OwnDataExchangeRequest request) {
        Partner partner = request.getPartner();
        List<MaterialSamm> materialsAffected = new ArrayList<>();
        if (request.getMaterials() != null) {
            for (Material material : request.getMaterials()) {
                MaterialPartnerRelation mpr = mprService.find(material, partner);
                if (mpr != null && mpr.isPartnerSuppliesMaterial()) {
                    // partner supplies the material to us
                    materialsAffected.add(new MaterialSamm(
                        mpr.getPartnerCXNumber(),
                        material.getOwnMaterialNumber(),
                        mpr.getPartnerMaterialNumber()
                    ));
                } else {
                    // we supply the material to the partner
                    materialsAffected.add(new MaterialSamm(
                        material.getMaterialNumberCx(),
                        mpr != null ? mpr.getPartnerMaterialNumber() : null,
                        material.getOwnMaterialNumber()
                    ));
                }
            }
        }
 
        var builder = DataExchangeRequestSamm.builder();
 
        return builder
                .requestId(request.getRequestId())
                .sourceDisruptionId(request.getSourceDisruptionId())
                .leadingRootCause(request.getLeadingRootCause())
                .effect(request.getEffect())
                .materialsAffected(materialsAffected)
                .affectedSitesSender(toBpnsList(request.getAffectedSitesSender()))
                .affectedSitesRecipient(toBpnsList(request.getAffectedSitesRecipient()))
                .criticality(request.getCriticality())
                .desiredStartDateTime(request.getDesiredStartDateTime())
                .desiredEndDateTime(request.getDesiredEndDateTime())
                .requestedTypes(request.getRequestedTypes() != null ? new ArrayList<>(request.getRequestedTypes()): null)
                .text(request.getText())
                .timestamp(request.getTimestamp())
                .build();
    }
 
    /**
     * Maps an incoming request.
     * If an own DCN to the sender with the same sourceDisruptionId exists, the request is linked to it. Otherwise it is
     * stored without DCN, as a request on a suspected disruption.
     */
    public ReportedDataExchangeRequest sammToReportedDataExchangeRequest(Partner partner, DataExchangeRequestSamm samm) {
        List<Material> materials = resolveRelatedMaterials(partner, samm);
        if (materials.isEmpty()) {
            log.error("Rejecting data exchange request {}.", samm.getRequestId());
            return null;
        }
 
        OwnDemandAndCapacityNotification notification = findMatchingOwnNotification(partner, samm.getSourceDisruptionId());
        if (notification == null) {
            log.info("No own notification to {} for source disruption id {}", partner.getBpnl(), samm.getSourceDisruptionId());
        }
 
        return ReportedDataExchangeRequest.builder()
                .requestId(samm.getRequestId())
                .partner(partner)
                .notification(notification)
                .sourceDisruptionId(samm.getSourceDisruptionId())
                .leadingRootCause(samm.getLeadingRootCause())
                .effect(samm.getEffect())
                .materials(materials)
                .affectedSitesSender(resolveSites(partner, samm.getAffectedSitesSender()))
                .affectedSitesRecipient(resolveSites(partnerService.getOwnPartnerEntity(), samm.getAffectedSitesRecipient()))
                .criticality(samm.getCriticality())
                .desiredStartDateTime(samm.getDesiredStartDateTime())
                .desiredEndDateTime(samm.getDesiredEndDateTime())
                .requestedTypes(samm.getRequestedTypes() != null ? new ArrayList<>(samm.getRequestedTypes()) : null)
                .text(samm.getText())
                .timestamp(samm.getTimestamp())
                .build();
    }
    
    private List<Material> resolveRelatedMaterials(Partner sender, DataExchangeRequestSamm samm) {
        Map<String, Material> materials = new LinkedHashMap<>();
        if (samm.getMaterialsAffected() == null) {
            return new ArrayList<>();
        }
        for (MaterialSamm materialSamm : samm.getMaterialsAffected()) {
            if (materialSamm == null) {
                continue;
            }
            Material material = resolveMaterialDeliveredToSender(sender, materialSamm);
            if (material == null) {
                material = resolveMaterialDeliveredBySender(sender, materialSamm);
            }
            if (material == null) {
                continue;
            }
            materials.putIfAbsent(material.getOwnMaterialNumber(), material);
        }
        return new ArrayList<>(materials.values());
    }
 
    private Material resolveMaterialDeliveredToSender(Partner sender, MaterialSamm materialSamm) {
        if (materialSamm.getMaterialGlobalAssetId() != null) {
            Material material = materialService.findByMaterialNumberCx(materialSamm.getMaterialGlobalAssetId());
            MaterialPartnerRelation mpr = material != null ? mprService.find(material, sender) : null;
            if (mpr != null && mpr.isPartnerBuysMaterial()) {
                return material;
            }
        }
        if (materialSamm.getMaterialNumberSupplier() != null) {
            Material material = materialService.findByOwnMaterialNumber(materialSamm.getMaterialNumberSupplier());
            MaterialPartnerRelation mpr = material != null ? mprService.find(material, sender) : null;
            if (mpr != null && mpr.isPartnerBuysMaterial()) {
                return material;
            }
        }
        if (materialSamm.getMaterialNumberCustomer() != null) {
            List<MaterialPartnerRelation> mprs = mprService.findAllByCustomerPartnerAndPartnerMaterialNumber(sender, materialSamm.getMaterialNumberCustomer());
            if (mprs != null && !mprs.isEmpty()) {
                return mprs.getFirst().getMaterial();
            }
        }
        return null;
    }
 
    private Material resolveMaterialDeliveredBySender(Partner sender, MaterialSamm materialSamm) {
        if (materialSamm.getMaterialGlobalAssetId() != null) {
            MaterialPartnerRelation mpr = mprService.findByPartnerAndPartnerCXNumber(sender, materialSamm.getMaterialGlobalAssetId());
            if (mpr != null && mpr.isPartnerSuppliesMaterial()) {
                return mpr.getMaterial();
            }
        }
        if (materialSamm.getMaterialNumberCustomer() != null) {
            Material material = materialService.findByOwnMaterialNumber(materialSamm.getMaterialNumberCustomer());
            MaterialPartnerRelation mpr = material != null ? mprService.find(material, sender) : null;
            if (mpr != null && mpr.isPartnerSuppliesMaterial()) {
                return material;
            }
        }
        if (materialSamm.getMaterialNumberSupplier() != null) {
            List<MaterialPartnerRelation> mprs = mprService.findAllBySupplierPartnerAndPartnerMaterialNumber(sender, materialSamm.getMaterialNumberSupplier());
            if (mprs != null && !mprs.isEmpty()) {
                return mprs.getFirst().getMaterial();
            }
        }
        return null;
    }
 
    private OwnDemandAndCapacityNotification findMatchingOwnNotification(Partner partner, UUID sourceDisruptionId) {
        if (sourceDisruptionId == null) {
            return null;
        }
        List<OwnDemandAndCapacityNotification> candidates = ownDemandAndCapacityNotificationService.findBySourceDisruptionIdAndPartnerBpnl(sourceDisruptionId, partner.getBpnl());
        if (candidates == null) {
            return null;
        }
        return candidates.stream()
            .max(Comparator.comparing(OwnDemandAndCapacityNotification::getContentChangedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(null);
    }
 
    /**
     * Unknown sites are dropped, as for DCNs.
     */
    private static List<Site> resolveSites(Partner owner, List<String> bpnsList) {
        if (bpnsList == null || bpnsList.isEmpty() || owner == null || owner.getSites() == null) {
            return new ArrayList<>();
        }
        return owner.getSites().stream()
            .filter(site -> bpnsList.contains(site.getBpns()))
            .collect(Collectors.toList());
    }
 
    private static List<String> toBpnsList(List<Site> sites) {
        if (sites == null) {
            return new ArrayList<>();
        }
        return sites.stream().map(Site::getBpns).collect(Collectors.toList());
    }
}
