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

import jakarta.persistence.*;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;

import java.util.*;

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
    private String name;
    private String edcUrl;
    private String bpnl;
    @ElementCollection
    private Set<Address> addresses = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL)
    private Set<Site> sites = new HashSet<>();

    @OneToMany(mappedBy = "partner")
    private Set<MaterialPartnerRelation> materialPartnerRelations;

    @OneToMany
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<ProductStock> allocatedProductStocksForCustomer = new ArrayList<>();

    @OneToMany
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<PartnerProductStock> partnerProductStocks = new ArrayList<>();

    public Partner(String name, String edcUrl, String bpnl, String siteBpns, String siteName, String siteBpna, String streetAndNumber,
                   String zipCodeAndCity, String country) {
        this.name = name;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        Site site = new Site(siteBpns, siteName, siteBpna, streetAndNumber, zipCodeAndCity, country);
        sites.add(site);
        addresses.add(site.getAddresses().stream().findFirst().get());
    }

    public Partner(String name, String edcUrl, String bpnl, String bpna, String streetAndNumber, String zipCodeAndCity, String country) {
        this.name = name;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        addresses.add(new Address(bpna, streetAndNumber, zipCodeAndCity, country));
    }

    public Partner(String name, String edcUrl, String bpnl, String bpna, String geoCoordinates) {
        this.name = name;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        addresses.add(new Address(bpna, geoCoordinates));
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
