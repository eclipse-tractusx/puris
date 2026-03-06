/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunEntryStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.InformationEnum;
import org.eclipse.tractusx.puris.backend.common.domain.model.DirectionEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDataUpdateBatchRunEntryDto {
    @NotNull
    private UUID id;

    @NotNull
    private UUID materialId;

    @NotNull
    private UUID partnerId;

    @NotNull
    private String ownMaterialNumber;

    @NotNull
    private String partnerBpnl;

    @NotNull
    private String partnerName;

    @NotNull
    private DirectionEnum direction;

    @NotNull
    private InformationEnum informationType;

    @NotNull
    private BatchRunEntryStatusEnum status;

    private String errorMessage;
}

