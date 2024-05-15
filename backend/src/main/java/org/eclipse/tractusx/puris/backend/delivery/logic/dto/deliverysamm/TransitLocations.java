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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Generated class for Transit Locations. Transit locations describes the
 * specific location where products undergo the process of being transported
 * from the source or production facility (origin) to the designated endpoint
 * location (destination).
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TransitLocations {

    @NotNull
    private Location origin;

    @NotNull
    private Location destination;

    @JsonCreator
    public TransitLocations(@JsonProperty(value = "origin") Location origin,
            @JsonProperty(value = "destination") Location destination) {
        this.origin = origin;
        this.destination = destination;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TransitLocations that = (TransitLocations) o;
        return Objects.equals(origin, that.origin) && Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination);
    }
}
