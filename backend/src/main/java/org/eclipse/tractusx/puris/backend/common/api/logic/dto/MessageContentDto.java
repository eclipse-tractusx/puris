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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

/**
 * Dto for {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageContent}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonTypeInfo(use = DEDUCTION) // Intended usage
@JsonSubTypes({@JsonSubTypes.Type(MessageContentErrorDto.class),
        @JsonSubTypes.Type(ProductStockSammDto.class)
})
public abstract class MessageContentDto {

    /**
     * Technical identifier for a Message Content.
     * <p>
     * Only set for existing entities.
     */
    @JsonIgnore
    private UUID uuid;
}
