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
package org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;

/**
 * Generated class for Position. The Position can be planned for production at
 * several sites. A position may be anonymous or may reference a position within
 * an order.
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Position {
    @Valid
    private OrderPositionReference orderPositionReference;

    @NotNull
    @Valid
    private HashSet<AllocatedPlannedProductionOutput> allocatedPlannedProductionOutputs;

    @JsonCreator
    public Position(
            @JsonProperty(value = "orderPositionReference") OrderPositionReference orderPositionReference,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime,
            @JsonProperty(value = "allocatedPlannedProductionOutputs") HashSet<AllocatedPlannedProductionOutput> allocatedPlannedProductionOutputs) {
        super();
        this.orderPositionReference = orderPositionReference;
        this.allocatedPlannedProductionOutputs = allocatedPlannedProductionOutputs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Position that = (Position) o;
        return Objects.equals(orderPositionReference, that.orderPositionReference)
                && Objects.equals(allocatedPlannedProductionOutputs, that.allocatedPlannedProductionOutputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderPositionReference, allocatedPlannedProductionOutputs);
    }
}
