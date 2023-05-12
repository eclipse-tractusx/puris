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
package org.eclipse.tractusx.puris.backend.common.api.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception for
 * {@link org.eclipse.tractusx.puris.backend.common.api.controller.ResponseApiController}, if no
 * request exists for requestId set in header.
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "No Request for Request ID has" +
        " been found.")
public class RequestIdNotFoundException extends RuntimeException {

    /**
     * Create NotFound exception with exception message for specific request
     *
     * @param requestUuid requestUuid to set in message
     */
    public RequestIdNotFoundException(UUID requestUuid) {
        super(String.format("Request with ID %s not found.", requestUuid));
    }
}
