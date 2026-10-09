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

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJobStateEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IrsJobServiceTest {

    private static final String GLOBAL_ASSET_ID = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0";
    private static final String OTHER_GLOBAL_ASSET_ID = "urn:uuid:00000000-0000-0000-0000-0000000000aa";
    private static final String CHILD_MATERIAL_NUMBER = "MNR-002";
    private static final String OTHER_MATERIAL_NUMBER = "MNR-003";

    @Mock
    private IrsJobRepository irsJobRepository;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Mock
    private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialRelationService materialRelationService;

    @Mock
    private OwnDataExchangeRequestRepository ownDataExchangeRequestRepository;

    @Mock
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;

    @InjectMocks
    private IrsJobService irsJobService;

    private IrsJob validJob;
    private Material material;
    private OwnDataExchangeRequest approvedRequest;

    @BeforeEach
    void setUp() {
        material = new Material();
        material.setOwnMaterialNumber("MNR-001");
        material.setProductFlag(true);

        validJob = new IrsJob();
        validJob.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        validJob.setMaterial(material);
        validJob.setState(IrsJobStateEnumeration.INITIAL);

        approvedRequest = ownRequest(CHILD_MATERIAL_NUMBER);

        lenient().when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        lenient().when(materialRelationService.resolveChildOwnMaterialNumbers(any(), any())).thenReturn(Set.of(CHILD_MATERIAL_NUMBER));
        lenient().when(ownDataExchangeRequestRepository.findAll()).thenReturn(List.of(approvedRequest));
        lenient().when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(approvedRequest.getUuid()))
            .thenReturn(new ReportedDataExchangeApproval());
    }

    // --- createAndSend ---

    private IrsJob savedJobWithUuid() {
        IrsJob saved = new IrsJob();
        saved.setUuid(UUID.randomUUID());
        saved.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        saved.setMaterial(material);
        return saved;
    }

    @Test
    void createAndSend_WhenAdapterDisabled_SkipsJobCreation() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);

        IrsJob result = irsJobService.createAndSend(validJob);

        assertThat(result).isNull();
        verify(irsJobRepository, never()).save(any());
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createAndSend_WhenEnqueueSucceeds_SetsRequestStatusPendingAndUpdates() {
        IrsJob saved = savedJobWithUuid();
        ObjectNode body = new ObjectMapper().createObjectNode();
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsJobRepository.save(validJob)).thenReturn(saved);
        when(irsRequestBodybuilder.buildJobCreationRequestBody(saved)).thenReturn(body);
        when(irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, "irs/recursive/jobs", body.toString(), null,
            IrsQueuedRequestTypeEnumeration.JOB_CREATE, saved.getUuid())).thenReturn(queuedRequest);
        when(irsJobRepository.findById(saved.getUuid())).thenReturn(Optional.of(saved));
        when(irsJobRepository.save(saved)).thenReturn(saved);

        IrsJob result = irsJobService.createAndSend(validJob);

        assertThat(result.getRequestStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.PENDING);
        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, "irs/recursive/jobs", body.toString(), null,
            IrsQueuedRequestTypeEnumeration.JOB_CREATE, saved.getUuid());
        verify(irsJobRepository, times(1)).save(saved);
    }

    @Test
    void createAndSend_WhenMaterialIneligible_ThrowsBeforeSending() {
        material.setProductFlag(false);

        assertThrows(IllegalArgumentException.class, () -> irsJobService.createAndSend(validJob));

        verify(irsJobRepository, never()).save(any());
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
        verify(irsRequestBodybuilder, never()).buildJobCreationRequestBody(any());
    }

    // --- createJobsForRequest ---

    private IrsChainOpeningRootGrant rootGrant(String globalAssetId, String sourceDisruptionId) {
        return IrsChainOpeningRootGrant.builder()
            .globalAssetId(globalAssetId)
            .sourceDisruptionId(sourceDisruptionId)
            .build();
    }

    private void stubSuccessfulSend() {
        ObjectNode body = new ObjectMapper().createObjectNode();
        when(irsJobRepository.save(any(IrsJob.class))).thenAnswer(invocation -> {
            IrsJob job = invocation.getArgument(0);
            job.setUuid(UUID.randomUUID());
            return job;
        });
        when(irsRequestBodybuilder.buildJobCreationRequestBody(any())).thenReturn(body);
        when(irsJobRepository.findById(any(UUID.class))).thenReturn(Optional.of(new IrsJob()));
    }

    @Test
    void createJobsForRequest_WhenGrantMaterialNotFound_SkipsGrantWithoutThrowing() {
        OwnDataExchangeRequest request = ownRequest(CHILD_MATERIAL_NUMBER);
        IrsChainOpeningRootGrant grant = rootGrant(GLOBAL_ASSET_ID, request.getSourceDisruptionId().toString());
        when(irsChainOpeningRootGrantRepository.findAllByDataExchangeRequests_Uuid(request.getUuid())).thenReturn(List.of(grant));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        irsJobService.createJobsForRequest(request);

        verify(irsJobRepository, never()).save(any());
    }

    @Test
    void createJobsForRequest_WhenOneGrantIneligible_ContinuesWithRemainingGrants() {
        OwnDataExchangeRequest request = ownRequest(CHILD_MATERIAL_NUMBER);
        String sourceDisruptionId = request.getSourceDisruptionId().toString();
        IrsChainOpeningRootGrant ineligibleGrant = rootGrant(OTHER_GLOBAL_ASSET_ID, sourceDisruptionId);
        IrsChainOpeningRootGrant eligibleGrant = rootGrant(GLOBAL_ASSET_ID, sourceDisruptionId);
        when(irsChainOpeningRootGrantRepository.findAllByDataExchangeRequests_Uuid(request.getUuid()))
            .thenReturn(List.of(ineligibleGrant, eligibleGrant));

        Material ineligibleMaterial = new Material();
        ineligibleMaterial.setOwnMaterialNumber("MNR-002");
        ineligibleMaterial.setProductFlag(false);
        when(materialService.findByMaterialNumberCx(OTHER_GLOBAL_ASSET_ID)).thenReturn(ineligibleMaterial);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(material);
        stubSuccessfulSend();

        irsJobService.createJobsForRequest(request);

        // one successful create+send cycle for the eligible grant: one save on create, one on update
        verify(irsJobRepository, times(2)).save(any(IrsJob.class));
    }

    /** An own request for the given materials, whose desired window runs from an hour ago to in an hour. */
    private OwnDataExchangeRequest ownRequest(String... ownMaterialNumbers) {
        List<Material> materials = new ArrayList<>();
        for (String ownMaterialNumber : ownMaterialNumbers) {
            Material requested = new Material();
            requested.setOwnMaterialNumber(ownMaterialNumber);
            materials.add(requested);
        }
        UUID uuid = UUID.randomUUID();
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setUuid(uuid);
        request.setRequestId("urn:uuid:" + uuid);
        request.setSourceDisruptionId(UUID.randomUUID());
        request.setMaterials(materials);
        request.setDesiredStartDateTime(Date.from(Instant.now().minusSeconds(3600)));
        request.setDesiredEndDateTime(Date.from(Instant.now().plusSeconds(3600)));
        return request;
    }

    /**
     * Stubs the request's root grant for GLOBAL_ASSET_ID and its material, and makes the request the only stored request, which
     * overrides the approved request of setUp.
     */
    private IrsChainOpeningRootGrant stubRootGrantOfRequest(OwnDataExchangeRequest request) {
        IrsChainOpeningRootGrant grant = rootGrant(GLOBAL_ASSET_ID, request.getSourceDisruptionId().toString());
        when(irsChainOpeningRootGrantRepository.findAllByDataExchangeRequests_Uuid(request.getUuid())).thenReturn(List.of(grant));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(material);
        when(ownDataExchangeRequestRepository.findAll()).thenReturn(List.of(request));
        return grant;
    }

    @Test
    void createJobsForRequest_WhenRequestWasApproved_CreatesAndSendsJob() {
        OwnDataExchangeRequest request = ownRequest(CHILD_MATERIAL_NUMBER);
        IrsChainOpeningRootGrant grant = stubRootGrantOfRequest(request);
        when(reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid())).thenReturn(new ReportedDataExchangeApproval());
        stubSuccessfulSend();

        irsJobService.createJobsForRequest(request);

        ArgumentCaptor<IrsJob> captor = ArgumentCaptor.forClass(IrsJob.class);
        verify(irsJobRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getMaterial()).isEqualTo(material);
        assertThat(captor.getValue().getSourceDisruptionId()).isEqualTo(grant.getSourceDisruptionId());
    }

    @Test
    void createJobsForRequest_WhenRequestWasNotApproved_SkipsGrant() {
        OwnDataExchangeRequest request = ownRequest(CHILD_MATERIAL_NUMBER);
        stubRootGrantOfRequest(request);
        irsJobService.createJobsForRequest(request);

        verify(irsJobRepository, never()).save(any());
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createJobsForRequest_WhenRequestWindowHasEnded_SkipsGrant() {
        OwnDataExchangeRequest request = ownRequest(CHILD_MATERIAL_NUMBER);
        request.setDesiredEndDateTime(Date.from(Instant.now().minusSeconds(3600)));
        stubRootGrantOfRequest(request);

        irsJobService.createJobsForRequest(request);

        verify(irsJobRepository, never()).save(any());
    }

    @Test
    void createJobsForRequest_WhenRequestAffectsNoChildMaterial_SkipsGrant() {
        OwnDataExchangeRequest request = ownRequest(OTHER_MATERIAL_NUMBER);
        stubRootGrantOfRequest(request);

        irsJobService.createJobsForRequest(request);

        verify(irsJobRepository, never()).save(any());
    }
}
