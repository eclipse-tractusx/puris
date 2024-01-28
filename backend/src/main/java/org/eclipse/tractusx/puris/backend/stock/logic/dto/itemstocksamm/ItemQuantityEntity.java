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

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.ItemUnitEnumeration;

import java.util.Objects;

/**
 * Generated class for Item Quantity Entity. Entity for common measurements of
 * an item (mass, count, linear, area, volume, misc) with an unit and a value.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ItemQuantityEntity {

    @NotNull
    private Double value;

    @NotNull
    private ItemUnitEnumeration unit;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ItemQuantityEntity that = (ItemQuantityEntity) o;
        return Objects.equals(value, that.value) && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }
}
