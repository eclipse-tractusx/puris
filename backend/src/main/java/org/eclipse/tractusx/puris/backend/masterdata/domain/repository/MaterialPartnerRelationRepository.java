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
package org.eclipse.tractusx.puris.backend.masterdata.domain.repository;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialPartnerRelationRepository extends JpaRepository<MaterialPartnerRelation, MaterialPartnerRelation.Key> {

    List<MaterialPartnerRelation> findAllByPartner_Uuid(UUID partnerUuid);

    List<MaterialPartnerRelation> findAllByPartner_UuidAndPartnerSuppliesMaterialIsTrue(UUID partnerUuid);

    List<MaterialPartnerRelation> findAllByPartner_UuidAndPartnerBuysMaterialIsTrue(UUID partnerUuid);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumberAndPartnerSuppliesMaterialIsTrue(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumberAndPartnerBuysMaterialIsTrue(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerMaterialNumber(String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerMaterialNumberAndPartnerSuppliesMaterialIsTrue(String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerMaterialNumberAndPartnerBuysMaterialIsTrue(String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerAndPartnerMaterialNumberAndPartnerSuppliesMaterialIsTrue(Partner partner, String partnerMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerAndPartnerMaterialNumberAndPartnerBuysMaterialIsTrue(Partner partner, String partnerMaterialNumber);
}
