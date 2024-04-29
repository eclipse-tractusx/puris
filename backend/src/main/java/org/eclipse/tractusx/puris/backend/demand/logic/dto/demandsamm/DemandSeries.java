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

import java.util.Date;
import java.util.Objects;
import java.util.Set;

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

/**
 * Generated class for Demand Series. Encapsulates the demand series related
 * information.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DemandSeries {

    @NotNull
    @Valid
    private Set<Demand> demands;

    @NotNull
    private DemandCategoryCharacteristic demandCategory;

    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String expectedSupplierLocationBpns;

    @NotNull
    @Pattern(regexp = "^BPNS[a-zA-Z0-9]{12}$")
    private String customerLocationBpns;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @JsonCreator
    public DemandSeries(@JsonProperty(value = "demands") Set<Demand> demands,
                        @JsonProperty(value = "demandCategory") DemandCategoryCharacteristic demandCategory,
                        @JsonProperty(value = "expectedSupplierLocationBpns") String expectedSupplierLocationBpns,
                        @JsonProperty(value = "customerLocationBpns") String customerLocationBpns,
                        @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
        this.demands = demands;
        this.demandCategory = demandCategory;
        this.expectedSupplierLocationBpns = expectedSupplierLocationBpns;
        this.customerLocationBpns = customerLocationBpns;
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

        final DemandSeries that = (DemandSeries) o;
        return Objects.equals(demands, that.demands) && Objects.equals(demandCategory, that.demandCategory)
                && Objects.equals(expectedSupplierLocationBpns, that.expectedSupplierLocationBpns)
                && Objects.equals(customerLocationBpns, that.customerLocationBpns)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(demands, demandCategory, expectedSupplierLocationBpns, customerLocationBpns, lastUpdatedOnDateTime);
    }
}
