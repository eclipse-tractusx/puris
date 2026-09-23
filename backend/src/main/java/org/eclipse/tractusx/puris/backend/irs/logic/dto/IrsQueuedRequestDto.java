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
package org.eclipse.tractusx.puris.backend.irs.logic.dto;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Read-only representation of an {@link IrsQueuedRequest} as it is served to the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class IrsQueuedRequestDto {
 
    private UUID uuid;
 
    private IrsQueuedRequestMethodEnumeration method;
 
    @Pattern(regexp = PatternStore.RELATIVE_PATH_STRING)
    private String path;
 
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String queryParams;
 
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String body;
 
    private IrsQueuedRequestStatusEnumeration status;
 
    private int attemptCount;
 
    private int maxAttempts;
 
    private Instant nextAttemptAt;
 
    private Instant lastAttemptAt;
 
    private Instant createdAt;
 
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String lastErrorMessage;
 
    private IrsQueuedRequestTypeEnumeration type;
 
    private UUID linkedEntityUuid;
}