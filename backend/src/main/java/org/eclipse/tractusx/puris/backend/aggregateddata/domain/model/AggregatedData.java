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
package org.eclipse.tractusx.puris.backend.aggregateddata.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedAnonymizedDelivery;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedAnonymizedProduction;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedAnonymizedStock;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class AggregatedData {
    @Id
    @GeneratedValue
    protected UUID uuid;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "aggregated_data_uuid")
    @Valid
    protected Set<ReportedAnonymizedProduction> productions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "aggregated_data_uuid")
    @Valid
    protected Set<ReportedAnonymizedDelivery> deliveries = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "aggregated_data_uuid")
    @Valid
    protected Set<ReportedAnonymizedStock> stocks = new HashSet<>();

    @OneToMany(mappedBy = "parentData")
    protected List<ChildAggregatedData> childData;
}
