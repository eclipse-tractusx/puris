/*
 * Copyright (c) 2025 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.common.util;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;

public class DuplicateEntityException extends RuntimeException {
    private final UUID conflictingId;
    private final double quantity;
    private final ItemUnitEnumeration measurementUnit;

    public DuplicateEntityException(String message,
                                    UUID conflictingId,
                                    double quantity,
                                    ItemUnitEnumeration measurementUnit) {
        super(message);
        this.conflictingId = conflictingId;
        this.quantity = quantity;
        this.measurementUnit = measurementUnit;
    }

    public UUID getConflictingId() { return conflictingId; }
    public double getQuantity() { return quantity; }
    public ItemUnitEnumeration getMeasurementUnit() { return measurementUnit; }
}
