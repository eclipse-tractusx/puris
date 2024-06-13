/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.LeadingRootCauseEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.StatusEnumeration;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class DemandAndCapacityNotificationSamm {
    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String notificationId;
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String relatedNotificationId;
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String sourceNotificationId;

    private String text;
    @NotNull
    private LeadingRootCauseEnumeration leadingRootCause;
    @NotNull
    private EffectEnumeration effect;
    @NotNull
    private Date startDateOfEffect;
    private Date expectedEndDateOfEffect;

    private List<@Pattern(regexp = PatternStore.URN_OR_UUID_STRING) String> materialGlobalAssetId;
    private List<@Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING) String> materialNumberSupplier;
    private List<@Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING) String> materialNumberCustomer;

    private List<@Pattern(regexp = PatternStore.BPNS_STRING) String> affectedSitesSender;
    private List<@Pattern(regexp = PatternStore.BPNS_STRING) String> affectedSitesRecipient;

    @NotNull
    private StatusEnumeration status;
    @NotNull
    private Date contentChangedAt;

    @JsonCreator
    public DemandAndCapacityNotificationSamm(
            @JsonProperty(value = "affectedSitesSender") List<String> affectedSitesSender,
            @JsonProperty(value = "affectedSitesRecipient") List<String> affectedSitesRecipient,
            @JsonProperty(value = "leadingRootCause") LeadingRootCauseEnumeration leadingRootCause,
            @JsonProperty(value = "effect") EffectEnumeration effect,
            @JsonProperty(value = "text") String text,
            @JsonProperty(value = "materialGlobalAssetId") List<String> materialGlobalAssetId,
            @JsonProperty(value = "startDateOfEffect") Date startDateOfEffect,
            @JsonProperty(value = "expectedEndDateOfEffect") Date expectedEndDateOfEffect,
            @JsonProperty(value = "status") StatusEnumeration status,
            @JsonProperty(value = "contentChangedAt") Date contentChangedAt,
            @JsonProperty(value = "sourceNotificationId") String sourceNotificationId,
            @JsonProperty(value = "materialNumberSupplier") List<String> materialNumberSupplier,
            @JsonProperty(value = "materialNumberCustomer") List<String> materialNumberCustomer,
            @JsonProperty(value = "notificationId") String notificationId,
            @JsonProperty(value = "relatedNotificationId") String relatedNotificationId) {
        this.affectedSitesSender = affectedSitesSender;
        this.affectedSitesRecipient = affectedSitesRecipient;
        this.leadingRootCause = leadingRootCause;
        this.effect = effect;
        this.text = text;
        this.materialGlobalAssetId = materialGlobalAssetId;
        this.startDateOfEffect = startDateOfEffect;
        this.expectedEndDateOfEffect = expectedEndDateOfEffect;
        this.status = status;
        this.contentChangedAt = contentChangedAt;
        this.sourceNotificationId = sourceNotificationId;
        this.materialNumberSupplier = materialNumberSupplier;
        this.materialNumberCustomer = materialNumberCustomer;
        this.notificationId = notificationId;
        this.relatedNotificationId = relatedNotificationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DemandAndCapacityNotificationSamm that = (DemandAndCapacityNotificationSamm) o;
        return Objects.equals(affectedSitesSender, that.affectedSitesSender)
                && Objects.equals(affectedSitesRecipient, that.affectedSitesRecipient)
                && Objects.equals(leadingRootCause, that.leadingRootCause) && Objects.equals(effect, that.effect)
                && Objects.equals(text, that.text) && Objects.equals(materialGlobalAssetId, that.materialGlobalAssetId)
                && Objects.equals(startDateOfEffect, that.startDateOfEffect)
                && Objects.equals(expectedEndDateOfEffect, that.expectedEndDateOfEffect)
                && Objects.equals(status, that.status) && Objects.equals(contentChangedAt, that.contentChangedAt)
                && Objects.equals(sourceNotificationId, that.sourceNotificationId)
                && Objects.equals(materialNumberSupplier, that.materialNumberSupplier)
                && Objects.equals(materialNumberCustomer, that.materialNumberCustomer)
                && Objects.equals(notificationId, that.notificationId)
                && Objects.equals(relatedNotificationId, that.relatedNotificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(affectedSitesSender, affectedSitesRecipient, leadingRootCause, effect, text,
                materialGlobalAssetId, startDateOfEffect, expectedEndDateOfEffect, status, contentChangedAt,
                sourceNotificationId, materialNumberSupplier, materialNumberCustomer, notificationId,
                relatedNotificationId);
    }
}
