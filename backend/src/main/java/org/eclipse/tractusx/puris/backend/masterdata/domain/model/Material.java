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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "material")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Material {

    /**
     * If true, then the Material is a material (input for production / something I buy).
     * <p>
     * Boolean because there could be companies (trademen company) that buy and sell the same material.
     */
    private boolean materialFlag;

    /**
     * If true, then the Material is a product (output of production / something I sell.
     * <p>
     * Boolean because there could be companies (trademen company) that buy and sell the same material.
     */
    private boolean productFlag;

    @Id
    private String ownMaterialNumber;

    private String materialNumberCx;

    private String name;

    @OneToMany(mappedBy = "material")
    Set<MaterialPartnerRelation> materialPartnerRelations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Material)) return false;
        Material material = (Material) o;
        return Objects.equals(ownMaterialNumber, material.ownMaterialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownMaterialNumber);
    }

}
