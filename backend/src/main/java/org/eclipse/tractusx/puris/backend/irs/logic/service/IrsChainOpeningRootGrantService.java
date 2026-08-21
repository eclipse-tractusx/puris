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
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.repository.ReportedDemandAndCapacityNotificationRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.DemandAndCapacityNotificationService;
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

	private final ReportedDemandAndCapacityNotificationRepository reportedNotificationRepository;

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
	 * Ensures that, for each material affected by the given reported notification, a Chain
	 * Opening Root Grant exists (created or updated) covering each currently-valid parent material
	 * of that affected material, with the notification added to the grant's reportedNotifications.
	 * The grant's requesterBpn is our own company BPNL.
	 * <p>
	 * Also attempts to push each created/updated grant to the IRS, updating its syncStatus based
	 * on the outcome. Failures (ineligibility, IRS/network errors) are logged and reflected in
	 * syncStatus, but never propagate, since this method is invoked as a side effect of saving a
	 * {@link ReportedDemandAndCapacityNotification} and must not fail that save.
	 *
	 * @param notification the reported notification that was just created or updated
	 */
	public void syncGrantsForNotification(ReportedDemandAndCapacityNotification notification) {
		if (notification.getMaterials() == null) {
			return;
		}

		String requesterBpn = variablesService.getOwnBpnl();
		String sourceDisruptionId = notification.getSourceDisruptionId().toString();

		Set<String> parentOwnMaterialNumbers = resolveAffectedParentOwnMaterialNumbers(notification, new Date());

		for (String parentOwnMaterialNumber : parentOwnMaterialNumbers) {
			Material parentMaterial = materialService.findByOwnMaterialNumber(parentOwnMaterialNumber);
			if (parentMaterial == null || parentMaterial.getMaterialNumberCx() == null) {
				continue;
			}
			addNotificationToGrant(requesterBpn, parentMaterial.getMaterialNumberCx(), sourceDisruptionId, notification);
		}
	}

	/**
	 * Resolves the set of currently-valid parent own-material-numbers for the materials affected
	 * by the given notification, i.e. the globalAssetId-bearing materials whose chain opening
	 * grants the notification should contribute to.
	 */
	private Set<String> resolveAffectedParentOwnMaterialNumbers(ReportedDemandAndCapacityNotification notification, Date now) {
		if (notification.getMaterials() == null) {
			return Set.of();
		}
		return notification.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getOwnMaterialNumber)
			.flatMap(childOwnMaterialNumber -> materialRelationService.findAllParents(childOwnMaterialNumber).stream())
			.filter(relation -> MaterialRelationService.isRelationValidNow(relation, now))
			.map(MaterialRelation::getParentOwnMaterialNumber)
			.collect(Collectors.toSet());
	}

	/**
	 * Adds the given notification to the locally persisted grant identified by
	 * (requesterBpn, globalAssetId, sourceDisruptionId), creating it if it 
	 * does not exist, and attempts to push it to the IRS. 
	 * The grant's validity window is widened to cover the notification, and its syncStatus is updated based on the outcome of the push attempt.
	 * 
	 * @param requesterBpn the BPNL of the grant's requester (our own company)
	 * @param globalAssetId the globalAssetId of the grant's material
	 * @param sourceDisruptionId the sourceDisruptionId of the grant's reported notification
	 * @param notification the reported notification to add to the grant
	 * @throws IllegalArgumentException if the grant is not eligible to be created or updated
	 */
	private void addNotificationToGrant(String requesterBpn, String globalAssetId, String sourceDisruptionId,
			ReportedDemandAndCapacityNotification notification) {
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

		boolean changed = IrsChainOpeningGrantSyncUtils.addNotificationIfAbsent(grant, notification);

		Instant notificationStart = notification.getStartDateOfEffect().toInstant();
		Instant notificationEnd = notification.getExpectedEndDateOfEffect() != null
			? notification.getExpectedEndDateOfEffect().toInstant() : null;

		Instant newValidFrom = grant.getValidFrom() == null || notificationStart.isBefore(grant.getValidFrom())
			? notificationStart : grant.getValidFrom();
		Instant newValidUntil = grant.getValidUntil() == null || notificationEnd == null
			? null
			: (notificationEnd.isAfter(grant.getValidUntil()) ? notificationEnd : grant.getValidUntil());

		if (!Objects.equals(grant.getValidFrom(), newValidFrom) || !Objects.equals(grant.getValidUntil(), newValidUntil)) {
			changed = true;
		}
		grant.setValidFrom(newValidFrom);
		grant.setValidUntil(newValidUntil);

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
			log.error("Failed to enqueue chain opening root grant sync for requesterBpn {}, globalAssetId {}, sourceDisruptionId {}",
				requesterBpn, globalAssetId, sourceDisruptionId, e);
			saved.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		irsChainOpeningRootGrantRepository.save(saved);
	}

	/**
	 * Reacts to an update of a reported notification (e.g. resolution, or a change of affected
	 * materials), keeping the chain opening root grants derived from it in sync.
	 * <p>
	 * If the notification was resolved, it is removed from every grant it currently contributes
	 * to. Otherwise, it is removed from the grants of parent materials it no longer affects, and
	 * {@link #syncGrantsForNotification} is invoked to create or update grants for its
	 * (still and newly) affected parent materials.
	 *
	 * @param previous the notification's state before the update
	 * @param updated  the notification's state after the update
	 */
	public void onReportedNotificationUpdated(ReportedDemandAndCapacityNotification previous, ReportedDemandAndCapacityNotification updated) {
		if (updated.getStatus() == StatusEnumeration.RESOLVED) {
			removeNotificationFromAllGrants(updated);
			return;
		}

		Date now = new Date();
		Set<String> previousParents = resolveAffectedParentOwnMaterialNumbers(previous, now);
		Set<String> currentParents = resolveAffectedParentOwnMaterialNumbers(updated, now);

		String requesterBpn = variablesService.getOwnBpnl();
		String sourceDisruptionId = updated.getSourceDisruptionId().toString();

		for (String removedParentOwnMaterialNumber : previousParents) {
			if (currentParents.contains(removedParentOwnMaterialNumber)) {
				continue;
			}
			Material parentMaterial = materialService.findByOwnMaterialNumber(removedParentOwnMaterialNumber);
			if (parentMaterial == null || parentMaterial.getMaterialNumberCx() == null) {
				continue;
			}
			irsChainOpeningRootGrantRepository
				.findByRequesterBpnAndGlobalAssetIdAndSourceDisruptionId(requesterBpn, parentMaterial.getMaterialNumberCx(), sourceDisruptionId)
				.ifPresent(grant -> removeNotificationFromGrant(grant, updated));
		}

		syncGrantsForNotification(updated);
	}

	/**
	 * Removes the notification from every chain opening root grant it currently contributes to.
	 */
	private void removeNotificationFromAllGrants(ReportedDemandAndCapacityNotification notification) {
		irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notification.getUuid())
			.forEach(grant -> removeNotificationFromGrant(grant, notification));
	}

	/**
	 * Removes the notification (matched by uuid) from the grant's reportedNotifications. If this
	 * empties the grant, a deletion request is enqueued for the IRS (the sync status is set to
	 * {@code DELETED} asynchronously by {@link IrsRequestQueueWorker} once that succeeds).
	 * Otherwise, since the grant's allowedBpnls just shrank, the grant is re-pushed to the IRS.
	 */
	private void removeNotificationFromGrant(IrsChainOpeningRootGrant grant, ReportedDemandAndCapacityNotification notification) {
		boolean removed = IrsChainOpeningGrantSyncUtils.removeNotificationIfPresent(grant, notification);
		if (!removed) {
			return;
		}

		if (grant.getReportedNotifications().isEmpty()) {
			IrsQueuedRequest queuedRequest = deleteGrant(grant);
			if (queuedRequest != null) {
				grant.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
			}
			irsChainOpeningRootGrantRepository.save(grant);
			return;
		}

		if (grant.getSyncStatus() == IrsGrantSyncStatusEnumeration.SYNCED) {
			grant.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}
		IrsChainOpeningRootGrant saved = irsChainOpeningRootGrantRepository.save(grant);

		try {
			IrsQueuedRequest queuedRequest = createOrUpdateGrant(saved);
			if (queuedRequest != null) {
				saved.setSyncStatus(IrsGrantSyncStatusEnumeration.PENDING);
			}
		} catch (IllegalArgumentException e) {
			log.error("Failed to enqueue chain opening root grant re-sync after notification removal for requesterBpn {}, "
				+ "globalAssetId {}, sourceDisruptionId {}", saved.getRequesterBpn(), saved.getGlobalAssetId(),
				saved.getSourceDisruptionId(), e);
			saved.setSyncStatus(IrsGrantSyncStatusEnumeration.OUT_OF_SYNC);
		}

		irsChainOpeningRootGrantRepository.save(saved);
	}

	/**
	 * Ensures that a chain opening root grant is allowed to be created: there must be at least one
	 * active reported notification with a matching sourceDisruptionId that affects a child material
	 * of the grant's globalAssetId (see {@link #assertSelfRequestedGrantEligible}), and for every
	 * BPNL in the grant's allowedBpnls, there must be a valid reported notification from that BPNL,
	 * covering at least one child material of the material identified by the grant's globalAssetId.
	 *
	 * @throws IllegalArgumentException if the grant does not satisfy the applicable conditions
	 */
	private void assertGrantEligible(IrsChainOpeningRootGrant grant) {
		Date now = new Date();

		List<ReportedDemandAndCapacityNotification> relatedReportedNotifications = assertSelfRequestedGrantEligible(grant, now);

		Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
		if (material == null) {
			log.error("No material found for globalAssetId {} while checking allowed BPNL eligibility", grant.getGlobalAssetId());
			throw new IllegalArgumentException("A chain opening grant requires the globalAssetId to reference a known material.");
		}
		Set<String> childMaterialNumbers = MaterialRelationService.resolveChildOwnMaterialNumbers(
			materialRelationService, material.getOwnMaterialNumber(), now);

		IrsChainOpeningGrantSyncUtils.assertAllowedBpnlsEligible(grant.getAllowedBpnls(), relatedReportedNotifications, childMaterialNumbers, now);
	}

	/**
	 * Ensures that a root grant is eligible: the globalAssetId must resolve to a known material,
	 * and there must be at least one active reported notification with a matching sourceDisruptionId
	 * affecting a currently-valid child material of it.
	 *
	 * @return the list of active reported notifications establishing eligibility
	 * @throws IllegalArgumentException if the globalAssetId does not resolve to a known material,
	 *                                   or no such reported notification exists
	 */
	private List<ReportedDemandAndCapacityNotification> assertSelfRequestedGrantEligible(IrsChainOpeningRootGrant grant, Date now) {
		Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
		if (material == null) {
			log.error("No material found for globalAssetId {} while checking self-requested grant eligibility", grant.getGlobalAssetId());
			throw new IllegalArgumentException("A chain opening grant requires the globalAssetId to reference a known material.");
		}

		Set<String> childMaterialNumbers = MaterialRelationService.resolveChildOwnMaterialNumbers(
			materialRelationService, material.getOwnMaterialNumber(), now);

		UUID sourceDisruptionId = UUID.fromString(grant.getSourceDisruptionId());
		List<ReportedDemandAndCapacityNotification> relatedReportedNotifications = reportedNotificationRepository
			.findAllBySourceDisruptionId(sourceDisruptionId).stream()
			.filter(notification -> DemandAndCapacityNotificationService.isNotificationActiveNow(notification, now))
			.filter(notification -> notification.getMaterials() != null && notification.getMaterials().stream()
				.filter(Objects::nonNull)
				.map(Material::getOwnMaterialNumber)
				.anyMatch(childMaterialNumbers::contains))
			.toList();

		if (relatedReportedNotifications.isEmpty()) {
			log.error("No active reported notification found matching sourceDisruptionId {} and covering a child material of {}",
				grant.getSourceDisruptionId(), grant.getGlobalAssetId());
			throw new IllegalArgumentException(
				"A self-requested chain opening grant requires an active reported notification with matching "
					+ "sourceDisruptionId, covering a child material of the grant's material.");
		}

		return relatedReportedNotifications;
	}
}
