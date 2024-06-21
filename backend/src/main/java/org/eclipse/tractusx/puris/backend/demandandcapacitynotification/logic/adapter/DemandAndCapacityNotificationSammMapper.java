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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.DemandAndCapacityNotificationSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemandAndCapacityNotificationSammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private PartnerService partnerService;

    public DemandAndCapacityNotificationSamm ownNotificationToSamm(OwnDemandAndCapacityNotification notification) {
        List<String> affectedMaterialGlobalAssetIds = notification.getMaterials().stream().map(mat -> mat.getMaterialNumberCx()).collect(Collectors.toList());
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
        var materials = samm.getMaterialGlobalAssetId().stream()
            .map(partnerMaterialNumberCx -> mprService.findByPartnerAndPartnerCXNumber(partner, partnerMaterialNumberCx).getMaterial())
            .collect(Collectors.toList());
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
                .materials(materials)
                .affectedSitesSender(affectedSitesSender)
                .affectedSitesRecipient(affectedSitesBpnsRecipient)
                .contentChangedAt(samm.getContentChangedAt())
                .build();
        return notification;
    }
}
