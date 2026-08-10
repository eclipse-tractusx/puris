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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a Chain Opening Grant as it is created at and deleted from the IRS.
 * <p>
 * A grant allows the {@link #requesterBpn} to recursively query the set of
 * {@link #getAllowedBpnls()} for the material identified by {@link #globalAssetId},
 * for the duration of the [{@link #validFrom}, {@link #validUntil}] window. The allowed
 * BPNLs are derived from the partners of {@link #reportedNotifications}, the set of
 * reported notifications currently backing this grant.
 * <p>
 * A grant is uniquely identified by the combination of {@link #requesterBpn},
 * {@link #globalAssetId} and {@link #sourceDisruptionId}. The {@code @JsonProperty}
 * annotations reflect the wire shape expected by the IRS grant-creation request, so
 * that this class can be serialized directly instead of being mapped field by field;
 * fields that are not part of that wire shape are marked {@code @JsonIgnore}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "chain_opening_grant", uniqueConstraints = @UniqueConstraint(
	name = "uc_chain_opening_grant_key",
	columnNames = { "requester_bpn", "global_asset_id", "source_disruption_id" }
))
public class IrsChainOpeningGrant {

	@Id
	@GeneratedValue
	@JsonIgnore
	private UUID uuid;

	private String globalAssetId;

	@JsonProperty("openingId")
	private String sourceDisruptionId;

	private String requesterBpn;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "chain_opening_grant_notification",
		joinColumns = @JoinColumn(name = "chain_opening_grant_uuid"),
		inverseJoinColumns = @JoinColumn(name = "reported_notification_uuid")
	)
	@JsonIgnore
	@Builder.Default
	private Set<ReportedDemandAndCapacityNotification> reportedNotifications = new HashSet<>();

	private Instant validFrom;

	private Instant validUntil;

	@Builder.Default
	private String useCase = IrsAdapterConfiguration.PURIS_USE_CASE;

	@Enumerated(EnumType.STRING)
	@JsonIgnore
	private IrsGrantSyncStatusEnumeration syncStatus;

	/**
	 * The BPNLs allowed to be recursively queried under this grant, derived from the partners
	 * of {@link #reportedNotifications}. This is the wire shape expected by the IRS
	 * grant-creation request.
	 */
	@JsonProperty("allowedBpnls")
	public Set<String> getAllowedBpnls() {
		return reportedNotifications.stream()
			.map(ReportedDemandAndCapacityNotification::getPartner)
			.filter(Objects::nonNull)
			.map(Partner::getBpnl)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
