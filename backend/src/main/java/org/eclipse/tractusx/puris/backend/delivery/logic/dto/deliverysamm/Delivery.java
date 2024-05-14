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
package org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.IncotermEnumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Delivery {

    @NotNull
    private ItemQuantityEntity deliveryQuantity;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @NotNull
    @Size(min = 1, max = 2)
    private Set<TransitEvent> transitEvents;

    @NotNull
    private TransitLocations transitLocations;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String trackingNumber;
    
    private IncotermEnumeration incoterm;

    @JsonCreator
    public Delivery(@JsonProperty(value = "deliveryQuantity") ItemQuantityEntity deliveryQuantity,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime,
            @JsonProperty(value = "transitEvents") Set<TransitEvent> transitEvents,
            @JsonProperty(value = "transitLocations") TransitLocations transitLocations,
            @JsonProperty(value = "trackingNumber") String trackingNumber,
            @JsonProperty(value = "incoterm") IncotermEnumeration incoterm) {
        this.deliveryQuantity = deliveryQuantity;
        this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
        this.transitEvents = transitEvents;
        this.transitLocations = transitLocations;
        this.trackingNumber = trackingNumber;
        this.incoterm = incoterm;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Delivery that = (Delivery) o;
        return Objects.equals(deliveryQuantity, that.deliveryQuantity)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime)
                && Objects.equals(transitEvents, that.transitEvents)
                && Objects.equals(transitLocations, that.transitLocations)
                && Objects.equals(trackingNumber, that.trackingNumber) && Objects.equals(incoterm, that.incoterm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryQuantity, lastUpdatedOnDateTime, transitEvents, transitLocations, trackingNumber,
                incoterm);
    }
}
