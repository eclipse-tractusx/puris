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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
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
public class AggregatedMaterialData {
    @Id
    @GeneratedValue
    protected UUID uuid;

    @ManyToOne()
    @JoinColumn(name = "material_ownMaterialNumber")
    @ToString.Exclude
    @NotNull
    protected Material material;

    @OneToMany(mappedBy = "aggregatedMaterialData", cascade = CascadeType.ALL)
    @SQLRestriction("parent_node_id is null")
    @ToString.Exclude
    protected List<AggregatedMaterialDataNode> childMaterialData = new ArrayList<>();
}
