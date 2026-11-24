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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdateAction;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdatePublisher;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdateType;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.LeadingRootCauseEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.OwnDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.ReportedDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningPartnerGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningRootGrantService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Checks that every create, update (including resolve) and delete of an own or reported
 * (received) notification announces a live update, and that rejected changes announce nothing.
 */
@ExtendWith(MockitoExtension.class)
class DemandAndCapacityNotificationLiveUpdateTest {

    @Mock
    private OwnDemandAndCapacityNotificationRepository ownRepository;
    @Mock
    private ReportedDemandAndCapacityNotificationRepository reportedRepository;
    @Mock
    private PartnerService partnerService;
    @Mock
    private MaterialPartnerRelationService mprService;
    @Mock
    private MaterialRelationService materialRelationService;
    @Mock
    private IrsChainOpeningPartnerGrantService irsChainOpeningPartnerGrantService;
    @Mock
    private IrsChainOpeningRootGrantService irsChainOpeningRootGrantService;
    @Mock
    private LiveUpdatePublisher liveUpdatePublisher;

    @InjectMocks
    private OwnDemandAndCapacityNotificationService ownService;
    @InjectMocks
    private ReportedDemandAndCapacityNotificationService reportedService;

    @Test
    void ownCreate_publishesCreated() {
        var notification = validOwnNotification();
        when(ownRepository.save(notification)).thenReturn(notification);

        ownService.create(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.CREATED);
    }

    @Test
    void ownUpdate_publishesUpdated() {
        var notification = validOwnNotification();
        notification.setUuid(UUID.randomUUID());
        when(ownRepository.findById(notification.getUuid())).thenReturn(Optional.of(validOwnNotification()));
        when(ownRepository.save(notification)).thenReturn(notification);

        ownService.update(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.UPDATED);
    }

    @Test
    void ownResolve_publishesUpdated() {
        var notification = validOwnNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStatus(StatusEnumeration.RESOLVED);
        notification.setResolvingMeasureDescription("Strike is over.");
        when(ownRepository.findById(notification.getUuid())).thenReturn(Optional.of(validOwnNotification()));
        when(ownRepository.save(notification)).thenReturn(notification);

        ownService.update(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.UPDATED);
    }

    @Test
    void ownDelete_publishesDeleted() {
        ownService.delete(UUID.randomUUID());

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.DELETED);
    }

    @Test
    void ownCreate_whenInvalid_publishesNothing() {
        var notification = validOwnNotification();
        notification.setText(null);

        assertThrows(IllegalArgumentException.class, () -> ownService.create(notification));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void ownUpdate_whenUnknown_publishesNothing() {
        var notification = validOwnNotification();
        notification.setUuid(UUID.randomUUID());
        when(ownRepository.findById(notification.getUuid())).thenReturn(Optional.empty());

        assertNull(ownService.update(notification));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedCreate_publishesCreated() {
        var notification = validReportedNotification();
        when(reportedRepository.save(notification)).thenReturn(notification);

        reportedService.create(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.CREATED);
    }

    @Test
    void reportedUpdate_publishesUpdated() {
        var notification = validReportedNotification();
        notification.setUuid(UUID.randomUUID());
        when(reportedRepository.findById(notification.getUuid())).thenReturn(Optional.of(validReportedNotification()));
        when(reportedRepository.save(notification)).thenReturn(notification);

        reportedService.update(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.UPDATED);
    }

    @Test
    void reportedResolve_publishesUpdated() {
        var notification = validReportedNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStatus(StatusEnumeration.RESOLVED);
        notification.setResolvingMeasureDescription("Strike is over.");
        when(reportedRepository.findById(notification.getUuid())).thenReturn(Optional.of(validReportedNotification()));
        when(reportedRepository.save(notification)).thenReturn(notification);

        reportedService.update(notification);

        verify(liveUpdatePublisher).publish(LiveUpdateType.NOTIFICATION, LiveUpdateAction.UPDATED);
    }

    @Test
    void reportedCreate_whenInvalid_publishesNothing() {
        var notification = validReportedNotification();
        notification.setPartner(null);

        assertThrows(IllegalArgumentException.class, () -> reportedService.create(notification));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedUpdate_whenUnknown_publishesNothing() {
        var notification = validReportedNotification();
        notification.setUuid(UUID.randomUUID());
        when(reportedRepository.findById(notification.getUuid())).thenReturn(Optional.empty());

        assertNull(reportedService.update(notification));

        verifyNoInteractions(liveUpdatePublisher);
    }

    private OwnDemandAndCapacityNotification validOwnNotification() {
        return OwnDemandAndCapacityNotification.builder()
            .notificationId(UUID.randomUUID())
            .sourceDisruptionId(UUID.randomUUID())
            .partner(mock(Partner.class))
            .text("Capacity reduction due to strike.")
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .leadingRootCause(LeadingRootCauseEnumeration.STRIKE)
            .status(StatusEnumeration.OPEN)
            .startDateOfEffect(new Date())
            .build();
    }

    private ReportedDemandAndCapacityNotification validReportedNotification() {
        return ReportedDemandAndCapacityNotification.builder()
            .notificationId(UUID.randomUUID())
            .sourceDisruptionId(UUID.randomUUID())
            .partner(mock(Partner.class))
            .text("Capacity reduction due to strike.")
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .leadingRootCause(LeadingRootCauseEnumeration.STRIKE)
            .status(StatusEnumeration.OPEN)
            .startDateOfEffect(new Date())
            .build();
    }
}
