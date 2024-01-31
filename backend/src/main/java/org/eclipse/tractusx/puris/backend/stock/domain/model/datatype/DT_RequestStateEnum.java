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
package org.eclipse.tractusx.puris.backend.stock.domain.model.datatype;

/**
 * Enum to track the status of
 * ItemStockRequestRequestMessage
 */
public enum DT_RequestStateEnum {
    /**
     * The consumer requested something.
     */
    Requested,

    /**
     * The supplier received the request of the consumer.
     */
    Received,

    /**
     * The supplier works on fulfilling the requested service or data.
     */
    Working,

    /**
     * The supplier fulfilled the service and sent the data to the consumer.
     */
    Completed,

    /**
     * An error occured between the start and completion of a request.
     */
    Error;



}
