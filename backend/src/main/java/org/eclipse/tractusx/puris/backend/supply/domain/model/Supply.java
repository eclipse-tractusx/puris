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

package org.eclipse.tractusx.puris.backend.supply.domain.model;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.UUID;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class Supply {
    // customer controller, supplier controller
    // in dto flatten material and partner
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

    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String stockLocationBPNA;
    
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String stockLocationBPNS;

    private Date date;
    private double daysOfSupply;

    @ToString.Include
    private String material_ownMaterialNumber() {
        return material.getOwnMaterialNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Supply that = (Supply) o;
        return this.getMaterial().getOwnMaterialNumber().equals(that.getMaterial().getOwnMaterialNumber()) &&
                this.getPartner().getUuid().equals(that.getPartner().getUuid()) &&
                this.getStockLocationBPNA().equals(that.getStockLocationBPNA()) &&
                this.getStockLocationBPNS().equals(that.getStockLocationBPNS()) &&
                this.getDate().equals(that.getDate()) &&
                this.getDaysOfSupply() == that.getDaysOfSupply();
    }

    @Override
    public int hashCode() {
        return Objects.hash(partner, material, stockLocationBPNA, stockLocationBPNS, date, daysOfSupply);
    }
}
