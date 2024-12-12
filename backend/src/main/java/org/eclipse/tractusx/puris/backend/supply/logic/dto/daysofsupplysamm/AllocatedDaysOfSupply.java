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
import java.util.List;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AllocatedDaysOfSupply {

    @NotNull
    @Valid
    private List<QuantityOfDaysOfSupply> amountOfAllocatedDaysOfSupply;

    @NotNull
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String stockLocationBPNS;

    @NotNull
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String stockLocationBPNA;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @JsonCreator
    public AllocatedDaysOfSupply(
            @JsonProperty(value = "amountOfAllocatedDaysOfSupply") List<QuantityOfDaysOfSupply> amountOfAllocatedDaysOfSupply,
            @JsonProperty(value = "stockLocationBPNS") String stockLocationBPNS,
            @JsonProperty(value = "stockLocationBPNA") String stockLocationBPNA,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
        this.amountOfAllocatedDaysOfSupply = amountOfAllocatedDaysOfSupply;
        this.stockLocationBPNS = stockLocationBPNS;
        this.stockLocationBPNA = stockLocationBPNA;
        this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AllocatedDaysOfSupply that = (AllocatedDaysOfSupply) o;
        return Objects.equals(amountOfAllocatedDaysOfSupply, that.amountOfAllocatedDaysOfSupply)
                && Objects.equals(stockLocationBPNS, that.stockLocationBPNS)
                && Objects.equals(stockLocationBPNA, that.stockLocationBPNA)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountOfAllocatedDaysOfSupply, stockLocationBPNS, stockLocationBPNA, lastUpdatedOnDateTime);
    }
}
