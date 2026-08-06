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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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

        IrsQueuedRequest result = irsRequestQueueService.enqueue("POST", IrsAdapterConfiguration.POLICIES_PATH, "{}",
                null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, UUID.randomUUID());

        assertThat(result).isNull();
        verify(irsQueuedRequestRepository, never()).save(any());
    }

    @Test
    void enqueue_WhenEnabled_PersistsPendingRequest() {
        UUID jobUuid = UUID.randomUUID();

        IrsQueuedRequest result = irsRequestQueueService.enqueue("POST", IrsAdapterConfiguration.POLICIES_PATH,
                "{\"a\":1}", null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, jobUuid);

        assertThat(result).isNotNull();
        assertThat(result.getMethod()).isEqualTo("POST");
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

        IrsQueuedRequest result = irsRequestQueueService.enqueue("DELETE", IrsAdapterConfiguration.POLICIES_PATH,
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

        irsRequestQueueService.enqueue("POST", IrsAdapterConfiguration.POLICIES_PATH, "{}", null,
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

        irsRequestQueueService.enqueue("POST", IrsAdapterConfiguration.POLICIES_PATH, "{}", null,
                IrsQueuedRequestTypeEnumeration.POLICY_CREATE, grantUuid);

        assertThat(existingPending.getStatus()).isEqualTo(IrsQueuedRequestStatusEnumeration.CANCELLED);
        verify(irsQueuedRequestRepository, times(1)).saveAll(List.of(existingPending));
    }
}
