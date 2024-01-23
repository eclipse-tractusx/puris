/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.stock.domain.model.measurement;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MeasurementUnit {

    gram,
    kilogram,
    tonneMetricTon,
    tonUsOrShortTonUkorus,
    ounceAvoirdupois,
    pound,
    centimetre,
    metre,
    kilometre,
    inch,
    foot,
    yard,
    squareCentimetre,
    squareMetre,
    squareInch,
    squareFoot,
    squareYard,
    cubicCentimetre,
    cubicMetre,
    cubicInch,
    cubicFoot,
    cubicYard,
    millilitre,
    litre,
    hectolitre,
    secondUnitOfTime,
    minuteUnitOfTime,
    hourUnitOfTime,
    day,
    piece,
    set,
    pair,
    page,
    cycle,
    kilowattHour;

    /**
     * Returns a json representation needed for de-/serialization of the ProductStockSammDto.
     * For example: "unit:piece"
     *
     * @return the json representation as String
     */
    @JsonValue
    public String jsonRepresentation() {
        return "unit:" + this;
    }

}
