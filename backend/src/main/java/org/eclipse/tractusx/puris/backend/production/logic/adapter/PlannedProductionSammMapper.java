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
package org.eclipse.tractusx.puris.backend.production.logic.adapter;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PlannedProductionSammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;

    public PlannedProductionOutput ownProductionToSamm(List<OwnProduction> production) {
        if (production == null || production.isEmpty()) {
            log.warn("Can't map empty list");
            return null;
        }
        Partner partner = production.get(0).getPartner();
        Material material = production.get(0).getMaterial();
        if (production.stream().anyMatch(prod -> !prod.getPartner().equals(partner))) {
            log.warn("Can't map production list with different partners");
            return null;
        }

        if (production.stream().anyMatch(prod -> !prod.getMaterial().equals(material))) {
            log.warn("Can't map production list with different materials");
            return null;
        }
        var groupedByPositionAttributes = production
                .stream()
                .collect(Collectors.groupingBy(prod -> new PositionsMappingHelper(
                    prod.getCustomerOrderNumber(),
                    prod.getSupplierOrderNumber(),
                    prod.getCustomerOrderPositionNumber()
                )));
        PlannedProductionOutput samm = new PlannedProductionOutput();

        samm.setMaterialGlobalAssetId(material.getMaterialNumberCx());

        var posList = new HashSet<Position>();
        samm.setPositions(posList);
        var currentDate = new Date();
        for (var mappingHelperListEntry : groupedByPositionAttributes.entrySet()) {
            var key = mappingHelperListEntry.getKey();
            var prod = mappingHelperListEntry.getValue().get(0);
            Position position = new Position();
            posList.add(position);
            if (key.customerOrderId != null || key.customerOrderPositionId != null || key.supplierOrderId != null) {
                OrderPositionReference opr = new OrderPositionReference(
                    prod.getSupplierOrderNumber(),
                    prod.getCustomerOrderNumber(),
                    prod.getCustomerOrderPositionNumber()
                );
                position.setOrderPositionReference(opr);
            }
            var allocatedProductionList = new HashSet<AllocatedPlannedProductionOutput>();
            position.setAllocatedPlannedProductionOutputs(allocatedProductionList);
            for (var v : mappingHelperListEntry.getValue()) {
                ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(v.getQuantity(), v.getMeasurementUnit());
                AllocatedPlannedProductionOutput allocatedProduction = new AllocatedPlannedProductionOutput(
                        itemQuantityEntity, v.getProductionSiteBpns(), v.getEstimatedTimeOfCompletion(), currentDate);
                allocatedProductionList.add(allocatedProduction);
            }
        }
        return samm;
    }

    public List<ReportedProduction> sammToReportedProduction(PlannedProductionOutput samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedProduction> outputList = new ArrayList<>();
        var mpr = mprService.findByPartnerAndPartnerCXNumber(partner, samm.getMaterialGlobalAssetId());

        if (mpr == null) {
            log.warn("Could not identify materialPartnerRelation with matNbrCatenaX " + matNbrCatenaX
                    + " and partner bpnl " + partner.getBpnl());
            return outputList;
        }
        Material material = mpr.getMaterial();
        if (material == null) {
            log.warn("Could not identify material with CatenaXNbr " + matNbrCatenaX);
            return outputList;
        }

        for (var position : samm.getPositions()) {
            String supplierOrderNumber = null, customerOrderPositionNumber = null, customerOrderNumber = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderNumber = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderNumber = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionNumber = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var allocatedProduction : position.getAllocatedPlannedProductionOutputs()) {
                var builder = ReportedProduction.builder();
                var production = builder
                        .partner(partner)
                        .material(material)
                        .productionSiteBpns(allocatedProduction.getProductionSiteBpns())
                        .estimatedTimeOfCompletion(allocatedProduction.getEstimatedTimeOfCompletion())
                        .customerOrderNumber(customerOrderNumber)
                        .supplierOrderNumber(supplierOrderNumber)
                        .customerOrderPositionNumber(customerOrderPositionNumber)
                        .measurementUnit(allocatedProduction.getPlannedProductionQuantity().getUnit())
                        .quantity(allocatedProduction.getPlannedProductionQuantity().getValue())
                        .build();
                outputList.add(production);
            }
        }
        return outputList;
    }

    private record PositionsMappingHelper(String customerOrderId, String supplierOrderId, String customerOrderPositionId) {}
}
