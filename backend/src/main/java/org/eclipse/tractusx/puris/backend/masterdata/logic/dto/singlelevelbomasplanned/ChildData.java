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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ChildData {

    @NotNull
    private Date createdOn;

    @NotNull
    @Valid
    private ItemQuantity quantity;

    @Nullable
    private Date lastModifiedOn;

    @Nullable
    @Valid
    private ValidityPeriodEntity validityPeriod;

    @NotNull
    @Pattern(regexp = PatternStore.BPNL_STRING)
    private String businessPartner;

    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String catenaXId;

    @JsonCreator
    public ChildData(@JsonProperty(value = "createdOn") Date createdOn,
                     @JsonProperty(value = "quantity") ItemQuantity quantity,
                     @JsonProperty(value = "lastModifiedOn") Date lastModifiedOn,
                     @JsonProperty(value = "validityPeriod") ValidityPeriodEntity validityPeriod,
                     @JsonProperty(value = "businessPartner") String businessPartner,
                     @JsonProperty(value = "catenaXId") String catenaXId) {
        this.createdOn = createdOn;
        this.quantity = quantity;
        this.lastModifiedOn = lastModifiedOn;
        this.validityPeriod = validityPeriod;
        this.businessPartner = businessPartner;
        this.catenaXId = catenaXId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ChildData that = (ChildData) o;
        return Objects.equals(createdOn, that.createdOn)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(lastModifiedOn, that.lastModifiedOn)
                && Objects.equals(validityPeriod, that.validityPeriod)
                && Objects.equals(businessPartner, that.businessPartner)
                && Objects.equals(catenaXId, that.catenaXId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdOn, quantity, lastModifiedOn, validityPeriod, businessPartner, catenaXId);
    }
}
