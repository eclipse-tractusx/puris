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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a Chain Opening Grant requesting recursive access to a material's chain for a
 * partner, as it is created at and deleted from the IRS.
 * <p>
 * A grant's {@link #requesterBpn} (a partner's BPNL) allows it to recursively query the set of
 * {@link #getAllowedBpnls()} for child materials of the material identified by
 * {@link #globalAssetId} (a material directly affected by one of our own disruption
 * notifications that we approved data exchange for), for the duration of the
 * [{@link #validFrom}, {@link #validUntil}] window. The allowed BPNLs are derived from the
 * partners of {@link #reportedNotifications}, the set of reported notifications currently
 * backing this grant.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "chain_opening_grant", uniqueConstraints = @UniqueConstraint(
	name = "uc_chain_opening_grant_key",
	columnNames = { "requester_bpn", "global_asset_id", "source_disruption_id" }
))
public class IrsChainOpeningPartnerGrant extends IrsChainOpeningGrant {

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "chain_opening_grant_notification",
		joinColumns = @JoinColumn(name = "chain_opening_grant_uuid"),
		inverseJoinColumns = @JoinColumn(name = "reported_notification_uuid")
	)
	@JsonIgnore
	@Builder.Default
	private Set<ReportedDemandAndCapacityNotification> reportedNotifications = new HashSet<>();

	@Override
	public Set<ReportedDemandAndCapacityNotification> getReportedNotifications() {
		return reportedNotifications;
	}
}
