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

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.*;

/**
 * <p>This class represents business partner entities.
 * Each Partner is required to have a BPNL number, a BPNA and
 * an EDC-URL.</p>
 *
 * <p>Each Partner may have zero or more BPNS.
 * Since each BPNS contains a BPNA, the requirement of having a BPNA
 * is fulfilled by having at least one BPNS. </p>
 * <p>If there is no BPNS, then this Partner has to have at least one
 * BPNA, that is not attached to any other BPNS. </p>
 */
@Entity
@Table(name = "partner")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Partner {

    @Id
    @GeneratedValue
    private UUID uuid;
    /**
     * A human-readable, distinctive name of this partner.
     */
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String name;
    /**
     * The EDC-URL of the partner.
     */
    @Pattern(regexp = PatternStore.URL_STRING)
    private String edcUrl;
    /**
     * The BPNL of the partner.
     */
    @Pattern(regexp = PatternStore.BPNL_STRING)
    private String bpnl;
    @ElementCollection
    @Valid
    /**
     * Contains all Addresses (BPNAs) that are directly assigned to this
     * Partner's BPNL.
     */
    private SortedSet<Address> addresses = new TreeSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @Valid
    /**
     * Contains all Sites (BPNSs) that are assigned to this
     * Partner's BPNL. Each BPNS has one or more addresses (BPNAs).
     */
    private SortedSet<Site> sites = new TreeSet<>();


    /**
     * Use this constructor to generate a new Partner with a BPNS and a BPNA attached.
     *
     * @param name            the human-readable name of this Partner
     * @param edcUrl          the edc-url of this Partner
     * @param bpnl            the BPNL of this Partner
     * @param siteBpns        the BPNS of this Partner
     * @param siteName        the name of the BPNS-site
     * @param siteBpna        the BPNA attached to the site
     * @param streetAndNumber street and number of this BPNA
     * @param zipCodeAndCity  zip code and city of this BPNA
     * @param country         country of this BPNA
     */
    public Partner(String name, String edcUrl, String bpnl, String siteBpns, String siteName, String siteBpna, String streetAndNumber,
                   String zipCodeAndCity, String country) {
        this.name = name;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        Site site = new Site(siteBpns, siteName, siteBpna, streetAndNumber, zipCodeAndCity, country);
        sites.add(site);
    }

    /**
     * Use this constructor to generate a new Partner with a BPNS and a BPNA, but no Site/BPNS.
     *
     * @param name            the human-readable name of this Partner
     * @param edcUrl          the edc-url of this Partner
     * @param bpnl            the BPNL of this Partner
     * @param bpna            the BPNA attached to the Partner
     * @param streetAndNumber street and number of this BPNA
     * @param zipCodeAndCity  zip code and city of this BPNA
     * @param country         country of this BPNA
     */
    public Partner(String name, String edcUrl, String bpnl, String bpna, String streetAndNumber, String zipCodeAndCity, String country) {
        this.name = name;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        addresses.add(new Address(bpna, streetAndNumber, zipCodeAndCity, country));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partner)) return false;
        Partner partner = (Partner) o;
        return Objects.equals(uuid, partner.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
