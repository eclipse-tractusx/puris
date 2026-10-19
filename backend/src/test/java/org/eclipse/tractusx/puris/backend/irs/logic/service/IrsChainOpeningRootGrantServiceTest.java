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
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
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
    private static final Instant VALID_TO = Instant.now().plusSeconds(7 * 24 * 3600L);
    private static final String PARENT_MATERIAL_NUMBER = "MNR-001";
    private static final String CHILD_MATERIAL_NUMBER = "MNR-002";
    private static final String OWN_BPNL = "BPNLXXOWNCOMPANYX";
    private static final String OTHER_CHILD_MATERIAL_NUMBER = "MNR-003";
    private static final String OTHER_SUPPLIER_BPNL = "BPNLXXOTHERSUPPLIER";

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

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
        chainOpeningGrantService = new IrsChainOpeningRootGrantService(irsRequestService, gateway,
            materialService, materialRelationService, variablesService, irsChainOpeningRootGrantRepository);
        lenient().when(irsChainOpeningRootGrantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * A grant for the parent material, backed by one own request per given BPNL that affects the child material and is
     * valid from VALID_FROM to VALID_TO. Its allowedBpnls are derived from those requests.
     */
    private IrsChainOpeningRootGrant grant(Set<String> allowedBpnls) {
        return grantBackedByRequests(requestsFor(allowedBpnls));
    }

    private Set<OwnDataExchangeRequest> requestsFor(Set<String> bpnls) {
        Set<OwnDataExchangeRequest> requests = new HashSet<>();
        for (String bpnl : bpnls) {
            requests.add(ownRequest(bpnl, List.of(childMaterial(CHILD_MATERIAL_NUMBER)), VALID_FROM, VALID_TO));
        }
        return requests;
    }

    /** A grant for the parent material, backed by the given own requests. */
    private IrsChainOpeningRootGrant grantBackedByRequests(Set<OwnDataExchangeRequest> requests) {
        return IrsChainOpeningRootGrant.builder()
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .requesterBpn(OWN_BPNL)
            .dataExchangeRequests(requests)
            .validFrom(VALID_FROM)
            .validTo(VALID_TO)
            .build();
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

    private Material childMaterial(String ownMaterialNumber) {
        Material material = new Material();
        material.setOwnMaterialNumber(ownMaterialNumber);
        return material;
    }

    /**
     * An own request to the given partner for the given materials with the given desired window. The requestId is set
     * because the request's equals/hashCode are based on it.
     */
    private OwnDataExchangeRequest ownRequest(String bpnl, List<Material> materials, Instant start, Instant end) {
        Partner partner = new Partner();
        partner.setBpnl(bpnl);
        UUID uuid = UUID.randomUUID();
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setUuid(uuid);
        request.setRequestId("urn:uuid:" + uuid);
        request.setPartner(partner);
        request.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        request.setMaterials(materials);
        request.setDesiredStartDateTime(Date.from(start));
        request.setDesiredEndDateTime(Date.from(end));
        return request;
    }

    /** An own request to ALLOWED_BPNL for the child material, valid from VALID_FROM to VALID_TO. */
    private OwnDataExchangeRequest ownRequestAffectingChild() {
        return ownRequest(ALLOWED_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)), VALID_FROM, VALID_TO);
    }

    /** Date has millisecond precision, Instant.now() does not, so the grant's window is compared against the truncated value. */
    private Instant asDateInstant(Instant instant) {
        return Date.from(instant).toInstant();
    }

    // --- createGrant ---

    @Test
    void createGrant_WhenDisabled_DoesNotSendAndReturnsNull() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grant(Set.of()));

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEnabled_BuildsBodyAndSends() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL));
        ObjectMapper mapper = new ObjectMapper();
        var body = mapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
        verify(irsRequestBodybuilder, times(1)).buildGrantCreationRequestBody(grant);
        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantAlreadySynced_SendsPut() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL));
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createOrUpdateGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.PUT, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantOutOfSync_SendsPut() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL));
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createOrUpdateGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.PUT, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE, grant.getUuid());
    }

    @Test
    void createGrant_WhenGrantWasDeletedAtIrs_SendsPostToRecreate() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL));
        grant.setSyncStatus(IrsGrantSyncStatusEnumeration.DELETED);
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.createOrUpdateGrant(grant);

        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, GRANTS_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grant.getUuid());
    }

    // --- createGrant: eligibility ---

    @Test
    void createGrant_WhenGlobalAssetIdUnknown_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenNoRequestBacksGrant_Throws() {
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant(Set.of())));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenRequestHasNotStartedYet_Succeeds() {
        OwnDataExchangeRequest futureRequest = ownRequest(ALLOWED_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)),
            Instant.now().plusSeconds(24 * 3600L), Instant.now().plusSeconds(8 * 24 * 3600L));
        IrsChainOpeningRootGrant grant = grantBackedByRequests(new HashSet<>(Set.of(futureRequest)));
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
    }

    @Test
    void createGrant_WhenRequestHasExpired_Throws() {
        OwnDataExchangeRequest expired = ownRequest(ALLOWED_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)),
            Instant.now().minusSeconds(14 * 24 * 3600L), Instant.now().minusSeconds(7 * 24 * 3600L));

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createOrUpdateGrant(grantBackedByRequests(new HashSet<>(Set.of(expired)))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenRequestAffectsNoChildMaterial_Throws() {
        OwnDataExchangeRequest unrelated = ownRequest(ALLOWED_BPNL, List.of(childMaterial(OTHER_CHILD_MATERIAL_NUMBER)), VALID_FROM, VALID_TO);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));

        assertThrows(IllegalArgumentException.class,
            () -> chainOpeningGrantService.createOrUpdateGrant(grantBackedByRequests(new HashSet<>(Set.of(unrelated)))));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGrant_WhenEveryAllowedBpnlIsBackedByARequest_Succeeds() {
        IrsChainOpeningRootGrant grant = grant(Set.of(ALLOWED_BPNL, OTHER_SUPPLIER_BPNL));
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        assertThat(grant.getAllowedBpnls()).containsExactlyInAnyOrder(ALLOWED_BPNL, OTHER_SUPPLIER_BPNL);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(grant)).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        IrsQueuedRequest result = chainOpeningGrantService.createOrUpdateGrant(grant);

        assertThat(result).isEqualTo(queuedRequest);
    }

    @Test
    void createGrant_WhenTheRequestBackingAnAllowedBpnlHasExpired_Throws() {
        OwnDataExchangeRequest expired = ownRequest(OTHER_SUPPLIER_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)),
            Instant.now().minusSeconds(14 * 24 * 3600L), Instant.now().minusSeconds(7 * 24 * 3600L));
        Set<OwnDataExchangeRequest> requests = new HashSet<>(requestsFor(Set.of(ALLOWED_BPNL)));
        requests.add(expired);
        IrsChainOpeningRootGrant grant = grantBackedByRequests(requests);

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));

        assertThrows(IllegalArgumentException.class, () -> chainOpeningGrantService.createOrUpdateGrant(grant));

        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
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

    // --- syncGrantsForRequest ---

    @Test
    void syncGrantsForRequest_WhenNoExistingGrant_CreatesGrantBackedByRequestForItsWindow() {
        OwnDataExchangeRequest request = ownRequestAffectingChild();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.syncGrantsForRequest(request);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        IrsChainOpeningRootGrant saved = captor.getValue();

        assertThat(saved.getRequesterBpn()).isEqualTo(OWN_BPNL);
        assertThat(saved.getGlobalAssetId()).isEqualTo(GLOBAL_ASSET_ID);
        assertThat(saved.getSourceDisruptionId()).isEqualTo(SOURCE_DISRUPTION_ID.toString());
        assertThat(saved.getDataExchangeRequests()).containsExactly(request);
        assertThat(saved.getAllowedBpnls()).containsExactly(ALLOWED_BPNL);
        assertThat(saved.getValidFrom()).isEqualTo(asDateInstant(VALID_FROM));
        assertThat(saved.getValidTo()).isEqualTo(asDateInstant(VALID_TO));
        assertThat(saved.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.NOT_SYNCED);
    }

    @Test
    void syncGrantsForRequest_WhenGrantIsWidenedByRequest_TransitionsToOutOfSync() {
        Instant requestStart = VALID_FROM.minusSeconds(3600);
        Instant requestEnd = VALID_TO.plusSeconds(3600);
        OwnDataExchangeRequest request = ownRequest(ALLOWED_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)), requestStart, requestEnd);
        OwnDataExchangeRequest otherRequest = ownRequest(OTHER_SUPPLIER_BPNL, List.of(childMaterial(CHILD_MATERIAL_NUMBER)), VALID_FROM, VALID_TO);

        IrsChainOpeningRootGrant existing = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .dataExchangeRequests(new HashSet<>(Set.of(otherRequest)))
            .validFrom(VALID_FROM)
            .validTo(VALID_TO)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existing));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.syncGrantsForRequest(request);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        IrsChainOpeningRootGrant saved = captor.getValue();

        assertThat(saved.getAllowedBpnls()).containsExactlyInAnyOrder(ALLOWED_BPNL, OTHER_SUPPLIER_BPNL);
        assertThat(saved.getDataExchangeRequests()).containsExactlyInAnyOrder(request, otherRequest);
        assertThat(saved.getValidFrom()).isEqualTo(asDateInstant(requestStart));
        assertThat(saved.getValidTo()).isEqualTo(asDateInstant(requestEnd));
        assertThat(saved.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    @Test
    void syncGrantsForRequest_WhenRequestAlreadyOnGrantWithinItsWindow_KeepsSyncedStatus() {
        OwnDataExchangeRequest request = ownRequestAffectingChild();

        IrsChainOpeningRootGrant existing = IrsChainOpeningRootGrant.builder()
            .requesterBpn(OWN_BPNL)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID.toString())
            .dataExchangeRequests(new HashSet<>(Set.of(request)))
            .validFrom(asDateInstant(VALID_FROM))
            .validTo(asDateInstant(VALID_TO))
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.of(existing));
        when(irsRequestService.isEnabled()).thenReturn(false);

        chainOpeningGrantService.syncGrantsForRequest(request);

        assertThat(existing.getDataExchangeRequests()).containsExactly(request);
        assertThat(existing.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.SYNCED);
    }

    @Test
    void syncGrantsForRequest_WhenIrsEnabledAndGrantEligible_EnqueuesAndSetsPending() {
        OwnDataExchangeRequest request = ownRequestAffectingChild();
        var body = objectMapper.createObjectNode().put("openingId", SOURCE_DISRUPTION_ID.toString());
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(parentMaterial());
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq(PARENT_MATERIAL_NUMBER), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        when(irsRequestBodybuilder.buildGrantCreationRequestBody(any(IrsChainOpeningGrant.class))).thenReturn(body);
        when(irsRequestQueueService.enqueue(any(), any(), any(), any(), any(), any())).thenReturn(queuedRequest);

        chainOpeningGrantService.syncGrantsForRequest(request);

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.PENDING);
        verify(irsRequestQueueService, times(1)).enqueue(eq(IrsQueuedRequestMethodEnumeration.POST), eq(GRANTS_PATH), eq(body.toString()), isNull(),
            eq(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE), any());
    }

    @Test
    void syncGrantsForRequest_WhenIrsSyncFails_SetsOutOfSyncWithoutThrowing() {
        OwnDataExchangeRequest request = ownRequestAffectingChild();

        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(childRelation()));
        when(materialService.findByOwnMaterialNumber(PARENT_MATERIAL_NUMBER)).thenReturn(parentMaterial());
        when(irsChainOpeningRootGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(
            OWN_BPNL, GLOBAL_ASSET_ID, SOURCE_DISRUPTION_ID.toString())).thenReturn(Optional.empty());
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        assertDoesNotThrow(() -> chainOpeningGrantService.syncGrantsForRequest(request));

        ArgumentCaptor<IrsChainOpeningRootGrant> captor = ArgumentCaptor.forClass(IrsChainOpeningRootGrant.class);
        verify(irsChainOpeningRootGrantRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
    }

    @Test
    void syncGrantsForRequest_WhenRequestHasNoMaterials_DoesNothing() {
        OwnDataExchangeRequest request = ownRequestAffectingChild();
        request.setMaterials(null);

        chainOpeningGrantService.syncGrantsForRequest(request);

        verify(irsChainOpeningRootGrantRepository, never()).save(any());
    }
}
