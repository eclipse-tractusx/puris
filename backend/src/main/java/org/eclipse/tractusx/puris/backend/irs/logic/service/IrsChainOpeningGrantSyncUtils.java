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

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.DemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

/**
 * Stateless helpers shared by {@link IrsChainOpeningRootGrantService} and
 * {@link IrsChainOpeningPartnerGrantService}, extracted so both grant flavors reuse exactly the same
 * notification-activity, material-relation-validity and allowed-BPNL-eligibility logic.
 */
final class IrsChainOpeningGrantSyncUtils {

	private IrsChainOpeningGrantSyncUtils() {
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
				.filter(notification -> DemandAndCapacityNotificationService.isNotificationActiveNow(notification, now))
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

	/**
	 * Determines whether a data exchange request without notification can back a grant at the given point in
	 * time
	 */
	static boolean isRequestActiveNow(DataExchangeRequest request, Date now) {
		return request.getDesiredEndDateTime() != null && !request.getDesiredEndDateTime().before(now);
	}

	/**
	 * Determines whether one of the request's materials has the given materialNumberCx.
	 */
	static boolean affectsMaterialWithMaterialNumberCx(DataExchangeRequest request, String materialNumberCx) {
		return request.getMaterials() != null && materialNumberCx != null && request.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getMaterialNumberCx)
			.anyMatch(materialNumberCx::equals);
	}

	/**
	 * Determines whether one of the request's materials has one of the given own material numbers.
	 */
	static boolean affectsAnyMaterial(DataExchangeRequest request, Set<String> ownMaterialNumbers) {
		return request.getMaterials() != null && request.getMaterials().stream()
			.filter(Objects::nonNull)
			.map(Material::getOwnMaterialNumber)
			.anyMatch(ownMaterialNumbers::contains);
	}

	/**
	 * Adds the request to the grant's dataExchangeRequests if no request with the same uuid is already present,
	 * the counterpart of {@link #addNotificationIfAbsent}.
	 *
	 * @return {@code true} if the request was added, {@code false} if it was already present
	 */
	static boolean addRequestIfAbsent(IrsChainOpeningGrant grant, OwnDataExchangeRequest request) {
		boolean alreadyPresent = grant.getDataExchangeRequests().stream()
			.anyMatch(existing -> existing.getUuid().equals(request.getUuid()));
		if (alreadyPresent) {
			return false;
		}
		return grant.getDataExchangeRequests().add(request);
	}

	/**
	 * Adding missing entries and removing stale ones, the counterpart of {@link #reconcile}.
	 *
	 * @return {@code true} if the grant's dataExchangeRequests changed as a result
	 */
	static boolean reconcileRequests(IrsChainOpeningGrant grant, List<OwnDataExchangeRequest> desired) {
		Set<UUID> desiredUuids = desired.stream()
			.map(OwnDataExchangeRequest::getUuid)
			.collect(Collectors.toSet());

		boolean changed = grant.getDataExchangeRequests().removeIf(existing -> !desiredUuids.contains(existing.getUuid()));
		for (OwnDataExchangeRequest request : desired) {
			if (addRequestIfAbsent(grant, request)) {
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Same as {@link #assertAllowedBpnlsEligible(Set, List, Set, Date)}, but an allowed BPNL can also be
	 * backed by a related request without notification Only requests their partner approved may be passed.
	 *
	 * @throws IllegalArgumentException if any allowed BPNL lacks a matching related reported notification or request
	 */
	static void assertAllowedBpnlsEligible(Set<String> allowedBpnls,
			List<ReportedDemandAndCapacityNotification> relatedReportedNotifications,
			List<OwnDataExchangeRequest> relatedRequests,
			Set<String> childMaterialNumbers, Date now) {
		if (allowedBpnls == null || allowedBpnls.isEmpty()) {
			return;
		}

		Set<String> backedByRequest = relatedRequests.stream()
			.filter(request -> isRequestActiveNow(request, now))
			.filter(request -> affectsAnyMaterial(request, childMaterialNumbers))
			.map(OwnDataExchangeRequest::getPartner)
			.filter(Objects::nonNull)
			.map(Partner::getBpnl)
			.collect(Collectors.toSet());
		Set<String> remainingBpnls = allowedBpnls.stream()
			.filter(allowedBpnl -> !backedByRequest.contains(allowedBpnl))
			.collect(Collectors.toSet());

		assertAllowedBpnlsEligible(remainingBpnls, relatedReportedNotifications, childMaterialNumbers, now);
	}
}
