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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedAnonymizedDelivery;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedAnonymizedProduction;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedAnonymizedStock;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@ToString
public class AggregatedMaterialDataNode {
    @Id
    @GeneratedValue
    protected UUID uuid;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "aggregated_material_data_id")
    @ToString.Exclude
    @JsonIgnore
    protected AggregatedMaterialData aggregatedMaterialData;

    @ManyToOne
    @JoinColumn(name = "parent_node_id")
    @ToString.Exclude
    @JsonIgnore
    protected AggregatedMaterialDataNode parentNode;

    @OneToMany(mappedBy = "parentNode", cascade = CascadeType.ALL)
    @ToString.Exclude
    protected List<AggregatedMaterialDataNode> childMaterialData;

    protected Double quantity;

    protected ItemUnitEnumeration measurementUnit;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    protected String externalMaterialNumber;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    protected String externalMaterialName;

    @OneToMany(mappedBy = "aggregatedMaterialDataNode", cascade = CascadeType.ALL)
    @Valid
    protected Set<ReportedAnonymizedProduction> productions;

    @OneToMany(mappedBy = "aggregatedMaterialDataNode", cascade = CascadeType.ALL)
    @Valid
    protected Set<ReportedAnonymizedDelivery> deliveries;

    @OneToMany(mappedBy = "aggregatedMaterialDataNode", cascade = CascadeType.ALL)
    @Valid
    protected Set<ReportedAnonymizedStock> stocks;
}
