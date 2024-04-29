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
package org.eclipse.tractusx.puris.backend.demand.logic.adapter;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.Demand;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.DemandCategoryCharacteristic;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.DemandSeries;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.ShortTermMaterialDemand;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;

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
public class ShortTermMaterialDemandSammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private MaterialService materialService;

    public ShortTermMaterialDemand ownDemandToSamm(List<OwnDemand> demandList) {
        if (demandList == null || demandList.isEmpty()) {
            log.warn("Can't map empty list");
            return null;
        }
        Partner partner = demandList.get(0).getPartner();
        Material material = demandList.get(0).getMaterial();
        if (demandList.stream().anyMatch(dem -> !dem.getPartner().equals(partner))) {
            log.warn("Can't map demand list with different partners");
            return null;
        }

        if (demandList.stream().anyMatch(prod -> !prod.getMaterial().equals(material))) {
            log.warn("Can't map demand list with different materials");
            return null;
        }
        var groupedByCategory = demandList
                .stream()
                .collect(Collectors.groupingBy(demand -> new DemandGroupingHelper(demand.getDemandCategoryCode(), demand.getDemandLocationBpns(), demand.getSupplierLocationBpns())));
        ShortTermMaterialDemand samm = new ShortTermMaterialDemand();

        var mpr = mprService.findAll().stream().filter(mr -> mr.getMaterial().equals(material) && mr.getPartner().equals(partner)).findFirst().orElse(null);
        if (mpr == null) {
            log.warn("Could not identify materialPartnerRelation with ownMaterialNumber " + material.getOwnMaterialNumber()
                    + " and partner bpnl " + partner.getBpnl());
            return null;
        }
        samm.setMaterialGlobalAssetId(mpr.getPartnerCXNumber());

        var demandSeriesList = new HashSet<DemandSeries>();
        samm.setDemandSeries(demandSeriesList);
        for (var mappingHelperListEntry : groupedByCategory.entrySet()) {
            var key = mappingHelperListEntry.getKey();
            DemandSeries demandSeries = new DemandSeries();
            demandSeriesList.add(demandSeries);
            demandSeries.setLastUpdatedOnDateTime(new Date());
            demandSeries.setDemandCategory(mapDemandCategory(key.category()));
            demandSeries.setCustomerLocationBpns(key.customerLocationBpns());
            demandSeries.setExpectedSupplierLocationBpns(key.expectedSupplierLocationBpns());
            var demands = new HashSet<Demand>();
            demandSeries.setDemands(demands);
            for (var v : mappingHelperListEntry.getValue()) {
                ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(v.getQuantity(), v.getMeasurementUnit());
                Demand dailyDemand = new Demand(itemQuantityEntity, v.getDay());
                demands.add(dailyDemand);
            }
        }
        return samm;
    }

    public List<ReportedDemand> sammToReportedDemand(ShortTermMaterialDemand samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedDemand> outputList = new ArrayList<>();

        var material = materialService.findByMaterialNumberCx(matNbrCatenaX);
        if (material == null) {
            log.warn("Could not identify material with given CatenaXNbr ");
            return outputList;
        }

        for (var demandSeries : samm.getDemandSeries()) {
            for (var demand : demandSeries.getDemands()) {
                var builder = ReportedDemand.builder();
                var reportedDemand = builder
                        .partner(partner)
                        .material(material)
                        .demandCategoryCode(mapDemandCategory(demandSeries.getDemandCategory()))
                        .demandLocationBpns(demandSeries.getCustomerLocationBpns())
                        .supplierLocationBpns(demandSeries.getExpectedSupplierLocationBpns())
                        .quantity(demand.getDemand().getValue())
                        .measurementUnit(demand.getDemand().getUnit())
                        .day(demand.getDay())
                        .build();
                outputList.add(reportedDemand);
            }
        }
        return outputList;
    }

    private record DemandGroupingHelper(DemandCategoryEnumeration category, String customerLocationBpns, String expectedSupplierLocationBpns) {}

    private DemandCategoryCharacteristic mapDemandCategory(DemandCategoryEnumeration category) {
        return switch (category) {
            case DEMAND_DEFAULT -> DemandCategoryCharacteristic.DEMAND_CATEGORY;
            case DEMAND_AFTER_SALES -> DemandCategoryCharacteristic.DEMAND_CATEGORY_AFTER_SALES;
            case DEMAND_SERIES -> DemandCategoryCharacteristic.DEMAND_CATEGORY_SERIES;
            case DEMAND_PHASE_IN_PERIOD -> DemandCategoryCharacteristic.DEMAND_CATEGORY_PHASE_IN_PERIOD;
            case DEMAND_PHASE_OUT_PERIOD -> DemandCategoryCharacteristic.DEMAND_CATEGORY_PHASE_OUT_PERIOD;
            case DEMAND_SINGLE_ORDER -> DemandCategoryCharacteristic.DEMAND_CATEGORY_SINGLE_ORDER;
            case DEMAND_SMALL_SERIES -> DemandCategoryCharacteristic.DEMAND_CATEGORY_SMALL_SERIES;
            case DEMAND_EXTRAORDINARY_DEMAND -> DemandCategoryCharacteristic.DEMAND_CATEGORY_EXTRAORDINARY_DEMAND;
        };
    }

    private DemandCategoryEnumeration mapDemandCategory(DemandCategoryCharacteristic category) {
        return switch (category) {
            case DEMAND_CATEGORY -> DemandCategoryEnumeration.DEMAND_DEFAULT;
            case DEMAND_CATEGORY_AFTER_SALES -> DemandCategoryEnumeration.DEMAND_AFTER_SALES;
            case DEMAND_CATEGORY_SERIES -> DemandCategoryEnumeration.DEMAND_SERIES;
            case DEMAND_CATEGORY_PHASE_IN_PERIOD -> DemandCategoryEnumeration.DEMAND_PHASE_IN_PERIOD;
            case DEMAND_CATEGORY_PHASE_OUT_PERIOD -> DemandCategoryEnumeration.DEMAND_PHASE_OUT_PERIOD;
            case DEMAND_CATEGORY_SINGLE_ORDER -> DemandCategoryEnumeration.DEMAND_SINGLE_ORDER;
            case DEMAND_CATEGORY_SMALL_SERIES -> DemandCategoryEnumeration.DEMAND_SMALL_SERIES;
            case DEMAND_CATEGORY_EXTRAORDINARY_DEMAND -> DemandCategoryEnumeration.DEMAND_EXTRAORDINARY_DEMAND;
        };
    }
}
