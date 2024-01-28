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
package org.eclipse.tractusx.puris.backend.stock.domain.model.measurement;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Generated class {@link ItemUnitEnumeration}.
 */
public enum ItemUnitEnumeration {
    UNIT_PIECE("unit:piece"),
    UNIT_SET("unit:set"),
    UNIT_PAIR("unit:pair"),
    UNIT_PAGE("unit:page"),
    UNIT_CYCLE("unit:cycle"),
    UNIT_KILOWATT_HOUR("unit:kilowattHour"),
    UNIT_GRAM("unit:gram"),
    UNIT_KILOGRAM("unit:kilogram"),
    UNIT_TONNE_METRIC_TON("unit:tonneMetricTon"),
    UNIT_TON_US_OR_SHORT_TON_UKORUS("unit:tonUsOrShortTonUkorus"),
    UNIT_OUNCE_AVOIRDUPOIS("unit:ounceAvoirdupois"),
    UNIT_POUND("unit:pound"),
    UNIT_METRE("unit:metre"),
    UNIT_CENTIMETRE("unit:centimetre"),
    UNIT_KILOMETRE("unit:kilometre"),
    UNIT_INCH("unit:inch"),
    UNIT_FOOT("unit:foot"),
    UNIT_YARD("unit:yard"),
    UNIT_SQUARE_CENTIMETRE("unit:squareCentimetre"),
    UNIT_SQUARE_METRE("unit:squareMetre"),
    UNIT_SQUARE_INCH("unit:squareInch"),
    UNIT_SQUARE_FOOT("unit:squareFoot"),
    UNIT_SQUARE_YARD("unit:squareYard"),
    UNIT_CUBIC_CENTIMETRE("unit:cubicCentimetre"),
    UNIT_CUBIC_METRE("unit:cubicMetre"),
    UNIT_CUBIC_INCH("unit:cubicInch"),
    UNIT_CUBIC_FOOT("unit:cubicFoot"),
    UNIT_CUBIC_YARD("unit:cubicYard"),
    UNIT_LITRE("unit:litre"),
    UNIT_MILLILITRE("unit:millilitre"),
    UNIT_HECTOLITRE("unit:hectolitre"),
    UNIT_SECOND_UNIT_OF_TIME("unit:secondUnitOfTime"),
    UNIT_MINUTE_UNIT_OF_TIME("unit:minuteUnitOfTime"),
    UNIT_HOUR_UNIT_OF_TIME("unit:hourUnitOfTime"),
    UNIT_DAY("unit:day");

    private String value;

    ItemUnitEnumeration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}
