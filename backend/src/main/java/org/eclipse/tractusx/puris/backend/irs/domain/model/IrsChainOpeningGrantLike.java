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
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;

/**
 * Common shape shared by {@link IrsChainOpeningRootGrant} and {@link IrsChainOpeningGrant}, letting
 * the IRS enqueue plumbing ({@code IrsChainOpeningGrantGateway}) and the notification
 * add/remove/reconcile helpers ({@code IrsChainOpeningGrantSyncSupport}) be written once and reused
 * by both grant flavors.
 * <p>
 * {@code getAllowedBpnls()} is deliberately not part of this interface: each entity implements it
 * directly so it stays a plain {@code @JsonProperty}-annotated method on a concrete class, which
 * Jackson serializes reliably; declaring it as a default method here risks inconsistent
 * introspection of annotations on inherited interface methods.
 */
public interface IrsChainOpeningGrantLike {

	UUID getUuid();

	String getGlobalAssetId();

	String getSourceDisruptionId();

	String getRequesterBpn();

	String getUseCase();

	Instant getValidFrom();

	void setValidFrom(Instant validFrom);

	Instant getValidUntil();

	void setValidUntil(Instant validUntil);

	IrsGrantSyncStatusEnumeration getSyncStatus();

	void setSyncStatus(IrsGrantSyncStatusEnumeration syncStatus);

	Set<ReportedDemandAndCapacityNotification> getReportedNotifications();
}
