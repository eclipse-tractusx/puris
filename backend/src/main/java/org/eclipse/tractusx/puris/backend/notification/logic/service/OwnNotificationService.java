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

package org.eclipse.tractusx.puris.backend.notification.logic.service;

import java.util.List;

import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.notification.domain.model.OwnNotification;
import org.eclipse.tractusx.puris.backend.notification.domain.repository.OwnNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class OwnNotificationService extends NotificationService<OwnNotification, OwnNotificationRepository>{

    public OwnNotificationService(OwnNotificationRepository ownNotificationRepository, PartnerService partnerService, MaterialPartnerRelationService mpr) {
        super(ownNotificationRepository, partnerService, mpr);
    }

    public List<OwnNotification>  findAllByPartnerBpnl(String bpnl) {
        return repository.findAll().stream().filter(notification -> notification.getPartner().getBpnl().equals(bpnl)).toList();
    }

    public boolean validate(OwnNotification notification) {
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

    public boolean validateMaterials(OwnNotification notification) {
        if (notification.getMaterials() == null || notification.getMaterials().isEmpty()) {
            return true;
        }
        return mprService.findAll().stream().filter(mpr ->
            mpr.getPartner().equals(notification.getPartner()) &&
            notification.getMaterials().contains(mpr.getMaterial())).count() == notification.getMaterials().size();
    }

    public boolean validateSites(OwnNotification notification) {
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
