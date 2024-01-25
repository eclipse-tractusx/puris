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

package org.eclipse.tractusx.puris.backend.stock.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.ItemUnitEnumeration;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class ItemStock {

    static final String ORDER_ID_REGEX = "^[a-zA-Z0-9\\-\\.]{1,255}$";

    @Id
    @GeneratedValue
    protected UUID uuid;

    @ManyToOne()
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    protected Partner partner;
    @ManyToOne()
    @JoinColumn(name = "material_ownMaterialNumber")
    @ToString.Exclude
    @NotNull
    protected Material material;

    protected double quantity;
    @NotNull
    protected ItemUnitEnumeration measurementUnit;
    @NotNull
    @Pattern(regexp = PatternStore.BPNA_STRING)
    protected String locationBpna;
    @NotNull
    @Pattern(regexp = PatternStore.BPNS_STRING)
    protected String locationBpns;
    @NotNull
    protected Date lastUpdatedOnDateTime;

    protected boolean isBlocked;
    @Pattern(regexp = ORDER_ID_REGEX)
    protected String supplierOrderId;
    @Pattern(regexp = ORDER_ID_REGEX)
    protected String customerOrderId;
    @Pattern(regexp = ORDER_ID_REGEX)
    protected String customerOrderPositionId;

    @ToString.Include
    private String material_ownMaterialNumber() {
        return material.getOwnMaterialNumber();
    }

    @ToString.Include
    private String partner_partnerBpnl() {
        return partner.getBpnl();
    }

    public String getNonNullSupplierOrderId() {
        return supplierOrderId == null ? "" : supplierOrderId;
    }

    public String getNonNullCustomerOrderId() {
        return customerOrderId == null ? "" : customerOrderId;
    }

    public String getNonNullCustomerOrderPositionId() {
        return customerOrderPositionId == null ? "" : customerOrderPositionId;
    }

}



