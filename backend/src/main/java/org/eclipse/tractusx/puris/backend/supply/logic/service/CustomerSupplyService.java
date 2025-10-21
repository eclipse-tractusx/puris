/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedCustomerSupplyRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupplyService extends SupplyService<OwnCustomerSupply, ReportedCustomerSupply, ReportedCustomerSupplyRepository, MaterialItemStock, MaterialItemStockService> {
    
    private OwnDeliveryService ownDeliveryService;
    private ReportedDeliveryService reportedDeliveryService;
    private OwnDemandService demandService;
    
    public CustomerSupplyService(MaterialItemStockService stockService, MaterialService materialService,
            PartnerService partnerService, ReportedCustomerSupplyRepository repository, OwnDeliveryService ownDeliveryService, ReportedDeliveryService reportedDeliveryService, OwnDemandService demandService) {
        super(stockService, materialService, partnerService, repository);
        this.ownDeliveryService = ownDeliveryService;
        this.reportedDeliveryService = reportedDeliveryService;
        this.demandService = demandService;
    }

    @Override
    protected OwnCustomerSupply createSupplyInstance() {
        return new OwnCustomerSupply();
    }

    @Override
    protected List<Double> getAddedValues(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays) {
        List<Double> ownDeliveries = ownDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, DirectionCharacteristic.INBOUND, numberOfDays);
        List<Double> reportedDeliveries = reportedDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, DirectionCharacteristic.INBOUND, numberOfDays);
        List<Double> deliveries = mergeDeliveries(ownDeliveries, reportedDeliveries);
        return deliveries;
    }

    @Override
    protected List<Double> getConsumedValues(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays) {
        List<Double> demands = demandService.getQuantityForDays(material, partnerBpnl, siteBpns, numberOfDays);
        return demands;
    }

    /**
     * Calculates the customer's days of supply for a given material, partner, and site over a specified number of days.
     * It combines own and reported deliveries, and demand quantities to forecast the number of days the stock will last.
     *
     * @param material the material identifier for which the days of supply are being calculated.
     * @param partnerBpnl The bpnl of the customer's partner.
     * @param siteBpns the bpns of the site where the deliveries and demands are recorded.
     * @param numberOfDays the number of days over which the forecast should be calculated.
     * @return a list of {@link OwnCustomerSupply} objects, each containing the calculated days of supply for a specific date.
     */
    public List<OwnCustomerSupply> calculateCustomerDaysOfSupply(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays) {
        return calculateDaysOfSupply(material, partnerBpnl, siteBpns, numberOfDays);
    }
    
    public final List<ReportedCustomerSupply> findAllByMaterialNumberAndPartnerBpnl(String ownMaterialNumber, String partnerBpnl) {
        return repository.findByMaterial_OwnMaterialNumberAndPartner_Bpnl(ownMaterialNumber, partnerBpnl);
    }

    public final ReportedCustomerSupply findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final List<ReportedCustomerSupply> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpnl, Optional<String> siteBpns) {
        Stream<ReportedCustomerSupply> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (siteBpns.isPresent()) {
            stream = stream.filter(daysOfSupply -> daysOfSupply.getStockLocationBPNS().equals(siteBpns.get()));
        }
        return stream.toList();
    }

    public boolean validate(ReportedCustomerSupply daysOfSupply) {
        return validateWithDetails(daysOfSupply).isEmpty();
    }

    public List<String> validateWithDetails(ReportedCustomerSupply daysOfSupply) {
        List<String> validationErrors = new ArrayList<>();
        validationErrors.addAll(basicValidation(daysOfSupply));
        return validationErrors;
    }
}
