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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.OwnDemandAndCapacityNotificationRepository;
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
 * {@code globalAssetId} is a material directly affected by the notification behind that approval
 * (no parent-walk, unlike {@link IrsChainOpeningRootGrantService}). Its {@code reportedNotifications}
 * are populated by walking the {@code OwnDataExchangeRequest.relatedDataExchangeRequest} &rarr;
 * {@code ReportedDataExchangeApproval} chain: further-upstream partners who have approved their own
 * piece of the same disruption, for a child material of ours.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IrsChainOpeningPartnerGrantService {

	private final IrsRequestService irsRequestService;

	private final IrsChainOpeningGrantGateway gateway;

	/*
	 * Uses the repository directly (not OwnDemandAndCapacityNotificationService) to avoid a
	 * circular bean dependency: OwnDemandAndCapacityNotificationService depends on this class to
	 * sync grants after every update.
	 */
	private final OwnDemandAndCapacityNotificationRepository ownNotificationRepository;

	private final ReportedDataExchangeRequestRepository reportedDataExchangeRequestRepository;

	private final OwnDataExchangeRequestRepository ownDataExchangeRequestRepository;

	private final OwnDataExchangeApprovalService ownDataExchangeApprovalService;

	private final ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;

	private final MaterialService materialService;

	private final MaterialRelationService materialRelationService;

	private final IrsChainOpeningPartnerGrantRepository irsChainOpeningPartnerGrantRepository;

	/**
	 * Enqueues a Chain Opening Grant creation request, to be sent and retried asynchronously by
	 * {@link IrsRequestQueueWorker}.
	 *
	 * @param grant the Chain Opening Grant to create
	 * @return the queued request, or {@code null} if the IRS adapter is disabled
	 * @throws IllegalArgumentException if the grant is not eligible to be created
	 */
	public IrsQueuedRequest createGrant(IrsChainOpeningPartnerGrant grant) {
		if (!irsRequestService.isEnabled()) {
			log.info("IRS adapter is disabled. Skipping creation of chain opening grant for sourceDisruptionId {}",
				grant.getSourceDisruptionId());
			return null;
		}

		assertGrantEligible(grant);

		IrsQueuedRequest queuedRequest = gateway.createOrUpdate(grant, IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_CREATE);
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

		IrsQueuedRequest queuedRequest = gateway.delete(grant, IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_GRANT_DELETE);
		log.info("Enqueued chain opening grant deletion request for sourceDisruptionId {}", grant.getSourceDisruptionId());

		return queuedRequest;
	}

	/**
	 * Creates or updates a Chain Opening Grant for the partner, for each material affected by the
	 * notification behind the given approval. Invoked once we've successfully sent this approval to
	 * the partner.
	 *
	 * @param approval the own approval that was just sent to the partner
	 */
	public void createGrantsForApproval(OwnDataExchangeApproval approval) {
		ReportedDataExchangeRequest triggeringRequest = approval.getDataExchangeRequest();
		OwnDemandAndCapacityNotification notification = triggeringRequest.getNotification();
		if (notification.getMaterials() == null) {
			return;
		}
		for (Material material : notification.getMaterials()) {
			if (material == null || material.getMaterialNumberCx() == null) {
				continue;
			}
			syncGrant(notification, triggeringRequest, material);
		}
	}

	/**
	 * Reacts to a {@link ReportedDataExchangeApproval} received for a forwarded (non-root) request,
	 * i.e. one whose {@code relatedDataExchangeRequest} is set: if we have already sent our own
	 * approval for that triggering request, re-syncs the grants derived from it so they pick up the
	 * newly-approved notification.
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
	 * Reacts to an update of a reported notification, keeping the chain opening grants that (via the
	 * relatedDataExchangeRequest chain) depend on it in sync.
	 *
	 * @param previous the notification's state before the update
	 * @param updated  the notification's state after the update
	 */
	public void onReportedNotificationUpdated(ReportedDemandAndCapacityNotification updated) {
		OwnDataExchangeRequest forwardedRequest = ownDataExchangeRequestRepository.findByNotification_Uuid(updated.getUuid()).orElse(null);
		if (forwardedRequest == null) {
			return;
		}
		ReportedDataExchangeRequest triggeringRequest = forwardedRequest.getRelatedDataExchangeRequest();
		if (triggeringRequest == null) {
			return;
		}
		OwnDataExchangeApproval sentApproval = ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggeringRequest.getUuid());
		if (sentApproval == null) {
			return;
		}
		createGrantsForApproval(sentApproval);
	}

	/**
	 * Reacts to an update of one of our own notifications, creating/tearing down the grants keyed
	 * off its affected materials as they change.
	 *
	 * @param previous the notification's state before the update
	 * @param updated  the notification's state after the update
	 */
	public void onOwnNotificationUpdated(OwnDemandAndCapacityNotification previous, OwnDemandAndCapacityNotification updated) {
		Set<String> previousMaterialCxIds = affectedMaterialCxIds(previous);
		Set<String> currentMaterialCxIds = updated.getStatus() == StatusEnumeration.RESOLVED
			? Set.of() : affectedMaterialCxIds(updated);

		String requesterBpn = updated.getPartner().getBpnl();
		String sourceDisruptionId = updated.getSourceDisruptionId().toString();

		for (String removedCx : previousMaterialCxIds) {
			if (currentMaterialCxIds.contains(removedCx)) {
				continue;
			}
			irsChainOpeningPartnerGrantRepository.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(requesterBpn, removedCx, sourceDisruptionId)
				.ifPresent(this::tearDownGrant);
		}

		if (currentMaterialCxIds.isEmpty()) {
			return;
		}
		ReportedDataExchangeRequest triggeringRequest = reportedDataExchangeRequestRepository.findByNotification_Uuid(updated.getUuid()).orElse(null);
		if (triggeringRequest == null) {
			return;
		}
		OwnDataExchangeApproval sentApproval = ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(triggeringRequest.getUuid());
		if (sentApproval == null) {
			// No approval sent yet for this notification - nothing to create prematurely.
			return;
		}
		for (String cx : currentMaterialCxIds) {
			Material material = materialService.findByMaterialNumberCx(cx);
			if (material == null) {
				continue;
			}
			syncGrant(updated, triggeringRequest, material);
		}
	}

	private static Set<String> affectedMaterialCxIds(OwnDemandAndCapacityNotification notification) {
		if (notification.getMaterials() == null) {
			return Set.of();
		}
		return notification.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getMaterialNumberCx)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	/**
	 * Core reconciliation: recomputes the correct reportedNotifications set for the grant keyed by
	 * (ownNotification's partner BPNL, material, ownNotification's sourceDisruptionId) from current
	 * live state, and applies the diff - a full recompute rather than an incremental add, since
	 * partner grants have many-to-one fan-in from multiple {@link OwnDataExchangeRequest}s and only
	 * a recompute-and-diff can also correctly shrink the set.
	 */
	private void syncGrant(OwnDemandAndCapacityNotification ownNotification, ReportedDataExchangeRequest triggeringRequest, Material material) {
		String requesterBpn = ownNotification.getPartner().getBpnl();
		String globalAssetId = material.getMaterialNumberCx();
		String sourceDisruptionId = ownNotification.getSourceDisruptionId().toString();
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
				.validFrom(ownNotification.getStartDateOfEffect().toInstant())
				.validUntil(ownNotification.getExpectedEndDateOfEffect() == null
					? null : ownNotification.getExpectedEndDateOfEffect().toInstant())
				.syncStatus(IrsGrantSyncStatusEnumeration.NOT_SYNCED)
				.build();
		}

		Set<String> childMaterialNumbers = IrsChainOpeningGrantSyncUtils.resolveChildOwnMaterialNumbers(
			materialRelationService, material.getOwnMaterialNumber(), now);
		Set<ReportedDemandAndCapacityNotification> candidateNotifications =
			resolveCandidateNotifications(triggeringRequest, childMaterialNumbers, now);

		boolean changed = IrsChainOpeningGrantSyncUtils.reconcile(grant, candidateNotifications);

		if (grant.getReportedNotifications().isEmpty() && !isNew) {
			tearDownGrant(grant);
			return;
		}

		if (!isNew && changed && grant.getSyncStatus() == IrsGrantSyncStatusEnumeration.SYNCED) {
			grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		IrsChainOpeningPartnerGrant saved = irsChainOpeningPartnerGrantRepository.save(grant);

		try {
			IrsQueuedRequest queuedRequest = createGrant(saved);
			if (queuedRequest != null) {
				saved.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
			}
		} catch (IllegalArgumentException e) {
			log.error("Failed to enqueue chain opening grant sync for requesterBpn {}, globalAssetId {}, sourceDisruptionId {}",
				requesterBpn, globalAssetId, sourceDisruptionId, e);
			saved.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		irsChainOpeningPartnerGrantRepository.save(saved);
	}

	/**
	 * Empties the grant's reportedNotifications and enqueues its deletion at the IRS (the sync
	 * status is set to {@code DELETED} asynchronously by {@link IrsRequestQueueWorker} once that
	 * succeeds).
	 */
	private void tearDownGrant(IrsChainOpeningPartnerGrant grant) {
		grant.getReportedNotifications().clear();
		IrsQueuedRequest queuedRequest = deleteGrant(grant);
		if (queuedRequest != null) {
			grant.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
		}
		irsChainOpeningPartnerGrantRepository.save(grant);
	}

	/**
	 * Resolves the currently-live set of reported notifications reachable from the triggering
	 * request via the relatedDataExchangeRequest &rarr; ReportedDataExchangeApproval chain, active,
	 * and covering one of the given child material numbers. Reused both to populate a grant's
	 * notification set ({@link #syncGrant}) and to re-verify it at push time ({@link #assertGrantEligible}).
	 */
	private Set<ReportedDemandAndCapacityNotification> resolveCandidateNotifications(
			ReportedDataExchangeRequest triggeringRequest, Set<String> childMaterialNumbers, Date now) {
		return ownDataExchangeRequestRepository.findAllByRelatedDataExchangeRequest_Uuid(triggeringRequest.getUuid()).stream()
			.map(forwardedRequest -> reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(forwardedRequest.getUuid()))
			.filter(Objects::nonNull)
			.map(approval -> approval.getDataExchangeRequest().getNotification())
			.filter(notification -> IrsChainOpeningGrantSyncUtils.isNotificationActiveNow(notification, now))
			.filter(notification -> notification.getMaterials() != null && notification.getMaterials().stream()
				.filter(Objects::nonNull)
				.map(Material::getOwnMaterialNumber)
				.anyMatch(childMaterialNumbers::contains))
			.collect(Collectors.toSet());
	}

	/**
	 * Ensures that a chain opening grant is allowed to be created: there must be an active own
	 * notification matching the grant's sourceDisruptionId and requesterBpn, within the grant's
	 * validity window and affecting the grant's material; that notification must be backed by an
	 * incoming request; and for every BPNL in the grant's allowedBpnls, there must be a valid
	 * related reported notification (via the relatedDataExchangeRequest chain) covering a child
	 * material of the grant's material.
	 *
	 * @throws IllegalArgumentException if the grant does not satisfy the applicable conditions
	 */
	private void assertGrantEligible(IrsChainOpeningPartnerGrant grant) {
		Date now = new Date();
		UUID sourceDisruptionId = UUID.fromString(grant.getSourceDisruptionId());

		OwnDemandAndCapacityNotification matchingNotification = ownNotificationRepository
			.findBySourceDisruptionIdAndPartnerBpnl(sourceDisruptionId, grant.getRequesterBpn()).stream()
			.filter(notification -> IrsChainOpeningGrantSyncUtils.isNotificationActiveNow(notification, now))
			.filter(notification -> IrsChainOpeningGrantSyncUtils.isWithinNotificationBounds(notification, grant.getValidFrom(), grant.getValidUntil()))
			.filter(notification -> IrsChainOpeningGrantSyncUtils.affectsMaterialWithCx(notification, grant.getGlobalAssetId()))
			.findFirst()
			.orElse(null);

		if (matchingNotification == null) {
			log.error("No active own notification found matching grant for sourceDisruptionId {}, requesterBpn {} and globalAssetId {}",
				grant.getSourceDisruptionId(), grant.getRequesterBpn(), grant.getGlobalAssetId());
			throw new IllegalArgumentException(
				"A chain opening grant requires an active own notification with matching sourceDisruptionId, "
					+ "partnerBpnl, validity bounds and affected material.");
		}

		ReportedDataExchangeRequest triggeringRequest = reportedDataExchangeRequestRepository
			.findByNotification_Uuid(matchingNotification.getUuid())
			.orElse(null);
		if (triggeringRequest == null) {
			log.error("No incoming request found for notification {} while checking grant eligibility", matchingNotification.getUuid());
			throw new IllegalArgumentException(
				"A chain opening grant requires an incoming data exchange request backing its matching own notification.");
		}

		Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
		if (material == null) {
			log.error("No material found for globalAssetId {} while checking allowed BPNL eligibility", grant.getGlobalAssetId());
			throw new IllegalArgumentException("A chain opening grant requires the globalAssetId to reference a known material.");
		}
		Set<String> childMaterialNumbers = IrsChainOpeningGrantSyncUtils.resolveChildOwnMaterialNumbers(
			materialRelationService, material.getOwnMaterialNumber(), now);

		List<ReportedDemandAndCapacityNotification> relatedReportedNotifications =
			resolveCandidateNotifications(triggeringRequest, childMaterialNumbers, now).stream().toList();

		IrsChainOpeningGrantSyncUtils.assertAllowedBpnlsEligible(grant.getAllowedBpnls(), relatedReportedNotifications, childMaterialNumbers, now);
	}
}
