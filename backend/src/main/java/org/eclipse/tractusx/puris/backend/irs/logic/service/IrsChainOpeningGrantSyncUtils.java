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

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.DemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;

/**
 * Stateless helpers shared by {@link IrsChainOpeningRootGrantService} and
 * {@link IrsChainOpeningPartnerGrantService}, extracted so both grant flavors reuse exactly the same
 * notification-activity, material-relation-validity and allowed-BPNL-eligibility logic.
 */
final class IrsChainOpeningGrantSyncUtils {

	private IrsChainOpeningGrantSyncUtils() {
	}

	/**
	 * Determines whether the notification is active at the given point in time. A resolved
	 * notification is always considered inactive. A {@code null} expectedEndDateOfEffect is
	 * treated as open-ended.
	 */
	static boolean isNotificationActiveNow(DemandAndCapacityNotification notification, Date now) {
		if (notification.getStatus() == StatusEnumeration.RESOLVED) {
			return false;
		}
		Date start = notification.getStartDateOfEffect();
		Date end = notification.getExpectedEndDateOfEffect();
		boolean started = start != null && !start.after(now);
		boolean notEnded = end == null || !end.before(now);
		return started && notEnded;
	}

	/**
	 * Determines whether the given material relation is valid at the given point in time.
	 * A {@code null} validFrom or validTo bound is treated as open-ended.
	 */
	static boolean isRelationValidNow(MaterialRelation relation, Date now) {
		Date validFrom = relation.getValidFrom();
		Date validTo = relation.getValidTo();
		boolean startedOrOpen = validFrom == null || !validFrom.after(now);
		boolean notEndedOrOpen = validTo == null || !validTo.before(now);
		return startedOrOpen && notEndedOrOpen;
	}

	/**
	 * Determines whether the requested grant validity range lies within the notification's effect
	 * window. The requested bounds must be present. A {@code null} expectedEndDateOfEffect is treated
	 * as open-ended.
	 */
	static boolean isWithinNotificationBounds(OwnDemandAndCapacityNotification notification,
			Instant validFrom, Instant validUntil) {
		if (validFrom == null || validUntil == null) {
			return false;
		}
		Date start = notification.getStartDateOfEffect();
		if (start == null || validFrom.isBefore(start.toInstant())) {
			return false;
		}
		Date end = notification.getExpectedEndDateOfEffect();
		return end == null || !validUntil.isAfter(end.toInstant());
	}

	static boolean affectsMaterialWithCx(OwnDemandAndCapacityNotification notification, String globalAssetId) {
		if (notification.getMaterials() == null || globalAssetId == null) {
			return false;
		}
		return notification.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getMaterialNumberCx)
			.anyMatch(globalAssetId::equals);
	}

	/**
	 * Resolves the set of currently-valid child own-material-numbers of the given parent
	 * own-material-number.
	 */
	static Set<String> resolveChildOwnMaterialNumbers(MaterialRelationService materialRelationService,
			String parentOwnMaterialNumber, Date now) {
		return materialRelationService.findAllChildren(parentOwnMaterialNumber).stream()
			.filter(relation -> isRelationValidNow(relation, now))
			.map(MaterialRelation::getChildOwnMaterialNumber)
			.collect(Collectors.toSet());
	}

	/**
	 * Adds the notification to the grant's reportedNotifications if no notification with the same
	 * uuid is already present (entities have no overridden equals/hashCode, so membership is
	 * checked explicitly by uuid rather than relying on Set semantics).
	 *
	 * @return {@code true} if the notification was added, {@code false} if it was already present
	 */
	static boolean addNotificationIfAbsent(IrsChainOpeningGrant grant, ReportedDemandAndCapacityNotification notification) {
		boolean alreadyPresent = grant.getReportedNotifications().stream()
			.anyMatch(existing -> existing.getUuid().equals(notification.getUuid()));
		if (alreadyPresent) {
			return false;
		}
		return grant.getReportedNotifications().add(notification);
	}

	/**
	 * Removes the notification (matched by uuid) from the grant's reportedNotifications.
	 *
	 * @return {@code true} if the notification was present and removed
	 */
	static boolean removeNotificationIfPresent(IrsChainOpeningGrant grant, ReportedDemandAndCapacityNotification notification) {
		return grant.getReportedNotifications().removeIf(existing -> existing.getUuid().equals(notification.getUuid()));
	}

	/**
	 * Reconciles the grant's reportedNotifications to exactly match {@code desired} (uuid-based),
	 * adding missing entries and removing stale ones.
	 *
	 * @return {@code true} if the grant's reportedNotifications changed as a result
	 */
	static boolean reconcile(IrsChainOpeningGrant grant, Set<ReportedDemandAndCapacityNotification> desired) {
		Set<UUID> desiredUuids = desired.stream()
			.map(ReportedDemandAndCapacityNotification::getUuid)
			.collect(Collectors.toSet());

		boolean changed = grant.getReportedNotifications().removeIf(existing -> !desiredUuids.contains(existing.getUuid()));
		for (ReportedDemandAndCapacityNotification notification : desired) {
			if (addNotificationIfAbsent(grant, notification)) {
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Ensures that, for every given allowed BPNL, the given related reported notifications contain
	 * a valid one (i.e. one that is currently active) from that BPNL, covering at least one of the
	 * given child material numbers. An empty or {@code null} allowedBpnls is trivially eligible.
	 *
	 * @throws IllegalArgumentException if any allowed BPNL lacks a matching related reported notification
	 */
	static void assertAllowedBpnlsEligible(Set<String> allowedBpnls,
			List<ReportedDemandAndCapacityNotification> relatedReportedNotifications,
			Set<String> childMaterialNumbers, Date now) {
		if (allowedBpnls == null || allowedBpnls.isEmpty()) {
			return;
		}

		for (String allowedBpnl : allowedBpnls) {
			boolean hasMatchingNotification = relatedReportedNotifications.stream()
				.filter(notification -> notification.getPartner() != null
					&& Objects.equals(notification.getPartner().getBpnl(), allowedBpnl))
				.filter(notification -> isNotificationActiveNow(notification, now))
				.filter(notification -> notification.getMaterials() != null)
				.flatMap(notification -> notification.getMaterials().stream())
				.filter(Objects::nonNull)
				.map(Material::getOwnMaterialNumber)
				.anyMatch(childMaterialNumbers::contains);

			if (!hasMatchingNotification) {
				throw new IllegalArgumentException(
					"Each allowed BPNL of a chain opening grant requires a valid related reported notification covering "
						+ "a child material of the grant's material.");
			}
		}
	}
}
