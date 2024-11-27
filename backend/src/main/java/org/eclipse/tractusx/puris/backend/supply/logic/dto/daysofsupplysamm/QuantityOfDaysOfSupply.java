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
package org.eclipse.tractusx.puris.backend.supply.logic.dto.daysofsupplysamm;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class QuantityOfDaysOfSupply {

    @NotNull
    private Date date;

    @NotNull
    private Double daysOfSupply;

    @JsonCreator
    public QuantityOfDaysOfSupply(@JsonProperty(value = "date") Date date, @JsonProperty(value = "daysOfSupply") Double daysOfSupply) {
        this.date = date;
        this.daysOfSupply = daysOfSupply;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final QuantityOfDaysOfSupply that = (QuantityOfDaysOfSupply) o;
        return Objects.equals(date, that.date) && Objects.equals(daysOfSupply, that.daysOfSupply);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, daysOfSupply);
    }
}
