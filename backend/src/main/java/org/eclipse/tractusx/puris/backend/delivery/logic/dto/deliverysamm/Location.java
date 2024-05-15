
/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm;

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
public class Location {
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String bpnaProperty;

    @NotNull
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String bpnsProperty;

    @JsonCreator
    public Location(@JsonProperty(value = "bpnaProperty") String bpnaProperty,
            @JsonProperty(value = "bpnsProperty") String bpnsProperty) {
        this.bpnaProperty = bpnaProperty;
        this.bpnsProperty = bpnsProperty;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Location that = (Location) o;
        return Objects.equals(bpnaProperty, that.bpnaProperty) && Objects.equals(bpnsProperty, that.bpnsProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bpnaProperty, bpnsProperty);
    }
}
