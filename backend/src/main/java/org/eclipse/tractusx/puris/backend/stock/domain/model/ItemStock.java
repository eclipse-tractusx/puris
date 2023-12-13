/*
 * Copyright (c) 2023 Volkswagen AG
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemUnitEnumeration;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@NoArgsConstructor
@Entity
@ToString(onlyExplicitlyIncluded = true)
public class ItemStock {

    @Getter
    @EmbeddedId
    @JsonIgnore
    private Key key = new Key();
    private Date lastUpdatedOnDateTime;
    private QuantityOnAllocatedStock quantityOnAllocatedStock = new QuantityOnAllocatedStock();
    @Getter
    @Setter
    @ToString.Include
    private boolean isBlocked;
    private String supplierOrderId;
    private String customerOrderId;
    private String customerOrderPositionId;

    @JsonIgnore
    public Partner getPartner() {
        return key.partner;
    }
    @ToString.Include
    public String getPartnerBpnl(){
        return key.partner.getBpnl();
    }
    @JsonIgnore
    public Material getMaterial() {
        return key.material;
    }
    @ToString.Include
    public String getOwnMaterialNumber() {
        return key.material.getOwnMaterialNumber();
    }

    @ToString.Include
    public DirectionCharacteristic getDirection() {
        return key.direction;
    }
    @ToString.Include
    public String getSupplierOrderId() {
        return supplierOrderId;
    }
    @ToString.Include
    public String getCustomerOrderId() {
        return customerOrderId;
    }
    @ToString.Include
    public String getCustomerOrderPositionId() {
        return customerOrderPositionId;
    }
    @ToString.Include
    public String getLocationBpna() {
        return key.locationBpna;
    }
    @ToString.Include
    public String getLocationBpns() {
        return key.locationBpns;
    }
    @ToString.Include
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

        @ManyToOne
        @MapsId("uuid")
        @JoinColumn(name = "partner_uuid")
        private Partner partner;
        @ManyToOne
        @MapsId("ownMaterialNumber")
        @JoinColumn(name = "material_ownMaterialNumber")
        private Material material;
        private DirectionCharacteristic direction;
        private String locationBpna;
        private String locationBpns;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(partner, key.partner) && Objects.equals(material, key.material)
                && direction == key.direction && Objects.equals(locationBpna, key.locationBpna) &&
                Objects.equals(locationBpns, key.locationBpns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(partner, material,
                direction, locationBpna, locationBpns);
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

        private Partner partner;
        private Material material;
        private DirectionCharacteristic direction;
        private String supplierOrderId;
        private String customerOrderId;
        private String customerOrderPositionId;
        private String locationBpna;
        private String locationBpns;
        private Date lastUpdatedOnDateTime;
        private final QuantityOnAllocatedStock quantityOnAllocatedStock = new QuantityOnAllocatedStock();
        private boolean isBlocked;

        public Builder partner(Partner partner) {
            this.partner = partner;
            return this;
        }

        public Builder material(Material material) {
            this.material = material;
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
            itemStock.key.partner = partner;
            itemStock.key.material = material;
            itemStock.key.direction = direction;
            itemStock.supplierOrderId = supplierOrderId;
            itemStock.customerOrderId = customerOrderId;
            itemStock.customerOrderPositionId = customerOrderPositionId;
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
