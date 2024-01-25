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
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@Slf4j
/**
 * This class provides a service to handle an item stock response
 * message received from a customer or supplier partner.
 */
public class ItemStockResponseApiService {
    @Autowired
    private ItemStockSammMapper sammMapper;
    @Autowired
    private ReportedProductItemStockService reportedProductItemStockService;
    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;
    @Autowired
    private ItemStockRequestMessageService itemStockRequestMessageService;

    /**
     * This method should be called asynchronously. It will generate new ReportedMaterialItemStock
     * or ReportedProductItemStock entities from the given response and store them to the database.
     * <p>
     * Additionally, all previous entities of the respective ItemStock-type with the same Partner and Material
     * will be removed from the database.
     *
     * @param responseDto the Response - Dto
     * @param partner the partner you received the message from
     * @param initialRequest the initial request that the response is meant to answer to
     */
    public void consumeResponse(ItemStockResponseDto responseDto, Partner partner, ItemStockRequestMessage initialRequest){
        HashSet<ReportedMaterialItemStock> oldReportedMaterialItemStocks = new HashSet<>();
        HashSet<ReportedMaterialItemStock> newReportedMaterialItemStocks = new HashSet<>();
        HashSet<ReportedProductItemStock> oldReportedProductItemStocks = new HashSet<>();
        HashSet<ReportedProductItemStock> newReportedProductItemStocks = new HashSet<>();
        for (var itemStockSamm : responseDto.getContent().getItemStock()) {
            switch (itemStockSamm.getDirection()) {
                case INBOUND -> {
                    // ReportedProductItemStock
                    var reportedProductItemStocks = sammMapper.itemStockSammToReportedProductItemStock(itemStockSamm, partner);
                    for (var reportedProductItemStock : reportedProductItemStocks) {
                        oldReportedProductItemStocks.addAll(reportedProductItemStockService.findByPartnerAndMaterial(reportedProductItemStock.getPartner(), reportedProductItemStock.getMaterial()));
                        newReportedProductItemStocks.add(reportedProductItemStock);
                    }
                }
                case OUTBOUND -> {
                    // ReportedMaterialItemStock
                    var reportedMaterialItemStocks = sammMapper.itemStockSammToReportedMaterialItemStock(itemStockSamm, partner);
                    for (var reportedMaterialItemStock : reportedMaterialItemStocks) {
                        oldReportedMaterialItemStocks.addAll(reportedMaterialItemStockService.findByPartnerAndMaterial(reportedMaterialItemStock.getPartner(), reportedMaterialItemStock.getMaterial()));
                        newReportedMaterialItemStocks.add(reportedMaterialItemStock);
                    }
                }
                default -> log.error("Missing direction in Samm object");
            }
        }
        // Remove older reported ItemStocks
        for (var oldReportedMaterialItemStock : oldReportedMaterialItemStocks) {
            reportedMaterialItemStockService.delete(oldReportedMaterialItemStock.getUuid());
        }
        for (var oldReportedProductItemStock : oldReportedProductItemStocks) {
            reportedProductItemStockService.delete(oldReportedProductItemStock.getUuid());
        }

        // Store newly received reported ItemStocks
        for (var newReportedMaterialItemStock : newReportedMaterialItemStocks) {
            reportedMaterialItemStockService.create(newReportedMaterialItemStock);
        }

        for (var newReportedProductItemStock : newReportedProductItemStocks) {
            reportedProductItemStockService.create(newReportedProductItemStock);
        }
        initialRequest.setState(DT_RequestStateEnum.Completed);
        itemStockRequestMessageService.update(initialRequest);

    }
}
