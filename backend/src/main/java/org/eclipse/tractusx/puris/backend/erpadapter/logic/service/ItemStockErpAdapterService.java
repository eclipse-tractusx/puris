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

package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.erpadapter.controller.ErpAdapterController;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
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

    private final static String SUPPORTEDSAMMVERSION = "2.0";


    /**
     * This method handles a response for an ItemStock Request from the ERP Adapter.
     * It's return value is the status code that is being sent back to the ERP Adapter.
     *
     * @param   dto contains the parameters and the body of the response message
     * @return  the appropriate HTTP response code
     */
    public int receiveItemStockUpdate(ErpAdapterController.Dto dto) {
        try {
            ItemStockSamm samm = mapper.treeToValue(dto.body(), ItemStockSamm.class);
            ErpAdapterRequest request = erpAdapterRequestService.get(dto.requestId());
            if (request == null) {
                log.error("Unknown request-id {}", dto.requestId());
                return 404;
            }
            // TODO: uncomment the following block when removing mock request, also edit swagger description in ErpAdapterController
//            if (request.getResponseReceivedDate() != null) {
//                log.error("Received duplicate response for messageId {}", request.getId());
//                return 409;
//            }
            if (request.getResponseCode() == null || request.getResponseCode() < 200 || request.getResponseCode() >= 400) {
                log.error("Unexpected response, erp adapter had not confirmed request");
                return 404;
            }
            if (!request.getPartnerBpnl().equals(dto.partnerBpnl())) {
                log.error("BPNL mismatch! request BPNL: {}, message BPNL: {}",
                    request.getPartnerBpnl(), dto.partnerBpnl());
                return 400;
            }
            if (!request.getDirectionCharacteristic().equals(samm.getDirection())) {
                log.error("Direction mismatch! request direction: {}, message direction: {}",
                    request.getDirectionCharacteristic(), samm.getDirection());
                return 400;
            }
            if (!SUPPORTEDSAMMVERSION.equals(dto.sammVersion()) || !SUPPORTEDSAMMVERSION.equals(request.getSammVersion())) {
                log.error("Unsupported Samm Version! Supported: " + SUPPORTEDSAMMVERSION + ", request: {}, message: {}",
                    request.getSammVersion(), dto.sammVersion());
                return 400;
            }

            Partner partner = partnerService.findByBpnl(request.getPartnerBpnl());
            Material material = materialService.findByOwnMaterialNumber(request.getOwnMaterialNumber());

            switch (samm.getDirection()) {
                case INBOUND -> {
                    var mpr = mprService.find(material, partner);
                    if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
                        log.error("Partner {} is not registered as supplier for {}", partner.getBpnl(), material.getOwnMaterialNumber());
                        return 400;
                    }
                    List<MaterialItemStock> materialItemStockList = sammMapper.erpSammToMaterialItemStock(samm, partner, material);
                    int initialSize = materialItemStockList.size();
                    materialItemStockList.removeIf(stock -> !materialItemStockService.validate(stock));
                    int removed = initialSize - materialItemStockList.size();
                    if (removed > 0) {
                        log.warn("Removed {} out of {} MaterialItemStocks because of failing validation.", removed, initialSize);
                    }
                    materialItemStockService.findByPartnerAndMaterial(partner, material).forEach(stock -> materialItemStockService.delete(stock.getUuid()));
                    materialItemStockList.forEach(stock -> materialItemStockService.create(stock));
                    log.info("Inserted {} MaterialItemStocks for {} and {}", materialItemStockList.size(), material.getOwnMaterialNumber(), partner.getBpnl());
                    request.setResponseReceivedDate(dto.responseTimeStamp());
                    erpAdapterRequestService.update(request);
                    return 201;
                }
                case OUTBOUND -> {
                    var mpr = mprService.find(material, partner);
                    if (mpr == null || !mpr.isPartnerBuysMaterial()) {
                        log.error("Partner {} is not registered as customer for {}", partner.getBpnl(), material.getOwnMaterialNumber());
                        return 400;
                    }
                    List<ProductItemStock> productItemStockList = sammMapper.erpSammToProductItemStock(samm, partner, material);
                    int initialSize = productItemStockList.size();
                    productItemStockList.removeIf(stock -> !productItemStockService.validate(stock));
                    int removed = initialSize - productItemStockList.size();
                    if (removed > 0) {
                        log.warn("Removed {} out of {} ProductItemStocks because of failing validation.", removed, initialSize);
                    }
                    productItemStockService.findByPartnerAndMaterial(partner, material).forEach(stock -> productItemStockService.delete(stock.getUuid()));
                    productItemStockList.forEach(stock -> productItemStockService.create(stock));
                    log.info("Inserted {} ProductItemStocks for {} and {}", productItemStockList.size(), material.getOwnMaterialNumber(), partner.getBpnl());
                    request.setResponseReceivedDate(dto.responseTimeStamp());
                    erpAdapterRequestService.update(request);
                    return 201;
                }
            }

        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof JsonProcessingException) {
                // treeToValue method has thrown this exception
                log.error("Error parsing request body: \n{}", dto.body().toPrettyString(), e);
                return 400;
            } else {
                log.error("Error while receiving erp itemstock update", e);
                return 500;
            }
        }
        log.error("Unexpected error while receiving erp itemstock update");
        return 500;
    }
}
