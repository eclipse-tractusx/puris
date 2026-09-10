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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJobStateEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningPartnerGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsQueuedRequestRepository;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService.IrsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	private static final String JOB_STATUS_PATH_PREFIX = "irs/recursive/jobs/";

	@Autowired
	private IrsQueuedRequestRepository irsQueuedRequestRepository;

	@Autowired
	private IrsRequestQueueService irsRequestQueueService;

	@Autowired
	private IrsChainOpeningPartnerGrantRepository irsChainOpeningPartnerGrantRepository;

	@Autowired
	private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

	@Autowired
	private IrsJobRepository irsJobRepository;

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
			request.setLastErrorMessage(sanitizeForStorage(e.getMessage()));
		}

		boolean successful = response != null && response.isSuccessful();

		if (successful) {
			request.setStatus(IrsQueuedRequestStatusEnumeration.SUCCEEDED);
			try {
				updateLinkedEntity(request, response);
			} catch (Exception e) {
				log.error("Failed to update linked entity for queued request {}: {}", request.getUuid(), e.getMessage());
				request.setStatus(IrsQueuedRequestStatusEnumeration.FAILED);
				request.setLastErrorMessage(sanitizeForStorage(e.getMessage()));
			}
		} else {
			if (response != null) {
				request.setLastErrorMessage(sanitizeForStorage("HTTP " + response.getStatusCode() + ": " + response.getResponseBody()));
			}
			if (request.getAttemptCount() >= request.getMaxAttempts()) {
				request.setStatus(IrsQueuedRequestStatusEnumeration.FAILED);
				try {
					updateLinkedEntity(request, response);
				} catch (Exception e) {
					log.error("Failed to update linked entity for queued request {}: {}", request.getUuid(), e.getMessage());
					request.setLastErrorMessage(sanitizeForStorage(e.getMessage()));
				}
			} else {
				request.setNextAttemptAt(Instant.now().plusSeconds(computeBackoffSeconds(request.getAttemptCount())));
			}
		}

		irsQueuedRequestRepository.save(request);
	}

	private IrsResponse dispatch(IrsQueuedRequest request) {
		try {
			return irsRequestService.execute(request);
		} catch(IllegalArgumentException e) {
			log.error("Error dispatching queued request {}: {}", request.getUuid(), e.getMessage());
			throw new IllegalStateException("Unsupported queued request method: " + request.getMethod());
		}
	}

	private void updateLinkedEntity(IrsQueuedRequest request, IrsResponse response) {
		boolean successful = response != null && response.isSuccessful();
		if (request.getLinkedEntityUuid() == null) {
			return;
		}

		switch (request.getType()) {
			case CHAIN_OPENING_ROOT_GRANT_CREATE, CHAIN_OPENING_ROOT_GRANT_UPDATE -> 
				irsChainOpeningRootGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
						if (successful) {
							grant.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
						} else if (grant.getSyncStatus() != IrsGrantSyncStatusEnumeration.NOT_SYNCED) {
							grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
						}
						// else: leave as NOT_SYNCED - this create never reached IRS, so the next attempt should still POST
						irsChainOpeningRootGrantRepository.save(grant);
					},
					() -> log.warn("Linked chain opening root grant {} for queued request {} no longer exists", request.getLinkedEntityUuid(), request.getUuid())
				);
			case CHAIN_OPENING_ROOT_GRANT_DELETE -> irsChainOpeningRootGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
				grant.setSyncStatus(successful ? IrsGrantSyncStatusEnumeration.DELETED : IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
				irsChainOpeningRootGrantRepository.save(grant);
			}, () -> log.warn("Linked chain opening root grant {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case CHAIN_OPENING_PARTNER_GRANT_CREATE, CHAIN_OPENING_PARTNER_GRANT_UPDATE -> 
				irsChainOpeningPartnerGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
						if (successful) {
							grant.setSyncStatus(IrsGrantSyncStatusEnumeration.SYNCED);
						} else if (grant.getSyncStatus() != IrsGrantSyncStatusEnumeration.NOT_SYNCED) {
							grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
						}
						// else: leave as NOT_SYNCED - this create never reached IRS, so the next attempt should still POST
						irsChainOpeningPartnerGrantRepository.save(grant);
					},
					() -> log.warn("Linked chain opening grant {} for queued request {} no longer exists", request.getLinkedEntityUuid(), request.getUuid())
				);
			case CHAIN_OPENING_PARTNER_GRANT_DELETE -> irsChainOpeningPartnerGrantRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(grant -> {
				grant.setSyncStatus(successful ? IrsGrantSyncStatusEnumeration.DELETED : IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
				irsChainOpeningPartnerGrantRepository.save(grant);
			}, () -> log.warn("Linked chain opening grant {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case JOB_CREATE -> irsJobRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(irsJob -> {
				irsJob.setRequestStatus(successful ? IrsQueuedRequestStatusEnumeration.SUCCEEDED : IrsQueuedRequestStatusEnumeration.FAILED);
				if (successful) {
					irsJob.setJobId(extractJobId(response.getResponseBody(), request));
				}
				irsJobRepository.save(irsJob);
				if (successful) {
					enqueueJobStatusPoll(irsJob);
				}
			}, () -> log.warn("Linked IRS job {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case JOB_GET -> irsJobRepository.findById(request.getLinkedEntityUuid()).ifPresentOrElse(irsJob -> {
				if (!successful) {
					log.warn("IRS job status GET failed for job {} (queued request {})", irsJob.getUuid(), request.getUuid());
					return;
				}
				IrsJobStateEnumeration state = extractJobState(response.getResponseBody(), request);
				irsJob.setState(state);
				irsJobRepository.save(irsJob);
				log.info("IRS job {} status is {}", irsJob.getUuid(), state);
				if (!state.isTerminal()) {
					enqueueJobStatusPoll(irsJob);
				}
			}, () -> log.warn("Linked IRS job {} for queued request {} no longer exists",
				request.getLinkedEntityUuid(), request.getUuid()));
			case POLICY_CREATE ->
				log.warn("Policy requests should have no Linked entity. No update is required. Please check why the policy request {} has a linked entity.", request.getUuid());
		}
	}

	private void enqueueJobStatusPoll(IrsJob irsJob) {
		irsRequestQueueService.enqueue(IrsQueuedRequestMethodEnumeration.GET, JOB_STATUS_PATH_PREFIX + irsJob.getJobId(), null, null,
			IrsQueuedRequestTypeEnumeration.JOB_GET, irsJob.getUuid(), Duration.ofSeconds(irsAdapterConfiguration.getJobPollDelaySeconds()));
	}

	/**
	 * Extracts the {@code jobId} property from the stringified JSON IRS response body.
	 *
	 * @return the parsed job id
	 * @throws IllegalStateException if the body is missing, unparseable, or lacks a valid
	 *         {@code jobId}
	 */
	private UUID extractJobId(String responseBody, IrsQueuedRequest request) {
		if (responseBody == null || responseBody.isBlank()) {
			throw new IllegalStateException("IRS response for queued request " + request.getUuid() + " has no body to extract a jobId from");
		}
		try {
			var node = objectMapper.readTree(responseBody).get("jobId");
			if (node == null || node.isNull()) {
				throw new IllegalStateException("IRS response for queued request " + request.getUuid() + " is missing a jobId property");
			}
			return UUID.fromString(node.asText());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to extract jobId from IRS response for queued request " + request.getUuid(), e);
		}
	}

	/**
	 * Extracts the {@code job.state} property from the stringified JSON IRS response body.
	 *
	 * @return the parsed job state
	 * @throws IllegalStateException if the body is missing, unparseable, or lacks a valid
	 *         {@code job.state}
	 */
	private IrsJobStateEnumeration extractJobState(String responseBody, IrsQueuedRequest request) {
		if (responseBody == null || responseBody.isBlank()) {
			throw new IllegalStateException("IRS response for queued request " + request.getUuid() + " has no body to extract a job state from");
		}
		try {
			var node = objectMapper.readTree(responseBody).path("job").get("state");
			if (node == null || node.isNull()) {
				throw new IllegalStateException("IRS response for queued request " + request.getUuid() + " is missing a job.state property");
			}
			return IrsJobStateEnumeration.valueOf(node.asText());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to extract job state from IRS response for queued request " + request.getUuid(), e);
		}
	}

	private long computeBackoffSeconds(int attemptCount) {
		double delay = irsAdapterConfiguration.getQueueInitialRetryDelaySeconds()
			* Math.pow(irsAdapterConfiguration.getQueueBackoffMultiplier(), attemptCount - 1);
		return Math.min((long) delay, irsAdapterConfiguration.getQueueMaxRetryDelaySeconds());
	}

	/**
	 * Replaces runs of vertical whitespace (the characters
	 * {@code PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING} excludes) with a single space
	 * and trims the result, so the value satisfies that pattern before being stored in
	 * {@link IrsQueuedRequest#getLastErrorMessage()}. Messages sourced from exceptions or raw IRS
	 * response bodies can otherwise contain newlines, which would fail that constraint at save
	 * time and leave the request's attempt never recorded.
	 *
	 * @return the sanitized, non-blank message, or {@code null} if {@code raw} is {@code null} or
	 *         blank after sanitization
	 */
	private String sanitizeForStorage(String raw) {
		if (raw == null) {
			return null;
		}
		String sanitized = raw.replaceAll("[\\n\\x0B\\f\\r\\x85\\u2028\\u2029]+", " ").trim();
		return sanitized.isEmpty() ? null : sanitized;
	}
}
