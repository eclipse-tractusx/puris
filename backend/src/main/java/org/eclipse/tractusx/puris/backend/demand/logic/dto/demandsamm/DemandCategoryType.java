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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * Generated class for Demand Category Type. Describes the type of a demand
 * category.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DemandCategoryType {

	@NotNull
	private String demandCategoryCode;
	private String demandCategoryName;

	@JsonCreator
	public DemandCategoryType(@JsonProperty(value = "demandCategoryCode") String demandCategoryCode, @JsonProperty(value = "demandCategoryName") String demandCategoryName) {
		this.demandCategoryCode = demandCategoryCode;
		this.demandCategoryName = demandCategoryName;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DemandCategoryType that = (DemandCategoryType) o;
		return Objects.equals(demandCategoryCode, that.demandCategoryCode)
				&& Objects.equals(demandCategoryName, that.demandCategoryName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(demandCategoryCode, demandCategoryName);
	}
}
