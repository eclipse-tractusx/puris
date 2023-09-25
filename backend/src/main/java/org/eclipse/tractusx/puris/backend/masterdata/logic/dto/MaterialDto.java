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
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto;

import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.StockDto;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
/**
 *  This Dto class is being used within the ProductStockSammDto.
 */
public class MaterialDto implements Serializable {

    private UUID uuid;


    /**
     * If true, then the Material is a material (input for production / something I buy).
     * <p>
     * Boolean because there could be companies (tradesmen company) that buy and sell the same material.
     */
    private boolean materialFlag;

    /**
     * If true, then the Material is a product (output of production / something I sell).
     * <p>
     * Boolean because there could be companies (tradesmen company) that buy and sell the same material.
     */
    private boolean productFlag;

    private String materialNumberCustomer;

    private String materialNumberSupplier;

    private String materialNumberCx;

    private String name;

    public MaterialDto(boolean materialFlag, boolean productFlag, String materialNumberCustomer, String materialNumberSupplier, String materialNumberCx, String name) {
        super();
        this.materialFlag = materialFlag;
        this.productFlag = productFlag;
        this.materialNumberCustomer = materialNumberCustomer;
        this.materialNumberSupplier = materialNumberSupplier;
        this.materialNumberCx = materialNumberCx;
        this.name = name;
    }



}
