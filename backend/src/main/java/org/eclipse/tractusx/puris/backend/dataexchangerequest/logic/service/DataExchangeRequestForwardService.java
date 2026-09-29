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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestForwardService {
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedNotificationService;
    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;

    public record ForwardTarget(ReportedDemandAndCapacityNotification notification, Date start, Date end) {}

    /**
     * Resolves the related notifications that a request can be forwarded to
     * And skips those belonging to the same partner as well as those where the notification's
     * effect window does not overlap the requested window.
     */
    public List<ForwardTarget> resolveForwardTargets(ReportedDataExchangeRequest origin) {
        OwnDemandAndCapacityNotification originNotification = origin.getNotification();
        List<UUID> relatedIds = originNotification.getRelatedNotificationIds();
        if (relatedIds == null || relatedIds.isEmpty()) {
            return List.of();
        }
        String requesterBpnl = originNotification.getPartner().getBpnl();
        List<ForwardTarget> targets = new ArrayList<>();

        for (ReportedDemandAndCapacityNotification target : reportedNotificationService.findByNotificationIdIn(relatedIds)) {
            if (requesterBpnl.equals(target.getPartner().getBpnl())) {
                log.info("Skipping forward target {}: notification belongs to the requesting partner", target.getNotificationId());
                continue;
            }
            Date start = maxDate(origin.getDesiredStartDateTime(), target.getStartDateOfEffect());
            Date end = target.getExpectedEndDateOfEffect() == null ? origin.getDesiredEndDateTime() : minDate(origin.getDesiredEndDateTime(), target.getExpectedEndDateOfEffect());

            if (!start.before(end)) {
                log.info("Skipping forward target {}: requested window does not overlap the notification window", target.getNotificationId());
                continue;
            }
            targets.add(new ForwardTarget(target, start, end));
        }
        return targets;
    }

    public List<OwnDataExchangeRequest> createForwardedRequests(ReportedDataExchangeRequest origin, List<ForwardTarget> targets) throws KeyAlreadyExistsException {
        List<OwnDataExchangeRequest> created = new ArrayList<>();
        for (ForwardTarget target : targets) {
            OwnDataExchangeRequest forwarded = OwnDataExchangeRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .notification(target.notification())
                .relatedDataExchangeRequest(origin)
                .criticality(origin.getCriticality())
                .desiredStartDateTime(target.start())
                .desiredEndDateTime(target.end())
                .requestedTypes(new ArrayList<>(origin.getRequestedTypes()))
                .text("Forwarded data exchange request from a downstream partner")
                .build();
            try {
                created.add(ownDataExchangeRequestService.create(forwarded));
            } catch (IllegalArgumentException e) {
                log.warn("Could not forward to notification {}: {} ({})", target.notification().getNotificationId(), e.getMessage(), ownDataExchangeRequestService.validateWithDetails(forwarded));
            }
        }
        return created;
    }

    private static Date maxDate(Date a, Date b) { return a.before(b) ? b : a; }
    private static Date minDate(Date a, Date b) { return a.before(b) ? a : b; }
}
