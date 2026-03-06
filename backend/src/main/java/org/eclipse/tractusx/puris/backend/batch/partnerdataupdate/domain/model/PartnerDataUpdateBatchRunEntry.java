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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunEntryStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.InformationEnum;
import org.eclipse.tractusx.puris.backend.common.domain.model.DirectionEnum;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PartnerDataUpdateBatchRunEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @NotNull
    private PartnerDataUpdateBatchRun batchRun;

    @NotNull
    @Column(nullable = false)
    private UUID partnerId;

    @NotNull
    @Column(nullable = false)
    private String ownMaterialNumber;

    @NotNull
    @Column(nullable = false)
    private String partnerBpnl;

    @NotNull
    @Column(nullable = false)
    private String partnerName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectionEnum direction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InformationEnum informationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchRunEntryStatusEnum status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
