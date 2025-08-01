/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class Delivery {
    @Id
    @GeneratedValue
    protected UUID uuid;

    @ManyToOne()
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    protected Partner partner;

    @ManyToOne()
    @JoinColumn(name = "material_ownMaterialNumber")
    @ToString.Exclude
    @NotNull
    protected Material material;

    private double quantity;
    private ItemUnitEnumeration measurementUnit;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String trackingNumber;

    private IncotermEnumeration incoterm;

    // Order Position Reference
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String supplierOrderNumber;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String customerOrderNumber;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String customerOrderPositionNumber;

    // Transit Location
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String destinationBpns;
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String destinationBpna;
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String originBpns;
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String originBpna;

    // Transit Event
    private Date dateOfDeparture;
    private Date dateOfArrival;
    private EventTypeEnumeration departureType;
    private EventTypeEnumeration arrivalType;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @ToString.Include
    private String material_ownMaterialNumber() {
        return material.getOwnMaterialNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Delivery that = (Delivery) o;
        return this.getMaterial().getOwnMaterialNumber().equals(that.getMaterial().getOwnMaterialNumber()) &&
            this.getPartner().getUuid().equals(that.getPartner().getUuid()) &&
            Objects.equals(this.getTrackingNumber(), that.getTrackingNumber()) &&
            this.getIncoterm().equals(that.getIncoterm()) &&
            this.getDestinationBpns().equals(that.getDestinationBpns()) &&
            Objects.equals(this.getDestinationBpna(), that.getDestinationBpna()) &&
            this.getOriginBpns().equals(that.getOriginBpns()) &&
            Objects.equals(this.getOriginBpna(), that.getOriginBpna()) &&
            this.getDateOfDeparture().equals(that.getDateOfDeparture()) &&
            this.getDateOfArrival().equals(that.getDateOfArrival()) &&
            this.getDepartureType().equals(that.getDepartureType()) &&
            this.getArrivalType().equals(that.getArrivalType()) &&
            this.getIncoterm().equals(that.getIncoterm()) &&
            (
                Objects.equals(this.getCustomerOrderNumber(), that.getCustomerOrderNumber()) &&
                Objects.equals(this.getCustomerOrderPositionNumber(), that.getCustomerOrderPositionNumber()) &&
                Objects.equals(this.getSupplierOrderNumber(), that.getSupplierOrderNumber())
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            partner, material, quantity, measurementUnit,
            trackingNumber, incoterm,
            supplierOrderNumber, customerOrderNumber, customerOrderPositionNumber,
            destinationBpns, destinationBpna, originBpns, originBpna,
            dateOfDeparture, dateOfArrival, departureType, arrivalType
        );
    }
}
