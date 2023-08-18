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
package org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype;

import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockResponse;

/**
 * Enum to track the status of
 * {@link ProductStockRequest} and
 * {@link ProductStockResponse}.
 */
public enum DT_RequestStateEnum {
    /**
     * The consumer requested something.
     */
    REQUESTED("Requested"),

    /**
     * The provider receipt the request of the consumer.
     */
    RECEIPT("Received"),

    /**
     * The provider works on fulfilling the requested service or data.
     */
    WORKING("Working"),

    /**
     * The provider fulfilled the service and sent the data to the consumer.
     */
    COMPLETED("Completed"),

    /**
     * An error occured between the start and completion of a request.
     */
    ERROR("Error");

    final public String STATUSTEXT;
    private DT_RequestStateEnum(String statustext) {
        this.STATUSTEXT = statustext;
    }

}
