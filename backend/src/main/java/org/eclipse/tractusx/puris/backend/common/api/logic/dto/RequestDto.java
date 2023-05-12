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
package org.eclipse.tractusx.puris.backend.common.api.logic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;

import java.util.UUID;

/**
 * Dto for {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.Request}
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class RequestDto {

    /**
     * This ID prevents the application from collision with external IDs, because the partner
     * creates the request when performing a Request API call.
     * <p>
     * Set only for existing {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.Request}
     */
    private UUID internalRequestUuid;

    /**
     * State of the request.
     *
     * @see DT_RequestStateEnum
     */
    @NotNull
    private DT_RequestStateEnum state;

    /**
     * Actual content of the request (or response) message.
     */
    @NotNull
    private MessageDto message;

}
