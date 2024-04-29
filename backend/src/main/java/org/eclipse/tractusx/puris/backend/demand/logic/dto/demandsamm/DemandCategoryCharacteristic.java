
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

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Generated class {@link DemandCategoryCharacteristic}.
 */

public enum DemandCategoryCharacteristic {
    DEMAND_CATEGORY(new DemandCategoryType("0001", "Default")),
    DEMAND_CATEGORY_AFTER_SALES(new DemandCategoryType("A1S1", "After-Sales")),
    DEMAND_CATEGORY_SERIES(new DemandCategoryType("SR99", "Series")),
    DEMAND_CATEGORY_PHASE_IN_PERIOD(new DemandCategoryType("PI01", "Phase-In Period")),
    DEMAND_CATEGORY_SINGLE_ORDER(new DemandCategoryType("OS01", "Single Order")),
    DEMAND_CATEGORY_SMALL_SERIES(new DemandCategoryType("OI01", "Small Series")),
    DEMAND_CATEGORY_EXTRAORDINARY_DEMAND(new DemandCategoryType("ED01", "Extraordinary Demand")),
    DEMAND_CATEGORY_PHASE_OUT_PERIOD(new DemandCategoryType("PO01", "Phase-Out Period"));

    DemandCategoryCharacteristic(DemandCategoryType value) {
        this.value = value;
    }

    private DemandCategoryType value;

    @JsonCreator
	static DemandCategoryCharacteristic enumDeserializationConstructor(DemandCategoryType value) {
		return fromValue(value).orElseThrow();
	}

	@JsonValue
	public DemandCategoryType getValue() {
		return value;
	}

    public static Optional<DemandCategoryCharacteristic> fromValue(DemandCategoryType value) {
		return Arrays.stream(DemandCategoryCharacteristic.values())
				.filter(enumValue -> compareEnumValues(enumValue, value)).findAny();
	}

    private static boolean compareEnumValues(DemandCategoryCharacteristic enumValue, DemandCategoryType value) {
		return enumValue.getValue().getDemandCategoryCode().equals(value.getDemandCategoryCode());
	}
}
