/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.tractusx.puris.backend.delivery.logic.service.DeliveryRequestApiService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.DemandRequestApiService;
import org.eclipse.tractusx.puris.backend.production.logic.service.ProductionRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.DaysOfSupplyRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaterialRefreshService {
    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private DemandRequestApiService demandRequestApiService;

    @Autowired
    private ProductionRequestApiService productionRequestApiService;

    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;

    @Autowired
    private DeliveryRequestApiService deliveryRequestApiService;

    @Autowired
    private DaysOfSupplyRequestApiService daysOfSupplyRequestApiService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void refreshPartnerData(String ownMaterialNumber) {
        var material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        var customers = partnerService.findAllCustomerPartnersForMaterialId(ownMaterialNumber);
        var suppliers = partnerService.findAllSupplierPartnersForMaterialId(ownMaterialNumber);
        var allPartners = new ArrayList<>(customers);
        allPartners.addAll(suppliers);
        var numberOfTasks = (customers.size() + suppliers.size()) * 3 + allPartners.size();
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfTasks);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        customers.forEach(customer -> {
            futures.add(CompletableFuture.runAsync(
                    () -> demandRequestApiService.doReportedDemandRequest(customer, material),
                    executorService));
            futures.add(
                    CompletableFuture.runAsync(
                            () -> itemStockRequestApiService
                                    .doItemStockSubmodelReportedProductItemStockRequest(
                                            customer, material),
                            executorService));
            futures.add(
                    CompletableFuture.runAsync(
                            () -> daysOfSupplyRequestApiService
                                    .doReportedDaysOfSupplyRequest(customer,
                                            material,
                                            DirectionCharacteristic.INBOUND),
                            executorService));
        });
        suppliers.forEach(supplier -> {
            futures.add(CompletableFuture.runAsync(
                    () -> productionRequestApiService.doReportedProductionRequest(supplier,
                            material),
                    executorService));
            futures.add(
                    CompletableFuture.runAsync(
                            () -> itemStockRequestApiService
                                    .doItemStockSubmodelReportedMaterialItemStockRequest(
                                            supplier, material),
                            executorService));
            futures.add(
                    CompletableFuture.runAsync(
                            () -> daysOfSupplyRequestApiService
                                    .doReportedDaysOfSupplyRequest(supplier,
                                            material,
                                            DirectionCharacteristic.OUTBOUND),
                            executorService));
        });
        allPartners.forEach(partner -> {
            futures.add(CompletableFuture.runAsync(
                    () -> deliveryRequestApiService.doReportedDeliveryRequest(partner, material),
                    executorService));
        });
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            var topic = "/topic/material/" + material.getOwnMaterialNumber();
            messagingTemplate.convertAndSend(topic, "SUCCESS");
            log.info("Refreshed Material data for " + material.getOwnMaterialNumber());
        });
        executorService.shutdown();
    }
}
