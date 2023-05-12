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

/**
 * Dto for {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.Request}
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class RequestDto extends MessageDto {

    /**
     * State of the request.
     *
     * @see DT_RequestStateEnum
     */
    @NotNull
    private DT_RequestStateEnum state;

    /**
     * Create a RequestDto from message
     *
     * @param state   state to set
     * @param message message to set
     */
    public RequestDto(DT_RequestStateEnum state, MessageDto message) {
        this.state = state;
        this.setPayload(message.getPayload());
        this.setHeader(message.getHeader());
    }
}
