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
 * Generated class for Stock Allocated to Customer. This is the quantity of
 * already produced goods at a given location that has not yet been shipped. The
 * allocated stock is the stock available for the customer and may refer to a
 * given position (see OrderPositionReference). Quantities of Materials
 * belonging to this stock are not available for other positions or customers.
 * This stock consists only of the good parts.
 */
@ToString
public class AllocatedStock {

    @NotNull
    private Quantity quantityOnAllocatedStock;

    @NotNull
    private LocationId supplierStockLocationId;

    @JsonCreator
    public AllocatedStock(@JsonProperty(value = "quantityOnAllocatedStock") Quantity quantityOnAllocatedStock,
                          @JsonProperty(value = "supplierStockLocationId") LocationId supplierStockLocationId) {
        super(

        );
        this.quantityOnAllocatedStock = quantityOnAllocatedStock;
        this.supplierStockLocationId = supplierStockLocationId;
    }

    /**
     * Returns Quantity on Allocated Stock
     *
     * @return {@link #quantityOnAllocatedStock}
     */
    public Quantity getQuantityOnAllocatedStock() {
        return this.quantityOnAllocatedStock;
    }

    /**
     * Returns Location ID of Supplier's Stock
     *
     * @return {@link #supplierStockLocationId}
     */
    public LocationId getSupplierStockLocationId() {
        return this.supplierStockLocationId;
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
                && Objects.equals(supplierStockLocationId, that.supplierStockLocationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantityOnAllocatedStock, supplierStockLocationId);
    }
}
