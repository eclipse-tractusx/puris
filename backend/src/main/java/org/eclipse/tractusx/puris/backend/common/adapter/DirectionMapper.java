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

package org.eclipse.tractusx.puris.backend.common.adapter;

import org.eclipse.tractusx.puris.backend.common.domain.model.DirectionEnum;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;

/**
 * This class handles the mapping between DirectionCharacteristic and DirectionEnum
 */
public class DirectionMapper {
    public static DirectionEnum toDirectionEnum(DirectionCharacteristic direction) {
        if (direction == null) {
            return null;
        }

        switch (direction) {
            case INBOUND:
                return DirectionEnum.INBOUND;
            case OUTBOUND:
                return DirectionEnum.OUTBOUND;
            default:
                throw new IllegalArgumentException(
                    "Unsupported DirectionCharacteristic: " + direction
                );
        }
    }

    public static DirectionCharacteristic toDirectionCharacteristic(DirectionEnum direction) {
        if (direction == null) {
            return null;
        }

        switch (direction) {
            case INBOUND:
                return DirectionCharacteristic.INBOUND;
            case OUTBOUND:
                return DirectionCharacteristic.OUTBOUND;
            default:
                throw new IllegalArgumentException(
                    "Unsupported DirectionEnum: " + direction
                );
        }
    }
}