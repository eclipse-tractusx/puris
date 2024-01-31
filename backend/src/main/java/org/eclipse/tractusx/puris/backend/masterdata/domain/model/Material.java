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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;

/**
 * <p>This class represents materials and products that are to be traded between
 * partners that collaborate via the PURIS application. </p>
 * <p>Whether a material is a product or a "material", is determined by the point of
 * view. If the owner of the current instance of a PURIS application is selling a
 * certain product to customers, the product flag must be set to "true" and the business
 * logic will treat this material as a product if the circumstances require a differentiation
 * between products and materials.</p>
 * <p>If, on the other hand, the owner of the current instance of a PURIS application is buying
 * a material from external partners, then the material-flag must be set to "true", in order to
 * allow the business logic to handle this correctly.</p>
 * <p>This approach allows for flexibility, especially in cases when the the owner of the current
 * instance of a PURIS application is a trader who is buying a certain good from one partner just
 * to sell it immediately to another partner. </p>
 * <p>Each entity of this class is uniquely identified by the material number defined
 * by the partner that runs an instance of the PURIS app.</p>
 * <p>Example: A certain type of semiconductors is produced by partner A. He has labeled his
 * product with the material number "SC-456.001". Partner B is buying this type of
 * semiconductors from A. In his own ERP-System, he is referring to this semiconductor
 * as "A-CHIP-0815".</p>
 * <p>In partner A's PURIS app this material will be registered under the ownMaterialNumber
 * "SC-456.001", while partner B will registered as "A-CHIP-0815". Let's further assume
 * that Partner C now wants to buy this material from A as well. In his own domain, C names
 * this material "XYZ-123". Therefore C will register the material under this ownMaterialNumber
 * in his own PURIS app.</p>
 * <p>When two partners are preparing to establish a business relationship in regard to a specific
 * material, then each partner will create a {@link MaterialPartnerRelation}, where he
 * designates the other partner as supplier or customer of this entity. He also must define,
 * under which material number his partner refers to this material.</p>
 *
 */
@Entity
@Table(name = "material")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * The unique material number defined by the owner of this
     * PURIS instance for this material.
     */
    @Id
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String ownMaterialNumber;

    /**
     * If there is a Catena-X material number defined
     * for this material, this is stored here.
     */
    @Pattern(regexp = PatternStore.URN_STRING)
    private String materialNumberCx;

    /**
     * Informal name or description of the material.
     */
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String name;

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
