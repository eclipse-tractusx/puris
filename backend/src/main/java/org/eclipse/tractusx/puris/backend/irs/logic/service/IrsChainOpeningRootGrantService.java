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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates, updates and deletes Chain Opening Root Grants (grants requesting recursive access to a
 * material's chain for ourselves), both locally (persisted via
 * {@link IrsChainOpeningRootGrantRepository}) and at the IRS.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IrsChainOpeningRootGrantService {

	private final IrsRequestService irsRequestService;

	private final IrsChainOpeningGrantGateway gateway;

	private final MaterialService materialService;

	private final MaterialRelationService materialRelationService;

	private final VariablesService variablesService;

	private final IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;

	/**
	 * Enqueues a Chain Opening Root Grant creation or update request, 
	 * to be sent and retried asynchronously
	 * by {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Root Grant to create
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 * @throws IllegalArgumentException if the grant is not eligible to be created
	 */
	public IrsQueuedRequest createOrUpdateGrant(IrsChainOpeningRootGrant grant) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping creation of chain opening root grant for sourceDisruptionId {}",
				grant.getSourceDisruptionId());
			return null;
		}

		assertGrantEligible(grant);

		IrsQueuedRequest queuedRequest = gateway.createOrUpdate(grant, true);
		log.info("Enqueued chain opening root grant creation request for sourceDisruptionId {}", grant.getSourceDisruptionId());

		return queuedRequest;
	}

	/**
	 * Enqueues a Chain Opening Root Grant deletion request, to be sent and retried asynchronously
	 * by {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Root Grant to delete
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest deleteGrant(IrsChainOpeningRootGrant grant) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping deletion of chain opening root grant for sourceDisruptionId {}",
				grant.getSourceDisruptionId());
			return null;
		}

		IrsQueuedRequest queuedRequest = gateway.delete(grant, IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_DELETE);
		log.info("Enqueued chain opening root grant deletion request for sourceDisruptionId {}", grant.getSourceDisruptionId());

		return queuedRequest;
	}

	/**
	 * Ensures that a chain opening root grant is allowed to be created: its dataExchangeRequests
	 * must contain at least one request that is still active and affects a child material of the 
	 * of the grant's globalAssetId (see {@link #assertSelfRequestedGrantEligible}), and for every
	 * BPNL in the grant's allowedBpnls, one of them must be a request to that partner.
	 *
	 * @throws IllegalArgumentException if the grant does not satisfy the applicable conditions
	 */
	private void assertGrantEligible(IrsChainOpeningRootGrant grant) {
		Date now = new Date();

		Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
		if (material == null) {
			log.error("No material found for globalAssetId {} while checking allowed BPNL eligibility", grant.getGlobalAssetId());
			throw new IllegalArgumentException("A chain opening grant requires the globalAssetId to reference a known material.");
		}
		Set<String> childMaterialNumbers = materialRelationService.resolveChildOwnMaterialNumbers(material.getOwnMaterialNumber(), now);

		List<OwnDataExchangeRequest> relatedRequests = resolveRelatedRequests(grant, childMaterialNumbers, now);
		if (relatedRequests.isEmpty()) {
			log.error("No active data exchange request found backing sourceDisruptionId {} and covering a child material of {}", grant.getSourceDisruptionId(), grant.getGlobalAssetId());
			throw new IllegalArgumentException(
				"A self-requested chain opening grant requires an approved, active data exchange request covering "
					+ "a child material of the grant's material.");
		}

		IrsChainOpeningGrantSyncUtils.assertAllowedBpnlsEligible(grant.getAllowedBpnls(), relatedRequests, childMaterialNumbers, now);
	}

	/**
	 * Once an own root request was approved by its partner: for each currently-valid parent material of the
	 * request's materials, a grant is created or updated with the request added to its dataExchangeRequests. The
	 * grant's requesterBpn is our own company BPNL.
	 * <p>
	 * Also attempts to push each created/updated grant to the IRS, updating its syncStatus based on the outcome.
	 * Failures (ineligibility, IRS/network errors) are logged and reflected in syncStatus, but never propagate.
	 *
	 * @param request the own root request that was just approved
	 */
	public void syncGrantsForRequest(OwnDataExchangeRequest request) {
		if (request.getMaterials() == null) {
			return;
		}

		String requesterBpn = variablesService.getOwnBpnl();
		String sourceDisruptionId = request.getSourceDisruptionId().toString();
		Date now = new Date();

		Set<String> parentOwnMaterialNumbers = request.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getOwnMaterialNumber)
			.flatMap(childOwnMaterialNumber -> materialRelationService.findAllParents(childOwnMaterialNumber).stream())
			.filter(relation -> MaterialRelationService.isRelationValidNow(relation, now))
			.map(MaterialRelation::getParentOwnMaterialNumber)
			.collect(Collectors.toSet());

		for (String parentOwnMaterialNumber : parentOwnMaterialNumbers) {
			Material parentMaterial = materialService.findByOwnMaterialNumber(parentOwnMaterialNumber);
			if (parentMaterial == null || parentMaterial.getMaterialNumberCx() == null) {
				continue;
			}
			addRequestToGrant(requesterBpn, parentMaterial.getMaterialNumberCx(), sourceDisruptionId, request);
		}
	}

	/**
	 * Adds the request to the grant's dataExchangeRequests, creating the grant if it does not exist, widens the
	 * grant's validity window to cover the request's desired window and attempts to push the grant to the IRS.
	 */
	private void addRequestToGrant(String requesterBpn, String globalAssetId, String sourceDisruptionId, OwnDataExchangeRequest request) {
		IrsChainOpeningRootGrant grant = irsChainOpeningRootGrantRepository
			.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(requesterBpn, globalAssetId, sourceDisruptionId)
			.orElse(null);

		boolean isNew = grant == null;
		if (isNew) {
			grant = IrsChainOpeningRootGrant.builder()
				.requesterBpn(requesterBpn)
				.globalAssetId(globalAssetId)
				.sourceDisruptionId(sourceDisruptionId)
				.useCase(IrsAdapterConfiguration.PURIS_USE_CASE)
				.syncStatus(IrsGrantSyncStatusEnumeration.NOT_SYNCED)
				.build();
		}

		boolean changed = IrsChainOpeningGrantSyncUtils.addRequestIfAbsent(grant, request);

		Instant requestStart = request.getDesiredStartDateTime().toInstant();
		Instant requestEnd = request.getDesiredEndDateTime().toInstant();
		Instant newValidFrom = grant.getValidFrom() == null || requestStart.isBefore(grant.getValidFrom()) ? requestStart : grant.getValidFrom();
		Instant newValidTo = grant.getValidTo() == null || requestEnd.isAfter(grant.getValidTo()) ? requestEnd : grant.getValidTo();

		if (!Objects.equals(grant.getValidFrom(), newValidFrom) || !Objects.equals(grant.getValidTo(), newValidTo)) {
			changed = true;
		}
		grant.setValidFrom(newValidFrom);
		grant.setValidTo(newValidTo);

		if (!isNew && changed && grant.getSyncStatus() == IrsGrantSyncStatusEnumeration.SYNCED) {
			grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		IrsChainOpeningRootGrant saved = irsChainOpeningRootGrantRepository.save(grant);

		try {
			IrsQueuedRequest queuedRequest = createOrUpdateGrant(saved);
			if (queuedRequest != null) {
				saved.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
			}
		} catch (IllegalArgumentException e) {
			log.error("Failed to enqueue chain opening root grant sync for requesterBpn {}, globalAssetId {}, sourceDisruptionId {}", requesterBpn, globalAssetId, sourceDisruptionId, e);
			saved.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		irsChainOpeningRootGrantRepository.save(saved);
	}

	private List<OwnDataExchangeRequest> resolveRelatedRequests(IrsChainOpeningRootGrant grant, Set<String> childMaterialNumbers, Date now) {
		return grant.getDataExchangeRequests().stream()
			.filter(request -> IrsChainOpeningGrantSyncUtils.isRequestActiveNow(request, now))
			.filter(request -> IrsChainOpeningGrantSyncUtils.affectsAnyMaterial(request, childMaterialNumbers))
			.toList();
	}
}
