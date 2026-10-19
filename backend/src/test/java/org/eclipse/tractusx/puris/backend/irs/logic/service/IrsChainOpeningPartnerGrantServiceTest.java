/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.OwnDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningPartnerGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningPartnerGrantRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsChainOpeningPartnerGrantServiceTest {

    private static final String OWN_MATERIAL_NUMBER = "MNR-001";
    private static final String GLOBAL_ASSET_ID = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0";
    private static final String CHILD_MATERIAL_NUMBER = "MNR-002";
    private static final String OTHER_MATERIAL_NUMBER = "MNR-003";
    private static final String OTHER_GLOBAL_ASSET_ID = "urn:uuid:00000000-0000-0000-0000-0000000000aa";
    private static final String PARTNER_BPNL = "BPNLXXCUSTOMERXX";
    private static final String SUPPLIER_BPNL = "BPNLXXSUPPLIERXX";
    private static final String OTHER_SUPPLIER_BPNL = "BPNLXXSUPPLIER2X";
    private static final UUID SOURCE_DISRUPTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant VALID_FROM = Instant.now().minusSeconds(7 * 24 * 3600L);
    private static final Instant VALID_TO = Instant.now().plusSeconds(7 * 24 * 3600L);

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @Mock
    private ReportedDataExchangeRequestRepository reportedDataExchangeRequestRepository;

    @Mock
    private OwnDataExchangeRequestRepository ownDataExchangeRequestRepository;

    @Mock
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;

    @Mock
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialRelationService materialRelationService;

    @Mock
    private IrsChainOpeningPartnerGrantRepository irsChainOpeningPartnerGrantRepository;

    private IrsChainOpeningPartnerGrantService chainOpeningGrantService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IrsChainOpeningGrantGateway gateway = new IrsChainOpeningGrantGateway(irsRequestBodybuilder, irsRequestQueueService);
        chainOpeningGrantService = new IrsChainOpeningPartnerGrantService(irsRequestService, gateway,
            reportedDataExchangeRequestRepository, ownDataExchangeRequestRepository, ownDataExchangeApprovalService,
            reportedDataExchangeApprovalService, materialService, materialRelationService, irsChainOpeningPartnerGrantRepository);
        lenient().when(irsChainOpeningPartnerGrantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- test data helpers ---

    private Partner partner(String bpnl) {
        Partner partner = new Partner();
        partner.setBpnl(bpnl);
        return partner;
    }

    private Material material(String ownMaterialNumber, String materialNumberCx) {
        Material material = new Material();
        material.setOwnMaterialNumber(ownMaterialNumber);
        material.setMaterialNumberCx(materialNumberCx);
        return material;
    }

    private Material grantMaterial() {
        return material(OWN_MATERIAL_NUMBER, GLOBAL_ASSET_ID);
    }

    /** A ReportedDataExchangeRequest from PARTNER_BPNL for the given materials, with the window [VALID_FROM, VALID_TO]. */
    private ReportedDataExchangeRequest incomingRequest(UUID uuid, List<Material> materials) {
        ReportedDataExchangeRequest request = new ReportedDataExchangeRequest();
        request.setUuid(uuid);
        request.setRequestId("urn:uuid:" + uuid);
        request.setPartner(partner(PARTNER_BPNL));
        request.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        request.setMaterials(materials);
        request.setDesiredStartDateTime(Date.from(VALID_FROM));
        request.setDesiredEndDateTime(Date.from(VALID_TO));
        return request;
    }

    /** The OwnDataExchangeApproval we send to PARTNER_BPNL, approving the incoming request. */
    private OwnDataExchangeApproval sentApproval(ReportedDataExchangeRequest request) {
        OwnDataExchangeApproval approval = new OwnDataExchangeApproval();
        approval.setDataExchangeRequest(request);
        return approval;
    }

    /** An OwnDataExchangeRequest to the given further-upstream partner, forwarded along the bill of material. */
    private OwnDataExchangeRequest forwardedRequest(UUID uuid, String bpnl, List<Material> materials,
            ReportedDataExchangeRequest related) {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setUuid(uuid);
        request.setRequestId("urn:uuid:" + uuid);
        request.setPartner(partner(bpnl));
        request.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        request.setMaterials(materials);
        request.setRelatedDataExchangeRequest(related);
        request.setDesiredStartDateTime(Date.from(VALID_FROM));
        request.setDesiredEndDateTime(Date.from(VALID_TO));
        return request;
    }

    /** The ReportedDataExchangeApproval the further-upstream partner sent back for the forwarded request. */
    private ReportedDataExchangeApproval receivedApproval(OwnDataExchangeRequest forwardedRequest) {
        ReportedDataExchangeApproval approval = new ReportedDataExchangeApproval();
        approval.setDataExchangeRequest(forwardedRequest);
        return approval;
    }

    private IrsChainOpeningPartnerGrant grant(Set<OwnDataExchangeRequest> requests) {
        return IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .dataExchangeRequests(requests)
            .validFrom(VALID_FROM)
            .validTo(VALID_TO)
            .build();
    }

    /** Stubs the approved incoming request and its forwarded request, approved by the further-upstream partner. */
    private void stubApprovedChain(ReportedDataExchangeRequest request, OwnDataExchangeRequest forwarded) {
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
    }

    // --- createGrant / deleteGrant ---

    @Test
    void createGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grant(Set.of()));

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = chainOpeningGrantService.deleteGrant(grant(Set.of()));

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteGrant_WhenEnabled_SendsExpectedQueryParams() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsChainOpeningPartnerGrant grant = grant(Set.of());
        IrsQueuedRequest result = chainOpeningGrantService.deleteGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
        verify(irsRequestQueueService, atLeastOnce()).enqueue(eq(IrsQueuedRequestMethodEnumeration.DELETE), any(), isNull(), any(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_DELETE), eq(grant.getUuid()));
    }

    // --- createGrant: eligibility ---

    @Test
    void createGrant_WhenNoIncomingRequest_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenIncomingRequestIsNotApproved_Throws() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenIncomingRequestHasExpired_Throws() {
        ReportedDataExchangeRequest expired = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        expired.setDesiredEndDateTime(Date.from(Instant.now().minusSeconds(3600)));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(expired));

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenIncomingRequestDoesNotAffectTheGrantMaterial_Throws() {
        ReportedDataExchangeRequest otherMaterialRequest = incomingRequest(UUID.randomUUID(), List.of(material(OTHER_MATERIAL_NUMBER, OTHER_GLOBAL_ASSET_ID)));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(otherMaterialRequest));

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenGlobalAssetIdUnknown_Throws() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenAllowedBpnlHasNoMatchingForwardedRequest_Throws() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest staleRequest = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(), request);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createOrUpdateGrant(grant(new HashSet<>(Set.of(staleRequest)))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenForwardedRequestIsNotApproved_Throws() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createOrUpdateGrant(grant(new HashSet<>(Set.of(forwarded)))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEveryAllowedBpnlHasApprovedForwardedRequest_Succeeds() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(irsRequestService.isEnabled()).thenReturn(true);
        stubApprovedChain(request, forwarded);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(forwarded)));
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grantToCreate);

        assertThat(result).isEqualTo(queuedRequest);
    }

    @Test
    void createGrant_WhenGrantAlreadySynced_SendsPut() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(irsRequestService.isEnabled()).thenReturn(true);
        stubApprovedChain(request, forwarded);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(forwarded)));
        grantToCreate.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createOrUpdateGrant(grantToCreate);

        verify(irsRequestQueueService, times(1)).enqueue(
            eq(IrsQueuedRequestMethodEnumeration.PUT), any(), eq(body.toString()), isNull(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_UPDATE), eq(grantToCreate.getUuid()));
    }

    @Test
    void createGrant_WhenGrantWasDeletedAtIrs_SendsPostToRecreate() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(irsRequestService.isEnabled()).thenReturn(true);
        stubApprovedChain(request, forwarded);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(forwarded)));
        grantToCreate.setSyncStatus(IrsGrantSyncStatusEnumeration.DELETED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createOrUpdateGrant(grantToCreate);

        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.POST), any(), eq(body.toString()), isNull(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_CREATE), eq(grantToCreate.getUuid()));
    }

    // --- createGrantsForApproval ---

    @Test
    void createGrantsForApproval_CreatesGrantPerAffectedMaterial() {
        Material material1 = grantMaterial();
        Material material2 = material(OTHER_MATERIAL_NUMBER, OTHER_GLOBAL_ASSET_ID);
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(material1, material2));
        OwnDataExchangeApproval approval = sentApproval(request);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, OTHER_GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OTHER_MATERIAL_NUMBER), any())).thenReturn(Set.of());
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        List<String> savedGlobalAssetIds = captor.getAllValues().stream().map(IrsChainOpeningPartnerGrant::getGlobalAssetId).distinct().toList();
        assertThat(savedGlobalAssetIds).containsExactlyInAnyOrder(GLOBAL_ASSET_ID, OTHER_GLOBAL_ASSET_ID);
    }

    // --- syncGrant reconciliation (exercised via createGrantsForApproval) ---

    @Test
    void createGrantsForApproval_CreatesGrantFromRequestAndAddsForwardedRequestCoveringChildMaterial() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval approval = sentApproval(request);
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        IrsChainOpeningPartnerGrant saved = captor.getValue();
        assertThat(saved.getRequesterBpn()).isEqualTo(PARTNER_BPNL);
        assertThat(saved.getGlobalAssetId()).isEqualTo(GLOBAL_ASSET_ID);
        assertThat(saved.getSourceDisruptionId()).isEqualTo(SOURCE_DISRUPTION_ID.toString());
        assertThat(saved.getValidFrom()).isEqualTo(request.getDesiredStartDateTime().toInstant());
        assertThat(saved.getValidTo()).isEqualTo(request.getDesiredEndDateTime().toInstant());
        assertThat(saved.getDataExchangeRequests()).extracting(OwnDataExchangeRequest::getUuid).containsExactly(forwarded.getUuid());
        assertThat(saved.getAllowedBpnls()).containsExactly(SUPPLIER_BPNL);
    }

    @Test
    void createGrantsForApproval_IgnoresForwardedRequestNotCoveringChildMaterial() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval approval = sentApproval(request);
        OwnDataExchangeRequest unrelated = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material("MNR-999", null)), request);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(unrelated));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(unrelated.getUuid())).thenReturn(receivedApproval(unrelated));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).isEmpty();
    }

    @Test
    void createGrantsForApproval_IgnoresExpiredForwardedRequest() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval approval = sentApproval(request);
        OwnDataExchangeRequest expired = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);
        expired.setDesiredEndDateTime(Date.from(Instant.now().minusSeconds(3600)));

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(expired));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(expired.getUuid())).thenReturn(receivedApproval(expired));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).isEmpty();
    }

    @Test
    void createGrantsForApproval_IgnoresForwardedRequestNotYetApproved() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval approval = sentApproval(request);
        OwnDataExchangeRequest waiting = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(waiting));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).isEmpty();
    }

    @Test
    void createGrantsForApproval_WhenSeveralRequestsAreApproved_BacksGrantWithForwardedRequestsOfAll() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest laterRequest = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);
        OwnDataExchangeRequest laterForwarded = forwardedRequest(UUID.randomUUID(), OTHER_SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), laterRequest);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request, laterRequest));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(laterRequest.getUuid())).thenReturn(sentApproval(laterRequest));
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(laterRequest.getUuid())).thenReturn(List.of(laterForwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(laterForwarded.getUuid())).thenReturn(receivedApproval(laterForwarded));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(sentApproval(request));

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).containsExactlyInAnyOrder(SUPPLIER_BPNL, OTHER_SUPPLIER_BPNL);
    }

    @Test
    void createGrantsForApproval_WhenAnotherRequestIsNotApproved_IgnoresItsForwardedRequests() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest notApproved = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request, notApproved));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(notApproved.getUuid())).thenReturn(null);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(sentApproval(request));

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).containsExactly(SUPPLIER_BPNL);
    }

    @Test
    void syncGrant_WhenReconciledSetNonEmptyAndChanged_MarksOutOfSyncThenRePushes() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval approval = sentApproval(request);
        OwnDataExchangeRequest staleRequest = forwardedRequest(UUID.randomUUID(), "BPNLXXOLDSUPPLIER", List.of(), request);
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);

        IrsChainOpeningPartnerGrant existingGrant = IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .dataExchangeRequests(new HashSet<>(Set.of(staleRequest)))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(approval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existingGrant));
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        assertThat(existingGrant.getDataExchangeRequests()).extracting(OwnDataExchangeRequest::getUuid)
            .containsExactly(forwarded.getUuid());
        assertThat(existingGrant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    // --- onRelatedApprovalReceived ---

    @Test
    void onRelatedApprovalReceived_WhenOwnApprovalAlreadySent_SyncsGrants() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeApproval sentApproval = sentApproval(request);
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(material(CHILD_MATERIAL_NUMBER, null)), request);
        ReportedDataExchangeApproval receivedApproval = receivedApproval(forwarded);

        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(sentApproval);
        when(reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL))
            .thenReturn(List.of(request));
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(OWN_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(request.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval);
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onRelatedApprovalReceived(receivedApproval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).containsExactly(SUPPLIER_BPNL);
    }

    @Test
    void onRelatedApprovalReceived_WhenOwnApprovalNotSentYet_NoOp() {
        ReportedDataExchangeRequest request = incomingRequest(UUID.randomUUID(), List.of(grantMaterial()));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), SUPPLIER_BPNL, List.of(), request);
        ReportedDataExchangeApproval receivedApproval = receivedApproval(forwarded);

        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(null);

        chainOpeningGrantService.onRelatedApprovalReceived(receivedApproval);

        verify(irsChainOpeningPartnerGrantRepository, never()).findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(any(), any(), any());
    }
}
