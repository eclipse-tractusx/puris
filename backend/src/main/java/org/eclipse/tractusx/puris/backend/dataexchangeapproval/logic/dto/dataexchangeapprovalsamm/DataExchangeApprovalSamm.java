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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
@ToString
public class DataExchangeApprovalSamm {
    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String approvalId;

    @NotNull
    @JsonProperty("isFinalized")
    private boolean finalized;

    private Date timestamp;

    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String dataExchangeRequestId;

    @NotEmpty
    private List<RequestedTypeEnumeration> approvedTypes;

    @JsonCreator
    public DataExchangeApprovalSamm(
            @JsonProperty(value = "approvalId") String approvalId,
            @JsonProperty(value = "dataExchangeRequestId") String dataExchangeRequestId,
            @JsonProperty(value = "isFinalized") boolean isFinalized,
            @JsonProperty(value = "approvedTypes") List<RequestedTypeEnumeration> approvedTypes,
            @JsonProperty(value = "timestamp") Date timestamp) {
        this.approvalId = approvalId;
        this.dataExchangeRequestId = dataExchangeRequestId;
        this.finalized = isFinalized;
        this.approvedTypes = approvedTypes;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataExchangeApprovalSamm that = (DataExchangeApprovalSamm) o;
        return Objects.equals(approvalId, that.approvalId)
                && Objects.equals(dataExchangeRequestId, that.dataExchangeRequestId)
                && Objects.equals(finalized, that.finalized)
                && Objects.equals(approvedTypes, that.approvedTypes)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(approvalId, dataExchangeRequestId, finalized, approvedTypes, timestamp);
    }
}
