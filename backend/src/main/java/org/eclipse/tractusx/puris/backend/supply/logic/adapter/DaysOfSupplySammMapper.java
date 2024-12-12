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
package org.eclipse.tractusx.puris.backend.supply.logic.adapter;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.daysofsupplysamm.AllocatedDaysOfSupply;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.daysofsupplysamm.DaysOfSupply;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.daysofsupplysamm.QuantityOfDaysOfSupply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
public class DaysOfSupplySammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private MaterialService materialService;

    public DaysOfSupply supplierSupplyToSamm(List<List<OwnSupplierSupply>> suppliesBySite, Partner partner, Material material) {
        if (suppliesBySite.stream().anyMatch(siteSupplies -> siteSupplies.stream().anyMatch(sup -> !sup.getPartner().equals(partner)))) {
            log.warn("Can't map supply list with different partners");
            return null;
        }
        if (suppliesBySite.stream().anyMatch(siteSupplies -> siteSupplies.stream().anyMatch(sup -> !sup.getMaterial().equals(material)))) {
            log.warn("Can't map supply list with different materials");
            return null;
        }
        DaysOfSupply samm = new DaysOfSupply();
        var mpr = mprService.findAll().stream().filter(mr -> mr.getMaterial().equals(material) && mr.getPartner().equals(partner)).findFirst().orElse(null);
        if (mpr == null) {
            log.warn("Could not identify materialPartnerRelation with ownMaterialNumber " + material.getOwnMaterialNumber()
                    + " and partner bpnl " + partner.getBpnl());
            return null;
        }
        samm.setMaterialGlobalAssetId(material.getMaterialNumberCx());
        samm.setDirection(DirectionCharacteristic.OUTBOUND);
        var suppliesPerDay = new HashSet<AllocatedDaysOfSupply>();
        samm.setAllocatedDaysOfSupply(suppliesPerDay);
        for (var supplyBySite : suppliesBySite) {
            var allocatedDaysOfSupply = new AllocatedDaysOfSupply();
            allocatedDaysOfSupply.setStockLocationBPNS(supplyBySite.get(0).getStockLocationBPNS());
            allocatedDaysOfSupply.setStockLocationBPNA(supplyBySite.get(0).getStockLocationBPNA());
            allocatedDaysOfSupply.setLastUpdatedOnDateTime(new Date());
            allocatedDaysOfSupply.setAmountOfAllocatedDaysOfSupply(
                supplyBySite.stream().map(sup -> new QuantityOfDaysOfSupply(sup.getDate(), sup.getDaysOfSupply())).toList());
            suppliesPerDay.add(allocatedDaysOfSupply);
        }
        return samm;
    }

    public DaysOfSupply customerSupplyToSamm(List<List<OwnCustomerSupply>> suppliesBySite, Partner partner, Material material) {
        if (suppliesBySite.stream().anyMatch(siteSupplies -> siteSupplies.stream().anyMatch(sup -> !sup.getPartner().equals(partner)))) {
            log.warn("Can't map supply list with different partners");
            return null;
        }
        if (suppliesBySite.stream().anyMatch(siteSupplies -> siteSupplies.stream().anyMatch(sup -> !sup.getMaterial().equals(material)))) {
            log.warn("Can't map supply list with different materials");
            return null;
        }
        var mpr = mprService.findAll().stream().filter(mr -> mr.getMaterial().equals(material) && mr.getPartner().equals(partner)).findFirst().orElse(null);
        if (mpr == null) {
            log.warn("Could not identify materialPartnerRelation with ownMaterialNumber " + material.getOwnMaterialNumber()
            + " and partner bpnl " + partner.getBpnl());
            return null;
        }
        DaysOfSupply samm = new DaysOfSupply();
        samm.setMaterialGlobalAssetId(mpr.getPartnerCXNumber());
        samm.setDirection(DirectionCharacteristic.INBOUND);
        var suppliesPerDay = new HashSet<AllocatedDaysOfSupply>();
        samm.setAllocatedDaysOfSupply(suppliesPerDay);
        for (var supplyBySite : suppliesBySite) {
            var allocatedDaysOfSupply = new AllocatedDaysOfSupply();
            allocatedDaysOfSupply.setStockLocationBPNS(supplyBySite.get(0).getStockLocationBPNS());
            allocatedDaysOfSupply.setStockLocationBPNA(supplyBySite.get(0).getStockLocationBPNA());
            allocatedDaysOfSupply.setLastUpdatedOnDateTime(new Date());
            allocatedDaysOfSupply.setAmountOfAllocatedDaysOfSupply(
                supplyBySite.stream().map(sup -> new QuantityOfDaysOfSupply(sup.getDate(), sup.getDaysOfSupply())).toList());
            suppliesPerDay.add(allocatedDaysOfSupply);
        }
        return samm;
    }

    public List<ReportedCustomerSupply> sammToReportedCustomerSupply(DaysOfSupply samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedCustomerSupply> outputList = new ArrayList<>();
        var material = materialService.findByMaterialNumberCx(matNbrCatenaX);
        if (material == null) {
            log.warn("Could not identify material with given CatenaXNbr ");
            return outputList;
        }
        for (var allocatedSupplies : samm.getAllocatedDaysOfSupply()) {
            for (var supply : allocatedSupplies.getAmountOfAllocatedDaysOfSupply()) {
                var builder = ReportedCustomerSupply.builder();
                var reportedCustomerSupply = builder
                        .date(supply.getDate())
                        .daysOfSupply(supply.getDaysOfSupply())
                        .material(material)
                        .partner(partner)
                        .stockLocationBPNA(allocatedSupplies.getStockLocationBPNA())
                        .stockLocationBPNS(allocatedSupplies.getStockLocationBPNS())
                        .build();
                outputList.add(reportedCustomerSupply);
            }
        }
        return outputList;
    }

    public List<ReportedSupplierSupply> sammToReportedSupplierSupply(DaysOfSupply samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedSupplierSupply> outputList = new ArrayList<>();
        var mpr = mprService.findByPartnerAndPartnerCXNumber(partner, matNbrCatenaX);
        if (mpr == null) {
            log.warn("Could not identify material partner relation with given partner and Material Number Cx");
            return outputList;
        }
        var material = mpr.getMaterial();
        for (var allocatedSupplies : samm.getAllocatedDaysOfSupply()) {
            for (var supply : allocatedSupplies.getAmountOfAllocatedDaysOfSupply()) {
                var builder = ReportedSupplierSupply.builder();
                var reportedSupplierSupply = builder
                        .date(supply.getDate())
                        .daysOfSupply(supply.getDaysOfSupply())
                        .material(material)
                        .partner(partner)
                        .stockLocationBPNA(allocatedSupplies.getStockLocationBPNA())
                        .stockLocationBPNS(allocatedSupplies.getStockLocationBPNS())
                        .build();
                outputList.add(reportedSupplierSupply);
            }
        }
        return outputList;
    }
}
