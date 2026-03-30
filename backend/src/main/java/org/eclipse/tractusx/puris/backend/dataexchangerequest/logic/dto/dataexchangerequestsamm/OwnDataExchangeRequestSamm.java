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

import java.sql.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;

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
@ToString(callSuper = true)
public class OwnDataExchangeRequestSamm extends DataExchangeRequestSamm {
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String relatedDataExchangeRequestId;

    @JsonCreator
    public OwnDataExchangeRequestSamm(
            @JsonProperty(value = "notificationId") UUID notificationId,
            @JsonProperty(value = "criticality") CriticalityEnumeration criticality,
            @JsonProperty(value = "desiredStartDateTime") Date desiredStartDateTime,
            @JsonProperty(value = "desiredEndDateTime") Date desiredEndDateTime,
            @JsonProperty(value = "requestedTypes") List<RequestedTypeEnumeration> requestedTypes,
            @JsonProperty(value = "text") String text,
            @JsonProperty(value = "timestamp") Date timestamp,
            @JsonProperty(value = "relatedDataExchangeRequestId") String relatedDataExchangeRequestId) {
        super(notificationId, criticality, desiredStartDateTime, desiredEndDateTime, requestedTypes, text, timestamp);
        this.relatedDataExchangeRequestId = relatedDataExchangeRequestId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final OwnDataExchangeRequestSamm that = (OwnDataExchangeRequestSamm) o;
        return Objects.equals(relatedDataExchangeRequestId, that.relatedDataExchangeRequestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relatedDataExchangeRequestId);
    }
}
