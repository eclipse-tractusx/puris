/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.irs.domain.model;

import java.time.Instant;
import java.util.UUID;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a single outbound IRS HTTP call, queued for asynchronous dispatch and retry by
 * {@code IrsRequestQueueWorker}. {@link #type} and {@link #linkedEntityUuid} identify the entity
 * this request originated from and whose status is updated once this request reaches a terminal
 * outcome; the worker looks that entity up by type rather than holding a direct association to it.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@ToString
public class IrsQueuedRequest {

    @Id
    @GeneratedValue
    private UUID uuid;

    private String method;

    private String path;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String body;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String queryParams;

    @Enumerated(EnumType.STRING)
    private IrsQueuedRequestStatusEnumeration status;

    private int attemptCount;

    private int maxAttempts;

    private Instant nextAttemptAt;

    @Nullable
    private Instant lastAttemptAt;

    private Instant createdAt;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Enumerated(EnumType.STRING)
    private IrsQueuedRequestTypeEnumeration type;

    @Nullable
    private UUID linkedEntityUuid;
}
