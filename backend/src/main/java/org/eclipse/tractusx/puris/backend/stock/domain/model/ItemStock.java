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

package org.eclipse.tractusx.puris.backend.stock.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemUnitEnumeration;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@NoArgsConstructor
@Entity
@ToString
public class ItemStock {

    @Getter
    @EmbeddedId
    private Key key = new Key();
    private Date lastUpdatedOnDateTime;

    private QuantityOnAllocatedStock quantityOnAllocatedStock = new QuantityOnAllocatedStock();
    @Getter
    @Setter
    private boolean isBlocked;

    public String getPartnerBpnl() {
        return key.partnerBpnl;
    }

    public String getMaterialNumberCustomer() {
        return key.materialNumberCustomer;
    }

    public String getMaterialNumberSupplier() {
        return key.materialNumberSupplier;
    }

    public String getMaterialGlobalAssetId() {
        return key.getMaterialGlobalAssetId();
    }

    public DirectionCharacteristic getDirection() {
        return key.direction;
    }

    public String getSupplierOrderId() {
        return key.supplierOrderId;
    }

    public String getCustomerOrderId() {
        return key.customerOrderId;
    }

    public String getCustomerOrderPositionId() {
        return key.customerOrderPositionId;
    }

    public String getLocationBpna() {
        return key.locationBpna;
    }

    public String getLocationBpns() {
        return key.locationBpns;
    }

    public Date getLastUpdatedOnDateTime() {
        return (Date) lastUpdatedOnDateTime.clone();
    }

    public void setLastUpdatedOnDateTime(Date lastUpdatedOnDateTime) {
        this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
    }

    public ItemUnitEnumeration getMeasurementUnit() {
        return quantityOnAllocatedStock.getMeasurementUnit();
    }

    public Double getQuantityAmount() {
        return quantityOnAllocatedStock.quantity;
    }

    public void setQuantityAmount(double quantityAmount) {
        quantityOnAllocatedStock.quantity = quantityAmount;
    }

    public void setQuantityOnAllocatedStock(QuantityOnAllocatedStock quantityOnAllocatedStock) {
        this.quantityOnAllocatedStock = quantityOnAllocatedStock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStock)) return false;
        ItemStock itemStock = (ItemStock) o;
        return key.equals(itemStock.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Embeddable
    @Getter
    @Setter
    @ToString
    public static class Key implements Serializable {

        private String partnerBpnl;
        private String materialNumberCustomer;
        private String materialNumberSupplier;
        private String materialGlobalAssetId;
        private DirectionCharacteristic direction;
        private String supplierOrderId;
        private String customerOrderId;
        private String customerOrderPositionId;
        private String locationBpna;
        private String locationBpns;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(partnerBpnl, key.partnerBpnl) && Objects.equals(materialNumberCustomer, key.materialNumberCustomer)
                && Objects.equals(materialNumberSupplier, key.materialNumberSupplier) && Objects.equals(materialGlobalAssetId,
                key.materialGlobalAssetId) && direction == key.direction && Objects.equals(supplierOrderId, key.supplierOrderId)
                && Objects.equals(customerOrderId, key.customerOrderId) && Objects.equals(customerOrderPositionId, key.customerOrderPositionId)
                && Objects.equals(locationBpna, key.locationBpna) && Objects.equals(locationBpns, key.locationBpns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(partnerBpnl, materialNumberCustomer, materialNumberSupplier, materialGlobalAssetId,
                direction, supplierOrderId, customerOrderId, customerOrderPositionId, locationBpna, locationBpns);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @ToString
    public static class QuantityOnAllocatedStock {
        private Double quantity;
        private ItemUnitEnumeration measurementUnit;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QuantityOnAllocatedStock)) return false;
            QuantityOnAllocatedStock that = (QuantityOnAllocatedStock) o;
            return Objects.equals(quantity, that.quantity) && measurementUnit == that.measurementUnit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(quantity, measurementUnit);
        }
    }

    public static class Builder {

        private Builder() {
        }

        private String partnerBpnl;
        private String materialNumberCustomer;
        private String materialNumberSupplier;
        private String materialGlobalAssetId;
        private DirectionCharacteristic direction;
        private String supplierOrderId;
        private String customerOrderId;
        private String customerOrderPositionId;
        private String locationBpna;
        private String locationBpns;
        private Date lastUpdatedOnDateTime;
        private final QuantityOnAllocatedStock quantityOnAllocatedStock = new QuantityOnAllocatedStock();
        private boolean isBlocked;

        public Builder partnerBpnl(String partnerBpnl) {
            this.partnerBpnl = partnerBpnl;
            return this;
        }

        public Builder materialNumberCustomer(String materialNumberCustomer) {
            this.materialNumberCustomer = materialNumberCustomer;
            return this;
        }

        public Builder materialNumberSupplier(String materialNumberSupplier) {
            this.materialNumberSupplier = materialNumberSupplier;
            return this;
        }

        public Builder materialGlobalAssetId(String materialGlobalAssetId) {
            this.materialGlobalAssetId = materialGlobalAssetId;
            return this;
        }

        public Builder direction(DirectionCharacteristic direction) {
            this.direction = direction;
            return this;
        }

        public Builder supplierOrderId(String supplierOrderId) {
            this.supplierOrderId = supplierOrderId;
            return this;
        }

        public Builder customerOrderId(String customerOrderId) {
            this.customerOrderId = customerOrderId;
            return this;
        }

        public Builder customerOrderPositionId(String customerOrderPositionId) {
            this.customerOrderPositionId = customerOrderPositionId;
            return this;
        }

        public Builder locationBpna(String locationBpna) {
            this.locationBpna = locationBpna;
            return this;
        }

        public Builder locationBpns(String locationBpns) {
            this.locationBpns = locationBpns;
            return this;
        }

        public Builder lastUpdatedOnDateTime(Date lastUpdatedOnDateTime) {
            this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
            return this;
        }

        public Builder measurementUnit(ItemUnitEnumeration measurementUnit) {
            quantityOnAllocatedStock.measurementUnit = measurementUnit;
            return this;
        }

        public Builder quantity(double quantityAmount) {
            quantityOnAllocatedStock.quantity = quantityAmount;
            return this;
        }

        public Builder isBlocked(boolean isBlocked) {
            this.isBlocked = isBlocked;
            return this;
        }

        public ItemStock build() {
            ItemStock itemStock = new ItemStock();
            itemStock.key.partnerBpnl = partnerBpnl;
            itemStock.key.materialNumberCustomer = materialNumberCustomer;
            itemStock.key.materialNumberSupplier = materialNumberSupplier;
            itemStock.key.materialGlobalAssetId = materialGlobalAssetId == null ? "" : materialGlobalAssetId;
            itemStock.key.direction = direction;
            itemStock.key.supplierOrderId = supplierOrderId;
            itemStock.key.customerOrderId = customerOrderId;
            itemStock.key.customerOrderPositionId = customerOrderPositionId;
            itemStock.key.locationBpna = locationBpna;
            itemStock.key.locationBpns = locationBpns;
            itemStock.quantityOnAllocatedStock = quantityOnAllocatedStock;
            itemStock.isBlocked = isBlocked;
            itemStock.lastUpdatedOnDateTime = this.lastUpdatedOnDateTime == null ? new Date() : this.lastUpdatedOnDateTime;
            return itemStock;
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
