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

    public ItemStockSamm materialItemStocksToItemStockSamm(List<MaterialItemStock> materialItemStocks) {
        return listToItemStockSamm(materialItemStocks, DirectionCharacteristic.INBOUND);
    }

    public ItemStockSamm productItemStocksToItemStockSamm(List<ProductItemStock> productItemStocks) {
        return listToItemStockSamm(productItemStocks, DirectionCharacteristic.OUTBOUND);
    }

    private ItemStockSamm listToItemStockSamm(List<? extends ItemStock> itemStocks, DirectionCharacteristic directionCharacteristic) {
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
            var stock = mappingHelperListEntry.getValue().get(0);
            Position position = new Position();
            posList.add(position);
            position.setLastUpdatedOnDateTime(key.date());
            if (!key.customerOrderId.isEmpty() || !key.supplierOrderId.isEmpty() || !key.customerOrderPositionId.isEmpty()) {
                // get opr from stock as this is nullable and prevents mapping empty strings to the samm.
                OrderPositionReference opr = new OrderPositionReference(stock.getSupplierOrderId(), stock.getCustomerOrderId(),
                    stock.getCustomerOrderPositionId());
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



    public List<ReportedProductItemStock> itemStockSammToReportedProductItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer();
        String matNbrSupplier = samm.getMaterialNumberSupplier(); // should be ownMaterialNumber
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedProductItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.INBOUND) {
            log.warn("Direction should be INBOUND, aborting");
            return outputList;
        }
        Material material = materialService.findFromSupplierPerspective(matNbrCatenaX, matNbrCustomer, matNbrSupplier, partner);
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

    public List<ReportedMaterialItemStock> itemStockSammToReportedMaterialItemStock(ItemStockSamm samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer(); // should be ownMaterialNumber
        String matNbrSupplier = samm.getMaterialNumberSupplier();
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedMaterialItemStock> outputList = new ArrayList<>();
        if (samm.getDirection() != DirectionCharacteristic.OUTBOUND) {
            log.warn("Direction should be OUTBOUND, aborting");
            return outputList;
        }
        Material material = materialService.findFromCustomerPerspective(matNbrCatenaX, matNbrCustomer, matNbrSupplier, partner);
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
