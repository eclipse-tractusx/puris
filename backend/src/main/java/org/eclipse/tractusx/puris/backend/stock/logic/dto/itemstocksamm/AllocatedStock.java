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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;

/**
 * Generated class for Stock Allocated to a Partner. This is the quantity of
 * items on stock at a location. A stock can either be - from a certain supplier
 * and ready to be consumed by a customer or - from a supplier and ready to be
 * shipped to a certain customer.
 * 
 * In case of stocks "from a supplier ready to be shipped to a certain
 * customer", the stock may refer to an order position of a customer. This stock
 * consists only of the good-finished items.
 */
@Getter
@Setter
@ToString
public class AllocatedStock {

	@NotNull
    @Valid
	private ItemQuantityEntity quantityOnAllocatedStock;

	@NotNull
	@Pattern(regexp = PatternStore.BPNS_STRING)
	private String stockLocationBPNS;

	@NotNull
	private Boolean isBlocked;

	@NotNull
	@Pattern(regexp = PatternStore.BPNA_STRING)
	private String stockLocationBPNA;

	@JsonCreator
	public AllocatedStock(@JsonProperty(value = "quantityOnAllocatedStock") ItemQuantityEntity quantityOnAllocatedStock,
			@JsonProperty(value = "stockLocationBPNS") String stockLocationBPNS,
			@JsonProperty(value = "isBlocked") Boolean isBlocked,
			@JsonProperty(value = "stockLocationBPNA") String stockLocationBPNA) {
		this.quantityOnAllocatedStock = quantityOnAllocatedStock;
		this.stockLocationBPNS = stockLocationBPNS;
		this.isBlocked = isBlocked;
		this.stockLocationBPNA = stockLocationBPNA;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final AllocatedStock that = (AllocatedStock) o;
		return Objects.equals(quantityOnAllocatedStock, that.quantityOnAllocatedStock)
				&& Objects.equals(stockLocationBPNS, that.stockLocationBPNS)
				&& Objects.equals(isBlocked, that.isBlocked)
				&& Objects.equals(stockLocationBPNA, that.stockLocationBPNA);
	}

	@Override
	public int hashCode() {
		return Objects.hash(quantityOnAllocatedStock, stockLocationBPNS, isBlocked, stockLocationBPNA);
	}
}
