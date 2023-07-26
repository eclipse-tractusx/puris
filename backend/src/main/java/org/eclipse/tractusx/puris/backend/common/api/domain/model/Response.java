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
package org.eclipse.tractusx.puris.backend.common.api.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This Response represents the message received via a Response API.
 * <p>
 * This Response may not be confused with an HTTP response.
 * Both, the Response and the Request, are called (api) request.
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Response {

    @Id
    @GeneratedValue
    /**
     * Technical identifier for a Message.
     */
    private UUID uuid;

    /**
     * Steering information of a {@link Request} or {@link Response} api message.
     */
    @Embedded
    private MessageHeader header;

    /**
     * List of actual content of the payload.
     * <p>
     * May contain also errors.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageContent> payload = new ArrayList<>();

}
