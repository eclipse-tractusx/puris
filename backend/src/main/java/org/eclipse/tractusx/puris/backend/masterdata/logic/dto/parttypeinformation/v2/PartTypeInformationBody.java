/*
Copyright (c) 2026 Volkswagen AG

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
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.v2;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Objects;
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PartTypeInformationBody {
    private LinkedHashSet<Classification> partClassification = new LinkedHashSet<>();
 
    @NotNull
    private String nameAtManufacturer;
 
    @NotNull
    private String manufacturerPartId;
 
    @JsonCreator
    public PartTypeInformationBody(@JsonProperty(value = "partClassification") LinkedHashSet<Classification> partClassification,
                                   @JsonProperty(value = "nameAtManufacturer") String nameAtManufacturer,
                                   @JsonProperty(value = "manufacturerPartId") String manufacturerPartId) {
        this.partClassification = partClassification;
        this.nameAtManufacturer = nameAtManufacturer;
        this.manufacturerPartId = manufacturerPartId;
    }
 
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
 
        final PartTypeInformationBody that = (PartTypeInformationBody) o;
        return Objects.equals(partClassification, that.partClassification)
                && Objects.equals(nameAtManufacturer, that.nameAtManufacturer)
                && Objects.equals(manufacturerPartId, that.manufacturerPartId);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(partClassification, nameAtManufacturer, manufacturerPartId);
    }
}
