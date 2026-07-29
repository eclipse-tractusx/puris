
/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.production.logic.dto.anonymizedplannedproductionsamm;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

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
public class AllocatedPlannedProductionOutputAnonymized {
    @NotNull
    private ItemQuantityEntity plannedProductionQuantity;
    @NotNull
    private Date estimatedTimeOfCompletion;
    @NotNull
    private Date lastUpdatedOnDateTime;
    @NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    private String productionSiteBpnsAnonymized;

    @JsonCreator
    public AllocatedPlannedProductionOutputAnonymized(
            @JsonProperty(value = "plannedProductionQuantity") ItemQuantityEntity plannedProductionQuantity,
            @JsonProperty(value = "productionSiteBpnsAnonymized") String productionSiteBpnsAnonymized,
            @JsonProperty(value = "estimatedTimeOfCompletion") Date estimatedTimeOfCompletion,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
                this.plannedProductionQuantity = plannedProductionQuantity;
                this.productionSiteBpnsAnonymized = productionSiteBpnsAnonymized;
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

        final AllocatedPlannedProductionOutputAnonymized that = (AllocatedPlannedProductionOutputAnonymized) o;
        return Objects.equals(plannedProductionQuantity, that.plannedProductionQuantity) &&
               Objects.equals(productionSiteBpnsAnonymized, that.productionSiteBpnsAnonymized) &&
               Objects.equals(estimatedTimeOfCompletion, that.estimatedTimeOfCompletion) &&
               Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plannedProductionQuantity, productionSiteBpnsAnonymized, estimatedTimeOfCompletion, lastUpdatedOnDateTime);
    }
}
