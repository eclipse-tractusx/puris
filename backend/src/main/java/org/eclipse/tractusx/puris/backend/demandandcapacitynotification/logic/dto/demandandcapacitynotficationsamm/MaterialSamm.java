/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm;

import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class MaterialSamm {
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING) 
    private String materialGlobalAssetId;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING) 
    private String materialNumberSupplier;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING) 
    private String materialNumberCustomer;

    @JsonCreator
    public MaterialSamm(
            @JsonProperty(value = "materialGlobalAssetId") String materialGlobalAssetId,
            @JsonProperty(value = "materialNumberCustomer") String materialNumberCustomer,
            @JsonProperty(value = "materialNumberSupplier") String materialNumberSupplier) {
        this.materialGlobalAssetId = materialGlobalAssetId;
        this.materialNumberCustomer = materialNumberCustomer;
        this.materialNumberSupplier = materialNumberSupplier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaterialSamm that = (MaterialSamm) o;
        return Objects.equals(materialGlobalAssetId, that.materialGlobalAssetId)
            && Objects.equals(materialNumberCustomer, that.materialNumberCustomer)
            && Objects.equals(materialNumberSupplier, that.materialNumberSupplier);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(materialGlobalAssetId, materialNumberCustomer, materialNumberSupplier);
    }
}
