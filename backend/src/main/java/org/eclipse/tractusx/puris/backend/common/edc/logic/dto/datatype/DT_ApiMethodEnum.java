/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype;

/**
 * Type for asset.property.apimethod of an EDC Asset
 */
public enum DT_ApiMethodEnum {
    /**
     * API is used to perform a request (Request API).
     */
    REQUEST("product-stock-request-api", "Product Stock Request Api", "request"),

    /**
     * API is used to respond to a request (Response API).
     */
    RESPONSE("product-stock-response-api", "Product Stock Response Api", "response"); 

    private DT_ApiMethodEnum(String id, String name, String purpose) {
        this.ID = id;
        this.NAME = name;
        this.PURPOSE = purpose;
    }

    public final String ID;
    public final String NAME;
    public final String PURPOSE;
}
