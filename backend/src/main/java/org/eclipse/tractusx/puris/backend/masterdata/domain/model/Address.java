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
package org.eclipse.tractusx.puris.backend.masterdata.domain.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>An Address either specifies the usual properties like street, house number,
 * zip code, city and country. Or instead of these it may just contain the
 * geographical coordinates, specified by longitude and latitude, like
 * for example "51.524784N, 7.443659E"</p>
 * <p>All Addresses are uniquely identified by their BPNA</p>
 */
@Embeddable
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Address {

    private String bpna;

    private String streetAndNumber;
    private String zipCodeAndCity;
    private String country;

    private String geoCoordinates;

    public Address(String bpna, String streetAndNumber, String zipCodeAndCity, String country) {
        this.bpna = bpna;
        this.streetAndNumber = streetAndNumber;
        this.zipCodeAndCity = zipCodeAndCity;
        this.country = country;
    }

    public Address(String bpna, String geoCoordinates) {
        this.bpna = bpna;
        this.geoCoordinates = geoCoordinates;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Address) {
            return bpna.equals(((Address) obj).bpna);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return bpna.hashCode();
    }
}
