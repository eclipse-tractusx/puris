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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdateAction;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdatePublisher;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdateType;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Checks that every create and update of an own or reported (received) data exchange request
 * announces a live update, and that rejected changes announce nothing.
 */
@ExtendWith(MockitoExtension.class)
class DataExchangeRequestLiveUpdateTest {

    private static final long DAY = 24 * 60 * 60 * 1000L;

    @Mock
    private OwnDataExchangeRequestRepository ownRepository;
    @Mock
    private ReportedDataExchangeRequestRepository reportedRepository;
    @Mock
    private LiveUpdatePublisher liveUpdatePublisher;

    @InjectMocks
    private OwnDataExchangeRequestService ownService;
    @InjectMocks
    private ReportedDataExchangeRequestService reportedService;

    @Test
    void ownCreate_publishesCreated() {
        var request = validOwnRequest();
        when(ownRepository.save(request)).thenReturn(request);

        ownService.create(request);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_REQUEST, LiveUpdateAction.CREATED);
    }

    @Test
    void ownCreate_whenInvalid_publishesNothing() {
        assertThrows(IllegalArgumentException.class, () -> ownService.create(new OwnDataExchangeRequest()));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedCreate_publishesCreated() {
        var request = validReportedRequest();
        when(reportedRepository.save(request)).thenReturn(request);

        reportedService.create(request);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_REQUEST, LiveUpdateAction.CREATED);
    }

    @Test
    void reportedUpdate_publishesUpdated() {
        var request = validReportedRequest();
        request.setUuid(UUID.randomUUID());
        request.setTimestamp(new Date());
        when(reportedRepository.findById(request.getUuid())).thenReturn(Optional.of(validReportedRequest()));
        when(reportedRepository.save(request)).thenReturn(request);

        reportedService.update(request);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_REQUEST, LiveUpdateAction.UPDATED);
    }

    @Test
    void reportedCreate_whenInvalid_publishesNothing() {
        assertThrows(IllegalArgumentException.class, () -> reportedService.create(new ReportedDataExchangeRequest()));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedUpdate_whenUnknown_publishesNothing() {
        var request = validReportedRequest();
        request.setUuid(UUID.randomUUID());
        request.setTimestamp(new Date());
        when(reportedRepository.findById(request.getUuid())).thenReturn(Optional.empty());

        assertNull(reportedService.update(request));

        verifyNoInteractions(liveUpdatePublisher);
    }

    private OwnDataExchangeRequest validOwnRequest() {
        var notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStartDateOfEffect(new Date(System.currentTimeMillis() + DAY));
        var request = new OwnDataExchangeRequest();
        request.setNotification(notification);
        fillValidRequestData(request);
        return request;
    }

    private ReportedDataExchangeRequest validReportedRequest() {
        var notification = new OwnDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStartDateOfEffect(new Date(System.currentTimeMillis() + DAY));
        var request = new ReportedDataExchangeRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setNotification(notification);
        fillValidRequestData(request);
        return request;
    }

    private void fillValidRequestData(DataExchangeRequest request) {
        request.setCriticality(CriticalityEnumeration.HIGH);
        request.setText("Please provide the requested data.");
        request.setRequestedTypes(List.of(RequestedTypeEnumeration.N_TIER));
        request.setDesiredStartDateTime(new Date(System.currentTimeMillis() + 2 * DAY));
        request.setDesiredEndDateTime(new Date(System.currentTimeMillis() + 3 * DAY));
    }
}
