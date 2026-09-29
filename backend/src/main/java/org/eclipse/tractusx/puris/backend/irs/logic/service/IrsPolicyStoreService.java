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
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers policies at the IRS Policy Store so that the IRS accepts contracts matching them
 * during EDC negotiation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IrsPolicyStoreService {

    private final IrsRequestService irsRequestService;

    private final IrsRequestBodybuilder irsRequestBodybuilder;

    private final IrsRequestQueueService irsRequestQueueService;

    /**
     * Enqueues creation of the PURIS framework policy at the IRS, to be sent and retried
     * asynchronously by {@link IrsRequestQueueWorker}.
     * @return the queued request, or {@code null} if the IRS adapter is disabled
     * @see IrsRequestQueueWorker
     */
    public IrsQueuedRequest createPurisFrameworkPolicy() {
        if (!irsRequestService.isEnabled()) {
            log.info("IRS adapter is disabled. Skipping creation of framework policy.");
            return null;
        }

        String payload = irsRequestBodybuilder.buildPurisFrameworkPolicyCreationRequestBody().toString();
        return createFrameworkPolicy(payload);
    }

    /**
     * Enqueues creation of the DTR framework policy at the IRS, to be sent and retried
     * asynchronously by {@link IrsRequestQueueWorker}.
     * @return the queued request, or {@code null} if the IRS adapter is disabled
     * @see IrsRequestQueueWorker
     */
    public IrsQueuedRequest createDtrFrameworkPolicy() {
        if (!irsRequestService.isEnabled()) {
            log.info("IRS adapter is disabled. Skipping creation of framework policy.");
            return null;
        }

        String payload = irsRequestBodybuilder.buildDtrFrameworkPolicyCreationRequestBody().toString();
        return createFrameworkPolicy(payload);
    }

    /**
     * Enqueues creation of the given framework policy request body at the IRS.
     *
     * @return the queued request
     */
    private IrsQueuedRequest createFrameworkPolicy(String payload) {
        IrsQueuedRequest queuedRequest = irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.POST, IrsAdapterConfiguration.POLICIES_PATH, payload, null,
            IrsQueuedRequestTypeEnumeration.POLICY_CREATE, null);
        log.info("Enqueued framework policy creation request");

        return queuedRequest;
    }
}
