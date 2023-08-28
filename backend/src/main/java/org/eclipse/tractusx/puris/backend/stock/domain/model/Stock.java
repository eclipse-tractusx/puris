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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;

import java.util.Date;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "stock_type")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "material_ownMaterialNumber")
    @NotNull
    private Material material;

    @NotNull
    private double quantity;

    @NotNull
    private String atSiteBpns;

    @Enumerated(EnumType.STRING)
    @NotNull
    private DT_StockTypeEnum type;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date lastUpdatedOn;

    public Stock(Material material, double quantity, String atSiteBpns, Date lastUpdatedOn) {
        this.material = material;
        this.quantity = quantity;
        this.atSiteBpns = atSiteBpns;
        this.lastUpdatedOn = lastUpdatedOn;
    }
}
