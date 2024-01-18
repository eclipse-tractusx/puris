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
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemStockRepository<T extends ItemStock> extends JpaRepository<T, UUID> {

    default List<T> getForPartnerAndMaterial(Partner partner, Material material) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method. 
        throw new UnsupportedOperationException("Implementation needed");
    }

    default List<T> getForPartner(Partner partner) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method.
        throw new UnsupportedOperationException("Implementation needed");
    }

    default List<T> getForMaterial(Material material) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method.
        throw new UnsupportedOperationException("Implementation needed");
    }

    default List<T> getForOwnMatNbr(String ownMaterialNumber) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method.
        throw new UnsupportedOperationException("Implementation needed");
    }

    default List<T> getForPartnerBpnl(String partnerBpnl) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method.
        throw new UnsupportedOperationException("Implementation needed");
    }

    default List<T> getForPartnerBpnlAndOwnMatNbr(String partnerBpnl, String ownMaterialNumber) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method.
        throw new UnsupportedOperationException("Implementation needed");
    }


}
