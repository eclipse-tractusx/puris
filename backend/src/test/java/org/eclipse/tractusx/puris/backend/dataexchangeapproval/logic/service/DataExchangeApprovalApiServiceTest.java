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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter.DataExchangeApprovalSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DataExchangeApprovalApiServiceTest {
 
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";
 
    final static String OTHER_SUPPLIER_BPNL = "BPNL2222222222LE";
    final static String OTHER_SUPPLIER_BPNS = "BPNS2222222222SI";
    final static String OTHER_SUPPLIER_BPNA = "BPNA2222222222AD";
 
    final static String CUSTOMER_BPNL = "BPNL4444444444XX";
    final static String CUSTOMER_BPNS = "BPNS4444444444XX";
    final static String CUSTOMER_BPNA = "BPNA4444444444ZZ";
 
    final static String UNRELATED_BPNL = "BPNL7777777777UU";
    final static String UNRELATED_BPNS = "BPNS7777777777UU";
    final static String UNRELATED_BPNA = "BPNA7777777777UU";
 
    final static Partner supplierPartner = new Partner(
        "Scenario Supplier",
        "http://supplier-control-plane:9184/api/v1/dsp",
        SUPPLIER_BPNL,
        SUPPLIER_BPNS,
        "Konzernzentrale Dudelsdorf",
        SUPPLIER_BPNA,
        "Heinrich-Supplier-Straße 1",
        "77785 Dudelsdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );
 
    final static Partner otherSupplierPartner = new Partner(
        "Scenario Second Supplier",
        "http://second-supplier-control-plane:9284/api/v1/dsp",
        OTHER_SUPPLIER_BPNL,
        OTHER_SUPPLIER_BPNS,
        "Zweigwerk Oberdorf",
        OTHER_SUPPLIER_BPNA,
        "Zweite-Supplier-Straße 7",
        "77786 Oberdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );
 
    final static Partner customerPartner = new Partner(
        "Scenario Customer",
        "http://customer-control-plane:8184/api/v1/dsp",
        CUSTOMER_BPNL,
        CUSTOMER_BPNS,
        "Hauptwerk Musterhausen",
        CUSTOMER_BPNA,
        "Musterstraße 35b",
        "77777 Musterhausen",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );
 
    final static Partner unrelatedPartner = new Partner(
        "Scenario Unrelated Partner",
        "http://unrelated-control-plane:9384/api/v1/dsp",
        UNRELATED_BPNL,
        UNRELATED_BPNS,
        "Fremdwerk Andernorts",
        UNRELATED_BPNA,
        "Fremde Straße 12",
        "77788 Andernorts",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );
 
    final static DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
 
    static Date dateFromString(String str) {
        LocalDateTime ldt = LocalDateTime.from(DTF.parse(str));
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
        return new Date(Instant.from(zdt).toEpochMilli());
    }
 
    @Mock
    private PartnerService partnerService;
    @Mock
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;
    @Mock
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;
    @Mock
    private OwnDataExchangeRequestService ownDataExchangeRequestService;
    @Mock
    private DataExchangeApprovalSammMapper sammMapper;
    @Mock
    private ExecutorService executorService;
    @Mock
    private IndustryCoreMessageService messageService;
    @Mock
    private EdcAdapterService edcAdapterService;
 
    @InjectMocks
    private DataExchangeApprovalApiService apiService;
 
    private DataExchangeApprovalSamm incomingSamm;
 
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        incomingSamm = approvalSamm();
    }
 
    @Test
    void returnsNullForUnknownPartner() {
        when(partnerService.findByBpnl(SUPPLIER_BPNL)).thenReturn(null);
 
        Assertions.assertNull(apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm));
        verifyNoInteractions(sammMapper);
    }
 
    @Test
    void returnsNullWhenMapperCannotResolveRequest() {
        when(partnerService.findByBpnl(SUPPLIER_BPNL)).thenReturn(supplierPartner);
        when(sammMapper.sammToReportedDataExchangeApproval(incomingSamm)).thenReturn(null);
 
        Assertions.assertNull(apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm));
        verify(reportedDataExchangeApprovalService, never()).create(any());
    }
 
    @Test
    void rejectsApprovalFromPartnerThatIsNotTheRequestRecipient() {
        ReportedDataExchangeRequest origin = originRequest();
        OwnDataExchangeRequest forwarded = forwardedRequest(origin, supplierPartner);
        ReportedDataExchangeApproval incoming = reportedApproval(forwarded, true);
 
        when(partnerService.findByBpnl(UNRELATED_BPNL)).thenReturn(unrelatedPartner);
        when(sammMapper.sammToReportedDataExchangeApproval(incomingSamm)).thenReturn(incoming);
 
        Assertions.assertNull(apiService.handleIncomingDataExchangeApproval(UNRELATED_BPNL, incomingSamm));
        verify(reportedDataExchangeApprovalService, never()).create(any());
        verify(ownDataExchangeApprovalService, never()).update(any());
    }
 
    @Test
    void approvalRequestWithoutRelatedRequest() {
        // relatedDataExchangeRequest == null
        OwnDataExchangeRequest plainRequest = forwardedRequest(null, supplierPartner);
        ReportedDataExchangeApproval incoming = reportedApproval(plainRequest, true);
        createNewApproval(incoming);
 
        ReportedDataExchangeApproval result = apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        Assertions.assertEquals(incoming, result);
        verifyNoInteractions(ownDataExchangeApprovalService);
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void doesNotFinalizeWhileRelatedIsUnapproved() {
        ReportedDataExchangeRequest origin = originRequest();
        OwnDataExchangeRequest answered = forwardedRequest(origin, supplierPartner);
        OwnDataExchangeRequest pending = forwardedRequest(origin, otherSupplierPartner);
 
        ReportedDataExchangeApproval incoming = reportedApproval(answered, true);
        createNewApproval(incoming);
 
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(origin.getUuid())).thenReturn(ownApproval(origin, false));
        when(ownDataExchangeRequestService.findByRelatedDataExchangeRequest(same(origin))).thenReturn(List.of(answered, pending));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(answered.getUuid())).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(pending.getUuid())).thenReturn(null);
 
        apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        verify(ownDataExchangeApprovalService, never()).update(any());
        verify(executorService, never()).submit(any(Runnable.class));
    }
 
    @Test
    void finalizesWhenAllForwardedRequestsAreApproved() {
        ReportedDataExchangeRequest origin = originRequest();
        OwnDataExchangeRequest first = forwardedRequest(origin, supplierPartner);
        OwnDataExchangeRequest second = forwardedRequest(origin, otherSupplierPartner);
 
        ReportedDataExchangeApproval incoming = reportedApproval(first, true);
        ReportedDataExchangeApproval secondApproval = reportedApproval(second, true);
        OwnDataExchangeApproval originApproval = ownApproval(origin, false);
        createNewApproval(incoming);
 
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(origin.getUuid())).thenReturn(originApproval);
        when(ownDataExchangeRequestService.findByRelatedDataExchangeRequest(same(origin))).thenReturn(List.of(first, second));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(first.getUuid())).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(second.getUuid())).thenReturn(secondApproval);
        when(ownDataExchangeApprovalService.update(any())).thenAnswer(inv -> inv.getArgument(0));
 
        apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        ArgumentCaptor<OwnDataExchangeApproval> captor = ArgumentCaptor.forClass(OwnDataExchangeApproval.class);
        verify(ownDataExchangeApprovalService).update(captor.capture());
        Assertions.assertTrue(captor.getValue().isFinalized());
        verify(executorService).submit(any(Runnable.class));
    }
 
    @Test
    void doesNotFinalizeWhenOriginApprovalMissing() {
        ReportedDataExchangeRequest origin = originRequest();
        OwnDataExchangeRequest only = forwardedRequest(origin, supplierPartner);
 
        createNewApproval(reportedApproval(only, true));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(origin.getUuid())).thenReturn(null);
 
        apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        verify(ownDataExchangeApprovalService, never()).update(any());
        verify(executorService, never()).submit(any(Runnable.class));
    }
 
    @Test
    void doesNotFinalizeWhenOriginHasNoForwardedRequests() {
        ReportedDataExchangeRequest origin = originRequest();
        OwnDataExchangeRequest orphan = forwardedRequest(origin, supplierPartner);
 
        createNewApproval(reportedApproval(orphan, true));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(origin.getUuid())).thenReturn(ownApproval(origin, false));
        when(ownDataExchangeRequestService.findByRelatedDataExchangeRequest(same(origin))).thenReturn(List.of());
 
        apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        verify(ownDataExchangeApprovalService, never()).update(any());
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void updatesExistingApprovalOnResend() {
        OwnDataExchangeRequest plainRequest = forwardedRequest(null, supplierPartner);
        ReportedDataExchangeApproval existing = reportedApproval(plainRequest, false);
        ReportedDataExchangeApproval incoming = reportedApproval(plainRequest, true);
        incoming.setApprovalId(existing.getApprovalId());
 
        when(partnerService.findByBpnl(SUPPLIER_BPNL)).thenReturn(supplierPartner);
        when(sammMapper.sammToReportedDataExchangeApproval(incomingSamm)).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByApprovalId(incoming.getApprovalId())).thenReturn(existing);
        when(reportedDataExchangeApprovalService.update(incoming)).thenReturn(incoming);
 
        ReportedDataExchangeApproval result = apiService.handleIncomingDataExchangeApproval(SUPPLIER_BPNL, incomingSamm);
 
        Assertions.assertEquals(incoming, result);
        Assertions.assertEquals(existing.getUuid(), incoming.getUuid());
        verify(reportedDataExchangeApprovalService, never()).create(any());
    }
 
    private void createNewApproval (ReportedDataExchangeApproval incoming) {
        when(partnerService.findByBpnl(SUPPLIER_BPNL)).thenReturn(supplierPartner);
        when(sammMapper.sammToReportedDataExchangeApproval(incomingSamm)).thenReturn(incoming);
        when(reportedDataExchangeApprovalService.findByApprovalId(incoming.getApprovalId())).thenReturn(null);
        when(reportedDataExchangeApprovalService.create(incoming)).thenReturn(incoming);
    }
 
    static DataExchangeApprovalSamm approvalSamm() {
        return DataExchangeApprovalSamm.builder()
            .approvalId(UUID.randomUUID().toString())
            .dataExchangeRequestId(UUID.randomUUID().toString())
            .timestamp(dateFromString("01-02-2026 00:00:00"))
            .approvedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .finalized(true)
            .build();
    }
 
    static ReportedDataExchangeRequest originRequest() {
        OwnDemandAndCapacityNotification ownNotification = OwnDemandAndCapacityNotification.builder()
            .uuid(UUID.randomUUID())
            .notificationId(UUID.randomUUID())
            .relatedNotificationIds(new ArrayList<>(List.of(UUID.randomUUID())))
            .partner(customerPartner)
            .startDateOfEffect(dateFromString("01-01-2026 00:00:00"))
            .expectedEndDateOfEffect(dateFromString("31-12-2026 00:00:00"))
            .build();
 
        return ReportedDataExchangeRequest.builder()
            .uuid(UUID.randomUUID())
            .requestId(UUID.randomUUID().toString())
            .notification(ownNotification)
            .criticality(CriticalityEnumeration.HIGH)
            .desiredStartDateTime(dateFromString("01-01-2026 00:00:00"))
            .desiredEndDateTime(dateFromString("31-12-2026 00:00:00"))
            .requestedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .text("Please provide the requested data.")
            .build();
    }
 
    static OwnDataExchangeRequest forwardedRequest(ReportedDataExchangeRequest origin, Partner supplier) {
        ReportedDemandAndCapacityNotification supplierNotification =
            ReportedDemandAndCapacityNotification.builder()
                .uuid(UUID.randomUUID())
                .notificationId(UUID.randomUUID())
                .partner(supplier)
                .startDateOfEffect(dateFromString("01-01-2026 00:00:00"))
                .expectedEndDateOfEffect(dateFromString("31-12-2026 00:00:00"))
                .build();
 
        return OwnDataExchangeRequest.builder()
            .uuid(UUID.randomUUID())
            .requestId(UUID.randomUUID().toString())
            .notification(supplierNotification)
            .relatedDataExchangeRequest(origin)
            .criticality(CriticalityEnumeration.HIGH)
            .desiredStartDateTime(dateFromString("01-01-2026 00:00:00"))
            .desiredEndDateTime(dateFromString("31-12-2026 00:00:00"))
            .requestedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .text("Please provide the requested data.")
            .build();
    }
 
    static ReportedDataExchangeApproval reportedApproval(OwnDataExchangeRequest request, boolean finalized) {
        return ReportedDataExchangeApproval.builder()
            .uuid(UUID.randomUUID())
            .approvalId(UUID.randomUUID().toString())
            .dataExchangeRequest(request)
            .isFinalized(finalized)
            .approvedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .timestamp(dateFromString("01-02-2026 00:00:00"))
            .build();
    }
 
    static OwnDataExchangeApproval ownApproval(ReportedDataExchangeRequest request, boolean finalized) {
        return OwnDataExchangeApproval.builder()
            .uuid(UUID.randomUUID())
            .approvalId(UUID.randomUUID().toString())
            .dataExchangeRequest(request)
            .isFinalized(finalized)
            .approvedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .timestamp(dateFromString("01-02-2026 00:00:00"))
            .build();
    }
}
