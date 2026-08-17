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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
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
    private static final UUID SOURCE_DISRUPTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant VALID_FROM = Instant.now().minusSeconds(7 * 24 * 3600L);
    private static final Instant VALID_UNTIL = Instant.now().plusSeconds(7 * 24 * 3600L);

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @Mock
    private OwnDemandAndCapacityNotificationRepository ownNotificationRepository;

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
        chainOpeningGrantService = new IrsChainOpeningPartnerGrantService(irsRequestService, gateway, ownNotificationRepository,
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

    private MaterialRelation childRelation() {
        MaterialRelation relation = new MaterialRelation();
        relation.setParentOwnMaterialNumber(OWN_MATERIAL_NUMBER);
        relation.setChildOwnMaterialNumber(CHILD_MATERIAL_NUMBER);
        return relation;
    }

    /** An OwnDemandAndCapacityNotification we sent to PARTNER_BPNL, affecting the given materials. */
    private OwnDemandAndCapacityNotification ownNotification(UUID uuid, List<Material> materials) {
        OwnDemandAndCapacityNotification notification = new OwnDemandAndCapacityNotification();
        notification.setUuid(uuid);
        notification.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        notification.setPartner(partner(PARTNER_BPNL));
        notification.setStatus(StatusEnumeration.OPEN);
        // Padded a bit wider than [VALID_FROM, VALID_UNTIL] (the grant's own bounds) so
        // isWithinNotificationBounds isn't tripped up by Instant<->Date millisecond truncation
        // at an exact boundary.
        notification.setStartDateOfEffect(Date.from(VALID_FROM.minusSeconds(3600)));
        notification.setExpectedEndDateOfEffect(Date.from(VALID_UNTIL.plusSeconds(3600)));
        notification.setMaterials(materials);
        return notification;
    }

    /** The ReportedDataExchangeRequest from PARTNER_BPNL asking approval for the given own notification. */
    private ReportedDataExchangeRequest triggeringRequest(UUID uuid, OwnDemandAndCapacityNotification notification) {
        ReportedDataExchangeRequest request = new ReportedDataExchangeRequest();
        request.setUuid(uuid);
        request.setNotification(notification);
        return request;
    }

    /** The OwnDataExchangeApproval we send to PARTNER_BPNL, approving the triggering request. */
    private OwnDataExchangeApproval sentApproval(ReportedDataExchangeRequest request) {
        OwnDataExchangeApproval approval = new OwnDataExchangeApproval();
        approval.setDataExchangeRequest(request);
        return approval;
    }

    /** A ReportedDemandAndCapacityNotification received from the given (further-upstream) partner. */
    private ReportedDemandAndCapacityNotification reportedNotification(UUID uuid, String bpnl, List<Material> materials) {
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(uuid);
        notification.setPartner(partner(bpnl));
        notification.setStatus(StatusEnumeration.OPEN);
        notification.setStartDateOfEffect(Date.from(Instant.now().minusSeconds(3600)));
        notification.setMaterials(materials);
        return notification;
    }

    /** An OwnDataExchangeRequest forwarded further upstream because of the triggering request. */
    private OwnDataExchangeRequest forwardedRequest(UUID uuid, ReportedDemandAndCapacityNotification notification, ReportedDataExchangeRequest related) {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setUuid(uuid);
        request.setNotification(notification);
        request.setRelatedDataExchangeRequest(related);
        return request;
    }

    /** The ReportedDataExchangeApproval the further-upstream partner sent back for the forwarded request. */
    private ReportedDataExchangeApproval receivedApproval(OwnDataExchangeRequest forwardedRequest) {
        ReportedDataExchangeApproval approval = new ReportedDataExchangeApproval();
        approval.setDataExchangeRequest(forwardedRequest);
        return approval;
    }

    private IrsChainOpeningPartnerGrant grant(Set<ReportedDemandAndCapacityNotification> notifications) {
        return IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .reportedNotifications(notifications)
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .build();
    }

    // --- createGrant / deleteGrant ---

    @Test
    void createGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grant(Set.of()));

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
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_DELETE), eq(grant.getUuid()));
    }

    // --- createGrant: eligibility ---

    @Test
    void createGrant_WhenNoMatchingOwnNotification_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenNoTriggeringRequestForNotification_Throws() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenGlobalAssetIdUnknown_Throws() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), matching);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.of(triggering));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenAllowedBpnlHasNoMatchingReportedNotification_Throws() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), matching);
        ReportedDemandAndCapacityNotification staleNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.of(triggering));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createGrant(grant(new HashSet<>(Set.of(staleNotification)))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEveryAllowedBpnlHasValidRelatedReportedNotification_Succeeds() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), matching);

        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.of(triggering));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(upstreamNotification)));
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grantToCreate);

        assertThat(result).isEqualTo(queuedRequest);
    }

    @Test
    void createGrant_WhenGrantAlreadySynced_SendsPut() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), matching);

        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.of(triggering));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(upstreamNotification)));
        grantToCreate.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrant(grantToCreate);

        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.PUT), any(), eq(body.toString()), isNull(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_CREATE), eq(grantToCreate.getUuid()));
    }

    @Test
    void createGrant_WhenGrantWasDeletedAtIrs_SendsPostToRecreate() {
        OwnDemandAndCapacityNotification matching = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), matching);

        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(ownNotificationRepository.findBySourceDisruptionIdAndPartnerBpnl(SOURCE_DISRUPTION_ID, PARTNER_BPNL)).thenReturn(List.of(matching));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(matching.getUuid())).thenReturn(Optional.of(triggering));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);

        IrsChainOpeningPartnerGrant grantToCreate = grant(new HashSet<>(Set.of(upstreamNotification)));
        grantToCreate.setSyncStatus(IrsGrantSyncStatusEnumeration.DELETED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grantToCreate)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrant(grantToCreate);

        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.POST), any(), eq(body.toString()), isNull(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_CREATE), eq(grantToCreate.getUuid()));
    }

    // --- createGrantsForApproval ---

    @Test
    void createGrantsForApproval_CreatesGrantPerAffectedMaterial() {
        Material material1 = grantMaterial();
        Material material2 = material(OTHER_MATERIAL_NUMBER, OTHER_GLOBAL_ASSET_ID);
        OwnDemandAndCapacityNotification notification = ownNotification(UUID.randomUUID(), List.of(material1, material2));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), notification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, OTHER_GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of());
        when(materialRelationService.findAllChildren(OTHER_MATERIAL_NUMBER)).thenReturn(List.of());
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        List<String> savedGlobalAssetIds = captor.getAllValues().stream().map(IrsChainOpeningPartnerGrant::getGlobalAssetId).distinct().toList();
        assertThat(savedGlobalAssetIds).containsExactlyInAnyOrder(GLOBAL_ASSET_ID, OTHER_GLOBAL_ASSET_ID);
    }

    // --- syncGrant reconciliation (exercised via createGrantsForApproval) ---

    @Test
    void createGrantsForApproval_AddsNotificationReachableViaChainCoveringChildMaterial() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        IrsChainOpeningPartnerGrant saved = captor.getValue();
        assertThat(saved.getAllowedBpnls()).containsExactly(SUPPLIER_BPNL);
    }

    @Test
    void createGrantsForApproval_IgnoresNotificationNotCoveringChildMaterial() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        Material unrelatedMaterial = material("MNR-999", null);
        ReportedDemandAndCapacityNotification unrelatedNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(unrelatedMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), unrelatedNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        IrsChainOpeningPartnerGrant saved = captor.getValue();
        assertThat(saved.getAllowedBpnls()).isEmpty();
    }

    @Test
    void createGrantsForApproval_IgnoresResolvedNotification() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification resolvedNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        resolvedNotification.setStatus(StatusEnumeration.RESOLVED);
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), resolvedNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        IrsChainOpeningPartnerGrant saved = captor.getValue();
        assertThat(saved.getAllowedBpnls()).isEmpty();
    }

    // --- empty->delete / non-empty->re-push transitions ---

    @Test
    void syncGrant_WhenReconciledSetBecomesEmptyOnExistingGrant_DeletesGrant() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        ReportedDemandAndCapacityNotification staleNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());
        IrsChainOpeningPartnerGrant existingGrant = IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .reportedNotifications(new HashSet<>(Set.of(staleNotification)))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existingGrant));
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of());
        when(irsRequestService.isEnabled()).thenReturn(true);
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrantsForApproval(approval);

        assertThat(existingGrant.getReportedNotifications()).isEmpty();
        assertThat(existingGrant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.PENDING);
        verify(irsRequestQueueService).enqueue(eq(IrsQueuedRequestMethodEnumeration.DELETE), any(), isNull(), any(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_DELETE), eq(existingGrant.getUuid()));
    }

    @Test
    void syncGrant_WhenReconciledSetNonEmptyAndChanged_MarksOutOfSyncThenRePushes() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval approval = sentApproval(triggering);

        ReportedDemandAndCapacityNotification staleNotification = reportedNotification(UUID.randomUUID(), "BPNLXXOLDSUPPLIER", List.of());
        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval upstreamApproval = receivedApproval(forwarded);

        IrsChainOpeningPartnerGrant existingGrant = IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .reportedNotifications(new HashSet<>(Set.of(staleNotification)))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existingGrant));
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(upstreamApproval);
        // Disabled during the re-push attempt so this test doesn't need full eligibility stubs - createGrant
        // short-circuits before assertGrantEligible, matching the existing "sync fails without throwing" pattern.
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.createGrantsForApproval(approval);

        assertThat(existingGrant.getReportedNotifications()).extracting(ReportedDemandAndCapacityNotification::getUuid)
            .containsExactly(upstreamNotification.getUuid());
        assertThat(existingGrant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    // --- onRelatedApprovalReceived ---

    @Test
    void onRelatedApprovalReceived_WhenOwnApprovalAlreadySent_SyncsGrants() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval sentApproval = sentApproval(triggering);

        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval receivedApproval = receivedApproval(forwarded);

        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(sentApproval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of());
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval);
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onRelatedApprovalReceived(receivedApproval);

        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(any());
    }

    @Test
    void onRelatedApprovalReceived_WhenOwnApprovalNotSentYet_NoOp() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);

        ReportedDemandAndCapacityNotification upstreamNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), upstreamNotification, triggering);
        ReportedDataExchangeApproval receivedApproval = receivedApproval(forwarded);

        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(null);

        chainOpeningGrantService.onRelatedApprovalReceived(receivedApproval);

        verify(irsChainOpeningPartnerGrantRepository, never()).findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(any(), any(), any());
    }

    // --- onReportedNotificationUpdated ---

    @Test
    void onReportedNotificationUpdated_WhenChainLinked_ResyncsGrant() {
        OwnDemandAndCapacityNotification ownNotification = ownNotification(UUID.randomUUID(), List.of(grantMaterial()));
        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), ownNotification);
        OwnDataExchangeApproval sentApproval = sentApproval(triggering);

        UUID notificationUuid = UUID.randomUUID();
        ReportedDemandAndCapacityNotification previous = reportedNotification(notificationUuid, SUPPLIER_BPNL, List.of());
        Material childMaterial = material(CHILD_MATERIAL_NUMBER, null);
        ReportedDemandAndCapacityNotification updated = reportedNotification(notificationUuid, SUPPLIER_BPNL, List.of(childMaterial));
        OwnDataExchangeRequest forwarded = forwardedRequest(UUID.randomUUID(), updated, triggering);

        when(ownDataExchangeRequestRepository.findByNotification_Uuid(notificationUuid)).thenReturn(Optional.of(forwarded));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(sentApproval);
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of(forwarded));
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwarded.getUuid())).thenReturn(receivedApproval(forwarded));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onReportedNotificationUpdated(previous, updated);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getAllowedBpnls()).containsExactly(SUPPLIER_BPNL);
    }

    @Test
    void onReportedNotificationUpdated_WhenNotChainLinked_NoOp() {
        UUID notificationUuid = UUID.randomUUID();
        ReportedDemandAndCapacityNotification previous = reportedNotification(notificationUuid, SUPPLIER_BPNL, List.of());
        ReportedDemandAndCapacityNotification updated = reportedNotification(notificationUuid, SUPPLIER_BPNL, List.of());

        when(ownDataExchangeRequestRepository.findByNotification_Uuid(notificationUuid)).thenReturn(Optional.empty());

        chainOpeningGrantService.onReportedNotificationUpdated(previous, updated);

        verify(irsChainOpeningPartnerGrantRepository, never()).findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(any(), any(), any());
    }

    // --- onOwnNotificationUpdated ---

    @Test
    void onOwnNotificationUpdated_WhenResolved_TearsDownGrant() {
        UUID notificationUuid = UUID.randomUUID();
        OwnDemandAndCapacityNotification previous = ownNotification(notificationUuid, List.of(grantMaterial()));
        OwnDemandAndCapacityNotification updated = ownNotification(notificationUuid, List.of());
        updated.setStatus(StatusEnumeration.RESOLVED);

        ReportedDemandAndCapacityNotification existingNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());
        IrsChainOpeningPartnerGrant existingGrant = IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .reportedNotifications(new HashSet<>(Set.of(existingNotification)))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existingGrant));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onOwnNotificationUpdated(previous, updated);

        assertThat(existingGrant.getReportedNotifications()).isEmpty();
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(existingGrant);
    }

    @Test
    void onOwnNotificationUpdated_WhenMaterialRemoved_TearsDownThatGrantOnly() {
        Material keptMaterial = grantMaterial();
        Material removedMaterial = material(OTHER_MATERIAL_NUMBER, OTHER_GLOBAL_ASSET_ID);
        UUID notificationUuid = UUID.randomUUID();
        OwnDemandAndCapacityNotification previous = ownNotification(notificationUuid, List.of(keptMaterial, removedMaterial));
        OwnDemandAndCapacityNotification updated = ownNotification(notificationUuid, List.of(keptMaterial));

        ReportedDemandAndCapacityNotification existingNotification = reportedNotification(UUID.randomUUID(), SUPPLIER_BPNL, List.of());
        IrsChainOpeningPartnerGrant removedGrant = IrsChainOpeningPartnerGrant.builder()
            .globalAssetId(OTHER_GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(PARTNER_BPNL)
            .reportedNotifications(new HashSet<>(Set.of(existingNotification)))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, OTHER_GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(removedGrant));
        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(notificationUuid)).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onOwnNotificationUpdated(previous, updated);

        assertThat(removedGrant.getReportedNotifications()).isEmpty();
        verify(irsChainOpeningPartnerGrantRepository, never()).findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString());
    }

    @Test
    void onOwnNotificationUpdated_WhenMaterialAddedAndApprovalAlreadySent_CreatesGrant() {
        UUID notificationUuid = UUID.randomUUID();
        OwnDemandAndCapacityNotification previous = ownNotification(notificationUuid, List.of());
        OwnDemandAndCapacityNotification updated = ownNotification(notificationUuid, List.of(grantMaterial()));

        ReportedDataExchangeRequest triggering = triggeringRequest(UUID.randomUUID(), updated);
        OwnDataExchangeApproval sentApproval = sentApproval(triggering);

        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(notificationUuid)).thenReturn(Optional.of(triggering));
        when(ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(sentApproval);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(grantMaterial());
        when(irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            PARTNER_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(materialRelationService.findAllChildren(OWN_MATERIAL_NUMBER)).thenReturn(List.of());
        when(ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggering.getUuid())).thenReturn(List.of());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onOwnNotificationUpdated(previous, updated);

        ArgumentCaptor<IrsChainOpeningPartnerGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningPartnerGrant.class);
        verify(irsChainOpeningPartnerGrantRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(g -> GLOBAL_ASSET_ID.equals(g.getGlobalAssetId()));
    }

    @Test
    void onOwnNotificationUpdated_WhenMaterialAddedButNoApprovalSentYet_NoOp() {
        UUID notificationUuid = UUID.randomUUID();
        OwnDemandAndCapacityNotification previous = ownNotification(notificationUuid, List.of());
        OwnDemandAndCapacityNotification updated = ownNotification(notificationUuid, List.of(grantMaterial()));

        when(reportedDataExchangeRequestRepository.findByNotification_Uuid(notificationUuid)).thenReturn(Optional.empty());

        chainOpeningGrantService.onOwnNotificationUpdated(previous, updated);

        verify(irsChainOpeningPartnerGrantRepository, never()).findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(any(), any(), any());
    }
}
