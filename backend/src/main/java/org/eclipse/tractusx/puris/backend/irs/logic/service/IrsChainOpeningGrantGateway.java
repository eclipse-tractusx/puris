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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrantLike;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Enqueues Chain Opening Grant creation/deletion requests at the IRS, shared by
 * {@link IrsChainOpeningRootGrantService} and {@link IrsChainOpeningGrantService}.
 * <p>
 * Deliberately does not check {@code IrsRequestService.isEnabled()} or assert grant eligibility
 * itself — both stay the caller's responsibility, and specifically in that order, so that a
 * disabled IRS adapter short-circuits before any eligibility-related repository calls happen.
 */
@Service
@RequiredArgsConstructor
public class IrsChainOpeningGrantGateway {

	private static final String GRANTS_PATH = "irs/recursive/chain-openings/grants";

	private final IrsRequestBodybuilder irsRequestBodybuilder;

	private final IrsRequestQueueService irsRequestQueueService;

	/**
	 * Enqueues a Chain Opening Grant creation request, to be sent and retried asynchronously by
	 * {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Grant to create
	 * @param type  the queued request type identifying which grant flavor/repository the worker
	 *              should update once the request reaches a terminal outcome
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest create(IrsChainOpeningGrantLike grant, IrsQueuedRequestTypeEnumeration type) {
		String payload = irsRequestBodybuilder.buildGrantCreationRequestBody(grant).toString();
		return irsRequestQueueService.enqueue("POST", GRANTS_PATH, payload, null, type, grant.getUuid());
	}

	/**
	 * Enqueues a Chain Opening Grant deletion request, to be sent and retried asynchronously by
	 * {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Grant to delete
	 * @param type  the queued request type identifying which grant flavor/repository the worker
	 *              should update once the request reaches a terminal outcome
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest delete(IrsChainOpeningGrantLike grant, IrsQueuedRequestTypeEnumeration type) {
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("openingId", grant.getSourceDisruptionId());
		queryParams.put("useCase", grant.getUseCase());
		queryParams.put("requesterBpn", grant.getRequesterBpn());
		queryParams.put("globalAssetId", grant.getGlobalAssetId());

		return irsRequestQueueService.enqueue("DELETE", GRANTS_PATH, null, queryParams, type, grant.getUuid());
	}
}
