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
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>This class represents a relation between an instance of the {@link Material} Entity
 * and one instance of the {@link Partner} Entity. For each pair of Material and Partner
 * Entities there exists at most one MaterialPartnerRelation. </p>
 * <p>This class stores, under which identifier (material number) the Partner knows
 * this Material, and whether this Partner is a supplier or a customer of the Material.</p>
 *
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
public class MaterialPartnerRelation {

    @EmbeddedId
    private Key key;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String partnerMaterialNumber;
    private boolean partnerSuppliesMaterial;
    private boolean partnerBuysMaterial;

    @ManyToOne
    private Material material;

    @ManyToOne
    private Partner partner;

    public MaterialPartnerRelation() {
        this.key = new Key();
    }

    public MaterialPartnerRelation(Material material, Partner partner, String partnerMaterialNumber, boolean partnerSupplies, boolean partnerBuys) {
        this.material = material;
        this.partner = partner;
        this.key = new Key(material.getOwnMaterialNumber(), partner.getUuid());
        this.partnerMaterialNumber = partnerMaterialNumber;
        this.partnerSuppliesMaterial = partnerSupplies;
        this.partnerBuysMaterial = partnerBuys;
    }

    @Override
    public String toString() {
        return "MaterialPartnerRelation{" +
            "key=" + key +
            ", partnerMaterialNumber='" + partnerMaterialNumber + '\'' +
            ", partnerSuppliesMaterial=" + partnerSuppliesMaterial +
            ", partnerBuysMaterial=" + partnerBuysMaterial +
            ", material=" + material.getOwnMaterialNumber() +
            ", partner=" + partner.getBpnl() +
            '}';
    }

    @Embeddable
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Key implements Serializable {

        @Column(name = "key_ownMaterialNumber")
        @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
        private String ownMaterialNumber;

        @Column(name = "key_uuid")
        private UUID partnerUuid;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(ownMaterialNumber, key.ownMaterialNumber) && Objects.equals(partnerUuid, key.partnerUuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownMaterialNumber, partnerUuid);
        }
    }
}
