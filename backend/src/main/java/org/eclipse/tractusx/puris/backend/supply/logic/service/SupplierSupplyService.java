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

package org.eclipse.tractusx.puris.backend.supply.logic.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialServiceImpl;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.logic.service.OwnProductionService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedSupplierSupplyRepository;
import org.springframework.stereotype.Service;

@Service
public class SupplierSupplyService {
    private final ReportedSupplierSupplyRepository repository; 
    private final PartnerService partnerService;
    private final OwnDeliveryService ownDeliveryService;
    private final ReportedDeliveryService reportedDeliveryService;
    private final OwnProductionService productionService;
    private final MaterialItemStockService stockService;
    private final MaterialServiceImpl materialService;

    protected final Function<ReportedSupplierSupply, Boolean> validator;

    public SupplierSupplyService(
        ReportedSupplierSupplyRepository repository,
        PartnerService partnerService,
        OwnDeliveryService ownDeliveryService,
        ReportedDeliveryService reportedDeliveryService,
        OwnProductionService productionService,
        MaterialItemStockService stockService,
        MaterialServiceImpl materialService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.ownDeliveryService = ownDeliveryService;
        this.reportedDeliveryService = reportedDeliveryService;
        this.productionService = productionService;
        this.stockService = stockService;
        this.materialService = materialService;
        this.validator = this::validate;
    }

        public final List<OwnSupplierSupply> calculateSupplierDaysOfSupply(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<OwnSupplierSupply> supplierSupply = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        List<Double> ownDeliveries = ownDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, DirectionCharacteristic.OUTBOUND, numberOfDays);
        List<Double> reportedDeliveries = reportedDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, DirectionCharacteristic.OUTBOUND, numberOfDays);
        List<Double> deliveries = mergeDeliveries(ownDeliveries, reportedDeliveries);
        List<Double> productions = productionService.getQuantityForDays(material, partnerBpnl, siteBpns, numberOfDays);
        double stockQuantity = stockService.getInitialStockQuantity(material, partnerBpnl);
        
        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (i == numberOfDays - 1) {
                stockQuantity += productions.get(i);
            }

            double daysOfSupply = getDaysOfSupply(
                stockQuantity,
                deliveries.subList(i, deliveries.size()));

            OwnSupplierSupply supply = new OwnSupplierSupply();
            supply.setMaterial(materialService.findByOwnMaterialNumber(material));
            supply.setDate(date);
            supply.setDaysOfSupply(daysOfSupply);
            supplierSupply.add(supply);

            stockQuantity = stockQuantity - deliveries.get(i) + productions.get(i);

            localDate = localDate.plusDays(1);
        }

        return supplierSupply;
    }

    public final List<ReportedSupplierSupply> findAll() {
        return repository.findAll();
    }

    public final ReportedSupplierSupply findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final List<ReportedSupplierSupply> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpnl) {
        Stream<ReportedSupplierSupply> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getPartner().getBpnl().equals(bpnl.get()));
        } 
        return stream.toList();
    }

    public boolean validate(ReportedSupplierSupply daysOfSupply) {
        return 
            daysOfSupply.getPartner() != null &&
            daysOfSupply.getMaterial() != null &&
            daysOfSupply.getDate() != null &&
            daysOfSupply.getStockLocationBPNS() != null &&
            daysOfSupply.getPartner() != partnerService.getOwnPartnerEntity() &&
            daysOfSupply.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(daysOfSupply.getStockLocationBPNS())) &&
            (daysOfSupply.getStockLocationBPNA() == null || daysOfSupply.getStockLocationBPNA() == daysOfSupply.getStockLocationBPNS());
    }

    private final double getDaysOfSupply(double stockQuantity, List<Double> demands) {
        double daysOfSupply = 0;

        for (int i = 0; i < demands.size(); i++) {
            double demand = demands.get(i);

            if (stockQuantity >= demand) {
                daysOfSupply += 1;
                stockQuantity = stockQuantity - demand;
            } else if (stockQuantity < demand && stockQuantity > 0) {
                double fractional = stockQuantity / demand;
                daysOfSupply = daysOfSupply + fractional;
                break;
            } else {
                break;
            }
        }
        return daysOfSupply;
    }

    private static List<Double> mergeDeliveries(List<Double> list1, List<Double> list2) {
        if (list1.size() != list2.size()) {
            throw new IllegalArgumentException("Lists must be of the same length");
        }

        List<Double> mergedList = new ArrayList<>(list1.size());

        for (int i = 0; i < list1.size(); i++) {
            mergedList.add(list1.get(i) + list2.get(i));
        }

        return mergedList;
    }
}
