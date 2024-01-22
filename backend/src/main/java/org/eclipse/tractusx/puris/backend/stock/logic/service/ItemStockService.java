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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;


@Slf4j
public abstract class ItemStockService<T extends ItemStock> {

    protected final PartnerService partnerService;

    protected final MaterialPartnerRelationService mprService;

    protected final ItemStockRepository<T> repository;

    protected final Function<T, Boolean> validator;

    public ItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService, ItemStockRepository<T> repository) {
        this.partnerService = partnerService;
        this.mprService = mprService;
        this.repository = repository;
        this.validator = this::validate;
    }

    public final T create(T itemStock) {
        if (itemStock.getUuid() != null && repository.findById(itemStock.getUuid()).isPresent()) {
            return null;
        }
        if (!validator.apply(itemStock)) {
            return null;
        }
        return repository.save(itemStock);
    }

    public final T update(T itemStock) {
        if (itemStock.getUuid() == null || repository.findById(itemStock.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(itemStock);
    }

    public final T findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public final List<T> findAll() {
        return repository.findAll();
    }

    public final List<T> findByPartnerAndMaterial(Partner partner, Material material) {
        return repository.getForPartnerAndMaterial(partner, material);
    }

    public final List<T> findByPartner(Partner partner) {
        return repository.getForPartner(partner);
    }

    public final List<T> findByMaterial(Material material) {
        return repository.getForMaterial(material);
    }

    public final List<T> findByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.getForOwnMatNbr(ownMaterialNumber);
    }

    public final List<T> findByPartnerBpnl(String partnerBpnl) {
        return repository.getForPartnerBpnl(partnerBpnl);
    }

    public final List<T> findByPartnerBpnlAndOwnMaterialNumber(String partnerBpnl, String ownMaterialNumber) {
        return repository.getForPartnerBpnlAndOwnMatNbr(partnerBpnl, ownMaterialNumber);
    }

    public abstract boolean validate(T itemStock);

    protected boolean basicValidation(ItemStock itemStock) {
        try {
            Objects.requireNonNull(itemStock.getPartner(), "Missing Partner");
            Objects.requireNonNull(itemStock.getMaterial(), "Missing Material");
            Objects.requireNonNull(itemStock.getLocationBpna(), "Missing locationBpna");
            Objects.requireNonNull(itemStock.getLocationBpns(), "Missing locationBpns");
            Objects.requireNonNull(itemStock.getMeasurementUnit(), "Missing measurementUnit");
            Objects.requireNonNull(itemStock.getLastUpdatedOnDateTime(), "Missing lastUpdatedOnTime");
        } catch (Exception e) {
            log.error("Basic Validation failed: " + itemStock + "\n" + e.getMessage());
            return false;
        }
        return true;
    }

    protected boolean validateLocalStock(ItemStock itemStock) {
        return validateLocation(itemStock, partnerService.getOwnPartnerEntity());
    }

    protected boolean validateRemoteStock(ItemStock itemStock) {
        return validateLocation(itemStock, itemStock.getPartner());
    }

    protected final boolean validateMaterialItemStock(ItemStock itemStock) {
        try {
            Partner partner = itemStock.getPartner();
            Material material = itemStock.getMaterial();
            MaterialPartnerRelation relation = mprService.find(material, partner);
            Objects.requireNonNull(relation, "Missing MaterialPartnerRelation");
            if (!material.isMaterialFlag()) {
                throw new IllegalArgumentException("Material flag is missing");
            }
            if (!relation.isPartnerSuppliesMaterial()) {
                throw new IllegalArgumentException("Partner does not supply material");
            }
        } catch (Exception e) {
            log.error("MaterialItemStock Validation failed: " + itemStock + "\n" + e.getMessage());
            return false;
        }
        return true;
    }

    protected final boolean validateProductItemStock(ItemStock itemStock) {
        try {
            Partner partner = itemStock.getPartner();
            Material material = itemStock.getMaterial();
            MaterialPartnerRelation relation = mprService.find(material, partner);
            Objects.requireNonNull(relation, "Missing MaterialPartnerRelation");
            if (!material.isProductFlag()) {
                throw new IllegalArgumentException("Product flag is missing");
            }
            if (!relation.isPartnerBuysMaterial()) {
                throw new IllegalArgumentException("Partner does not buy material");
            }
        } catch (Exception e) {
            log.error("ProductItemStock Validation failed: " + itemStock + "\n" + e.getMessage());
            return false;
        }
        return true;
    }

    protected boolean validateLocation(ItemStock itemStock, Partner partner) {
        try {
            var stockSite = partner.getSites().stream()
                .filter(site -> site.getBpns().equals(itemStock.getLocationBpns()))
                .findFirst().orElse(null);
            Objects.requireNonNull(stockSite, "Site not found");
            var stockAddress = stockSite.getAddresses().stream()
                .filter(addr -> addr.getBpna().equals(itemStock.getLocationBpna()))
                .findFirst().orElse(null);
            Objects.requireNonNull(stockAddress, "Address not found");
        } catch (Exception e) {
            log.error("Location Validation failed: " + itemStock + "\n" + e.getMessage());
            return false;
        }
        return true;
    }

}
