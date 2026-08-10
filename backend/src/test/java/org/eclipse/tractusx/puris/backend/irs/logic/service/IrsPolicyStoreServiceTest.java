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

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsPolicyStoreServiceTest {

    @Mock
    private IrsRequestService irsRequestService;

    @Mock
    private IrsRequestBodybuilder irsRequestBodybuilder;

    @Mock
    private IrsRequestQueueService irsRequestQueueService;

    @InjectMocks
    private IrsPolicyStoreService irsPolicyStoreService;

    @Test
    void createPurisFrameworkPolicy_WhenDisabled_ReturnsNullWithoutEnqueueing() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = irsPolicyStoreService.createPurisFrameworkPolicy();

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPurisFrameworkPolicy_WhenEnabled_BuildsBodyAndEnqueues() {
        ObjectNode body = new ObjectMapper().createObjectNode().put("@id", "puris-framework-policy");
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsRequestBodybuilder.buildPurisFrameworkPolicyCreationRequestBody()).thenReturn(body);
        when(irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null)).thenReturn(queuedRequest);

        IrsQueuedRequest result = irsPolicyStoreService.createPurisFrameworkPolicy();

        assertThat(result).isEqualTo(queuedRequest);
        verify(irsRequestBodybuilder, times(1)).buildPurisFrameworkPolicyCreationRequestBody();
        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null);
    }

    @Test
    void createDtrFrameworkPolicy_WhenDisabled_ReturnsNullWithoutEnqueueing() {
        when(irsRequestService.isEnabled()).thenReturn(false);

        IrsQueuedRequest result = irsPolicyStoreService.createDtrFrameworkPolicy();

        assertThat(result).isNull();
        verify(irsRequestQueueService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createDtrFrameworkPolicy_WhenEnabled_BuildsBodyAndEnqueues() {
        ObjectNode body = new ObjectMapper().createObjectNode().put("@id", "dtr-framework-policy");
        IrsQueuedRequest queuedRequest = new IrsQueuedRequest();

        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsRequestBodybuilder.buildDtrFrameworkPolicyCreationRequestBody()).thenReturn(body);
        when(irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null)).thenReturn(queuedRequest);

        IrsQueuedRequest result = irsPolicyStoreService.createDtrFrameworkPolicy();

        assertThat(result).isEqualTo(queuedRequest);
        verify(irsRequestBodybuilder, times(1)).buildDtrFrameworkPolicyCreationRequestBody();
        verify(irsRequestQueueService, times(1)).enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, body.toString(), null,
            IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null);
    }
}
