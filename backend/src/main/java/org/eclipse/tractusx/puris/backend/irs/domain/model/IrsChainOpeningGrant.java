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
package org.eclipse.tractusx.puris.backend.irs.domain.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Common shape of a Chain Opening Grant as it is created at and deleted from the IRS, shared by
 * {@link IrsChainOpeningRootGrant} and {@link IrsChainOpeningPartnerGrant}.
 * <p>
 * A grant allows {@link #requesterBpn} to recursively query the set of {@link #getAllowedBpnls()}
 * for the material identified by {@link #globalAssetId}, for the duration of the
 * [{@link #validFrom}, {@link #validUntil}] window. A grant is uniquely identified by the
 * combination of {@link #requesterBpn}, {@link #globalAssetId} and {@link #sourceDisruptionId}.
 * <p>
 * {@link #getReportedNotifications()} is declared abstract rather than mapped here: a real
 * {@code @ManyToMany} needs its own join table per concrete subclass, since JPA can't express a
 * single shared join table whose "grant" side polymorphically references either of two separate
 * {@code TABLE_PER_CLASS}-mapped tables.
 * <p>
 * The {@code @JsonProperty} annotations reflect the wire shape expected by the IRS
 * grant-creation request, so that this class can be serialized directly instead of being mapped
 * field by field; fields that are not part of that wire shape are marked {@code @JsonIgnore}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class IrsChainOpeningGrant {

	@Id
	@GeneratedValue
	@JsonIgnore
	protected UUID uuid;

	@Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
	protected String globalAssetId;

	@JsonProperty("openingId")
	@Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
	protected String sourceDisruptionId;

	protected String requesterBpn;

	protected Instant validFrom;

	protected Instant validUntil;

	@Builder.Default
	protected String useCase = IrsAdapterConfiguration.PURIS_USE_CASE;

	@Enumerated(EnumType.STRING)
	@JsonIgnore
	protected IrsGrantSyncStatusEnumeration syncStatus;

	public abstract Set<ReportedDemandAndCapacityNotification> getReportedNotifications();

	/**
	 * The BPNLs allowed to be recursively queried under this grant, derived from the partners
	 * of {@link #getReportedNotifications()}. This is the wire shape expected by the IRS
	 * grant-creation request.
	 */
	@JsonProperty("allowedBpnls")
	public Set<String> getAllowedBpnls() {
		return getReportedNotifications().stream()
			.map(ReportedDemandAndCapacityNotification::getPartner)
			.filter(Objects::nonNull)
			.map(Partner::getBpnl)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Whether IRS currently has a copy of this grant, i.e. whether pushing a change should be a
	 * {@code PUT} (update) rather than a {@code POST} (create). {@code NOT_SYNCED} (never attempted,
	 * or a first attempt that never actually reached IRS) and {@code DELETED} (IRS confirmed it's
	 * gone) mean it does not currently exist there; every other status means it does (or very
	 * recently did).
	 */
	public boolean existsAtIrs() {
		return syncStatus != null && syncStatus != IrsGrantSyncStatusEnumeration.NOT_SYNCED && syncStatus != IrsGrantSyncStatusEnumeration.DELETED;
	}
}
