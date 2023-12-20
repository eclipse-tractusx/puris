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
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ItemStockSammMapper {

    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;

    public ItemStockSAMM toItemStockSAMM(MaterialItemStock materialItemStock) {
        ItemStockSAMM samm = new ItemStockSAMM();
        samm.setDirection(DirectionCharacteristic.INBOUND);
        samm.setPositions(new ArrayList<>());
        samm.setMaterialGlobalAssetId(materialItemStock.getMaterial().getMaterialNumberCx());
        samm.setMaterialNumberCustomer(materialItemStock.getMaterial().getOwnMaterialNumber());
        samm.setMaterialNumberSupplier(mprService.find(materialItemStock.getMaterial(),
            materialItemStock.getPartner()).getPartnerMaterialNumber());
        var posList = new ArrayList<Position>();
        samm.setPositions(posList);
        Position position = new Position();
        if (materialItemStock.getCustomerOrderId() != null || materialItemStock.getCustomerOrderPositionId() != null
            || materialItemStock.getSupplierOrderId() != null) {
            OrderPositionReference opr = new OrderPositionReference(materialItemStock.getSupplierOrderId(),
                materialItemStock.getCustomerOrderId(), materialItemStock.getCustomerOrderPositionId());
            position.setOrderPositionReference(opr);
        }
        ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(materialItemStock.getQuantity(),
            materialItemStock.getMeasurementUnit());
        AllocatedStock allocatedStock = new AllocatedStock(itemQuantityEntity, materialItemStock.getLocationBpns(),
            materialItemStock.isBlocked(), materialItemStock.getLocationBpna());
        var allocatedStocksList = new ArrayList<AllocatedStock>();
        allocatedStocksList.add(allocatedStock);
        position.setAllocatedStocks(allocatedStocksList);
        return samm;
    }

    public ItemStockSAMM toItemStockSamm(ProductItemStock productItemStock) {
        ItemStockSAMM samm = new ItemStockSAMM();
        samm.setDirection(DirectionCharacteristic.OUTBOUND);
        samm.setPositions(new ArrayList<>());
        samm.setMaterialGlobalAssetId(productItemStock.getMaterial().getMaterialNumberCx());
        samm.setMaterialNumberSupplier(productItemStock.getMaterial().getOwnMaterialNumber());
        samm.setMaterialNumberCustomer(mprService.find(productItemStock.getMaterial(),
            productItemStock.getPartner()).getPartnerMaterialNumber());
        var posList = new ArrayList<Position>();
        samm.setPositions(posList);
        Position position = new Position();
        if (productItemStock.getCustomerOrderId() != null || productItemStock.getCustomerOrderPositionId() != null
            || productItemStock.getSupplierOrderId() != null) {
            OrderPositionReference opr = new OrderPositionReference(productItemStock.getSupplierOrderId(),
                productItemStock.getCustomerOrderId(), productItemStock.getCustomerOrderPositionId());
            position.setOrderPositionReference(opr);
        }
        ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(productItemStock.getQuantity(),
            productItemStock.getMeasurementUnit());
        AllocatedStock allocatedStock = new AllocatedStock(itemQuantityEntity, productItemStock.getLocationBpns(),
            productItemStock.isBlocked(), productItemStock.getLocationBpna());
        var allocatedStocksList = new ArrayList<AllocatedStock>();
        allocatedStocksList.add(allocatedStock);
        position.setAllocatedStocks(allocatedStocksList);
        return samm;
    }

    public List<ReportedProductItemStock> sammToReportedProductItemStock(ItemStockSAMM samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer();
        String matNbrSupplier = samm.getMaterialNumberSupplier(); // should be ownMaterialNumber
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedProductItemStock> outputList = new ArrayList<>();
        if(samm.getDirection() != DirectionCharacteristic.INBOUND) {
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

    public List<ReportedMaterialItemStock> sammToReportedMaterialItemStock(ItemStockSAMM samm, Partner partner) {
        String matNbrCustomer = samm.getMaterialNumberCustomer(); // should be ownMaterialNumber
        String matNbrSupplier = samm.getMaterialNumberSupplier();
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedMaterialItemStock> outputList = new ArrayList<>();
        if(samm.getDirection() != DirectionCharacteristic.OUTBOUND) {
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
