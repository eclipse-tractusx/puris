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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Generated class for Position. The Position can be located at several stocks.
 * In case of a supplier's stock for a customer, a position may be either
 * anonymous or reference a position within a customer order. In case of a
 * customer's stock for a supplier, the order position reference MUST NOT be
 * set.
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Position {
    @Valid
	private OrderPositionReference orderPositionReference;

    @NotNull
	private Date lastUpdatedOnDateTime;

	@NotNull
    @Valid
	private List<AllocatedStock> allocatedStocks;

	@JsonCreator
	public Position(
			@JsonProperty(value = "orderPositionReference") OrderPositionReference orderPositionReference,
			@JsonProperty(value = "lastUpdatedOnDateTime") Date lastUpdatedOnDateTime,
			@JsonProperty(value = "allocatedStocks") List<AllocatedStock> allocatedStocks) {
		this.orderPositionReference = orderPositionReference;
		this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
		this.allocatedStocks = allocatedStocks;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Position that = (Position) o;
		return Objects.equals(orderPositionReference, that.orderPositionReference)
				&& Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime)
				&& Objects.equals(allocatedStocks, that.allocatedStocks);
	}

	@Override
	public int hashCode() {
		return Objects.hash(orderPositionReference, lastUpdatedOnDateTime, allocatedStocks);
	}
}
