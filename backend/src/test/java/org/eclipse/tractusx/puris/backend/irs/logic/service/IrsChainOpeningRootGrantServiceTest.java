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

import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.ReportedDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsChainOpeningRootGrantServiceTest {

    private static final String GRANTS_PATH = "irs/recursive/chain-openings/grants";

    private static final String GLOBAL_ASSET_ID = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0";
    private static final String ALLOWED_BPNL = "BPNLXXSUPPLIERXX";
    private static final UUID SOURCE_DISRUPTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant VALID_FROM = Instant.now().minusSeconds(7 * 24 * 3600L);
    private static final Instant VALID_UNTIL = Instant.now().plusSeconds(7 * 24 * 3600L);
    private static final String PARENT_MATERIAL_NUMBER = "MNR-001";
    private static final String CHILD_MATERIAL_NUMBER = "MNR-002";
    private static final String OWN_BPNL = "BPNLXXOWNCOMPANYX";
    private static final String OTHER_CHILD_MATERIAL_NUMBER = "MNR-003";
    private static final String OTHER_PARENT_MATERIAL_NUMBER = "MNR-004";
    private static final String OTHER_GLOBAL_ASSET_ID = "urn:uuid:00000000-0000-0000-0000-0000000000aa";

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @Mock
    private ReportedDemandAndCapacityNotificationRepository reportedNotificationRepository;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialRelationService materialRelationService;

    @Mock
    private VariablesService variablesService;

    @Mock
    private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

    private IrsChainOpeningRootGrantService chainOpeningGrantService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IrsChainOpeningGrantGateway gateway = new IrsChainOpeningGrantGateway(irsRequestBodybuilder, irsRequestQueueService);
        chainOpeningGrantService = new IrsChainOpeningRootGrantService(irsRequestService, gateway, reportedNotificationRepository,
            materialService, materialRelationService, variablesService, irsChainOpeningRootGrantRepository);
        lenient().when(irsChainOpeningRootGrantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private IrsChainOpeningRootGrant grant(Set<String> allowedBpnls) {
        return IrsChainOpeningRootGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(OWN_BPNL)
            .reportedNotifications(notificationsFor(allowedBpnls))
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .build();
    }

    /**
     * Builds one bare notification per given BPNL (each with a distinct uuid and a partner with
     * that BPNL), standing in for the reported notifications that would back a grant with those
     * allowedBpnls, since allowedBpnls is now derived from reportedNotifications.
     */
    private Set<ReportedDemandAndCapacityNotification> notificationsFor(Set<String> bpnls) {
        Set<ReportedDemandAndCapacityNotification> notifications = new HashSet<>();
        for (String bpnl : bpnls) {
            Partner partner = new Partner();
            partner.setBpnl(bpnl);
            ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
            notification.setUuid(UUID.randomUUID());
            notification.setPartner(partner);
            notifications.add(notification);
        }
        return notifications;
    }

    private Material parentMaterial() {
        Material material = new Material();
        material.setOwnMaterialNumber(PARENT_MATERIAL_NUMBER);
        material.setMaterialNumberCx(GLOBAL_ASSET_ID);
        return material;
    }

    private MaterialRelation childRelation() {
        MaterialRelation relation = new MaterialRelation();
        relation.setParentOwnMaterialNumber(PARENT_MATERIAL_NUMBER);
        relation.setChildOwnMaterialNumber(CHILD_MATERIAL_NUMBER);
        return relation;
    }

    private ReportedDemandAndCapacityNotification reportedNotificationAffectingChild() {
        Material childMaterial = new Material();
        childMaterial.setOwnMaterialNumber(CHILD_MATERIAL_NUMBER);

        Partner partner = new Partner();
        partner.setBpnl(ALLOWED_BPNL);

        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setPartner(partner);
        notification.setStatus(StatusEnumeration.OPEN);
        notification.setStartDateOfEffect(Date.from(Instant.now().minusSeconds(3600)));
        notification.setMaterials(List.of(childMaterial));
        return notification;
    }

    private Material childMaterial(String ownMaterialNumber) {
        Material material = new Material();
        material.setOwnMaterialNumber(ownMaterialNumber);
        return material;
    }

    private Material otherParentMaterial() {
        Material material = new Material();
        material.setOwnMaterialNumber(OTHER_PARENT_MATERIAL_NUMBER);
        material.setMaterialNumberCx(OTHER_GLOBAL_ASSET_ID);
        return material;
    }

    private MaterialRelation otherChildRelation() {
        MaterialRelation relation = new MaterialRelation();
        relation.setParentOwnMaterialNumber(OTHER_PARENT_MATERIAL_NUMBER);
        relation.setChildOwnMaterialNumber(OTHER_CHILD_MATERIAL_NUMBER);
        return relation;
    }

    private ReportedDemandAndCapacityNotification notificationWithMaterials(UUID uuid, List<Material> materials) {
        Partner partner = new Partner();
        partner.setBpnl(ALLOWED_BPNL);
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(uuid);
        notification.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        notification.setPartner(partner);
        notification.setStatus(StatusEnumeration.OPEN);
        notification.setStartDateOfEffect(Date.from(VALID_FROM));
        notification.setExpectedEndDateOfEffect(Date.from(VALID_UNTIL));
        notification.setMaterials(materials);
        return notification;
    }

    /**
     * A bare notification instance sharing the given uuid, standing in for the same underlying
     * notification row as loaded into a grant's reportedNotifications by a separate query, since
     * removal must be matched by uuid rather than by reference or default entity equality.
     */
    private ReportedDemandAndCapacityNotification bareNotificationCopy(UUID uuid) {
        Partner partner = new Partner();
        partner.setBpnl(ALLOWED_BPNL);
        ReportedDemandAndCapacityNotification copy = new ReportedDemandAndCapacityNotification();
        copy.setUuid(uuid);
        copy.setPartner(partner);
        return copy;
    }

    // --- createGrant ---

    @Test
    void createGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grant(Set.of()));

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEnabled_BuildsBodyAndSends() {
        IrsChainOpeningRootGrant grant = grant(Set.of());
        ObjectMapper mapper = new ObjectMapper();
        var body = mapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
        verify(irsRequestBodybuilder, times(1)).buildGrantCreationRequestBody(grant);
        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantAlreadySynced_SendsPut() {
        IrsChainOpeningRootGrant grant = grant(Set.of());
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.PUT, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantOutOfSync_SendsPut() {
        IrsChainOpeningRootGrant grant = grant(Set.of());
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.PUT, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantWasDeletedAtIrs_SendsPostToRecreate() {
        IrsChainOpeningRootGrant grant = grant(Set.of());
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.DELETED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    // --- createGrant: eligibility ---

    @Test
    void createGrant_WhenGlobalAssetIdUnknown_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenNoMatchingActiveReportedNotification_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenMatchingActiveReportedNotification_Succeeds() {
        IrsChainOpeningRootGrant grant = grant(Set.of());
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
    }

    @Test
    void createGrant_WhenAllowedBpnlHasNoMatchingReportedNotification_Throws() {
        ReportedDemandAndCapacityNotification fromOtherSupplier = reportedNotificationAffectingChild();
        fromOtherSupplier.getPartner().setBpnl("BPNLXXOTHERSUPPLIER");

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(fromOtherSupplier));

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createGrant(grant(Set.of(ALLOWED_BPNL))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEveryAllowedBpnlHasValidRelatedReportedNotification_Succeeds() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL));
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.findAllChildren(PARENT_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(reportedNotificationRepository.findAllBySourceDisruptionId(SOURCE_DISRUPTION_ID))
            .thenReturn(List.of(reportedNotificationAffectingChild()));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
    }

    // --- syncGrantsForNotification ---

    private ReportedDemandAndCapacityNotification incomingReportedNotification() {
        Material childMaterial = new Material();
        childMaterial.setOwnMaterialNumber(CHILD_MATERIAL_NUMBER);

        Partner partner = new Partner();
        partner.setBpnl(ALLOWED_BPNL);

        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        notification.setPartner(partner);
        notification.setStatus(StatusEnumeration.OPEN);
        notification.setStartDateOfEffect(Date.from(VALID_FROM));
        notification.setExpectedEndDateOfEffect(Date.from(VALID_UNTIL));
        notification.setMaterials(List.of(childMaterial));
        return notification;
    }

    @Test
    void syncGrantsForNotification_WhenNoExistingGrant_CreatesNewGrantWithAllowedBpnl() {
        ReportedDemandAndCapacityNotification notification = incomingReportedNotification();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.syncGrantsForNotification(notification);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        IrsChainOpeningRootGrant saved = captor.getValue();

        assertThat(saved.getRequesterBpn()).isEqualTo(OWN_BPNL);
        assertThat(saved.getGlobalAssetId()).isEqualTo(GLOBAL_ASSET_ID);
        assertThat(saved.getSourceDisruptionId()).isEqualTo(SOURCE_DISRUPTION_ID.toString());
        assertThat(saved.getAllowedBpnls()).containsExactly(ALLOWED_BPNL);
        assertThat(saved.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.NOT_SYNCED);
    }

    @Test
    void syncGrantsForNotification_WhenExistingSyncedGrantChanges_TransitionsToOutOfSync() {
        ReportedDemandAndCapacityNotification notification = incomingReportedNotification();

        IrsChainOpeningRootGrant existing = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .reportedNotifications(notificationsFor(new HashSet<>(Set.of("BPNLXXOTHERSUPPLIER"))))
            .validFrom(VALID_FROM)
            .validUntil(VALID_UNTIL)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existing));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.syncGrantsForNotification(notification);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        IrsChainOpeningRootGrant saved = captor.getValue();

        assertThat(saved.getAllowedBpnls()).containsExactlyInAnyOrder(ALLOWED_BPNL, "BPNLXXOTHERSUPPLIER");
        assertThat(saved.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    @Test
    void syncGrantsForNotification_WhenIrsSyncFails_SetsOutOfSyncWithoutThrowing() {
        ReportedDemandAndCapacityNotification notification = incomingReportedNotification();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        // IRS enabled, but eligibility fails (globalAssetId does not resolve to a material during the push),
        // simulating an IRS/eligibility failure that must not propagate out of syncGrantsForNotification.
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertDoesNotThrow(() -> chainOpeningGrantService.syncGrantsForNotification(notification));

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        IrsChainOpeningRootGrant saved = captor.getValue();

        assertThat(saved.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    // --- deleteGrant ---

    @Test
    void deleteGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .globalAssetId("asset-1")
            .sourceDisruptionId("opening-1")
            .requesterBpn(OWN_BPNL)
            .build();

        IrsQueuedRequest result = chainOpeningGrantService.deleteGrant(grant);

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteGrant_WhenEnabled_SendsExpectedQueryParams() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .globalAssetId("my-global-asset-id")
            .sourceDisruptionId("my-source-disruption-id")
            .requesterBpn(OWN_BPNL)
            .build();

        IrsQueuedRequest result = chainOpeningGrantService.deleteGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.DELETE), eq(GRANTS_PATH), isNull(), paramsCaptor.capture(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_DELETE), isNull());

        Map<String, String> params = paramsCaptor.getValue();
        assertThat(params).containsEntry("openingId", "my-source-disruption-id")
            .containsEntry("useCase", IrsAdapterConfiguration.PURIS_USE_CASE)
            .containsEntry("requesterBpn", OWN_BPNL)
            .containsEntry("globalAssetId", "my-global-asset-id");
    }

    // --- onReportedNotificationUpdated ---

    @Test
    void onReportedNotificationUpdated_WhenResolvedAndGrantBecomesEmpty_EnqueuesDeleteAndSetsPending() {
        UUID notificationUuid = UUID.randomUUID();
        ReportedDemandAndCapacityNotification previous = notificationWithMaterials(notificationUuid, List.of(childMaterial(CHILD_MATERIAL_NUMBER)));
        ReportedDemandAndCapacityNotification updated = notificationWithMaterials(notificationUuid, List.of());
        updated.setStatus(StatusEnumeration.RESOLVED);

        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .reportedNotifications(new HashSet<>(Set.of(bareNotificationCopy(notificationUuid))))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notificationUuid)).thenReturn(List.of(grant));
        when(irsRequestService.isEnabled()).thenReturn(true);
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.onReportedNotificationUpdated(previous, updated);

        assertThat(grant.getReportedNotifications()).isEmpty();
        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.PENDING);
        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.DELETE), eq(GRANTS_PATH), isNull(), any(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_DELETE), eq(grant.getUuid()));
    }

    @Test
    void onReportedNotificationUpdated_WhenResolvedButGrantStillHasOtherNotifications_MarksOutOfSyncAndRePushes() {
        UUID notificationUuid = UUID.randomUUID();
        UUID otherNotificationUuid = UUID.randomUUID();
        ReportedDemandAndCapacityNotification previous = notificationWithMaterials(notificationUuid, List.of(childMaterial(CHILD_MATERIAL_NUMBER)));
        ReportedDemandAndCapacityNotification updated = notificationWithMaterials(notificationUuid, List.of());
        updated.setStatus(StatusEnumeration.RESOLVED);

        Set<ReportedDemandAndCapacityNotification> notifications = new HashSet<>();
        notifications.add(bareNotificationCopy(notificationUuid));
        notifications.add(bareNotificationCopy(otherNotificationUuid));

        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .reportedNotifications(notifications)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notificationUuid)).thenReturn(List.of(grant));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onReportedNotificationUpdated(previous, updated);

        assertThat(grant.getReportedNotifications()).extracting(ReportedDemandAndCapacityNotification::getUuid)
            .containsExactly(otherNotificationUuid);
        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void onReportedNotificationUpdated_WhenAffectedMaterialsChange_MovesNotificationBetweenParentGrants() {
        UUID notificationUuid = UUID.randomUUID();
        UUID unrelatedNotificationUuid = UUID.randomUUID();
        ReportedDemandAndCapacityNotification previous = notificationWithMaterials(notificationUuid, List.of(childMaterial(CHILD_MATERIAL_NUMBER)));
        ReportedDemandAndCapacityNotification updated = notificationWithMaterials(notificationUuid, List.of(childMaterial(OTHER_CHILD_MATERIAL_NUMBER)));

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialRelationService.findAllParents(OTHER_CHILD_MATERIAL_NUMBER)).thenReturn(List.of(otherChildRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(materialService.findByOwnMaterialNumber(OTHER_PARENT_MATERIAL_NUMBER)).thenReturn(otherParentMaterial());

        Set<ReportedDemandAndCapacityNotification> oldGrantNotifications = new HashSet<>();
        oldGrantNotifications.add(bareNotificationCopy(notificationUuid));
        oldGrantNotifications.add(bareNotificationCopy(unrelatedNotificationUuid));

        IrsChainOpeningRootGrant oldParentGrant = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .reportedNotifications(oldGrantNotifications)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(oldParentGrant));
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, OTHER_GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.onReportedNotificationUpdated(previous, updated);

        assertThat(oldParentGrant.getReportedNotifications()).extracting(ReportedDemandAndCapacityNotification::getUuid)
            .containsExactly(unrelatedNotificationUuid);
        assertThat(oldParentGrant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, atLeastOnce()).save(captor.capture());
        IrsChainOpeningRootGrant newParentGrant = captor.getAllValues().stream()
            .filter(candidate -> OTHER_GLOBAL_ASSET_ID.equals(candidate.getGlobalAssetId()))
            .findFirst().orElseThrow();
        assertThat(newParentGrant.getReportedNotifications()).extracting(ReportedDemandAndCapacityNotification::getUuid)
            .containsExactly(notificationUuid);
        assertThat(newParentGrant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.NOT_SYNCED);
    }
}
