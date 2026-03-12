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
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
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
public class SingleLevelBomAsPlanned {

    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String catenaXId;

    @Valid
    private Set<ChildData> childItems = new HashSet<>();

    @JsonCreator
    public SingleLevelBomAsPlanned(@JsonProperty(value = "catenaXId") String catenaXId,
            @JsonProperty(value = "childItems") Set<ChildData> childItems) {
        this.catenaXId = catenaXId;
        this.childItems = childItems;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SingleLevelBomAsPlanned that = (SingleLevelBomAsPlanned) o;
        return Objects.equals(catenaXId, that.catenaXId)
                && Objects.equals(childItems, that.childItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catenaXId, childItems);
    }
}
