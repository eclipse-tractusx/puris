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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningPartnerGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningPartnerGrantRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates, updates and deletes Chain Opening Grants requesting recursive access to our own
 * materials' chains for a partner, both locally (persisted via
 * {@link IrsChainOpeningPartnerGrantRepository}) and at the IRS.
 * <p>
 * A grant's {@code requesterBpn} is the partner we approved a data exchange request for; its
 * {@code globalAssetId} is a material directly affected by directly affected by that request
 * (no parent-walk, unlike {@link IrsChainOpeningRootGrantService})and its validity window is the request's desired window.
 * {@code dataExchangeRequests} are the requests we forwarded along the bill of material &rarr;
 * ({@code OwnDataExchangeRequest.relatedDataExchangeRequest}) that their partner approved: further-upstream partners
 * who have approved their own piece of the same disruption, for a child material of ours.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IrsChainOpeningPartnerGrantService {

	private final IrsRequestService irsRequestService;

	private final IrsChainOpeningGrantGateway gateway;

	private final ReportedDataExchangeRequestRepository reportedDataExchangeRequestRepository;

	private final OwnDataExchangeRequestRepository ownDataExchangeRequestRepository;

	private final OwnDataExchangeApprovalService ownDataExchangeApprovalService;

	private final ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;

	private final MaterialService materialService;

	private final MaterialRelationService materialRelationService;

	private final IrsChainOpeningPartnerGrantRepository irsChainOpeningPartnerGrantRepository;

	/**
	 * Enqueues a Chain Opening Grant creation or update request,
	 * to be sent and retried asynchronously by
	 * {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Grant to create
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 * @throws IllegalArgumentException if the grant is not eligible to be created
	 */
	public IrsQueuedRequest createOrUpdateGrant(IrsChainOpeningPartnerGrant grant) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping creation of chain opening grant for sourceDisruptionId {}",
				grant.getSourceDisruptionId());
			return null;
		}

		assertGrantEligible(grant);

		IrsQueuedRequest queuedRequest = gateway.createOrUpdate(grant, false);
		log.info("Enqueued chain opening grant creation request for sourceDisruptionId {}", grant.getSourceDisruptionId());

		return queuedRequest;
	}

	/**
	 * Enqueues a Chain Opening Grant deletion request, to be sent and retried asynchronously by
	 * {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Grant to delete
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 */
	public IrsQueuedRequest deleteGrant(IrsChainOpeningPartnerGrant grant) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping deletion of chain opening grant for sourceDisruptionId {}",
				grant.getSourceDisruptionId());
			return null;
		}

		IrsQueuedRequest queuedRequest = gateway.delete(grant, IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_PARTNER_GRANT_DELETE);
		log.info("Enqueued chain opening grant deletion request for sourceDisruptionId {}", grant.getSourceDisruptionId());

		return queuedRequest;
	}

	/**
	 * Creates or updates a Chain Opening Grant for the partner, for each material affected by the
	 * request behind the given approval
	 * Invoked once we've successfully sent this approval to the partner.
	 *
	 * @param approval the own approval that was just sent to the partner
	 */
	public void createGrantsForApproval(OwnDataExchangeApproval approval) {
		ReportedDataExchangeRequest triggeringRequest = approval.getDataExchangeRequest();
		if (triggeringRequest.getMaterials() == null) {
			return;
		}
		for (Material material : triggeringRequest.getMaterials()) {
			if (material == null || material.getMaterialNumberCx() == null) {
				continue;
			}
			syncGrant(triggeringRequest, material);
		}
	}

	/**
	 * Reacts to a {@link ReportedDataExchangeApproval} received for a forwarded (non-root) request,
	 * i.e. one whose {@code relatedDataExchangeRequest} is set: if we have already sent our own
	 * approval for that triggering request, re-syncs the grants derived from it so they pick up the
	 * newly-approved forwarded request.
	 *
	 * @param receivedApproval the reported approval that was just received
	 */
	public void onRelatedApprovalReceived(ReportedDataExchangeApproval receivedApproval) {
		OwnDataExchangeRequest forwardedRequest = receivedApproval.getDataExchangeRequest();
		ReportedDataExchangeRequest triggeringRequest = forwardedRequest.getRelatedDataExchangeRequest();
		if (triggeringRequest == null) {
			return;
		}
		OwnDataExchangeApproval sentApproval = ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggeringRequest.getUuid());
		if (sentApproval == null) {
			// Our own approval for the triggering request hasn't been sent yet - nothing to attach to.
			// createGrantsForApproval will independently discover this same approval via the same
			// chain once we do send it, so nothing is lost.
			return;
		}
		createGrantsForApproval(sentApproval);
	}

	/**
	 * Core reconciliation: recomputes the correct dataExchangeRequests set for the grant keyed by
	 * (triggering request's partner BPNL, material, triggering request's sourceDisruptionId) from current
	 * live state, and applies the diff - a full recompute rather than an incremental add, since
	 * partner grants have many-to-one fan-in from multiple {@link OwnDataExchangeRequest}s and only
	 * a recompute-and-diff can also correctly shrink the set.
	 */
	private void syncGrant(ReportedDataExchangeRequest triggeringRequest, Material material) {
		String requesterBpn = triggeringRequest.getPartner().getBpnl();
		String globalAssetId = material.getMaterialNumberCx();
		String sourceDisruptionId = triggeringRequest.getSourceDisruptionId().toString();
		Date now = new Date();

		IrsChainOpeningPartnerGrant grant = irsChainOpeningPartnerGrantRepository
			.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(requesterBpn, globalAssetId, sourceDisruptionId)
			.orElse(null);

		boolean isNew = grant == null;
		if (isNew) {
			grant = IrsChainOpeningPartnerGrant.builder()
				.requesterBpn(requesterBpn)
				.globalAssetId(globalAssetId)
				.sourceDisruptionId(sourceDisruptionId)
				.useCase(IrsAdapterConfiguration.PURIS_USE_CASE)
				.validFrom(triggeringRequest.getDesiredStartDateTime().toInstant())
				.validTo(triggeringRequest.getDesiredEndDateTime().toInstant())
				.syncStatus(IrsGrantSyncStatusEnumeration.NOT_SYNCED)
				.build();
		}

		Set<String> childMaterialNumbers = materialRelationService.resolveChildOwnMaterialNumbers(material.getOwnMaterialNumber(), now);
		List<OwnDataExchangeRequest> candidateRequests = resolveCandidateRequests(
			resolveTriggeringRequests(requesterBpn, triggeringRequest.getSourceDisruptionId(), globalAssetId, now), childMaterialNumbers, now);

		boolean changed = IrsChainOpeningGrantSyncUtils.reconcileRequests(grant, candidateRequests);

		if (!isNew && changed && grant.getSyncStatus() == IrsGrantSyncStatusEnumeration.SYNCED) {
			grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		IrsChainOpeningPartnerGrant saved = irsChainOpeningPartnerGrantRepository.save(grant);

		try {
			IrsQueuedRequest queuedRequest = createOrUpdateGrant(saved);
			if (queuedRequest != null) {
				saved.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
			}
		} catch (IllegalArgumentException e) {
			log.error("Failed to enqueue chain opening grant sync for requesterBpn {}, globalAssetId {}, sourceDisruptionId {}", requesterBpn, globalAssetId, sourceDisruptionId, e);
			saved.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		irsChainOpeningPartnerGrantRepository.save(saved);
	}

	/**
	 * Ensures that a chain opening grant is allowed to be created: there must be an incoming 
	 * request of the grant's requesterBpn for the grant's sourceDisruptionId that we approved,
	 * whose desired window ended and that affects the grant's material (see {@link #resolveTriggeringRequests});
	 * and for every BPNL in the grant's allowedBpnls, there must be a forwarded request to that partner
	 * that it approved, that is still active and that covers a child material of the grant's material
	 * (see {@link #resolveCandidateRequests}).
	 *
	 * @throws IllegalArgumentException if the grant does not satisfy the applicable conditions
	 */
	private void assertGrantEligible(IrsChainOpeningPartnerGrant grant) {
		Date now = new Date();
		UUID sourceDisruptionId = UUID.fromString(grant.getSourceDisruptionId());

		List<ReportedDataExchangeRequest> triggeringRequests = resolveTriggeringRequests(grant.getRequesterBpn(), sourceDisruptionId, grant.getGlobalAssetId(), now);
		if (triggeringRequests.isEmpty()) {
			log.error("No approved incoming request found matching grant for sourceDisruptionId {}, requesterBpn {} and globalAssetId {}",
				grant.getSourceDisruptionId(), grant.getRequesterBpn(), grant.getGlobalAssetId());
			throw new IllegalArgumentException(
				"A chain opening grant requires an approved, active incoming data exchange request with matching "
					+ "sourceDisruptionId, partnerBpnl and affected material.");
		}

		Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
		if (material == null) {
			log.error("No material found for globalAssetId {} while checking allowed BPNL eligibility", grant.getGlobalAssetId());
			throw new IllegalArgumentException("A chain opening grant requires the globalAssetId to reference a known material.");
		}
		Set<String> childMaterialNumbers = materialRelationService.resolveChildOwnMaterialNumbers(material.getOwnMaterialNumber(), now);

		IrsChainOpeningGrantSyncUtils.assertAllowedBpnlsEligible(grant.getAllowedBpnls(), resolveCandidateRequests(triggeringRequests, childMaterialNumbers, now), childMaterialNumbers, now);
	}

	/**
	 * Resolves the incoming requests the grant keyed by (requesterBpn, globalAssetId, sourceDisruptionId) can be
	 * based on: requests of the requester for that disruption that we approved, whose desired window has not ended
	 * and that affect the material.
	 */
	private List<ReportedDataExchangeRequest> resolveTriggeringRequests(String requesterBpn, UUID sourceDisruptionId,
			String globalAssetId, Date now) {
		return reportedDataExchangeRequestRepository.findAllBySourceDisruptionIdAndPartnerBpnl(sourceDisruptionId, requesterBpn).stream()
			.filter(request -> IrsChainOpeningGrantSyncUtils.isRequestActiveNow(request, now))
			.filter(request -> IrsChainOpeningGrantSyncUtils.affectsMaterialWithMaterialNumberCx(request, globalAssetId))
			.filter(request -> ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(request.getUuid()) != null)
			.toList();
	}

	/**
	 * The requests we forwarded for the given requests (along the bill of material) that their partner
	 * approved, whose desired window has not ended and that cover one of the given child material numbers.
	 */
	private List<OwnDataExchangeRequest> resolveCandidateRequests(List<ReportedDataExchangeRequest> triggeringRequests,
			Set<String> childMaterialNumbers, Date now) {
		return triggeringRequests.stream()
			.flatMap(triggeringRequest -> ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggeringRequest.getUuid()).stream())
			.filter(forwardedRequest -> reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwardedRequest.getUuid()) != null)
			.filter(forwardedRequest -> IrsChainOpeningGrantSyncUtils.isRequestActiveNow(forwardedRequest, now))
			.filter(forwardedRequest -> IrsChainOpeningGrantSyncUtils.affectsAnyMaterial(forwardedRequest, childMaterialNumbers))
			.toList();
	}
}
