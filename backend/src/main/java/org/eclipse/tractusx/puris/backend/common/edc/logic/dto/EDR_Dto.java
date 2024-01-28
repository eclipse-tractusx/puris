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

package org.eclipse.tractusx.puris.backend.common.edc.logic.dto;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;

/**
 * An internal, immutable Dto class used by the {@link EndpointDataReferenceService}
 * It contains an authKey, authCode and endpoint.
 *
 * @param authKey  This defines the key, under which the
 *                 authCode is to be sent to the data plane.
 *                 For example: "Authorization"
 * @param authCode This is the secret key to be sent
 *                 to the data plane.
 * @param endpoint The address of the data plane that has
 *                 to handle the consumer pull.
 */
public record EDR_Dto(String authKey, String authCode, String endpoint) {
}
