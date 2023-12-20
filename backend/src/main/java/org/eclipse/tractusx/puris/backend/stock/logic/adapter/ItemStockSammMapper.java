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

package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemStockSammMapper {

    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;

    public ItemStockSamm materialItemStocksToSamm(List<MaterialItemStock> materialItemStocks) {
        return listToSamm(materialItemStocks, DirectionCharacteristic.INBOUND);
    }

    public ItemStockSamm productItemStocksToSamm(List<ProductItemStock> productItemStocks) {
        return listToSamm(productItemStocks, DirectionCharacteristic.OUTBOUND);
    }

    private ItemStockSamm listToSamm(List<? extends ItemStock> itemStocks, DirectionCharacteristic directionCharacteristic) {
        if (itemStocks == null || itemStocks.isEmpty()) {
            log.warn("Can't map empty list");
            return null;
        }
        Partner partner = itemStocks.get(0).getPartner();
        Material material = itemStocks.get(0).getMaterial();
        if (itemStocks.stream().anyMatch(stock -> !stock.getPartner().equals(partner))) {
            log.warn("Can't map item stock list with different partners");
            return null;
        }

        if (itemStocks.stream().anyMatch(stock -> !stock.getMaterial().equals(material))) {
            log.warn("Can't map item stock list with different materials");
            return null;
        }
        var groupedByPositionAttributes = itemStocks
            .stream()
            .collect(Collectors.groupingBy(
                itemStock -> new PositionsMappingHelper(itemStock.getLastUpdatedOnDateTime(),
                    itemStock.getNonNullCustomerOrderId(), itemStock.getNonNullSupplierOrderId(),
                    itemStock.getNonNullCustomerOrderPositionId())));
        ItemStockSamm samm = new ItemStockSamm();
        samm.setMaterialGlobalAssetId(material.getMaterialNumberCx());

        String partnerMaterialNumber = mprService.find(material, partner).getPartnerMaterialNumber();
        String customerMatNbr = directionCharacteristic ==
            DirectionCharacteristic.INBOUND ? material.getOwnMaterialNumber() : partnerMaterialNumber;
        String supplierMatNbr = directionCharacteristic ==
            DirectionCharacteristic.INBOUND ? partnerMaterialNumber : material.getOwnMaterialNumber();
        samm.setMaterialNumberCustomer(customerMatNbr);
        samm.setMaterialNumberSupplier(supplierMatNbr);
        samm.setDirection(directionCharacteristic);
        var posList = new ArrayList<Position>();
        samm.setPositions(posList);
        for (var mappingHelperListEntry : groupedByPositionAttributes.entrySet()) {
            var key = mappingHelperListEntry.getKey();
            Position position = new Position();
            posList.add(position);
            position.setLastUpdatedOnDateTime(key.date());
            if (!key.customerOrderId.isEmpty() || !key.supplierOrderId.isEmpty() || !key.customerOrderPositionId.isEmpty()) {
                OrderPositionReference opr = new OrderPositionReference(key.supplierOrderId, key.customerOrderId,
                    key.customerOrderPositionId);
                position.setOrderPositionReference(opr);
            }
            var allocatedStocksList = new ArrayList<AllocatedStock>();
            position.setAllocatedStocks(allocatedStocksList);
            for (var v : mappingHelperListEntry.getValue()) {
                ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(v.getQuantity(), v.getMeasurementUnit());
                AllocatedStock allocatedStock = new AllocatedStock(itemQuantityEntity, v.getLocationBpns(), v.isBlocked(), v.getLocationBpna());
                allocatedStocksList.add(allocatedStock);
            }
        }
        return samm;
    }

    private record PositionsMappingHelper(Date date, String customerOrderId, String supplierOrderId,
                                          String customerOrderPositionId) {
    }

    public ItemStockSamm toItemStockSamm(MaterialItemStock materialItemStock) {
        ItemStockSamm samm = new ItemStockSamm();
        samm.setDirection(DirectionCharacteristic.INBOUND);
        samm.setPositions(new ArrayList<>());
        samm.setMaterialGlobalAssetId(materialItemStock.getMaterial().getMaterialNumberCx());
        samm.setMaterialNumberCustomer(materialItemStock.getMaterial().getOwnMaterialNumber());
        samm.setMaterialNumberSupplier(mprService.find(materialItemStock.getMaterial(),
            materialItemStock.getPartner()).getPartnerMaterialNumber());
        return createPosition(materialItemStock, samm);
    }

    private static ItemStockSamm createPosition(ItemStock itemStock, ItemStockSamm samm) {
        Position position = new Position();
        samm.setPositions(List.of(position));
        position.setLastUpdatedOnDateTime(itemStock.getLastUpdatedOnDateTime());
        if (itemStock.getCustomerOrderId() != null || itemStock.getCustomerOrderPositionId() != null
            || itemStock.getSupplierOrderId() != null) {
            OrderPositionReference opr = new OrderPositionReference(itemStock.getSupplierOrderId(),
                itemStock.getCustomerOrderId(), itemStock.getCustomerOrderPositionId());
            position.setOrderPositionReference(opr);
        }
        ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(itemStock.getQuantity(),
            itemStock.getMeasurementUnit());
        AllocatedStock allocatedStock = new AllocatedStock(itemQuantityEntity, itemStock.getLocationBpns(),
            itemStock.isBlocked(), itemStock.getLocationBpna());
        position.setAllocatedStocks(List.of(allocatedStock));
        return samm;
    }

    public ItemStockSamm toItemStockSamm(ProductItemStock productItemStock) {
        ItemStockSamm samm = new ItemStockSamm();
        samm.setDirection(DirectionCharacteristic.OUTBOUND);
        samm.setPositions(new ArrayList<>());
        samm.setMaterialGlobalAssetId(productItemStock.getMaterial().getMaterialNumberCx());
        samm.setMaterialNumberSupplier(productItemStock.getMaterial().getOwnMaterialNumber());
        samm.setMaterialNumberCustomer(mprService.find(productItemStock.getMaterial(),
            productItemStock.getPartner()).getPartnerMaterialNumber());
        return createPosition(productItemStock, samm);
    }

    public List<ReportedProductItemStock> sammToReportedProductItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer();
        String matNbrSupplier = samm.getMaterialNumberSupplier(); // should be ownMaterialNumber
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedProductItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.INBOUND) {
            log.warn("Direction should be INBOUND, aborting");
            return outputList;
        }
        Material material = null;
        // Use CatenaXNbr
        if (matNbrCatenaX != null) {
            material = materialService.findByMaterialNumberCx(matNbrCatenaX);
            if (material != null) {
                if (!material.getOwnMaterialNumber().equals(matNbrSupplier)) {
                    log.warn("Mismatch between CatenaX Number " + matNbrCatenaX + " and ownMaterialNumber " + matNbrSupplier);
                }
                var mpr = mprService.find(material, partner);
                if (mpr == null) {
                    log.warn("Missing MaterialPartnerRelation for " + material.getOwnMaterialNumber() + " and Partner " + partner.getBpnl());
                } else {
                    if (!mpr.getPartnerMaterialNumber().equals(matNbrCustomer)) {
                        log.warn("Mismatch for MaterialNumberCustomer " + matNbrCustomer + " and " + material.getOwnMaterialNumber());
                    }
                }
            }
        }
        // Use MatNbrCustomer
        if (material == null && matNbrCustomer != null) {
            var list = mprService.findAllByPartnerMaterialNumber(matNbrCustomer).stream().filter(m -> {
                var mpr = mprService.find(m, partner);
                return mpr != null && mpr.isPartnerBuysMaterial();
            }).toList();
            if (!list.isEmpty()) {
                material = list.get(0);
                if (list.size() > 1) {
                    log.warn("CustomerMaterialNumber " + matNbrCustomer + " is ambiguous, arbitrarily choosing " + material.getOwnMaterialNumber());
                }
            }
        }
        // Use MatNbrSupplier
        if (material == null && matNbrSupplier != null) {
            material = materialService.findByOwnMaterialNumber(matNbrSupplier);

            if (material != null) {
                if (matNbrCatenaX != null) {
                    log.warn("Unknown CatenaXNumber for Material " + material.getOwnMaterialNumber());
                }
                var mpr = mprService.find(material, partner);
                if (mpr != null) {
                    if (!mpr.getPartnerMaterialNumber().equals(matNbrCustomer)) {
                        log.warn("Unknown MaterialNumberCustomer " + matNbrCustomer + " for Material " + material.getOwnMaterialNumber());
                    }
                }
            }
        }
        if (material == null) {
            log.warn("Could not identify material with CatenaXNbr " + matNbrCatenaX + " ,CustomerMaterialNbr " + matNbrCustomer + " and SupplierMaterialNbr " + matNbrSupplier);
            return outputList;
        }
        for (var position : samm.getPositions()) {
            Date lastUpdated = position.getLastUpdatedOnDateTime();
            String supplierOrderId = null, customerOrderPositionId = null, customerOrderId = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderId = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderId = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionId = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var allocatedStock : position.getAllocatedStocks()) {
                var builder = ReportedProductItemStock.builder();
                var itemStock = builder
                    .partner(partner)
                    .material(material)
                    .isBlocked(allocatedStock.getIsBlocked())
                    .locationBpna(allocatedStock.getStockLocationBPNA())
                    .locationBpns(allocatedStock.getStockLocationBPNS())
                    .lastUpdatedOnDateTime(lastUpdated)
                    .customerOrderId(customerOrderId)
                    .supplierOrderId(supplierOrderId)
                    .customerOrderPositionId(customerOrderPositionId)
                    .measurementUnit(allocatedStock.getQuantityOnAllocatedStock().getUnit())
                    .quantity(allocatedStock.getQuantityOnAllocatedStock().getValue())
                    .build();
                outputList.add(itemStock);
            }
        }
        return outputList;
    }

    public List<ReportedMaterialItemStock> sammToReportedMaterialItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer(); // should be ownMaterialNumber
        String matNbrSupplier = samm.getMaterialNumberSupplier();
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedMaterialItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.OUTBOUND) {
            log.warn("Direction should be OUTBOUND, aborting");
            return outputList;
        }
        Material material = null;
        // Use MatNbrCatenaX
        if (matNbrCatenaX != null) {
            material = materialService.findByMaterialNumberCx(matNbrCatenaX);
            if (material != null) {
                if (!material.getOwnMaterialNumber().equals(matNbrCustomer)) {
                    log.warn("Mismatch between CatenaX Number " + matNbrCatenaX + " and ownMaterialNumber " + matNbrCustomer);
                }
                var mpr = mprService.find(material, partner);
                if (mpr == null) {
                    log.warn("Missing MaterialPartnerRelation for " + material.getOwnMaterialNumber() + " and Partner " + partner.getBpnl());
                } else {
                    if (!mpr.getPartnerMaterialNumber().equals(matNbrSupplier)) {
                        log.warn("Mismatch for MaterialNumberSupplier " + matNbrSupplier + " and " + material.getOwnMaterialNumber());
                    }
                }
            }
        }
        // Use MatNbrCustomer
        if (material == null && matNbrCustomer != null) {
            material = materialService.findByOwnMaterialNumber(matNbrCustomer);
            if (material != null) {
                if (matNbrCatenaX != null) {
                    log.warn("Unknown CatenaXNumber for Material " + material.getOwnMaterialNumber());
                }
                var mpr = mprService.find(material, partner);
                if (mpr != null) {
                    if (!mpr.getPartnerMaterialNumber().equals(matNbrSupplier)) {
                        log.warn("Unknown MaterialNumberSupplier " + matNbrSupplier + " for Material " + material.getOwnMaterialNumber());
                    }
                }
            }
        }
        //Use MatNbrSupplier
        if (material == null && matNbrSupplier != null) {
            var list = mprService.findAllByPartnerMaterialNumber(matNbrSupplier)
                .stream()
                .filter(m -> {
                    var mpr = mprService.find(m, partner);
                    return mpr != null && mpr.isPartnerSuppliesMaterial();
                })
                .toList();
            if (!list.isEmpty()) {
                material = list.get(0);
                if (list.size() > 1) {
                    log.warn("SupplierMaterialNumber " + matNbrSupplier + " is ambiguous, arbitrarily choosing " + material.getOwnMaterialNumber());
                }
            }
        }
        if (material == null) {
            log.warn("Could not identify material with CatenaXNbr " + matNbrCatenaX + " ,CustomerMaterialNbr " + matNbrCustomer + " and SupplierMaterialNbr " + matNbrSupplier);
            return outputList;
        }
        for (var position : samm.getPositions()) {
            Date lastUpdated = position.getLastUpdatedOnDateTime();
            String supplierOrderId = null, customerOrderPositionId = null, customerOrderId = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderId = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderId = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionId = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var allocatedStock : position.getAllocatedStocks()) {
                var builder = ReportedMaterialItemStock.builder();
                var itemStock = builder
                    .partner(partner)
                    .material(material)
                    .isBlocked(allocatedStock.getIsBlocked())
                    .locationBpna(allocatedStock.getStockLocationBPNA())
                    .locationBpns(allocatedStock.getStockLocationBPNS())
                    .lastUpdatedOnDateTime(lastUpdated)
                    .customerOrderId(customerOrderId)
                    .supplierOrderId(supplierOrderId)
                    .customerOrderPositionId(customerOrderPositionId)
                    .measurementUnit(allocatedStock.getQuantityOnAllocatedStock().getUnit())
                    .quantity(allocatedStock.getQuantityOnAllocatedStock().getValue())
                    .build();
                outputList.add(itemStock);
            }
        }
        return outputList;
    }
}
