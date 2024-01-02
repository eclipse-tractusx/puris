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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ItemStockRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private ProductItemStockService productItemStockService;

    public void handleRequestFromCustomer(ItemStockRequestMessageDto requestMessage) {
        var header = requestMessage.getHeader();
        Partner customerPartner = partnerService.findByBpnl(header.getSenderBpn());
        if (customerPartner == null) {
            log.error("Unknown Partner in request \n" + requestMessage);
            return;
        }
        if (requestMessage.getContent().getDirection() != DirectionCharacteristic.INBOUND) {
            log.error("Wrong direction in request from customer " + requestMessage);
        }
        for (var productRequest : requestMessage.getContent().getItemStock()) {
            Material product = null;
            if (productRequest.getMaterialGlobalAssetId() != null) {
                product = materialService.findByMaterialNumberCx(productRequest.getMaterialGlobalAssetId());
            }
            if (product == null && productRequest.getMaterialNumberCustomer() != null) {
                product = materialService.findByOwnMaterialNumber(productRequest.getMaterialNumberCustomer());
            }
            if (product == null && productRequest.getMaterialNumberSupplier() != null) {
                var foundMaterials = mprService.findAllByPartnerMaterialNumber(productRequest.getMaterialNumberSupplier());
                foundMaterials = foundMaterials.stream()
                    .filter(Material::isProductFlag)
                    .filter(m -> mprService.partnerOrdersProduct(m, customerPartner))
                    .toList();
                if (!foundMaterials.isEmpty()) {
                    product = foundMaterials.get(0);
                }
                if (foundMaterials.size() > 1) {
                    log.warn("Ambiguous material definition in request \n" + productRequest);
                    log.warn("Arbitrarily choosing " + product.getOwnMaterialNumber());
                }
            }
            if (product == null) {
                log.error("Could not identify material in " + productRequest);
                continue;
            }
            var productItemStocks = productItemStockService.findByPartnerAndMaterial(customerPartner, product);


        }


    }

    public void handleRequestFromSupplier(ItemStockRequestMessageDto requestMessage) {
        var header = requestMessage.getHeader();
    }

    public void doRequestForMaterialItemStocks(Material material, Partner supplierPartner) {
        Partner mySelf = partnerService.getOwnPartnerEntity();
        ItemStockRequestMessage itemStockRequestMessage = new ItemStockRequestMessage();
        itemStockRequestMessage.setReceiverBpn(supplierPartner.getBpnl());
        itemStockRequestMessage.setSenderBpn(mySelf.getBpnl());
        itemStockRequestMessage.setDirection(DirectionCharacteristic.INBOUND);
        var itemStockList = itemStockRequestMessage.getItemStock();
        ItemStockRequestMessage.Request request = new ItemStockRequestMessage.Request();
        request.setMaterialGlobalAssetId(material.getMaterialNumberCx());
        request.setMaterialNumberCustomer(material.getOwnMaterialNumber());
        request.setMaterialNumberSupplier(mprService.find(material, supplierPartner).getPartnerMaterialNumber());
    }

    public void doRequestForProductItemStocks(Material material, Partner customerPartner) {
        Partner mySelf = partnerService.getOwnPartnerEntity();
        ItemStockRequestMessage itemStockRequestMessage = new ItemStockRequestMessage();
        itemStockRequestMessage.setReceiverBpn(customerPartner.getBpnl());
        itemStockRequestMessage.setSenderBpn(mySelf.getBpnl());
        itemStockRequestMessage.setDirection(DirectionCharacteristic.OUTBOUND);
        var itemStockList = itemStockRequestMessage.getItemStock();
        ItemStockRequestMessage.Request request = new ItemStockRequestMessage.Request();
        request.setMaterialGlobalAssetId(material.getMaterialNumberCx());
        request.setMaterialNumberSupplier(material.getOwnMaterialNumber());
        request.setMaterialNumberCustomer(mprService.find(material, customerPartner).getPartnerMaterialNumber());
    }

}
