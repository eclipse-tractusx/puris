/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.erp_adapter.logic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.erp_adapter.controller.ErpAdapterController;
import org.eclipse.tractusx.puris.backend.erp_adapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ItemStockErpAdapterService {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ErpAdapterRequestService erpAdapterRequestService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private ItemStockSammMapper sammMapper;

    @Autowired
    private MaterialItemStockService materialItemStockService;
    @Autowired
    private ProductItemStockService productItemStockService;


    public void receiveItemStockUpdate(ErpAdapterController.Dto dto) {
        try {
            ItemStockSamm samm = mapper.treeToValue(dto.responseBody(), ItemStockSamm.class);
            Partner partner = partnerService.findByBpnl(dto.partnerBpnl());
            ErpAdapterRequest request = erpAdapterRequestService.get(dto.requestId());
            Material material = materialService.findByOwnMaterialNumber(request.getOwnMaterialNumber());

            switch (samm.getDirection()) {
                case INBOUND -> {
                    var mpr = mprService.find(material, partner);
                    if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
                        log.error("Partner {} is not registered as supplier for {}", partner.getBpnl(), material.getOwnMaterialNumber());
                        return;
                    }
                    List<MaterialItemStock> materialItemStockList = sammMapper.erpSammToMaterialItemStock(samm, partner, material);
                    materialItemStockService.findByPartnerAndMaterial(partner, material).forEach(stock -> materialItemStockService.delete(stock.getUuid()));
                    materialItemStockList.forEach(stock -> materialItemStockService.create(stock));
                    log.info("Inserted {} MaterialItemStocks for {} and {}", materialItemStockList.size(), material.getOwnMaterialNumber(), partner.getBpnl());
                    request.setResponseReceivedDate(new Date());
                    erpAdapterRequestService.update(request);
                }
                case OUTBOUND -> {
                    var mpr = mprService.find(material, partner);
                    if (mpr == null || !mpr.isPartnerBuysMaterial()) {
                        log.error("Partner {} is not registered as customer for {}", partner.getBpnl(), material.getOwnMaterialNumber());
                        return;
                    }
                    List<ProductItemStock> productItemStockList = sammMapper.erpSammToProductItemStock(samm, partner, material);
                    productItemStockService.findByPartnerAndMaterial(partner, material).forEach(stock -> productItemStockService.delete(stock.getUuid()));
                    productItemStockList.forEach(stock -> productItemStockService.create(stock));
                    log.info("Inserted {} ProductItemStocks for {} and {}", productItemStockList.size(), material.getOwnMaterialNumber(), partner.getBpnl());
                    request.setResponseReceivedDate(new Date());
                    erpAdapterRequestService.update(request);
                }
                default -> throw new IllegalArgumentException("Invalid direction: " + samm.getDirection());
            }

        } catch (Exception e) {
            log.error("Error while receiving erp itemstock update", e);
        }
    }
}
