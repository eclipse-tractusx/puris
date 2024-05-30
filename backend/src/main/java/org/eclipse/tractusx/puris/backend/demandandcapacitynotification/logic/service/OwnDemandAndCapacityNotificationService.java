/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service;

import java.util.List;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.OwnDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class OwnDemandAndCapacityNotificationService extends DemandAndCapacityNotificationService<OwnDemandAndCapacityNotification, OwnDemandAndCapacityNotificationRepository>{

    public OwnDemandAndCapacityNotificationService(OwnDemandAndCapacityNotificationRepository ownNotificationRepository, PartnerService partnerService, MaterialPartnerRelationService mpr) {
        super(ownNotificationRepository, partnerService, mpr);
    }

    public List<OwnDemandAndCapacityNotification>  findAllByPartnerBpnl(String bpnl) {
        return repository.findAll().stream().filter(notification -> notification.getPartner().getBpnl().equals(bpnl)).toList();
    }

    @Override
    public boolean validate(OwnDemandAndCapacityNotification notification) {
        return notification.getPartner() != null &&
                notification.getText() != null &&
                notification.getLeadingRootCause() != null &&
                notification.getEffect() != null &&
                notification.getStatus() != null &&
                notification.getStartDateOfEffect() != null &&
                notification.getExpectedEndDateOfEffect() != null &&
                validateMaterials(notification) &&
                validateSites(notification);
    }

    public boolean validateMaterials(OwnDemandAndCapacityNotification notification) {
        if (notification.getMaterials() == null || notification.getMaterials().isEmpty()) {
            return true;
        }
        return mprService.findAll().stream().filter(mpr ->
            mpr.getPartner().equals(notification.getPartner()) &&
            notification.getMaterials().contains(mpr.getMaterial())).count() == notification.getMaterials().size();
    }

    public boolean validateSites(OwnDemandAndCapacityNotification notification) {
        return (
            notification.getAffectedSitesRecipient() == null ||
            notification.getAffectedSitesRecipient().isEmpty() ||
            notification.getAffectedSitesRecipient().stream().allMatch(site -> notification.getPartner().getSites().contains(site))
        ) && (
            notification.getAffectedSitesSender() == null ||
            notification.getAffectedSitesSender().isEmpty() ||
            notification.getAffectedSitesSender().stream().allMatch(site -> partnerService.getOwnPartnerEntity().getSites().contains(site))
        );
    }
}
