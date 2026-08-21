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

/**
 * Identifies the kind of entity an {@link IrsQueuedRequest} originated from and whose status is
 * updated once the request reaches a terminal outcome.
 */
public enum IrsQueuedRequestTypeEnumeration {
    POLICY_CREATE,
    CHAIN_OPENING_ROOT_GRANT_CREATE,
    CHAIN_OPENING_ROOT_GRANT_UPDATE,
    CHAIN_OPENING_ROOT_GRANT_DELETE,
    CHAIN_OPENING_PARTNER_GRANT_CREATE,
    CHAIN_OPENING_PARTNER_GRANT_UPDATE,
    CHAIN_OPENING_PARTNER_GRANT_DELETE
}
