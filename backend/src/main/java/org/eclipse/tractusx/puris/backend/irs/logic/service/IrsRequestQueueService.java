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
import java.util.Map;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enqueues outbound IRS HTTP calls for asynchronous dispatch and retry by
 * {@link IrsRequestQueueWorker}, instead of sending them synchronously.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IrsRequestQueueService {

	private final IrsRequestService irsRequestService;

	private final IrsAdapterConfiguration irsAdapterConfiguration;

	private final IrsQueuedRequestRepository irsQueuedRequestRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Enqueues a new IRS request, linked to the entity identified by {@code type} and
	 * {@code linkedEntityUuid}. Any request already {@code PENDING} for that same linked entity is
	 * cancelled first, so at most one live request per entity is ever in flight.
	 *
	 * @param method    the HTTP method ("GET", "POST", "PUT" or "DELETE")
	 * @param path      the IRS-relative path to call
	 * @param jsonBody  the resolved JSON request body, or {@code null} if not applicable
	 * @param queryParams the query parameters to send, or {@code null} if none
	 * @param type      the kind of entity this request originated from, or {@code null}
	 * @param linkedEntityUuid the UUID of the entity this request originated from, or {@code null}
	 * @return the persisted queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest enqueue(String method, String path, String jsonBody, Map<String, String> queryParams,
			IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping enqueue of {} {}", method, path);
			return null;
		}

		cancelPending(type, linkedEntityUuid);

		Instant now = Instant.now();
		IrsQueuedRequest request = IrsQueuedRequest.builder()
			.method(method)
			.path(path)
			.body(jsonBody)
			.queryParams(serializeQueryParams(queryParams))
			.status(IrsQueuedRequestStatusEnumeration.PENDING)
			.attemptCount(0)
			.maxAttempts(irsAdapterConfiguration.getQueueMaxAttempts())
			.nextAttemptAt(now)
			.createdAt(now)
			.type(type)
			.linkedEntityUuid(linkedEntityUuid)
			.build();

		return irsQueuedRequestRepository.save(request);
	}

	private void cancelPending(IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid) {
		if (linkedEntityUuid == null) {
			return;
		}

		List<IrsQueuedRequest> pending = irsQueuedRequestRepository
			.findAllByTypeAndLinkedEntityUuidAndStatus(type, linkedEntityUuid, IrsQueuedRequestStatusEnumeration.PENDING);

		if (pending.isEmpty()) {
			return;
		}

		pending.forEach(request -> request.setStatus(IrsQueuedRequestStatusEnumeration.CANCELLED));
		irsQueuedRequestRepository.saveAll(pending);
	}

	private String serializeQueryParams(Map<String, String> queryParams) {
		if (queryParams == null || queryParams.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(queryParams);
		} catch (Exception e) {
			log.error("Failed to serialize query params {}", queryParams, e);
			return null;
		}
	}
}
