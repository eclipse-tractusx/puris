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
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningGrant;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

/**
 * Stateless helpers shared by {@link IrsChainOpeningRootGrantService} and
 * {@link IrsChainOpeningPartnerGrantService}, extracted so both grant flavors reuse exactly the same
 * request-activity, material and allowed-BPNL-eligibility logic.
 */
final class IrsChainOpeningGrantSyncUtils {

	private IrsChainOpeningGrantSyncUtils() {
	}

	/**
	 * Ensures that, for every given allowed BPNL, the given backed by one of the given related 
	 * requests to that partner that is still active and covers at least one of the given child material numbers.
	 * An empty or {@code null} allowedBpnls is trivially eligible.
	 *
	 * @throws IllegalArgumentException if any allowed BPNL lacks a matching related request
	 */
	static void assertAllowedBpnlsEligible(Set<String> allowedBpnls,
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

		if (!backedByRequest.containsAll(allowedBpnls)) {
			throw new IllegalArgumentException(
				"Each allowed BPNL of a chain opening grant requires a valid related data exchange request covering "
					+ "a child material of the grant's material.");
		}
	}

	/**
	 * Determines whether a data exchange request can back a grant at the given point in time
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
	 * Reconciles the grant's dataExchangeRequests to exactly match {@code desired} (uuid-based),
	 * adding missing entries and removing stale ones.
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
}
