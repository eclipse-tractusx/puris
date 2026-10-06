/*
Copyright (c) 2026 Volkswagen AG

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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.LeadingRootCauseEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class DataExchangeRequest {
    /** 
     * uuid = internally generated unique identifier
     * requestId = stable UUID reference for communication with partner, expected to be used in SAMM   
     */
    @Id
    @GeneratedValue
    protected UUID uuid;
    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    protected String requestId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partner_uuid", nullable = false)
    @ToString.Exclude
    @NotNull
    protected Partner partner;

    @NotNull
    protected UUID sourceDisruptionId;

    @NotNull
    protected LeadingRootCauseEnumeration leadingRootCause;

    @NotNull
    protected EffectEnumeration effect;

    @ManyToMany
    @JoinTable(
        name = "data_exchange_request_material",
        joinColumns = @JoinColumn(name = "data_exchange_request_uuid"),
        inverseJoinColumns = @JoinColumn(name = "material_own_material_number"))
    @ToString.Exclude
    protected List<Material> materials;

    @ManyToMany
    @JoinTable(
        name = "data_exchange_request_affected_sites_sender",
        joinColumns = @JoinColumn(name = "data_exchange_request_uuid"),
        inverseJoinColumns = @JoinColumn(name = "site_bpns"))
    @ToString.Exclude
    protected List<Site> affectedSitesSender;

    @ManyToMany
    @JoinTable(
        name = "data_exchange_request_affected_sites_recipient",
        joinColumns = @JoinColumn(name = "data_exchange_request_uuid"),
        inverseJoinColumns = @JoinColumn(name = "site_bpns"))
    @ToString.Exclude
    protected List<Site> affectedSitesRecipient;

    @NotNull
    private CriticalityEnumeration criticality;

    @NotNull
    private Date desiredStartDateTime;

    @NotNull
    private Date desiredEndDateTime;

    @NotEmpty
    protected List<RequestedTypeEnumeration> requestedTypes;

    @NotBlank
    private String text;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Date timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataExchangeRequest that = (DataExchangeRequest) o;
        return Objects.equals(this.getRequestId(), that.getRequestId()) &&
            Objects.equals(this.getSourceDisruptionId(), that.getSourceDisruptionId()) &&
            this.getLeadingRootCause() == that.getLeadingRootCause() &&
            this.getEffect() == that.getEffect() &&
            Objects.equals(this.getCriticality().getValue(), that.getCriticality().getValue()) &&
            Objects.equals(toInstant(this.getDesiredStartDateTime()), toInstant(that.getDesiredStartDateTime())) &&
            Objects.equals(toInstant(this.getDesiredEndDateTime()), toInstant(that.getDesiredEndDateTime())) &&
            Objects.equals(this.getRequestedTypes(), that.getRequestedTypes()) && Objects.equals(this.getText(), that.getText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, sourceDisruptionId, leadingRootCause, effect, criticality, desiredStartDateTime, desiredEndDateTime, requestedTypes, text);
    }

    private static Instant toInstant(Date d) {
        return d == null ? null : d.toInstant();
    }
}
