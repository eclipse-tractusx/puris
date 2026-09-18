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
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.FunctionEnum;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PartSitesInformationAsPlanned {
    @NotNull
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String siteId;
 
    @NotNull
    private FunctionEnum function;
 
    @Nullable
    @Pattern(regexp = "^(?:[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?Z|[0-9]{4}-[0-9]{2}-[0-9]{2}(?:T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2}))?)$")
    private String functionValidFrom;
 
    @Nullable
    @Pattern(regexp = "^(?:[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?Z|[0-9]{4}-[0-9]{2}-[0-9]{2}(?:T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2}))?)$")
	private String functionValidUntil;
 
    @JsonCreator
    public PartSitesInformationAsPlanned(@JsonProperty(value = "siteId") String siteId,
                                         @JsonProperty(value = "function") FunctionEnum function,
                                         @JsonProperty(value = "functionValidFrom") String functionValidFrom,
                                         @JsonProperty(value = "functionValidUntil") String functionValidUntil) {
        this.siteId = siteId;
        this.function = function;
        this.functionValidFrom = functionValidFrom;
        this.functionValidUntil = functionValidUntil;
    }
 
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
 
        final PartSitesInformationAsPlanned that = (PartSitesInformationAsPlanned) o;
        return Objects.equals(siteId, that.siteId)
                && Objects.equals(function, that.function)
                && Objects.equals(functionValidFrom, that.functionValidFrom)
                && Objects.equals(functionValidUntil, that.functionValidUntil);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(siteId, function, functionValidFrom, functionValidUntil);
    }
}
