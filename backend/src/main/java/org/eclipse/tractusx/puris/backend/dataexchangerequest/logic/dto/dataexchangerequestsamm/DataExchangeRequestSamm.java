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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
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
public class DataExchangeRequestSamm {
    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String requestId;

    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private UUID sourceDisruptionId;

    @NotNull
    private CriticalityEnumeration criticality;

    @NotNull
    private Date desiredStartDateTime;

    @NotNull
    private Date desiredEndDateTime;

    @NotEmpty
    private List<@NotNull RequestedTypeEnumeration> requestedTypes;

    @NotBlank
    private String text;

    @NotNull
    private Date timestamp;

    @JsonCreator
    public DataExchangeRequestSamm(
            @JsonProperty(value = "requestId") String requestId,
            @JsonProperty(value = "sourceDisruptionId") UUID sourceDisruptionId,
            @JsonProperty(value = "criticality") CriticalityEnumeration criticality,
            @JsonProperty(value = "desiredStartDateTime") Date desiredStartDateTime,
            @JsonProperty(value = "desiredEndDateTime") Date desiredEndDateTime,
            @JsonProperty(value = "requestedTypes") List<RequestedTypeEnumeration> requestedTypes,
            @JsonProperty(value = "text") String text,
            @JsonProperty(value = "timestamp") Date timestamp) {
        this.requestId = requestId;
        this.sourceDisruptionId = sourceDisruptionId;
        this.criticality = criticality;
        this.desiredStartDateTime = desiredStartDateTime;
        this.desiredEndDateTime = desiredEndDateTime;
        this.requestedTypes = requestedTypes;
        this.text = text;
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
        final DataExchangeRequestSamm that = (DataExchangeRequestSamm) o;
        return Objects.equals(requestId, that.requestId)
                && Objects.equals(sourceDisruptionId, that.sourceDisruptionId)
                && Objects.equals(criticality, that.criticality)
                && Objects.equals(desiredStartDateTime, that.desiredStartDateTime)
                && Objects.equals(desiredEndDateTime, that.desiredEndDateTime)
                && Objects.equals(requestedTypes, that.requestedTypes)
                && Objects.equals(text, that.text)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, sourceDisruptionId, criticality, desiredStartDateTime, desiredEndDateTime, requestedTypes, text, timestamp);
    }
}
