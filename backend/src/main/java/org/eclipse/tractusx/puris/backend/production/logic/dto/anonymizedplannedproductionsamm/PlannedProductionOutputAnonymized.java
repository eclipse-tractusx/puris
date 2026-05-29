
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

import java.util.HashSet;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class PlannedProductionOutputAnonymized {
    @NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String materialGlobalAssetIdAnonymized;

    @NotNull
    private HashSet<AllocatedPlannedProductionOutputAnonymized> allocatedPlannedProductionOutputs = new HashSet<>();

    @JsonCreator
    public PlannedProductionOutputAnonymized(@JsonProperty(value = "materialGlobalAssetIdAnonymized") String materialGlobalAssetIdAnonymized) {
        this.materialGlobalAssetIdAnonymized = materialGlobalAssetIdAnonymized;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PlannedProductionOutputAnonymized that = (PlannedProductionOutputAnonymized) o;
        return Objects.equals(materialGlobalAssetIdAnonymized, that.materialGlobalAssetIdAnonymized) && Objects.equals(allocatedPlannedProductionOutputs, that.allocatedPlannedProductionOutputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocatedPlannedProductionOutputs, materialGlobalAssetIdAnonymized);
    }
}
