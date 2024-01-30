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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface MaterialPartnerRelationService {
    MaterialPartnerRelation create(MaterialPartnerRelation materialPartnerRelation);

    MaterialPartnerRelation update(MaterialPartnerRelation materialPartnerRelation);

    MaterialPartnerRelation find(Material material, Partner partner);

    List<Material> findAllMaterialsThatPartnerSupplies(Partner partner);

    List<Material> findAllProductsThatPartnerBuys(Partner partner);

    List<MaterialPartnerRelation> findAll();

    Map<String, String> getBPNL_To_MaterialNumberMap(String ownMaterialNumber);

    MaterialPartnerRelation find(String ownMaterialNumber, UUID partnerUuid);

    List<Partner> findAllSuppliersForOwnMaterialNumber(String ownMaterialNumber);

    List<Partner> findAllCustomersForOwnMaterialNumber(String ownMaterialNumber);

    List<Partner> findAllSuppliersForMaterial(Material material);

    List<Material> findAllByPartnerMaterialNumber(String partnerMaterialNumber);

    boolean partnerSuppliesMaterial(Material material, Partner partner);

    boolean partnerOrdersProduct(Material material, Partner partner);

    List<MaterialPartnerRelation> findAllBySupplierPartnerMaterialNumber(String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByCustomerPartnerMaterialNumber(String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllBySupplierPartnerAndPartnerMaterialNumber(Partner partner, String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByCustomerPartnerAndPartnerMaterialNumber(Partner partner, String partnerMaterialNumber);
}
