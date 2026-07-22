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
package org.eclipse.tractusx.puris.backend.stock.domain.model;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class ReportedAnonymizedStock {
    @Id
    @GeneratedValue
    protected UUID uuid;

    @Column(name = "aggregated_data_uuid")
    protected UUID aggregatedDataId;
    
    protected double quantity;

    @NotNull
    protected ItemUnitEnumeration measurementUnit;

    @NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
    protected String stockLocationBpnsAnonymized;
    protected boolean isBlocked;

    @NotNull
    protected Date lastUpdatedOnDateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReportedAnonymizedStock that = (ReportedAnonymizedStock) o;
        return this.getStockLocationBpnsAnonymized().equals(that.getStockLocationBpnsAnonymized()) && this.isBlocked == that.isBlocked;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            quantity, measurementUnit, getStockLocationBpnsAnonymized(), lastUpdatedOnDateTime, isBlocked
        );
    }
}
