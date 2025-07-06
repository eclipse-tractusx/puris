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

import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public abstract class ItemStockService<T extends ItemStock> {

    protected final PartnerService partnerService;

    protected final MaterialPartnerRelationService mprService;

    protected final ItemStockRepository<T> repository;

    protected final Function<T, Boolean> validator;

    public ItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService,
            ItemStockRepository<T> repository) {
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

    public final List<T> findAllByMaterialAndPartner(String ownMaterialNumber, String partnerBpnl) {
        Stream<T> stream = repository.findAll().stream();

        stream = stream.filter(stock -> stock.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber));
        stream = stream.filter(stock -> stock.getPartner().getBpnl().equals(partnerBpnl));
        return stream.toList();
    }

    public final double getSumOfQuantities(List<T> stocks) {
        double sum = 0;
        for (T stock : stocks) {
            sum = stock.getQuantity();
        }
        return sum;
    }

    public final double getInitialStockQuantity(String material, Optional<String> partnerBpnl, Optional<String> siteBpns) {
        List<T> stocks = repository.findAll().stream()
                .filter(stock -> 
                    stock.getMaterial().getOwnMaterialNumber().equals(material) &&
                    (partnerBpnl.isEmpty() || stock.getPartner().getBpnl().equals(partnerBpnl.get())) &&
                    (siteBpns.isEmpty() || stock.getLocationBpns().equals(siteBpns.get()))
                )
                .toList();
        double initialStockQuantity = getSumOfQuantities(stocks);

        return initialStockQuantity;
    }

    public abstract boolean validate(T itemStock);

    protected List<String> basicValidation(ItemStock itemStock) {
        List<String> errors = new ArrayList<>();
        try {
            if (itemStock.getPartner() == null) {
                errors.add("Missing Partner.");
            }
            if (itemStock.getMaterial() == null) {
                errors.add("Missing Material.");
            }
            if (itemStock.getLocationBpna() == null) {
                errors.add("Missing locationBpna.");
            }
            if (itemStock.getLocationBpns() == null) {
                errors.add("Missing locationBpns.");
            }
            if (itemStock.getMeasurementUnit() == null) {
                errors.add("Missing measurementUnit.");
            }
            if (itemStock.getLastUpdatedOnDateTime() == null) {
                errors.add("Missing lastUpdatedOnTime.");
            }
            if (!((itemStock.getCustomerOrderId() != null && itemStock.getCustomerOrderPositionId() != null) || 
                (itemStock.getCustomerOrderId() == null && itemStock.getCustomerOrderPositionId() == null && itemStock.getSupplierOrderId() == null))) {
                errors.add("If an order position reference is given, customer order number and customer order position number must be set.");
            }
        } catch (Exception e) {
            log.error("Basic Validation failed: " + itemStock + "\n" + e.getMessage());
            errors.add(e.getMessage());
        }
        return errors;
    }

    protected List<String> validateLocalStock(ItemStock itemStock) {
        return validateLocation(itemStock, partnerService.getOwnPartnerEntity());
    }

    protected List<String> validateRemoteStock(ItemStock itemStock) {
        return validateLocation(itemStock, itemStock.getPartner());
    }

    protected final List<String> validateMaterialItemStock(ItemStock itemStock) {
        List<String> errors = new ArrayList<>();
        try {
            Partner partner = itemStock.getPartner();
            Material material = itemStock.getMaterial();
            MaterialPartnerRelation relation = mprService.find(material, partner);
            if (relation == null) {
                errors.add("Missing MaterialPartnerRelation.");
            }
            if (!material.isMaterialFlag()) {
                errors.add("Material flag is missing.");
            }
            if (relation != null && !relation.isPartnerSuppliesMaterial()) {
                errors.add("Partner does not supply material.");
            }
        } catch (Exception e) {
            log.error("MaterialItemStock Validation failed: " + itemStock + "\n" + e.getMessage());
            errors.add(e.getMessage());
        }
        return errors;
    }

    protected final List<String> validateProductItemStock(ItemStock itemStock) {
        List<String> errors = new ArrayList<>();
        try {
            Partner partner = itemStock.getPartner();
            Material material = itemStock.getMaterial();
            MaterialPartnerRelation relation = mprService.find(material, partner);
            if (relation == null) {
                errors.add("Missing MaterialPartnerRelation.");
            }
            if (!material.isProductFlag()) {
                errors.add("Product flag is missing.");
            }
            if (relation != null && !relation.isPartnerBuysMaterial()) {
                errors.add("Partner does not buy material.");
            }
        } catch (Exception e) {
            log.error("ProductItemStock Validation failed: " + itemStock + "\n" + e.getMessage());
            errors.add(e.getMessage());
        }
        return errors;
    }

    protected List<String> validateLocation(ItemStock itemStock, Partner partner) {
        List<String> errors = new ArrayList<>();
        try {
            var stockSite = partner.getSites().stream()
                    .filter(site -> site.getBpns().equals(itemStock.getLocationBpns()))
                    .findFirst().orElse(null);
            if (stockSite == null) {
                errors.add("Site not found.");
            }
            var stockAddress = stockSite != null ? stockSite.getAddresses().stream()
                    .filter(addr -> addr.getBpna().equals(itemStock.getLocationBpna()))
                    .findFirst().orElse(null) : null;
            if (stockAddress == null) {
                errors.add("Address not found for partner and site.");
            }
        } catch (Exception e) {
            log.error("Location Validation failed: " + itemStock + "\n" + e.getMessage());
            errors.add(e.getMessage());
        }
        return errors;
    }

}
