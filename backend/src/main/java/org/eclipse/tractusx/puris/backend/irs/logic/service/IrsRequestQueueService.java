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
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
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

	private final IrsJobRepository irsJobRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Enqueues a new IRS request, linked to the entity identified by {@code type} and
	 * {@code linkedEntityUuid}. Any request already {@code PENDING} for that same linked entity is
	 * cancelled first, so at most one live request per entity is ever in flight.
	 *
	 * @param method    the HTTP method
	 * @param path      the IRS-relative path to call
	 * @param jsonBody  the resolved JSON request body, or {@code null} if not applicable
	 * @param queryParams the query parameters to send, or {@code null} if none
	 * @param type      the kind of entity this request originated from, or {@code null}
	 * @param linkedEntityUuid the UUID of the entity this request originated from, or {@code null}
	 * @return the persisted queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest enqueue(IrsQueuedRequestMethodEnumeration method, String path, String jsonBody, Map<String, String> queryParams,
			IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid) {
		return enqueue(method, path, jsonBody, queryParams, type, linkedEntityUuid, Duration.ZERO);
	}

	/**
	 * Enqueues a new IRS request the same way as {@link #enqueue(IrsQueuedRequestMethodEnumeration, String, String, Map, IrsQueuedRequestTypeEnumeration, UUID)},
	 * but only makes it due for dispatch after the given delay has elapsed.
	 *
	 * @param delay how long to wait before the request becomes due
	 */
	public IrsQueuedRequest enqueue(IrsQueuedRequestMethodEnumeration method, String path, String jsonBody, Map<String, String> queryParams,
			IrsQueuedRequestTypeEnumeration type, UUID linkedEntityUuid, Duration delay) {
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
			.nextAttemptAt(now.plus(delay))
			.createdAt(now)
			.type(type)
			.linkedEntityUuid(linkedEntityUuid)
			.build();

		return irsQueuedRequestRepository.save(request);
	}

	public List<IrsQueuedRequest> findAll() {
		return irsQueuedRequestRepository.findAll();
	}

	/**
	 * Retries a failed queued request by resetting its attempt count, setting it back to
	 * {@code PENDING} and making it due immediately, so that {@link IrsRequestQueueWorker} picks it
	 * up on its next poll. If the request creates an IRS job, the linked job's request status is set
	 * back to {@code PENDING} as well.
	 *
	 * @param uuid the UUID of the queued request to retry
	 * @return the updated queued request
	 * @throws NoSuchElementException     if no queued request with the given UUID exists
	 * @throws IllegalStateException      if the request is not {@code FAILED}
	 * @throws KeyAlreadyExistsException  if another request for the same linked entity is already
	 *                                    {@code PENDING}, which would be superseded by this retry
	 */
	public IrsQueuedRequest retry(UUID uuid) {
		IrsQueuedRequest request = irsQueuedRequestRepository.findById(uuid)
			.orElseThrow(() -> new NoSuchElementException("IRS queued request does not exist."));

		if (request.getStatus() != IrsQueuedRequestStatusEnumeration.FAILED) {
			log.error("Could not retry IRS queued request {} because its status is {}", uuid, request.getStatus());
			throw new IllegalStateException("Only failed requests can be retried.");
		}

		if (request.getLinkedEntityUuid() != null && !irsQueuedRequestRepository
				.findAllByTypeAndLinkedEntityUuidAndStatus(request.getType(), request.getLinkedEntityUuid(), IrsQueuedRequestStatusEnumeration.PENDING)
				.isEmpty()) {
			log.error("Could not retry IRS queued request {} because a newer request for entity {} is already pending",
				uuid, request.getLinkedEntityUuid());
			throw new KeyAlreadyExistsException("A newer request for the same entity is already pending.");
		}

		request.setAttemptCount(0);
		request.setStatus(IrsQueuedRequestStatusEnumeration.PENDING);
		request.setNextAttemptAt(Instant.now());
		IrsQueuedRequest saved = irsQueuedRequestRepository.save(request);

		if (request.getType() == IrsQueuedRequestTypeEnumeration.JOB_CREATE && request.getLinkedEntityUuid() != null) {
			irsJobRepository.findById(request.getLinkedEntityUuid()).ifPresent(job -> {
				job.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);
				irsJobRepository.save(job);
			});
		}

		log.info("Retrying IRS queued request {}", uuid);
		return saved;
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
