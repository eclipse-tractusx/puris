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
 * AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
 * available under CC0-1.0 and not subject to the project's licence.
 * The human contributor has reviewed and verified that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Claude Code Opus 5.5
 */
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsRequestQueueServiceTest {

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Mock
    private IrsQueuedRequestRepository irsQueuedRequestRepository;

    @Mock
    private IrsJobRepository irsJobRepository;

    @InjectMocks
    private IrsRequestQueueService irsRequestQueueService;

    @BeforeEach
    void setUp() {
        lenient().when(irsRequestService.isEnabled()).thenReturn(true);
        lenient().when(irsAdapterConfiguration.getQueueMaxAttempts()).thenReturn(5);
        lenient().when(irsQueuedRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void enqueue_WhenDisabled_ReturnsNullWithoutPersisting() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, "{}",
                null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, UUID.randomUUID());

        assertThat(result).isNull();
        verify(irsQueuedRequestRepository, never()).save(any());
    }

    @Test
    void enqueue_WhenEnabled_PersistsPendingRequest() {
        UUID jobUuid = UUID.randomUUID();

        IrsQueuedRequest result = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH,
                "{\"a\":1}", null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, jobUuid);

        assertThat(result).isNotNull();
        assertThat(result.getMethod()).isEqualTo(IrsQueuedRequestMethodEnumeration.POST);
        assertThat(result.getPath()).isEqualTo(IrsAdapterConfiguration.POLICIES_PATH);
        assertThat(result.getBody()).isEqualTo("{\"a\":1}");
        assertThat(result.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.PENDING);
        assertThat(result.getAttemptCount()).isZero();
        assertThat(result.getMaxAttempts()).isEqualTo(5);
        assertThat(result.getNextAttemptAt()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getType()).isEqualTo(IrsQueuedRequestTypeEnumeration.POLICY_CREATE);
        assertThat(result.getLinkedEntityUuid()).isEqualTo(jobUuid);
    }

    @Test
    void enqueue_WithQueryParams_SerializesToJson() {
        UUID jobUuid = UUID.randomUUID();

        IrsQueuedRequest result = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.DELETE, IrsAdapterConfiguration.POLICIES_PATH,
                null, Map.of("openingId", "abc"), IrsQueuedRequestTypeEnumeration.POLICY_CREATE, jobUuid);

        assertThat(result.getQueryParams()).contains("openingId").contains("abc");
        assertThat(result.getType()).isEqualTo(IrsQueuedRequestTypeEnumeration.POLICY_CREATE);
        assertThat(result.getLinkedEntityUuid()).isEqualTo(jobUuid);
    }

    @Test
    void enqueue_CancelsExistingPendingRequestForSameJob() {
        UUID jobUuid = UUID.randomUUID();
        IrsQueuedRequest existingPending = new IrsQueuedRequest();
        existingPending.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);

        when(irsQueuedRequestRepository.findAllByTypeAndLinkedEntityUuidAndStatus(
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, jobUuid, IrsQueuedRequestStatusEnumeration.PENDING))
                .thenReturn(List.of(existingPending));

        irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, "{}", null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, jobUuid);

        assertThat(existingPending.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.CANCELLED);
        ArgumentCaptor<List<IrsQueuedRequest>> captor = ArgumentCaptor.captor();
        verify(irsQueuedRequestRepository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(existingPending);
    }

    @Test
    void enqueue_CancelsExistingPendingRequestForSameGrant() {
        UUID grantUuid = UUID.randomUUID();
        IrsQueuedRequest existingPending = new IrsQueuedRequest();
        existingPending.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);

        when(irsQueuedRequestRepository.findAllByTypeAndLinkedEntityUuidAndStatus(
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, grantUuid, IrsQueuedRequestStatusEnumeration.PENDING))
                .thenReturn(List.of(existingPending));

        irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, "{}", null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, grantUuid);

        assertThat(existingPending.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.CANCELLED);
        verify(irsQueuedRequestRepository, times(1)).saveAll(List.of(existingPending));
    }

    @Test
    void enqueue_WithDelay_SetsNextAttemptAtInTheFuture() {
        UUID jobUuid = UUID.randomUUID();

        IrsQueuedRequest result = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.GET, "irs/recursive/jobs/" + jobUuid, null, null,
                IrsQueuedRequestTypeEnumeration.JOB_GET, jobUuid, Duration.ofSeconds(300));

        assertThat(result).isNotNull();
        assertThat(result.getNextAttemptAt()).isAfter(Instant.now().plusSeconds(295));
    }

    @Test
    void enqueue_WithZeroDelay_BehavesLikeNoDelayOverload() {
        UUID jobUuid = UUID.randomUUID();

        IrsQueuedRequest result = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.GET, "irs/recursive/jobs/" + jobUuid, null, null,
                IrsQueuedRequestTypeEnumeration.JOB_GET, jobUuid, Duration.ZERO);

        assertThat(result).isNotNull();
        assertThat(result.getNextAttemptAt()).isBeforeOrEqualTo(Instant.now());
    }

    // --- retry ---

    private IrsQueuedRequest failedRequest(IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid) {
        return IrsQueuedRequest.builder()
            .uuid(UUID.randomUUID())
            .status(IrsQueuedRequestStatusEnumeration.FAILED)
            .attemptCount(5)
            .maxAttempts(5)
            .nextAttemptAt(Instant.parse("2026-01-01T00:00:00Z"))
            .type(type)
            .linkedEntityUuid(linkedEntityUuid)
            .build();
    }

    @Test
    void retry_WhenFailed_ResetsAttemptsStatusAndNextAttemptAt() {
        IrsQueuedRequest request = failedRequest(IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null);
        when(irsQueuedRequestRepository.findById(request.getUuid())).thenReturn(Optional.of(request));
        Instant before = Instant.now();

        IrsQueuedRequest result = irsRequestQueueService.retry(request.getUuid());

        assertThat(result.getAttemptCount()).isZero();
        assertThat(result.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.PENDING);
        assertThat(result.getNextAttemptAt()).isBetween(before, Instant.now());
        verify(irsQueuedRequestRepository).save(request);
        verify(irsJobRepository, never()).save(any());
    }

    @Test
    void retry_WhenJobCreateFailed_SetsLinkedJobBackToPending() {
        IrsJob job = new IrsJob();
        job.setUuid(UUID.randomUUID());
        job.setRequestStatus(IrsQueuedRequestStatusEnumeration.FAILED);
        IrsQueuedRequest request = failedRequest(IrsQueuedRequestTypeEnumeration.JOB_CREATE, job.getUuid());
        when(irsQueuedRequestRepository.findById(request.getUuid())).thenReturn(Optional.of(request));
        when(irsQueuedRequestRepository.findAllByTypeAndLinkedEntityUuidAndStatus(IrsQueuedRequestTypeEnumeration.JOB_CREATE,
            job.getUuid(), IrsQueuedRequestStatusEnumeration.PENDING)).thenReturn(List.of());
        when(irsJobRepository.findById(job.getUuid())).thenReturn(Optional.of(job));

        irsRequestQueueService.retry(request.getUuid());

        assertThat(job.getRequestStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.PENDING);
        verify(irsJobRepository).save(job);
    }

    @Test
    void retry_WhenNotFound_Throws() {
        UUID uuid = UUID.randomUUID();
        when(irsQueuedRequestRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> irsRequestQueueService.retry(uuid));
    }

    @Test
    void retry_WhenNotFailed_ThrowsWithoutSaving() {
        IrsQueuedRequest request = failedRequest(IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null);
        request.setStatus(IrsQueuedRequestStatusEnumeration.SUCCEEDED);
        when(irsQueuedRequestRepository.findById(request.getUuid())).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> irsRequestQueueService.retry(request.getUuid()));

        verify(irsQueuedRequestRepository, never()).save(any());
    }

    @Test
    void retry_WhenNewerRequestForSameEntityPending_ThrowsWithoutSaving() {
        UUID grantUuid = UUID.randomUUID();
        IrsQueuedRequest request = failedRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE, grantUuid);
        IrsQueuedRequest newer = failedRequest(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE, grantUuid);
        newer.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        when(irsQueuedRequestRepository.findById(request.getUuid())).thenReturn(Optional.of(request));
        when(irsQueuedRequestRepository.findAllByTypeAndLinkedEntityUuidAndStatus(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE,
            grantUuid, IrsQueuedRequestStatusEnumeration.PENDING)).thenReturn(List.of(newer));

        assertThrows(KeyAlreadyExistsException.class, () -> irsRequestQueueService.retry(request.getUuid()));

        verify(irsQueuedRequestRepository, never()).save(any());
    }
}
