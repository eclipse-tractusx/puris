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
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;

/**
 * Generated class for Planned and Allocated Production Output. Quantity, site
 * of the supplier and date with time at which a production of a certain
 * material for a certain customer is planned to be finished.
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AllocatedPlannedProductionOutput {

    @NotNull
    @Valid
    private ItemQuantityEntity plannedProductionQuantity;

    @NotNull
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String productionSiteBpns;

    @NotNull
    private Date estimatedTimeOfCompletion;

    @NotNull
    private Date lastUpdatedOnDateTime;

    @JsonCreator
    public AllocatedPlannedProductionOutput(
            @JsonProperty(value = "plannedProductionQuantity") ItemQuantityEntity plannedProductionQuantity,
            @JsonProperty(value = "productionSiteBpns") String productionSiteBpns,
            @JsonProperty(value = "estimatedTimeOfCompletion") Date estimatedTimeOfCompletion,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
        this.plannedProductionQuantity = plannedProductionQuantity;
        this.productionSiteBpns = productionSiteBpns;
        this.estimatedTimeOfCompletion = estimatedTimeOfCompletion;
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

        final AllocatedPlannedProductionOutput that = (AllocatedPlannedProductionOutput) o;
        return Objects.equals(plannedProductionQuantity, that.plannedProductionQuantity)
                && Objects.equals(productionSiteBpns, that.productionSiteBpns)
                && Objects.equals(estimatedTimeOfCompletion, that.estimatedTimeOfCompletion)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plannedProductionQuantity, productionSiteBpns, estimatedTimeOfCompletion, lastUpdatedOnDateTime);
    }
}
