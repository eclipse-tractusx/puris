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

import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageContent;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;

/**
 * Service receives performs the tasks associated with the Request API.
 */
public interface RequestApiService {

    /**
     * handle the received request and respond to it
     * <p>
     * Commonly this involves:
     * <li>find the data requested in {@link MessageContent}</li>
     * <li>find the Response Api at partner via {@link org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader}</li>
     * <li>send the Response to the Response Api</li>
     *
     * @param requestDto request received
     */
    public void handleRequest(RequestDto requestDto);

}
