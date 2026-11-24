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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

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
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.OwnDataExchangeApprovalRepository;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.ReportedDataExchangeApprovalRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Checks that every create and update (including finalization) of an own or reported (received)
 * data exchange approval announces a live update, and that rejected changes announce nothing.
 */
@ExtendWith(MockitoExtension.class)
class DataExchangeApprovalLiveUpdateTest {

    @Mock
    private OwnDataExchangeApprovalRepository ownRepository;
    @Mock
    private ReportedDataExchangeApprovalRepository reportedRepository;
    @Mock
    private LiveUpdatePublisher liveUpdatePublisher;

    @InjectMocks
    private OwnDataExchangeApprovalService ownService;
    @InjectMocks
    private ReportedDataExchangeApprovalService reportedService;

    @Test
    void ownCreate_publishesCreated() {
        var approval = validOwnApproval();
        when(ownRepository.save(approval)).thenReturn(approval);

        ownService.create(approval);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.CREATED);
    }

    @Test
    void ownFinalize_publishesUpdated() {
        var approval = validOwnApproval();
        approval.setUuid(UUID.randomUUID());
        approval.setTimestamp(new Date());
        approval.setFinalized(true);
        when(ownRepository.findById(approval.getUuid())).thenReturn(Optional.of(validOwnApproval()));
        when(ownRepository.save(approval)).thenReturn(approval);

        ownService.update(approval);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.UPDATED);
    }

    @Test
    void ownCreate_whenInvalid_publishesNothing() {
        assertThrows(IllegalArgumentException.class, () -> ownService.create(new OwnDataExchangeApproval()));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void ownUpdate_whenUnknown_publishesNothing() {
        var approval = validOwnApproval();
        approval.setUuid(UUID.randomUUID());
        approval.setTimestamp(new Date());
        when(ownRepository.findById(approval.getUuid())).thenReturn(Optional.empty());

        assertNull(ownService.update(approval));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedCreate_publishesCreated() {
        var approval = validReportedApproval();
        when(reportedRepository.save(approval)).thenReturn(approval);

        reportedService.create(approval);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.CREATED);
    }

    @Test
    void reportedUpdate_publishesUpdated() {
        var approval = validReportedApproval();
        approval.setUuid(UUID.randomUUID());
        approval.setTimestamp(new Date());
        approval.setFinalized(true);
        when(reportedRepository.findById(approval.getUuid())).thenReturn(Optional.of(validReportedApproval()));
        when(reportedRepository.save(approval)).thenReturn(approval);

        reportedService.update(approval);

        verify(liveUpdatePublisher).publish(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.UPDATED);
    }

    @Test
    void reportedCreate_whenInvalid_publishesNothing() {
        assertThrows(IllegalArgumentException.class, () -> reportedService.create(new ReportedDataExchangeApproval()));

        verifyNoInteractions(liveUpdatePublisher);
    }

    @Test
    void reportedUpdate_whenUnknown_publishesNothing() {
        var approval = validReportedApproval();
        approval.setUuid(UUID.randomUUID());
        approval.setTimestamp(new Date());
        when(reportedRepository.findById(approval.getUuid())).thenReturn(Optional.empty());

        assertNull(reportedService.update(approval));

        verifyNoInteractions(liveUpdatePublisher);
    }

    private OwnDataExchangeApproval validOwnApproval() {
        var request = new ReportedDataExchangeRequest();
        request.setUuid(UUID.randomUUID());
        var approval = new OwnDataExchangeApproval();
        approval.setDataExchangeRequest(request);
        approval.setApprovedTypes(List.of(RequestedTypeEnumeration.N_TIER));
        return approval;
    }

    private ReportedDataExchangeApproval validReportedApproval() {
        var request = new OwnDataExchangeRequest();
        request.setUuid(UUID.randomUUID());
        var approval = new ReportedDataExchangeApproval();
        approval.setDataExchangeRequest(request);
        approval.setApprovedTypes(List.of(RequestedTypeEnumeration.N_TIER));
        return approval;
    }
}
