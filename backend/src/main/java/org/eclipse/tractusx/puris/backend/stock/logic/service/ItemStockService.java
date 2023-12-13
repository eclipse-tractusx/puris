/*
 * Copyright (c) 2023 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemStockService {

    private final ItemStockRepository itemStockRepository;
    private final PartnerService partnerService;
    private final MaterialPartnerRelationService mprService;

    public ItemStock create(ItemStock itemStock) {
        if(!validate(itemStock)) {
            return null;
        }
        return itemStockRepository.save(itemStock);
    }

    public ItemStock update(ItemStock itemStock) {
        if(!validate(itemStock)) {
            return null;
        }
        if(itemStockRepository.findById(itemStock.getKey()).isEmpty()){
            return null;
        }
        return itemStockRepository.save(itemStock);
    }

    public ItemStock findById(ItemStock.Key key) {
        return itemStockRepository.findById(key).orElse(null);
    }

    public List<ItemStock> findAll() {
        return itemStockRepository.findAll();
    }

    private boolean validate(ItemStock itemStock) {
        var key = itemStock.getKey();
        Partner partner = key.getPartner();
        try {
            Objects.requireNonNull(partner, "Missing Partner");
            Objects.requireNonNull(key.getMaterial(), "Missing Material");
            Objects.requireNonNull(key.getDirection(), "Missing direction");
            Objects.requireNonNull(key.getLocationBpna(), "Missing locationBpna");
            Objects.requireNonNull(key.getLocationBpns(), "Missing locationBpns");
            Objects.requireNonNull(itemStock.getMeasurementUnit(), "Missing measurementUnit");
            Objects.requireNonNull(itemStock.getQuantityAmount(), "Missing quantityAmount");
            Objects.requireNonNull(itemStock.getLastUpdatedOnDateTime(), "Missing lastUpdatedOnTime");
            Partner mySelf = partnerService.getOwnPartnerEntity();
            Partner customer = key.getDirection() == DirectionCharacteristic.INBOUND ? mySelf : partner;
            Partner supplier = customer == mySelf ? partner : mySelf;
            var stockBpns = supplier.getSites().stream()
                .filter(site -> site.getBpns().equals(key.getLocationBpns())).findFirst().orElse(null);
            Objects.requireNonNull(stockBpns, "Unknown Bpns: " + key.getLocationBpns());
            var stockBpna = supplier.getSites().stream().flatMap(site -> site.getAddresses().stream())
                .filter(address -> address.getBpna().equals(key.getLocationBpna())).findFirst().orElse(null);
            Objects.requireNonNull(stockBpna, "Unknown Bpna: " + key.getLocationBpna());
            var materialPartnerRelation = mprService.find(key.getMaterial(), partner);
            Objects.requireNonNull(materialPartnerRelation, "Missing MaterialPartnerRelation between Partner " +
                partner.getBpnl() + " and " + itemStock.getOwnMaterialNumber());
        } catch (Exception e) {
            log.error("Validation failed: " + itemStock + "\n" + e.getMessage());
            return false;
        }
        return true;
    }
}
