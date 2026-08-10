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
import java.util.concurrent.ExecutorService;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService.IrsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Continuously processes the {@link IrsQueuedRequest} queue: dispatches due requests to the IRS,
 * retries failures a bounded number of times with exponential backoff, and updates the linked
 * {@link IrsJob}/{@link IrsChainOpeningGrant} once a request reaches a terminal outcome.
 */
@Service
@Slf4j
public class IrsRequestQueueWorker {

	@Autowired
	private IrsQueuedRequestRepository irsQueuedRequestRepository;

	@Autowired
	private IrsChainOpeningGrantRepository irsChainOpeningGrantRepository;

	@Autowired
	private IrsRequestService irsRequestService;

	@Autowired
	private IrsAdapterConfiguration irsAdapterConfiguration;

	@Autowired
	private ExecutorService executorService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	public void start() {
		executorService.submit(this::runLoop);
	}

	private void runLoop() {
		log.info("IRS request queue worker started");
		while (true) {
			try {
				processDueRequests();
			} catch (Exception e) {
				log.error("Unexpected error while processing IRS request queue", e);
			}
			try {
				Thread.sleep(irsAdapterConfiguration.getQueuePollIntervalSeconds() * 1000L);
			} catch (InterruptedException ignore) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Processes every currently due, pending queued request. Public so it can be exercised
	 * directly in tests without running the infinite daemon loop.
	 */
	public void processDueRequests() {
		List<IrsQueuedRequest> due = irsQueuedRequestRepository
			.findAllByStatusAndNextAttemptAtBefore(IrsQueuedRequestStatusEnumeration.PENDING, Instant.now());
		for (IrsQueuedRequest request : due) {
			processOne(request);
		}
	}

	private void processOne(IrsQueuedRequest request) {
		request.setAttemptCount(request.getAttemptCount() + 1);
		request.setLastAttemptAt(Instant.now());

		IrsResponse response;
		try {
			response = dispatch(request);
		} catch (Exception e) {
			response = null;
			request.setLastErrorMessage(e.getMessage());
		}

		boolean successful = response != null && response.isSuccessful();

		if (successful) {
			request.setStatus(IrsQueuedRequestStatusEnumeration.SUCCEEDED);
			updateLinkedEntity(request, true);
		} else {
			if (response != null) {
				request.setLastErrorMessage("HTTP " + response.getStatusCode() + ": " + response.getResponseBody());
			}
			if (request.getAttemptCount() >= request.getMaxAttempts()) {
				request.setStatus(IrsQueuedRequestStatusEnumeration.FAILED);
				updateLinkedEntity(request, false);
			} else {
				request.setNextAttemptAt(Instant.now().plusSeconds(computeBackoffSeconds(request.getAttemptCount())));
			}
		}

		irsQueuedRequestRepository.save(request);
	}

	private IrsResponse dispatch(IrsQueuedRequest request) {
		Map<String, String> queryParams = deserializeQueryParams(request.getQueryParams());
		try {
			return irsRequestService.execute(request.getMethod(), request.getPath(), queryParams, request.getBody());
		} catch(IllegalArgumentException e) {
			log.error("Error dispatching queued request {}: {}", request.getUuid(), e.getMessage());
			throw new IllegalStateException("Unsupported queued request method: " + request.getMethod());
		}
	}

	private void updateLinkedEntity(IrsQueuedRequest request, boolean successful) {
		if (request.getLinkedEntityUuid() == null) {
			return;
		}

		switch (request.getType()) {
			case CHAIN_OPENING_GRANT_CREATE -> irsChainOpeningGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
				grant.setSyncStatus(successful ? IrsGrantSyncStatusEnumeration.SYNCED : IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
				irsChainOpeningGrantRepository.save(grant);
			}, () -> log.warn("Linked chain opening grant {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case CHAIN_OPENING_GRANT_DELETE -> irsChainOpeningGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
				grant.setSyncStatus(successful ? IrsGrantSyncStatusEnumeration.DELETED : IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
				irsChainOpeningGrantRepository.save(grant);
			}, () -> log.warn("Linked chain opening grant {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case POLICY_CREATE -> 
				log.warn("Policy requests should have no Linked entity. No update is required. Please check why the policy request {} has a linked entity.", request.getUuid());
		}
	}

	private long computeBackoffSeconds(int attemptCount) {
		double delay = irsAdapterConfiguration.getQueueInitialRetryDelaySeconds()
			* Math.pow(irsAdapterConfiguration.getQueueBackoffMultiplier(), attemptCount - 1);
		return Math.min((long) delay, irsAdapterConfiguration.getQueueMaxRetryDelaySeconds());
	}

	private Map<String, String> deserializeQueryParams(String queryParams) {
		if (queryParams == null || queryParams.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(queryParams, new TypeReference<Map<String, String>>() {
			});
		} catch (Exception e) {
			log.error("Failed to deserialize query params {}", queryParams, e);
			return null;
		}
	}
}
