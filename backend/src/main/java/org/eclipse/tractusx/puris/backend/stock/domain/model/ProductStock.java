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
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;

import java.util.Date;

/**
 * <p>This class represents a distinct stock of products that the owner of
 * the current instance of the PURIS application has in his warehouse and
 * that is dedicated to be sent to a certain customer partner later in time</p>
 */
@Entity
@Getter
@Setter
@ToString(callSuper = true)
@DiscriminatorValue("ProductStock")
public class ProductStock extends Stock {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    private Partner allocatedToCustomerPartner;

    public ProductStock(Material material, double quantity, MeasurementUnit measurementUnit, String locationId,
                        LocationIdTypeEnum locationIdType, Date lastUpdatedOn, Partner allocatedToCustomerPartner) {
        super(material, quantity, measurementUnit, locationId, locationIdType, lastUpdatedOn);
        super.setType(DT_StockTypeEnum.PRODUCT);
        this.setAllocatedToCustomerPartner(allocatedToCustomerPartner);
    }

    public ProductStock(Material material, double quantity, MeasurementUnit measurementUnit, String atSiteBpns,
                        LocationIdTypeEnum locationIdType, Date lastUpdatedOn) {
        super(material, quantity, measurementUnit, atSiteBpns, locationIdType, lastUpdatedOn);
        super.setType(DT_StockTypeEnum.PRODUCT);
    }

    public ProductStock() {
        super();
    }

}
