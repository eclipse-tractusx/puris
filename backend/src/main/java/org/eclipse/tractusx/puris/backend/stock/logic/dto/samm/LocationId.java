/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.ToString;

import java.util.Objects;

/**
 * Generated class for Location ID. A location can be described by different
 * kinds of identifiers. Within Catena-X, a location can either be described by
 * a BPNS or BPNA.
 */
@ToString
public class LocationId {

    @NotNull
    private LocationIdTypeEnum locationIdType;

    @NotNull
    private String locationId;

    @JsonCreator
    public LocationId(@JsonProperty(value = "locationIdType") LocationIdTypeEnum locationIdType,
                      @JsonProperty(value = "locationId") String locationId) {
        this.locationIdType = locationIdType;
        this.locationId = locationId;
    }

    /**
     * Returns Type of Location ID
     *
     * @return {@link #locationIdType}
     */
    public LocationIdTypeEnum getLocationIdType() {
        return this.locationIdType;
    }

    /**
     * Returns Location ID
     *
     * @return {@link #locationId}
     */
    public String getLocationId() {
        return this.locationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocationId that = (LocationId) o;
        return Objects.equals(locationIdType, that.locationIdType) && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationIdType, locationId);
    }
}
