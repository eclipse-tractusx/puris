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
package org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generated class for Demand. A single demand for a day.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Demand {

    @NotNull
    private ItemQuantityEntity demand;

    @NotNull
    private Date day;

    @JsonCreator
    public Demand(@JsonProperty(value = "demand") ItemQuantityEntity demand,
                  @JsonProperty(value = "day") Date day) {
        this.demand = demand;
        this.day = day;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Demand that = (Demand) o;
        return Objects.equals(demand, that.demand) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(demand, day);
    }
}
