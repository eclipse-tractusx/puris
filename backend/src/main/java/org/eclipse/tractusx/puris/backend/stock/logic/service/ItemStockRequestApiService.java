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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.erpadapter.logic.service.ErpAdapterTriggerService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
/**
 * This class is a Service that handles requests for MaterialItemStocks or ProductItemStocks.
 */
public class ItemStockRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private ProductItemStockService productItemStockService;
    @Autowired
    private MaterialItemStockService materialItemStockService;
    @Autowired
    private ReportedProductItemStockService reportedProductItemStockService;
    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;
    @Autowired
    private ErpAdapterTriggerService erpAdapterTriggerService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private ItemStockSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public ItemStockSamm handleItemStockSubmodelRequest(String bpnl, String materialNumber, DirectionCharacteristic direction) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL " + bpnl);
            return null;
        }
        switch (direction) {
            case OUTBOUND -> {
                // Partner is customer, requesting our ProductItemStocks for him
                // materialNumber is own CX id:
                Material material = materialService.findByMaterialNumberCx(materialNumber);
                if (material != null && mprService.find(material, partner).isPartnerBuysMaterial()) {
                    // only send an answer if partner is registered as customer
                    var currentStocks = productItemStockService.findByPartnerAndMaterial(partner, material);

                    erpAdapterTriggerService.notifyPartnerRequest(bpnl, material.getOwnMaterialNumber(), AssetType.ITEM_STOCK_SUBMODEL, direction);

                    return sammMapper.productItemStocksToItemStockSamm(currentStocks, partner, material);
                }
                return null;
            }
            case INBOUND -> {
                // Partner is supplier, requesting our MaterialItemStocks from him
                // materialNumber is partner's CX id:
                Material material = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumber).getMaterial();
                if (material == null) {
                    // Could not identify partner cx number. I.e. we do not have that partner's
                    // CX id in one of our MaterialPartnerRelation entities. Try to fix this by
                    // looking for MPR's, where that partner is a supplier and where we don't have
                    // a partnerCXId yet. Of course this can only work if there was previously an MPR
                    // created, but for some unforeseen reason, the initial PartTypeRetrieval didn't succeed.
                    log.warn("Could not find " + materialNumber + " from partner " + partner.getBpnl());
                    mprService.triggerPartTypeRetrievalTask(partner);
                    material = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumber).getMaterial();
                }

                if (material == null) {
                    log.error("Unknown Material");
                    return null;
                }
                var mpr = mprService.find(material, partner);
                if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
                    // only send an answer if partner is registered as supplier
                    return null;
                }

                // request looks valid
                erpAdapterTriggerService.notifyPartnerRequest(bpnl, material.getOwnMaterialNumber(), AssetType.ITEM_STOCK_SUBMODEL, direction);
                var currentStocks = materialItemStockService.findByPartnerAndMaterial(partner, material);

                return sammMapper.materialItemStocksToItemStockSamm(currentStocks, partner, material);

            }
            default -> {
                return null;
            }
        }

    }

    public void doItemStockSubmodelReportedMaterialItemStockRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            var data = edcAdapterService.doSubmodelRequest(AssetType.ITEM_STOCK_SUBMODEL, mpr, DirectionCharacteristic.OUTBOUND, 1);
            var samm = objectMapper.treeToValue(data, ItemStockSamm.class);
            var stocks = sammMapper.itemStockSammToReportedMaterialItemStock(samm, partner);
            for (var stock : stocks) {
                var stockPartner = stock.getPartner();
                var stockMaterial = stock.getMaterial();
                if (!partner.equals(stockPartner) || !material.equals(stockMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl() + "\n" + stocks);
                    return;
                }
            }
            var oldStocks = reportedMaterialItemStockService.findByPartnerAndMaterial(partner, material);
            for (var oldStock : oldStocks) {
                reportedMaterialItemStockService.delete(oldStock.getUuid());
            }
            for (var newStock : stocks) {
                reportedMaterialItemStockService.create(newStock);
            }
            log.info("Updated ReportedMaterialItemStocks for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());

            materialService.updateTimestamp(material.getOwnMaterialNumber());
        } catch (Exception e) {
            log.error("Error in ReportedMaterialItemStockRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }

    public void doItemStockSubmodelReportedProductItemStockRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            if (mpr.getPartnerCXNumber() == null) {
                mprService.triggerPartTypeRetrievalTask(partner);
                mpr = mprService.find(material, partner);
            }
            var data = edcAdapterService.doSubmodelRequest(AssetType.ITEM_STOCK_SUBMODEL ,mpr, DirectionCharacteristic.INBOUND, 1);
            var samm = objectMapper.treeToValue(data, ItemStockSamm.class);
            var stocks = sammMapper.itemStockSammToReportedProductItemStock(samm, partner);
            for (var stock : stocks) {
                var stockPartner = stock.getPartner();
                var stockMaterial = stock.getMaterial();
                if (!partner.equals(stockPartner) || !material.equals(stockMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl() + "\n" + stocks);
                    return;
                }
            }
            // delete older data:
            var oldStocks = reportedProductItemStockService.findByPartnerAndMaterial(partner, material);
            for (var oldStock : oldStocks) {
                reportedProductItemStockService.delete(oldStock.getUuid());
            }
            for (var newStock : stocks) {
                reportedProductItemStockService.create(newStock);
            }
            log.info("Updated ReportedProductItemStocks for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());

            materialService.updateTimestamp(material.getOwnMaterialNumber());
        } catch (Exception e) {
            log.error("Error in ReportedProductItemStockRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }

}
