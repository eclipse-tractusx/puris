/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;

/**
 * Semi-Generated class for Stock of Items. This aspect represents the latest
 * quantities of a partner's items that are on stock. The stock represent the
 * build-to-order (BTO) stocks already available.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ItemStockSamm {

    @NotNull
    @Valid
    private Set<Position> positions;

    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String materialGlobalAssetId;

    @NotNull
    private DirectionCharacteristic direction;

    @JsonCreator
    public ItemStockSamm(@JsonProperty(value = "positions") Set<Position> positions,
                         @JsonProperty(value = "materialGlobalAssetId") String materialGlobalAssetId,
                         @JsonProperty(value = "direction") DirectionCharacteristic direction) {
        this.positions = positions;
        this.materialGlobalAssetId = materialGlobalAssetId;
        this.direction = direction;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ItemStockSamm that = (ItemStockSamm) o;
        return Objects.equals(positions, that.positions)
            && Objects.equals(materialGlobalAssetId, that.materialGlobalAssetId)
            && Objects.equals(direction, that.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positions, materialGlobalAssetId, direction);
    }
}
