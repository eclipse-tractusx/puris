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
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "partner")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Partner {

    @Id
    @GeneratedValue
    private UUID uuid;
    private String name;
    @NotNull
    private boolean actsAsCustomerFlag;
    @NotNull
    private boolean actsAsSupplierFlag;
    private String edcUrl;
    private String bpnl;
    private String siteBpns;

    @ManyToMany(mappedBy = "suppliedByPartners")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<Material> suppliesMaterials;
    ;

    @ManyToMany(mappedBy = "orderedByPartners")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<Material> ordersProducts;
    ;

    @OneToMany(mappedBy = "uuid")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<ProductStock> allocatedProductStocksForCustomer;

    @OneToMany(mappedBy = "uuid")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<PartnerProductStock> partnerProductStocks;
}
