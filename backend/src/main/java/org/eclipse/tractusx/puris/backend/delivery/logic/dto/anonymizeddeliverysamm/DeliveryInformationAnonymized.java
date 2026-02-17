
/*
 * Copyright (c) 2026 Volkswagen AG
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.delivery.logic.dto.anonymizeddeliverysamm;

import java.util.HashSet;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DeliveryInformationAnonymized {
    @NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String materialGlobalAssetIdAnonymized;

    @NotNull
    private HashSet<DeliveryAnonymized> deliveries = new HashSet<>();

    @JsonCreator
    public DeliveryInformationAnonymized(@JsonProperty(value = "materialGlobalAssetIdAnonymized") String materialGlobalAssetIdAnonymized) {
        this.materialGlobalAssetIdAnonymized = materialGlobalAssetIdAnonymized;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DeliveryInformationAnonymized that = (DeliveryInformationAnonymized) o;
        return Objects.equals(materialGlobalAssetIdAnonymized, that.materialGlobalAssetIdAnonymized) && Objects.equals(deliveries, that.deliveries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveries, materialGlobalAssetIdAnonymized);
    }
}
