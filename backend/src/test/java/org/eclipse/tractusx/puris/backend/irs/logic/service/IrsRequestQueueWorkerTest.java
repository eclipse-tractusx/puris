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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningPartnerGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningPartnerGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsRequestQueueWorkerTest {

    @Mock
    private IrsQueuedRequestRepository irsQueuedRequestRepository;

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Mock
    private ExecutorService executorService;

    @Mock
    private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

    @Mock
    private IrsChainOpeningPartnerGrantRepository irsChainOpeningGrantRepository;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @InjectMocks
    private IrsRequestQueueWorker worker;

    @BeforeEach
    void setUp() {
        lenient().when(irsAdapterConfiguration.getQueueInitialRetryDelaySeconds()).thenReturn(30L);
        lenient().when(irsAdapterConfiguration.getQueueBackoffMultiplier()).thenReturn(2.0);
        lenient().when(irsAdapterConfiguration.getQueueMaxRetryDelaySeconds()).thenReturn(3600L);
    }

    private IrsQueuedRequest dueRequest(int attemptCount, int maxAttempts) {
        IrsQueuedRequest request = new IrsQueuedRequest();
        request.setUuid(UUID.randomUUID());
        request.setMethod(IrsQueuedRequestMethodEnumeration.POST);
        request.setPath("irs/policies");
        request.setBody("{}");
        request.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        request.setAttemptCount(attemptCount);
        request.setMaxAttempts(maxAttempts);
        request.setNextAttemptAt(Instant.now().minusSeconds(10));
        request.setCreatedAt(Instant.now().minusSeconds(100));
        return request;
    }

    private void stubDue(IrsQueuedRequest... requests) {
        when(irsQueuedRequestRepository.findAllByStatusAndNextAttemptAtBefore(eq(IrsQueuedRequestStatusEnumeration.PENDING), any()))
            .thenReturn(List.of(requests));
    }

    /** A due, terminal-on-first-failure (maxAttempts=1) queued grant create request. */
    private IrsQueuedRequest dueGrantCreateRequest(IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid) {
        IrsQueuedRequest request = new IrsQueuedRequest();
        request.setUuid(UUID.randomUUID());
        request.setMethod(IrsQueuedRequestMethodEnumeration.POST);
        request.setPath("irs/recursive/chain-openings/grants");
        request.setBody("{}");
        request.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        request.setAttemptCount(0);
        request.setMaxAttempts(1);
        request.setNextAttemptAt(Instant.now().minusSeconds(10));
        request.setCreatedAt(Instant.now().minusSeconds(100));
        request.setType(type);
        request.setLinkedEntityUuid(linkedEntityUuid);
        return request;
    }

    @Test
    void processDueRequests_WhenNoneAreDue_DoesNothing() {
        stubDue();

        worker.processDueRequests();

        verify(irsRequestService, never()).execute(any(IrsQueuedRequest.class));
        verify(irsQueuedRequestRepository, never()).save(any());
    }

    @Test
    void processDueRequests_OnDispatchException_TreatsAsFailureAndRetries() {
        IrsQueuedRequest request = dueRequest(0, 5);
        stubDue(request);

        when(irsRequestService.execute(request))
            .thenThrow(new IllegalStateException("adapter disabled"));

        worker.processDueRequests();

        assertThat(request.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.PENDING);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getLastErrorMessage()).contains("adapter disabled");
    }

    @Test
    void processDueRequests_OnDispatchExceptionWithNewlineMessage_SanitizesLastErrorMessageBeforeSaving() {
        IrsQueuedRequest request = dueRequest(0, 5);
        stubDue(request);

        when(irsRequestService.execute(request))
            .thenThrow(new IllegalStateException("line one\nline two"));

        assertThatCode(worker::processDueRequests).doesNotThrowAnyException();

        assertThat(request.getLastErrorMessage()).doesNotContain("\n").contains("line one", "line two");
        verify(irsQueuedRequestRepository, times(1)).save(request);
    }

    @Test
    void processDueRequests_OnFailedResponseWithNewlineBody_SanitizesLastErrorMessageBeforeSaving() {
        IrsQueuedRequest request = dueRequest(0, 5);
        stubDue(request);

        IrsRequestService.IrsResponse response = IrsRequestService.IrsResponse.builder()
            .statusCode(500)
            .responseBody("line one\nline two")
            .successful(false)
            .build();
        when(irsRequestService.execute(request))
            .thenReturn(response);

        assertThatCode(worker::processDueRequests).doesNotThrowAnyException();

        assertThat(request.getLastErrorMessage()).doesNotContain("\n").contains("line one", "line two");
        verify(irsQueuedRequestRepository, times(1)).save(request);
    }

    // --- updateLinkedEntity: CREATE-type status preservation (root grants) ---

    @Test
    void processDueRequests_WhenRootGrantCreateFailsAndWasNotSynced_KeepsNotSynced() {
        UUID grantUuid = UUID.randomUUID();
        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .uuid(grantUuid)
            .syncStatus(IrsGrantSyncStatusEnumeration.NOT_SYNCED)
            .build();
        IrsQueuedRequest request = dueGrantCreateRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grantUuid);
        stubDue(request);
        when(irsChainOpeningRootGrantRepository.findById(grantUuid)).thenReturn(Optional.of(grant));
        when(irsRequestService.execute(any(IrsQueuedRequest.class))).thenThrow(new IllegalStateException("adapter disabled"));

        worker.processDueRequests();

        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.NOT_SYNCED);
        verify(irsChainOpeningRootGrantRepository).save(grant);
    }

    @Test
    void processDueRequests_WhenRootGrantCreateFailsAndWasAlreadySynced_MarksOutOfSync() {
        UUID grantUuid = UUID.randomUUID();
        IrsChainOpeningRootGrant grant = IrsChainOpeningRootGrant.builder()
            .uuid(grantUuid)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .build();
        IrsQueuedRequest request = dueGrantCreateRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_CREATE, grantUuid);
        stubDue(request);
        when(irsChainOpeningRootGrantRepository.findById(grantUuid)).thenReturn(Optional.of(grant));
        when(irsRequestService.execute(any(IrsQueuedRequest.class))).thenThrow(new IllegalStateException("adapter disabled"));

        worker.processDueRequests();

        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
        verify(irsChainOpeningRootGrantRepository).save(grant);
    }

    // --- updateLinkedEntity: CREATE-type status preservation (partner grants) ---

    @Test
    void processDueRequests_WhenGrantCreateFailsAndWasNotSynced_KeepsNotSynced() {
        UUID grantUuid = UUID.randomUUID();
        IrsChainOpeningPartnerGrant grant = IrsChainOpeningPartnerGrant.builder()
            .uuid(grantUuid)
            .syncStatus(IrsGrantSyncStatusEnumeration.NOT_SYNCED)
            .build();
        IrsQueuedRequest request = dueGrantCreateRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_CREATE, grantUuid);
        stubDue(request);
        when(irsChainOpeningGrantRepository.findById(grantUuid)).thenReturn(Optional.of(grant));
        when(irsRequestService.execute(any(IrsQueuedRequest.class))).thenThrow(new IllegalStateException("adapter disabled"));

        worker.processDueRequests();

        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.NOT_SYNCED);
        verify(irsChainOpeningGrantRepository).save(grant);
    }

    @Test
    void processDueRequests_WhenGrantCreateFailsAndWasAlreadyOutOfSync_StaysOutOfSync() {
        UUID grantUuid = UUID.randomUUID();
        IrsChainOpeningPartnerGrant grant = IrsChainOpeningPartnerGrant.builder()
            .uuid(grantUuid)
            .syncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC)
            .build();
        IrsQueuedRequest request = dueGrantCreateRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_CREATE, grantUuid);
        stubDue(request);
        when(irsChainOpeningGrantRepository.findById(grantUuid)).thenReturn(Optional.of(grant));
        when(irsRequestService.execute(any(IrsQueuedRequest.class))).thenThrow(new IllegalStateException("adapter disabled"));

        worker.processDueRequests();

        assertThat(grant.getSyncStatus()).isEqualTo(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
        verify(irsChainOpeningGrantRepository).save(grant);
    }
}
