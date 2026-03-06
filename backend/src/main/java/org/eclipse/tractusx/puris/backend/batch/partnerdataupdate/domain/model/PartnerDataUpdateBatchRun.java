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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunStatusEnum;
import org.hibernate.annotations.Formula;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PartnerDataUpdateBatchRun {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchRunStatusEnum status;

    @OneToMany(mappedBy = "batchRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PartnerDataUpdateBatchRunEntry> entries = new HashSet<>();

    // aggregated properties used in dto
    @Formula("(SELECT COUNT(*) FROM partner_data_update_batch_run_entry e WHERE e.batch_run_id = id)")
    private int totalEntries;

    @Formula("(SELECT COUNT(*) FROM partner_data_update_batch_run_entry e WHERE e.batch_run_id = id AND e.status = 'ERROR')")
    private int totalErrorCount;

}

