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
package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.ToString;

import java.util.Objects;

/**
 * Generated class for Quantity. Comprises the number of objects and the unit of
 * measurement for the respective child objects
 */
@ToString
public class Quantity {

    @NotNull
    private Double quantityNumber;

    @NotNull
    //custom: made Curie a String
    private String measurementUnit;

    @JsonCreator
    public Quantity(@JsonProperty(value = "quantityNumber") Double quantityNumber,
                    @JsonProperty(value = "measurementUnit") String measurementUnit) {
        this.quantityNumber = quantityNumber;
        this.measurementUnit = measurementUnit;
    }

    /**
     * Returns Quantity Number
     *
     * @return {@link #quantityNumber}
     */
    public Double getQuantityNumber() {
        return this.quantityNumber;
    }

    /**
     * Returns Measurement Unit
     *
     * @return {@link #measurementUnit}
     */
    public String getMeasurementUnit() {
        return this.measurementUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Quantity that = (Quantity) o;
        return Objects.equals(quantityNumber, that.quantityNumber)
                && Objects.equals(measurementUnit, that.measurementUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantityNumber, measurementUnit);
    }
}
