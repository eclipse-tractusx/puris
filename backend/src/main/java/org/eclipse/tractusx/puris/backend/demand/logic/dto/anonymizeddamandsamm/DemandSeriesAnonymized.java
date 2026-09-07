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
package org.eclipse.tractusx.puris.backend.demand.logic.dto.anonymizeddamandsamm;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.Demand;

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
public class DemandSeriesAnonymized {
 
    @NotNull
    @Valid
    private Set<Demand> demands;
 
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    private String expectedSupplierLocationBpnsAnonymized;
 
    @NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    private String customerLocationBpnsAnonymized;
 
    @NotNull
    private Date lastUpdatedOnDateTime;
 
    @JsonCreator
    public DemandSeriesAnonymized(@JsonProperty(value = "demands") Set<Demand> demands,
            @JsonProperty(value = "expectedSupplierLocationBpnsAnonymized") String expectedSupplierLocationBpnsAnonymized,
            @JsonProperty(value = "customerLocationBpnsAnonymized") String customerLocationBpnsAnonymized,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
        this.demands = demands;
        this.expectedSupplierLocationBpnsAnonymized = expectedSupplierLocationBpnsAnonymized;
        this.customerLocationBpnsAnonymized = customerLocationBpnsAnonymized;
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
 
        final DemandSeriesAnonymized that = (DemandSeriesAnonymized) o;
        return Objects.equals(demands, that.demands)
                && Objects.equals(expectedSupplierLocationBpnsAnonymized, that.expectedSupplierLocationBpnsAnonymized)
                && Objects.equals(customerLocationBpnsAnonymized, that.customerLocationBpnsAnonymized)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(demands, expectedSupplierLocationBpnsAnonymized, customerLocationBpnsAnonymized,
            lastUpdatedOnDateTime);
    }
}
