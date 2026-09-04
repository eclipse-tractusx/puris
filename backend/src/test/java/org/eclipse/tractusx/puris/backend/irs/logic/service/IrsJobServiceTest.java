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

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
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
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IrsJobServiceTest {

    private static final String GLOBAL_ASSET_ID = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0";
    private static final String OTHER_GLOBAL_ASSET_ID = "urn:uuid:00000000-0000-0000-0000-0000000000aa";

    @Mock
    private IrsJobRepository irsJobRepository;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @Mock
    private ReportedDemandAndCapacityNotificationService reportedNotificationService;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Mock
    private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

    @Mock
    private MaterialService materialService;

    @InjectMocks
    private IrsJobService irsJobService;

    private IrsJob validJob;
    private Material material;

    @BeforeEach
    void setUp() {
        material = new Material();
        material.setOwnMaterialNumber("MNR-001");
        material.setProductFlag(true);

        validJob = new IrsJob();
        validJob.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        validJob.setMaterial(material);
        validJob.setState(IrsJobStateEnumeration.INITIAL);

        lenient().when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        lenient().when(reportedNotificationService.isAnyChildAffectedByActiveNotifications(material)).thenReturn(true);
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

    // --- createJobsForNotification ---

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
    void createJobsForNotification_WhenGrantMaterialNotFound_SkipsGrantWithoutThrowing() {
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        IrsChainOpeningRootGrant grant = rootGrant(GLOBAL_ASSET_ID, UUID.randomUUID().toString());
        when(irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notification.getUuid())).thenReturn(List.of(grant));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(null);

        irsJobService.createJobsForNotification(notification);

        verify(irsJobRepository, never()).save(any());
    }

    @Test
    void createJobsForNotification_WhenOneGrantIneligible_ContinuesWithRemainingGrants() {
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        String sourceDisruptionId = UUID.randomUUID().toString();
        IrsChainOpeningRootGrant ineligibleGrant = rootGrant(OTHER_GLOBAL_ASSET_ID, sourceDisruptionId);
        IrsChainOpeningRootGrant eligibleGrant = rootGrant(GLOBAL_ASSET_ID, sourceDisruptionId);
        when(irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notification.getUuid()))
            .thenReturn(List.of(ineligibleGrant, eligibleGrant));

        Material ineligibleMaterial = new Material();
        ineligibleMaterial.setOwnMaterialNumber("MNR-002");
        ineligibleMaterial.setProductFlag(false);
        when(materialService.findByMaterialNumberCx(OTHER_GLOBAL_ASSET_ID)).thenReturn(ineligibleMaterial);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(material);
        stubSuccessfulSend();

        irsJobService.createJobsForNotification(notification);

        // one successful create+send cycle for the eligible grant: one save on create, one on update
        verify(irsJobRepository, times(2)).save(any(IrsJob.class));
    }
}
