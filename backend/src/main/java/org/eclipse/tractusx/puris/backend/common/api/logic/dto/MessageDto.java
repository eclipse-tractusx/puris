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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dto for {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.Message}
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class MessageDto {

    /**
     * Technical identifier for a {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.Message}.
     * <p>
     * Only set for existing entities.
     */
    @JsonIgnore
    private UUID uuid;

    /**
     * Steering information of a {@link ProductStockRequest} or {@link Response} api message.
     */
    @NotNull
    @JsonProperty("headers")
    private MessageHeaderDto header;

    /**
     * List of actual content of the payload.
     * <p>
     * May contain also errors.
     */
    @NotNull
    @JsonProperty("payload")
    private List<MessageContentDto> payload = new ArrayList<>();
}
