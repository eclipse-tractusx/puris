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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter.DataExchangeApprovalSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningPartnerGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningRootGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsJobService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExchangeApprovalApiServiceTest {

    private static final String BPNL = "BPNLXXSUPPLIERXX";
    private static final String APPROVAL_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private PartnerService partnerService;

    @Mock
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;

    @Mock
    private IndustryCoreMessageService messageService;

    @Mock
    private EdcAdapterService edcAdapterService;

    @Mock
    private DataExchangeApprovalSammMapper sammMapper;

    @Mock
    private IrsChainOpeningRootGrantService irsChainOpeningRootGrantService;

    @Mock
    private IrsChainOpeningPartnerGrantService irsChainOpeningGrantService;

    @Mock
    private IrsJobService irsJobService;

    @InjectMocks
    private DataExchangeApprovalApiService dataExchangeApprovalApiService;

    private DataExchangeApprovalSamm samm;
    private ReportedDemandAndCapacityNotification notification;

    @BeforeEach
    void setUp() {
        Partner partner = new Partner();
        partner.setBpnl(BPNL);
        lenient().when(partnerService.findByBpnl(BPNL)).thenReturn(partner);

        samm = new DataExchangeApprovalSamm();
        notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setSourceDisruptionId(UUID.randomUUID());
    }

    private ReportedDataExchangeApproval approval(boolean finalized, ReportedDataExchangeRequest relatedRequest) {
        OwnDataExchangeRequest dataExchangeRequest = OwnDataExchangeRequest.builder()
            .notification(notification)
            .relatedDataExchangeRequest(relatedRequest)
            .build();
        return ReportedDataExchangeApproval.builder()
            .approvalId(APPROVAL_ID)
            .isFinalized(finalized)
            .dataExchangeRequest(dataExchangeRequest)
            .build();
    }

    // --- create branch ---

    @Test
    void handleIncoming_WhenNewRootRequestAndFinalized_SyncsGrantsAndCreatesJobs() {
        ReportedDataExchangeApproval approval = approval(true, null);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(approval);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(null);
        when(reportedDataExchangeApprovalService.create(approval)).thenReturn(approval);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsChainOpeningRootGrantService).syncGrantsForNotification(notification);
        verify(irsJobService).createJobsForNotification(notification);
        verify(irsChainOpeningGrantService, never()).onRelatedApprovalReceived(any());
    }

    @Test
    void handleIncoming_WhenNewRootRequestAndNotFinalized_OnlySyncsGrants() {
        ReportedDataExchangeApproval approval = approval(false, null);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(approval);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(null);
        when(reportedDataExchangeApprovalService.create(approval)).thenReturn(approval);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsChainOpeningRootGrantService).syncGrantsForNotification(notification);
        verify(irsJobService, never()).createJobsForNotification(any());
    }

    @Test
    void handleIncoming_WhenNewRelatedRequest_OnlyNotifiesRelatedGrantServiceRegardlessOfFinalized() {
        ReportedDataExchangeRequest relatedRequest = new ReportedDataExchangeRequest();
        ReportedDataExchangeApproval approval = approval(true, relatedRequest);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(approval);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(null);
        when(reportedDataExchangeApprovalService.create(approval)).thenReturn(approval);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsChainOpeningGrantService).onRelatedApprovalReceived(approval);
        verify(irsChainOpeningRootGrantService, never()).syncGrantsForNotification(any());
        verify(irsJobService, never()).createJobsForNotification(any());
    }

    // --- update branch ---

    @Test
    void handleIncoming_WhenExistingRootRequestFinalized_CreatesJobs() {
        ReportedDataExchangeApproval existing = approval(false, null);
        existing.setUuid(UUID.randomUUID());
        ReportedDataExchangeApproval incoming = approval(true, null);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(existing);
        when(reportedDataExchangeApprovalService.update(incoming)).thenReturn(incoming);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsJobService).createJobsForNotification(notification);
        verify(irsChainOpeningGrantService, never()).onRelatedApprovalReceived(any());
    }

    @Test
    void handleIncoming_WhenExistingRootRequestNotFinalized_DoesNothingIrsRelated() {
        ReportedDataExchangeApproval existing = approval(false, null);
        existing.setUuid(UUID.randomUUID());
        ReportedDataExchangeApproval incoming = approval(false, null);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(existing);
        when(reportedDataExchangeApprovalService.update(incoming)).thenReturn(incoming);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsJobService, never()).createJobsForNotification(any());
        verify(irsChainOpeningRootGrantService, never()).syncGrantsForNotification(any());
        verify(irsChainOpeningGrantService, never()).onRelatedApprovalReceived(any());
    }

    @Test
    void handleIncoming_WhenExistingRelatedRequest_NotifiesRelatedGrantService() {
        ReportedDataExchangeRequest relatedRequest = new ReportedDataExchangeRequest();
        ReportedDataExchangeApproval existing = approval(false, relatedRequest);
        existing.setUuid(UUID.randomUUID());
        ReportedDataExchangeApproval incoming = approval(true, relatedRequest);
        when(sammMapper.sammToReportedDataExchangeApproval(samm)).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByApprovalId(APPROVAL_ID)).thenReturn(existing);
        when(reportedDataExchangeApprovalService.update(incoming)).thenReturn(incoming);

        dataExchangeApprovalApiService.handleIncomingDataExchangeApproval(BPNL, samm);

        verify(irsChainOpeningGrantService).onRelatedApprovalReceived(incoming);
        verify(irsJobService, never()).createJobsForNotification(any());
    }
}
