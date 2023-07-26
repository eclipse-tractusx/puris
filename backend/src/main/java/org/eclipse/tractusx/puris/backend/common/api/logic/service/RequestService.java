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
package org.eclipse.tractusx.puris.backend.common.api.logic.service;

import org.eclipse.tractusx.puris.backend.common.api.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service providing the interface to the {@link ProductStockRequest}
 */
@Service
public interface RequestService {

    /**
     * create a {@link ProductStockRequest} in state REQUESTED.
     *
     * @param productStockRequest request to persist
     * @return created entity
     */
    public ProductStockRequest createRequest(ProductStockRequest productStockRequest);

    /**
     * update existing {@link ProductStockRequest}
     * <p>
     * Request must already have been persisted before (has internalUuid).
     *
     * @param productStockRequest existing request incl. updates
     * @return updated entity or null, if request has not yet been persisted.
     */
    public ProductStockRequest updateRequest(ProductStockRequest productStockRequest);

    /**
     * convenience method to only update the state of a {@link ProductStockRequest}
     *
     * @param productStockRequest existing request to update the state of
     * @param state   state to set
     * @return updated entity or null, if request has not yet been persisted.
     */
    public ProductStockRequest updateState(ProductStockRequest productStockRequest, DT_RequestStateEnum state);

    /**
     * find {@link ProductStockRequest} by internal uuid
     *
     * @param requestInternalUuid internalUuid of the request
     * @return found request or null, if request does not exist
     */
    public ProductStockRequest findByInternalUuid(UUID requestInternalUuid);

    /**
     * find {@link ProductStockRequest} by the header's uuid (set by sender)
     *
     * @param headerUuid requestUuid set by the sender in the header
     * @return found request or null if request does not exist
     */
    public ProductStockRequest findRequestByHeaderUuid(UUID headerUuid);

}
