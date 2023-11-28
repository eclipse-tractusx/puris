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
package org.eclipse.tractusx.puris.backend.common.edc.logic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_DataAddressTypeEnum;

/**
 * Type for asset.dataAddress.properties of an EDC Asset during creation ({@link CreateAssetDto})
 */
@Getter
@Setter
@NoArgsConstructor
public class DataAddressPropertiesDto {

    /**
     * address where to get the asset from
     */
    @JsonProperty("baseUrl")
    @NotNull
    private String baseUrl;

    /**
     * Allows to set other Http methods like POST, PUT, DELETE for Http Proxy
     */
    @JsonProperty("proxyMethod")
    @NotNull
    private boolean proxyMethod;

    /**
     * Allows to proxy the body through Http Proxy for e.g. an API call.
     */
    @JsonProperty("proxyBody")
    @NotNull
    private boolean proxyBody;

    /**
     * Defines the type of data asset e.g. HttpData
     */
    @JsonProperty("type")
    @NotNull
    private DT_DataAddressTypeEnum type;

    /**
     * Defines the auth key used in httpProxy e.g. "X-API-KEY"
     */
    @JsonProperty("authKey")
    @NotNull
    private String authKey;

    /**
     * Defines the auth code used in httpProxy e.g. "some value"
     */
    @JsonProperty("authCode")
    @NotNull
    private String authCode;
}
