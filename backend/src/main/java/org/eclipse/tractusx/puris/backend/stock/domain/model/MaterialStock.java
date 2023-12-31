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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;

import java.util.Date;

/**
 * <p>This class represents a distinct stock of a certain material, that the owner of
 * the current instance of the PURIS application has in his own warehouse.</p>
 *
 */
@Entity
@DiscriminatorValue("MaterialStock")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class MaterialStock extends Stock {

    public MaterialStock(Material material, double quantity, MeasurementUnit measurementUnit, String locationId,
                         LocationIdTypeEnum locationIdType,
                         Date lastUpdatedOn) {
        super(material, quantity, measurementUnit, locationId, locationIdType, lastUpdatedOn);
        super.setType(DT_StockTypeEnum.MATERIAL);
    }

}
