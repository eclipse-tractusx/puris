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

import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service providing the interface to the {@link Request}
 */
@Service
public interface RequestService {

    /**
     * create a {@link Request} in state REQUESTED.
     *
     * @param request request to persist
     * @return created entity
     */
    public Request createRequest(Request request);

    /**
     * update existing {@link Request}
     * <p>
     * Response must already have been persisted before (has internalUuid).
     *
     * @param request existing request incl. updates
     * @return updated entity or null, if response has not yet been persisted.
     */
    public Request updateRequest(Request request);

    /**
     * convenience method to only update the state of a {@link Request}
     *
     * @param request existing response to update the state of
     * @param state   state to set
     * @return updated entity or null, if response has not yet been persisted.
     */
    public Request updateState(Request request, DT_RequestStateEnum state);

    /**
     * find {@link Request} by internal uuid
     *
     * @param requestInternalUuid internalUuid of the response
     * @return found response or null, if response does not exist
     */
    public Request findByInternalUuid(UUID requestInternalUuid);

    /**
     * find {@link Request} by the header's uuid (set by sender)
     *
     * @param headerUuid requestUuid set by the sender in the header
     * @return found response or null if response does not exist
     */
    public Request findRequestByHeaderUuid(UUID headerUuid);

}
