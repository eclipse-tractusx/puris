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
package org.eclipse.tractusx.puris.backend.masterdata.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.domain.model.Stock;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "material")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Material {

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToMany
    @JoinTable(
            name = "partner_supplies_product",
            joinColumns = @JoinColumn(name = "material_uuid"),
            inverseJoinColumns = @JoinColumn(name = "partner_uuid")
    )
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<Partner> suppliedByPartners;
    ;

    @ManyToMany
    @JoinTable(
            name = "partner_orders_product",
            joinColumns = @JoinColumn(name = "material_uuid"),
            inverseJoinColumns = @JoinColumn(name = "partner_uuid")
    )
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<Partner> orderedByPartners;

    @OneToMany(mappedBy = "uuid")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<Stock> materialOnStocks;

    /**
     * If true, then the Material is a material (input for production / something I buy).
     * <p>
     * Boolean because there could be companies (trademen company) that buy and sell the same material.
     */
    private boolean materialFlag;

    /**
     * If true, then the Material is a product (output of production / something I sell.
     * <p>
     * Boolean because there could be companies (trademen company) that buy and sell the same material.
     */
    private boolean productFlag;

    private String materialNumberCustomer;

    private String materialNumberSupplier;

    private String materialNumberCx;

    private String name;

}
