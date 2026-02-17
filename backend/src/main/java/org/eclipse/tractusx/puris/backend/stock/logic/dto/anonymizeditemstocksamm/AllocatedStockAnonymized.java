/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm;

import java.util.Date;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AllocatedStockAnonymized {
    @NotNull
    @Valid
	private ItemQuantityEntity quantityOnAllocatedStock;

	@NotNull
    @Pattern.List({
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING),
        @Pattern(regexp = PatternStore.NOT_BPNS_STRING)
    })
	private String stockLocationBPNSAnonymized;

	@NotNull
	private Boolean isBlocked;

    @NotNull
    private Date lastUpdatedOnDateTime;

	@JsonCreator
	public AllocatedStockAnonymized(@JsonProperty(value = "quantityOnAllocatedStock") ItemQuantityEntity quantityOnAllocatedStock,
			@JsonProperty(value = "stockLocationBPNSAnonymized") String stockLocationBPNSAnonymized,
			@JsonProperty(value = "isBlocked") Boolean isBlocked,
            @JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime) {
		this.quantityOnAllocatedStock = quantityOnAllocatedStock;
		this.stockLocationBPNSAnonymized = stockLocationBPNSAnonymized;
		this.isBlocked = isBlocked;
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

		final AllocatedStockAnonymized that = (AllocatedStockAnonymized) o;
		return Objects.equals(quantityOnAllocatedStock, that.quantityOnAllocatedStock)
				&& Objects.equals(stockLocationBPNSAnonymized, that.stockLocationBPNSAnonymized)
				&& Objects.equals(isBlocked, that.isBlocked)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(quantityOnAllocatedStock, stockLocationBPNSAnonymized, isBlocked,
            lastUpdatedOnDateTime);
	}
}