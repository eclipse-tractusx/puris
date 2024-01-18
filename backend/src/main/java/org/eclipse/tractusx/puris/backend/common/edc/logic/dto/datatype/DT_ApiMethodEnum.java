/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype;

/**
 * Type for asset.property.apimethod of an EDC Asset
 */
public enum DT_ApiMethodEnum {
    /**
     * API is used to perform a request (Request API).
     */
    REQUEST("Asset to request stock information", "request",
        "ItemStockRequestApi", "data.res.itemStockRequestApi", "Item Stock Request Status API Endpoint"),

    /**
     * API is used to respond to a request (Response API).
     */
    RESPONSE("Asset to receive stock information", "response",
        "ItemStockResponseApi", "data.res.itemStockResponseApi", "Item Stock Response API Endpoint"),

    STATUS_REQUEST("Asset to receive status requests regarding a previous request", "status-request",
        "ItemStockRequestStatusApi", "data.res.itemStockRequestStatusApi", "Item Stock Request Status API Endpoint");

    private DT_ApiMethodEnum(String name, String purpose, String cxTaxo, String type, String description) {
        this.NAME = name;
        this.PURPOSE = purpose;
        this.CX_TAXO = cxTaxo;
        this.TYPE = type;
        this.DESCRIPTION = description;
    }


    public final String NAME;
    public final String PURPOSE;
    public final String CX_TAXO;
    public final String TYPE;

    public final String DESCRIPTION;
}
