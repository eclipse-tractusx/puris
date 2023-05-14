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

import java.util.Objects;
import java.util.Optional;

/**
 * Generated class for Reference to Order Position. Encapsulates the references
 * to identify a position within an order.
 */
public class OrderPositionReference {
    private Optional<String> supplierOrderId;

    @NotNull
    private String customerOrderId;

    @NotNull
    private String customerOrderPositionId;

    @JsonCreator
    public OrderPositionReference(@JsonProperty(value = "supplierOrderId") Optional<String> supplierOrderId,
                                  @JsonProperty(value = "customerOrderId") String customerOrderId,
                                  @JsonProperty(value = "customerOrderPositionId") String customerOrderPositionId) {
        super(

        );
        this.supplierOrderId = supplierOrderId;
        this.customerOrderId = customerOrderId;
        this.customerOrderPositionId = customerOrderPositionId;
    }

    /**
     * Returns Supplier Order ID
     *
     * @return {@link #supplierOrderId}
     */
    public Optional<String> getSupplierOrderId() {
        return this.supplierOrderId;
    }

    /**
     * Returns Customer Order ID
     *
     * @return {@link #customerOrderId}
     */
    public String getCustomerOrderId() {
        return this.customerOrderId;
    }

    /**
     * Returns Customer Order Position ID
     *
     * @return {@link #customerOrderPositionId}
     */
    public String getCustomerOrderPositionId() {
        return this.customerOrderPositionId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OrderPositionReference that = (OrderPositionReference) o;
        return Objects.equals(supplierOrderId, that.supplierOrderId)
                && Objects.equals(customerOrderId, that.customerOrderId)
                && Objects.equals(customerOrderPositionId, that.customerOrderPositionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierOrderId, customerOrderId, customerOrderPositionId);
    }
}
