/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.stock.logic.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class StockDto implements Serializable {

    private UUID uuid;

    private MaterialDto material;

    private double quantity;

    private ItemUnitEnumeration measurementUnit;

    private String stockLocationBpns;

    private String stockLocationBpna;

    private String customerOrderNumber;

    private String customerOrderPositionNumber;

    private String supplierOrderNumber;

    private LocationIdTypeEnum locationIdType;

    private DT_StockTypeEnum type;

    private Date lastUpdatedOn;

    private PartnerDto partner;

    @JsonProperty("isBlocked")
    private boolean isBlocked;

    public StockDto(MaterialDto material, double quantity, ItemUnitEnumeration measurementUnit, String stockLocationBpns,
                    String stockLocationBpna, Date lastUpdatedOn, PartnerDto partner, boolean isBlocked) {
        this.material = material;
        this.quantity = quantity;
        this.measurementUnit = measurementUnit;
        this.stockLocationBpns = stockLocationBpns;
        this.stockLocationBpna = stockLocationBpna;
        this.lastUpdatedOn = lastUpdatedOn;
        this.partner = partner;
        this.customerOrderNumber = null;
        this.customerOrderPositionNumber = null;
        this.supplierOrderNumber = null;
        this.isBlocked = isBlocked;
    }

    public StockDto(MaterialDto material, double quantity, ItemUnitEnumeration measurementUnit, String stockLocationBpns, String stockLocationBpna, String customerOrderNumber, String customerOrderPositionNumber, String supplierOrderNumber, Date lastUpdatedOn, PartnerDto partner, boolean isBlocked) {
        this.material = material;
        this.quantity = quantity;
        this.measurementUnit = measurementUnit;
        this.stockLocationBpns = stockLocationBpns;
        this.stockLocationBpna = stockLocationBpna;
        this.partner = partner;
        this.customerOrderNumber = customerOrderNumber;
        this.customerOrderPositionNumber = customerOrderPositionNumber;
        this.supplierOrderNumber = supplierOrderNumber;
        this.lastUpdatedOn = lastUpdatedOn;
        this.isBlocked = isBlocked;
    }
}
