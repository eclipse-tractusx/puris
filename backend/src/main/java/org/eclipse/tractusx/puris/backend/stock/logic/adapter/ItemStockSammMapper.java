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
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemStockSammMapper {

    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;

    public ItemStockSamm materialItemStocksToItemStockSamm(List<MaterialItemStock> materialItemStocks, Partner partner, Material material) {
        return listToItemStockSamm(materialItemStocks, DirectionCharacteristic.INBOUND, partner, material);
    }

    public ItemStockSamm productItemStocksToItemStockSamm(List<ProductItemStock> productItemStocks, Partner partner, Material material) {
        return listToItemStockSamm(productItemStocks, DirectionCharacteristic.OUTBOUND, partner, material);
    }

    private ItemStockSamm listToItemStockSamm(List<? extends ItemStock> itemStocks, DirectionCharacteristic directionCharacteristic, Partner partner, Material material) {
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
                itemStock -> new PositionsMappingHelper(itemStock.getNonNullCustomerOrderId(),
                    itemStock.getNonNullSupplierOrderId(),
                    itemStock.getNonNullCustomerOrderPositionId())));
        ItemStockSamm samm = new ItemStockSamm();

        if (directionCharacteristic == DirectionCharacteristic.INBOUND) {
            samm.setMaterialGlobalAssetId(mprService.find(material, partner).getPartnerCXNumber());
        } else {
            samm.setMaterialGlobalAssetId(material.getMaterialNumberCx());
        }

        samm.setDirection(directionCharacteristic);
        var posList = new HashSet<Position>();
        samm.setPositions(posList);
        for (var mappingHelperListEntry : groupedByPositionAttributes.entrySet()) {
            var key = mappingHelperListEntry.getKey();
            var stock = mappingHelperListEntry.getValue().get(0);
            Position position = new Position();
            posList.add(position);
            if (!key.customerOrderId.isEmpty() || !key.supplierOrderId.isEmpty() || !key.customerOrderPositionId.isEmpty()) {
                // get opr from stock as this is nullable and prevents mapping empty strings to the samm.
                OrderPositionReference opr = new OrderPositionReference(stock.getSupplierOrderId(), stock.getCustomerOrderId(),
                    stock.getCustomerOrderPositionId());
                position.setOrderPositionReference(opr);
            }
            var allocatedStocksList = new HashSet<AllocatedStock>();
            position.setAllocatedStocks(allocatedStocksList);
            for (var v : mappingHelperListEntry.getValue()) {
                ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(v.getQuantity(), v.getMeasurementUnit());
                AllocatedStock allocatedStock = new AllocatedStock(itemQuantityEntity, v.getLocationBpns(), v.isBlocked(), v.getLocationBpna(), v.getLastUpdatedOnDateTime());
                allocatedStocksList.add(allocatedStock);
            }
        }
        return samm;
    }

    private record PositionsMappingHelper(String customerOrderId, String supplierOrderId, String customerOrderPositionId) {
    }

    public List<MaterialItemStock> erpSammToMaterialItemStock(ItemStockSamm samm, Partner partner, Material material) {
        ArrayList<MaterialItemStock> materialItemStocks = new ArrayList<>();
        for (var position : samm.getPositions()) {
            String supplierOrderId = null, customerOrderPositionId = null, customerOrderId = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderId = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderId = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionId = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var allocatedStock : position.getAllocatedStocks()) {
                var builder = MaterialItemStock.builder();
                var itemStock = builder
                    .partner(partner)
                    .material(material)
                    .isBlocked(allocatedStock.getIsBlocked())
                    .locationBpna(allocatedStock.getStockLocationBPNA())
                    .locationBpns(allocatedStock.getStockLocationBPNS())
                    .lastUpdatedOnDateTime(allocatedStock.getLastUpdatedOnDateTime())
                    .customerOrderId(customerOrderId)
                    .supplierOrderId(supplierOrderId)
                    .customerOrderPositionId(customerOrderPositionId)
                    .measurementUnit(allocatedStock.getQuantityOnAllocatedStock().getUnit())
                    .quantity(allocatedStock.getQuantityOnAllocatedStock().getValue())
                    .build();
                materialItemStocks.add(itemStock);
            }
        }
        return materialItemStocks;
    }

    public List<ProductItemStock> erpSammToProductItemStock(ItemStockSamm samm, Partner partner, Material material) {
        ArrayList<ProductItemStock> productItemStocks = new ArrayList<>();
        for (var position : samm.getPositions()) {
            String supplierOrderId = null, customerOrderPositionId = null, customerOrderId = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderId = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderId = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionId = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var allocatedStock : position.getAllocatedStocks()) {
                var builder = ProductItemStock.builder();
                var itemStock = builder
                    .partner(partner)
                    .material(material)
                    .isBlocked(allocatedStock.getIsBlocked())
                    .locationBpna(allocatedStock.getStockLocationBPNA())
                    .locationBpns(allocatedStock.getStockLocationBPNS())
                    .lastUpdatedOnDateTime(allocatedStock.getLastUpdatedOnDateTime())
                    .customerOrderId(customerOrderId)
                    .supplierOrderId(supplierOrderId)
                    .customerOrderPositionId(customerOrderPositionId)
                    .measurementUnit(allocatedStock.getQuantityOnAllocatedStock().getUnit())
                    .quantity(allocatedStock.getQuantityOnAllocatedStock().getValue())
                    .build();
                productItemStocks.add(itemStock);
            }
        }
        return productItemStocks;
    }



    public List<ReportedProductItemStock> itemStockSammToReportedProductItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedProductItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.OUTBOUND) {
            throw new IllegalArgumentException("Direction should be OUTBOUND, aborting");
        }

        var mpr = mprService.findByPartnerAndPartnerCXNumber(partner, matNbrCatenaX);

        if (mpr == null) {
            throw new IllegalArgumentException("Could not identify materialPartnerRelation with matNbrCatenaX "
                + matNbrCatenaX + " and partner bpnl " + partner.getBpnl());
        }
        // When deserializing a Samm from a supplier, who has sent a report on the
        // stocks he has prepared for us, the materialGlobalAssetId used in the communication
        // was set by the supplying partner. Therefore, the materialGlobalAssetId in
        // the Samm is the one in our MaterialPartnerRelation entity with that partner.
        Material material = mpr.getMaterial();
        if (material == null) {
            throw new IllegalArgumentException("Could not identify material with CatenaXNbr " + matNbrCatenaX);
        }


        for (var position : samm.getPositions()) {
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
                    .material(mpr.getMaterial())
                    .isBlocked(allocatedStock.getIsBlocked())
                    .locationBpna(allocatedStock.getStockLocationBPNA())
                    .locationBpns(allocatedStock.getStockLocationBPNS())
                    .lastUpdatedOnDateTime(allocatedStock.getLastUpdatedOnDateTime())
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

    public List<ReportedMaterialItemStock> itemStockSammToReportedMaterialItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedMaterialItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.INBOUND) {
            throw new IllegalArgumentException("Direction should be INBOUND, aborting");
        }

        // When deserializing a Samm from a customer, who has sent a report on the
        // stocks he received from us, the materialGlobalAssetId used in the communication
        // was set by us (as the supplying side). Therefore the materialGlobalAssetId in
        // the Samm is the one in our Material entity.
        Material material = materialService.findByMaterialNumberCx(matNbrCatenaX);
        if (material == null) {
            throw new IllegalArgumentException("Could not identify material with CatenaXNbr " + matNbrCatenaX);
        }

        var mpr = mprService.find(material, partner);
        if (mpr == null) {
            throw new IllegalArgumentException("Could not identify materialPartnerRelation with matNbrCatenaX "
                + matNbrCatenaX + " and partner bpnl " + partner.getBpnl());
        }

        for (var position : samm.getPositions()) {
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
                    .lastUpdatedOnDateTime(allocatedStock.getLastUpdatedOnDateTime())
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
