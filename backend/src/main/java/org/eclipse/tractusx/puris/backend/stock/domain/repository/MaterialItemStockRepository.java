/*
 * Copyright (c) 2023, 2024 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.stock.domain.repository;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialItemStockRepository extends ItemStockRepository<MaterialItemStock> {
    List<MaterialItemStock> findByPartnerAndMaterial(Partner partner, Material material);

    List<MaterialItemStock> findByPartner(Partner partner);

    List<MaterialItemStock> findByMaterial(Material material);

    List<MaterialItemStock> findByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    List<MaterialItemStock> findByPartner_Bpnl(String partnerBpnl);

    List<MaterialItemStock> findByPartner_BpnlAndMaterial_OwnMaterialNumber(String partnerBpnl, String ownMaterialNumber);

    @Override
    default List<MaterialItemStock> getForPartnerAndMaterial(Partner partner, Material material) {
        return findByPartnerAndMaterial(partner, material);
    }

    @Override
    default List<MaterialItemStock> getForPartner(Partner partner) {
        return findByPartner(partner);
    }

    @Override
    default List<MaterialItemStock> getForMaterial(Material material) {
        return findByMaterial(material);
    }

    @Override
    default List<MaterialItemStock> getForOwnMatNbr(String ownMaterialNumber) {
        return findByMaterial_OwnMaterialNumber(ownMaterialNumber);
    }

    @Override
    default List<MaterialItemStock> getForPartnerBpnl(String partnerBpnl) {
        return findByPartner_Bpnl(partnerBpnl);
    }

    @Override
    default List<MaterialItemStock> getForPartnerBpnlAndOwnMatNbr(String partnerBpnl, String ownMaterialNumber) {
        return findByPartner_BpnlAndMaterial_OwnMaterialNumber(partnerBpnl, ownMaterialNumber);
    }


}
