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
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned;

import java.util.Date;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ValidityPeriodEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;
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
public class ParentData {
    @NotNull
    @Valid
    private ItemQuantityEntity quantity;
 
    @NotNull
    private Date createdOn;
 
    @NotNull
    @Pattern(regexp = PatternStore.BPNL_STRING)
    private String businessPartner;
 
    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String globalAssetId;
 
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Date lastModifiedOn;
 
    @Nullable
    @Valid
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ValidityPeriodEntity validityPeriod;
 
    @JsonCreator
    public ParentData(@JsonProperty(value = "quantity") ItemQuantityEntity quantity,
                      @JsonProperty(value = "createdOn") Date createdOn,
                      @JsonProperty(value = "businessPartner") String businessPartner,
                      @JsonProperty(value = "globalAssetId") String globalAssetId,
                      @JsonProperty(value = "lastModifiedOn") Date lastModifiedOn,
                      @JsonProperty(value = "validityPeriod") ValidityPeriodEntity validityPeriod) {
        this.quantity = quantity;
        this.createdOn = createdOn;
        this.businessPartner = businessPartner;
        this.globalAssetId = globalAssetId;
        this.lastModifiedOn = lastModifiedOn;
        this.validityPeriod = validityPeriod;
    }
 
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
 
        final ParentData that = (ParentData) o;
        return Objects.equals(quantity, that.quantity)
                && Objects.equals(createdOn, that.createdOn)
                && Objects.equals(businessPartner, that.businessPartner)
                && Objects.equals(globalAssetId, that.globalAssetId)
                && Objects.equals(lastModifiedOn, that.lastModifiedOn)
                && Objects.equals(validityPeriod, that.validityPeriod);
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(quantity, createdOn, businessPartner, globalAssetId, lastModifiedOn, validityPeriod);
    }
}
