/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;


/**
 * <p>An Address either specifies the usual properties like street, house number,
 * zip code, city and country.</p>
 * <p>All Addresses are uniquely identified by their BPNA</p>
 */
@Embeddable
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Address implements Comparable<Address> {

    /**
     * The BPNA of this Address.
     */
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String bpna;

    /**
     * The street and house number of this Address.
     */
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String streetAndNumber;
    /**
     * The zip code and city of this Address.
     */
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String zipCodeAndCity;
    /**
     * The country of this Address.
     */
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String country;

    /**
     * Use this constructor to generate a new Address, consisting of the BPNA, street and
     * number, zip code and city and country.
     *
     * @param bpna to identify the address
     * @param streetAndNumber to identify the address
     * @param zipCodeAndCity to identify the city
     * @param country to identify the country
     */
    public Address(String bpna, String streetAndNumber, String zipCodeAndCity, String country) {
        this.bpna = bpna;
        this.streetAndNumber = streetAndNumber;
        this.zipCodeAndCity = zipCodeAndCity;
        this.country = country;
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

    @Override
    public int compareTo(Address o) {
        return bpna.compareTo(o.bpna);
    }
}
