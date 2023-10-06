/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockResponse;
import org.eclipse.tractusx.puris.backend.stock.domain.model.Stock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implements the handling of a response for Product Stock
 * <p>
 * That means that one need to save
 * {@link org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock} according to the
 * API specification.
 */
@Component
@Slf4j
public class ProductStockResponseApiServiceImpl {

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;


    public void consumeResponse(ProductStockResponse response) {
        ProductStockRequest correspondingProductStockRequest = productStockRequestService.findRequestByHeaderUuid(response.getHeader().getRequestId());
        if (correspondingProductStockRequest == null) {
            log.error("Received Response without corresponding request\n" + response);
            return;
        }
        Partner partner = partnerService.findByBpnl(response.getHeader().getSender());
        if(partner == null) {
            log.error("Received response from unknown Partner\n" + response);
            return;
        }
        ArrayList<PartnerProductStock> consolidatedStocks = new ArrayList<>();
        for(var sammDto : response.getContent().getProductStocks()) {
            List<PartnerProductStock> foundStocks = productStockSammMapper.sammToPartnerProductStocks(sammDto, partner);
            if(!foundStocks.isEmpty()) {
                // Per definition each instance of the ProductStockSammDto contains
                // information about exactly one instance of Material.
                // If locationIds of any pair of foundStocks do match,
                // then these two should be merged.

                // Create lists of Stocks with same location
                var groupedByLocations = foundStocks.stream().collect(Collectors.groupingBy(Stock::getAtSiteBpns, Collectors.toList()));
                for(var locationGrouping : groupedByLocations.entrySet()) {
                    // From each grouping with same location, create sub-lists with same MeasurementUnit
                    var groupedByMeasurementUnit = locationGrouping.getValue().stream().collect(Collectors.groupingBy(
                        Stock::getMeasurementUnit, Collectors.toList()));
                    for (var entry : groupedByMeasurementUnit.values()) {
                        // Aggregate Stocks with same location and MeasurementUnit
                        if(entry.size() > 1) {
                            PartnerProductStock baseStock = entry.get(0);
                            baseStock.setQuantity(0);
                            PartnerProductStock aggregatedStock = entry.stream().reduce(baseStock, (stockA, stockB) -> {
                                stockA.setQuantity(stockA.getQuantity() + stockB.getQuantity());
                                return stockA;
                            });
                            consolidatedStocks.add(aggregatedStock);
                        } else {
                            consolidatedStocks.add(entry.get(0));
                        }
                    }
                }
            }
        }

        // Check whether a new PartnerProductStock must be created or whether
        // an existing PartnerProductStock gets updated.
        for (PartnerProductStock newStockData : consolidatedStocks) {
            var existingPartnerProductStocks = partnerProductStockService.findAllByPartnerAndMaterialAndLocationAndMeasurementUnit(
                newStockData.getSupplierPartner(), newStockData.getMaterial(), newStockData.getAtSiteBpns(), newStockData.getMeasurementUnit());
            // There should be at most one instance in that list, because we are aggregating them (see above)
            if(existingPartnerProductStocks.size()>1) {
                log.warn("Found multiple instances of PartnerProductStock: \n" + existingPartnerProductStocks);
            }
            if(existingPartnerProductStocks.isEmpty()) {
                // create new PartnerProductStock in database:
                partnerProductStockService.create(newStockData);
            } else {
                // update existing PartnerProductStock
                PartnerProductStock existingPartnerProductStock = existingPartnerProductStocks.get(0);
                existingPartnerProductStock.setQuantity(newStockData.getQuantity());
                partnerProductStockService.update(existingPartnerProductStock);
            }
        }
        productStockRequestService.updateState(correspondingProductStockRequest, DT_RequestStateEnum.Completed);
    }

}
