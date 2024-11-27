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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.logic.adapter.DaysOfSupplySammMapper;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.daysofsupplysamm.DaysOfSupply;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DaysOfSupplyRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private SupplierSupplyService supplierSupplyService;
    @Autowired
    private CustomerSupplyService customerSupplyService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DaysOfSupplySammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ModelMapper modelMapper;

    public DaysOfSupply handleDaysOfSupplySubmodelRequest(String bpnl, String materialNumberCx, DirectionCharacteristic direction) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        MaterialPartnerRelation mpr = switch (direction) {
            case OUTBOUND -> mprService.findAll().stream()
                    .filter(m -> m.getPartner().equals(partner) && m.getMaterial().getMaterialNumberCx().equals(materialNumberCx))
                    .findFirst().orElse(null);
            case INBOUND -> mprService.findByPartnerAndPartnerCXNumber(partner, materialNumberCx);
        };
        if (mpr == null) {
            log.error("Could not identify Material-Partner Relation with material number " + materialNumberCx + " and partner bpnl " + partner.getBpnl());
            return null;
        }
        Material material = mpr.getMaterial();

        var sites = partnerService.getOwnPartnerEntity().getSites();
        if (direction == DirectionCharacteristic.OUTBOUND) {
            List<List<OwnSupplierSupply>> suppliesBySite = new ArrayList<>();
            for (var site : sites) {
                var supplierSupply = supplierSupplyService.calculateSupplierDaysOfSupply(
                        material.getOwnMaterialNumber(), partner.getBpnl(), site.getBpns(), 28);
                supplierSupply.forEach(supply -> {
                    supply.setStockLocationBPNS(site.getBpns());
                    supply.setStockLocationBPNA(site.getAddresses().first().getBpna());
                });
                suppliesBySite.add(supplierSupply);
            }
            return sammMapper.supplierSupplyToSamm(suppliesBySite, partner, material);
        } else {
            List<List<OwnCustomerSupply>> suppliesBySite = new ArrayList<>();
            for (var site : sites) {
                var customerSupply = customerSupplyService.calculateCustomerDaysOfSupply(
                    material.getOwnMaterialNumber(), partner.getBpnl(), site.getBpns(), 28);
                customerSupply.forEach(supply -> {
                    supply.setStockLocationBPNS(site.getBpns());
                    supply.setStockLocationBPNA(site.getAddresses().first().getBpna());
                });
                suppliesBySite.add(customerSupply);
            }
            return sammMapper.customerSupplyToSamm(suppliesBySite, partner, material);
        }
    }

    public void doReportedDaysOfSupplyRequest(Partner partner, Material material, DirectionCharacteristic direction) {
        try {
            var mpr = mprService.find(material, partner);
            if (mpr.getPartnerCXNumber() == null) {
                mprService.triggerPartTypeRetrievalTask(partner);
                mpr = mprService.find(material, partner);
            }
            var data = edcAdapterService.doSubmodelRequest(AssetType.DAYS_OF_SUPPLY, mpr, direction, 1);
            var samm = objectMapper.treeToValue(data, DaysOfSupply.class);
            if (direction == DirectionCharacteristic.INBOUND) {
                var reportedCustomerSupplies = sammMapper.sammToReportedCustomerSupply(samm, partner);
                for (var reportedCustomerSupply : reportedCustomerSupplies) {
                    var supplyPartner = reportedCustomerSupply.getPartner();
                    var supplyMaterial = reportedCustomerSupply.getMaterial();
                    if (!partner.equals(supplyPartner) || !material.equals(supplyMaterial)) {
                        log.warn("Received inconsistent data from " + partner.getBpnl() + "\n"
                                + reportedCustomerSupplies);
                        return;
                    }
                }
                var oldSupplies = customerSupplyService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()));
                for (var oldSupply : oldSupplies) {
                customerSupplyService.deleteReportedSupply(oldSupply);
                }
                for (var newSupply : reportedCustomerSupplies) {
                    customerSupplyService.createReportedSupply(modelMapper.map(newSupply, ReportedCustomerSupply.class));
                }
            } else {
                var reportedSupplierSupplies = sammMapper.sammToReportedSupplierSupply(samm, partner);
                for (var reportedSupplierSupply : reportedSupplierSupplies) {
                    var supplyPartner = reportedSupplierSupply.getPartner();
                    var supplyMaterial = reportedSupplierSupply.getMaterial();
                    if (!partner.equals(supplyPartner) || !material.equals(supplyMaterial)) {
                        log.warn("Received inconsistent data from " + partner.getBpnl() + "\n"
                                + reportedSupplierSupplies);
                        return;
                    }
                }
                var oldSupplies = supplierSupplyService.findAllByFilters(Optional.of(material.getOwnMaterialNumber()), Optional.of(partner.getBpnl()));
                for (var oldSupply : oldSupplies) {
                supplierSupplyService.deleteReportedSupply(oldSupply);
                }
                for (var newSupply : reportedSupplierSupplies) {
                    supplierSupplyService.createReportedSupply(modelMapper.map(newSupply, ReportedSupplierSupply.class));
                }
            }
            log.info("Updated ReportedSupply for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
        } catch (Exception e) {
            log.error("Error in ReportedDaysOfSupply request for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }
}
