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

import java.util.Date;
import java.util.Objects;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TransitEvent {

    @NotNull
    private Date dateTimeOfEvent;

    @NotNull
    private EventTypeEnumeration eventType;

    @JsonCreator
    public TransitEvent(@JsonProperty(value = "dateTimeOfEvent") Date dateTimeOfEvent,
            @JsonProperty(value = "eventType") EventTypeEnumeration eventType) {
        this.dateTimeOfEvent = dateTimeOfEvent;
        this.eventType = eventType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TransitEvent that = (TransitEvent) o;
        return Objects.equals(dateTimeOfEvent, that.dateTimeOfEvent) && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateTimeOfEvent, eventType);
    }
}
