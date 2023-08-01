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
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;

import java.util.Date;

@Entity
@Getter
@Setter
@ToString(callSuper = true)
@DiscriminatorValue("ProductStock")
public class ProductStock extends Stock {

//    @ManyToOne
//    @MapsId("uuid")
//    @JoinColumn(name = "partner_uuid")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    private Partner allocatedToCustomerPartner;

    public ProductStock(Material material, double quantity, String atSiteBpnl, Date lastUpdatedOn, Partner allocatedToCustomerPartner) {
        super(material, quantity, atSiteBpnl, lastUpdatedOn);
        super.setType(DT_StockTypeEnum.PRODUCT);
        this.setAllocatedToCustomerPartner(allocatedToCustomerPartner);
    }

    public ProductStock(Material material, double quantity, String atSiteBpnl, Date lastUpdatedOn) {
        super(material, quantity, atSiteBpnl, lastUpdatedOn);
        super.setType(DT_StockTypeEnum.PRODUCT);
    }

    public ProductStock() {
        super();
    }

//    public void setAllocatedToCustomerPartner(Partner allocatedToCustomerPartner) {
//        this.allocatedToCustomerPartner = allocatedToCustomerPartner;
//        allocatedToCustomerPartner.getAllocatedProductStocksForCustomer().size();
//        allocatedToCustomerPartner.getAllocatedProductStocksForCustomer().add(this);
//    }
}
