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
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiBusinessObjectEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_AssetTypeEnum;

/**
 * Type for asset.property.apibusinessobject of an EDC Asset
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetPropertiesDto {

    /**
     * id of the asset
     */
    @JsonProperty("asset:prop:id")
    @NotNull
    private String id;

    @JsonProperty("asset:prop:name")
    @NotNull
    private String name;

    @JsonProperty("asset:prop:contenttype")
    @NotNull
    private String contentType;

    @JsonProperty("asset:prop:usecase")
    @NotNull
    private DT_UseCaseEnum useCase;

    @JsonProperty("asset:prop:type")
    @NotNull
    private DT_AssetTypeEnum type;

    @JsonProperty("asset:prop:apibusinessobject")
    @NotNull
    private DT_ApiBusinessObjectEnum apiBusinessObject;

    @JsonProperty("asset:prop:apimethod")
    @NotNull
    private DT_ApiMethodEnum apiMethod;
}
