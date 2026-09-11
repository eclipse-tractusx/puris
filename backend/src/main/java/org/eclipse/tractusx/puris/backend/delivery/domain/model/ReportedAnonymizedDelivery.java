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
package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
@Entity
@ToString
public class ReportedAnonymizedDelivery {

    @Id
    @GeneratedValue
    protected UUID uuid;

    @Column(name = "aggregated_material_data_node_id", insertable = false, updatable = false)
    protected UUID aggregatedMaterialDataNodeId;

    private double quantity;
    private ItemUnitEnumeration measurementUnit;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @NotNull
    private Date dateOfDeparture;
    private Date dateOfArrival;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EventTypeEnumeration departureType;

    @Enumerated(EnumType.STRING)
    private EventTypeEnumeration arrivalType;

    @NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    private String originBpnsAnonymized;

    @NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    private String destinationBpnsAnonymized;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReportedAnonymizedDelivery that = (ReportedAnonymizedDelivery) o;
        return Objects.equals(toInstant(this.getDateOfDeparture()), toInstant(that.getDateOfDeparture())) &&
            Objects.equals(toInstant(this.getDateOfArrival()), toInstant(that.getDateOfArrival())) &&
            Objects.equals(this.getDepartureType(), that.getDepartureType()) &&
            Objects.equals(this.getArrivalType(), that.getArrivalType()) &&
            Objects.equals(this.getOriginBpnsAnonymized(), that.getOriginBpnsAnonymized()) &&
            Objects.equals(this.getDestinationBpnsAnonymized(), that.getDestinationBpnsAnonymized());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            quantity, measurementUnit, dateOfDeparture, dateOfArrival, departureType, arrivalType, originBpnsAnonymized, destinationBpnsAnonymized
        );
    }

    private static Instant toInstant(Date d) {
        return d == null ? null : d.toInstant();
    }
}
