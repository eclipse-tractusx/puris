/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.aggregateddata.logic.adapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.ChildAggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.PartnerAggregatedData;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedAnonymizedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.anonymizeddeliverysamm.DeliveryInformationAnonymized;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedAnonymizedProduction;
import org.eclipse.tractusx.puris.backend.production.logic.dto.anonymizedplannedproductionsamm.PlannedProductionOutputAnonymized;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedAnonymizedStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm.ItemStockAnonymizedSamm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AggregatedDataMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;
 
    @Autowired
    private MaterialService materialService;
 
    @Autowired
    private ObjectMapper objectMapper;

 
    public PartnerAggregatedData jsonToPartnerAggregatedData(JsonNode json, Partner partner) {
        String globalAssetId = getText(json, "globalAssetId");
        if (globalAssetId == null) {
            log.warn("Missing globalAssetId in aggregated data payload");
            return null;
        }
        var mpr = mprService.findByPartnerAndPartnerCXNumber(partner, globalAssetId);
        Material material = materialService.findByMaterialNumberCx(globalAssetId);
        if (material == null && mpr == null) {
            log.warn("Could not identify material " + globalAssetId);
            return null;
        }
        if (material == null) {
            material = mpr.getMaterial();
        }
        var data = PartnerAggregatedData.builder()
            .partner(partner)
            .material(material)
            .productions(new HashSet<>())
            .deliveries(new HashSet<>())
            .stocks(new HashSet<>())
            .build();
 
        mapAspectItems(json.get("items"), data);
        data.setChildData(mapChildItems(json.get("childItems"), data));
        return data;
    }
 
    private void mapAspectItems(JsonNode itemsNode, AggregatedData target) {
        for (JsonNode itemNode : elements(itemsNode)) {
            String aspect = getText(itemNode, "aspect");
            JsonNode payload = itemNode.get("items");
            if (aspect == null || payload == null || payload.isNull()) {
                throw new IllegalArgumentException("Error in aspect item " + itemNode);
            }
            try {
                switch (AssetType.fromUrn(aspect)) {
                    case DELIVERY_ANONYMIZED_SUBMODEL -> target.getDeliveries().addAll(
                        mapDeliveries(objectMapper.treeToValue(payload, DeliveryInformationAnonymized.class)));
                    case ITEM_STOCK_ANONYMIZED_SUBMODEL -> target.getStocks().addAll(
                        mapStocks(objectMapper.treeToValue(payload, ItemStockAnonymizedSamm.class)));
                    case PRODUCTION_ANONYMIZED_SUBMODEL -> target.getProductions().addAll(
                        mapProductions(objectMapper.treeToValue(payload, PlannedProductionOutputAnonymized.class)));
                    default -> throw new IllegalArgumentException("Unexpected aspect: " + aspect);
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error processing aspect " + aspect, e);
            }
        }
    }
 
    private Set<ReportedAnonymizedDelivery> mapDeliveries(DeliveryInformationAnonymized samm) {
        var deliveries = new HashSet<ReportedAnonymizedDelivery>();
        for (var deliveryAnonymized : samm.getDeliveries()) {
            var departureEvent = deliveryAnonymized.getTransitEvents().stream()
                .filter(e -> e.getEventType() == EventTypeEnumeration.ACTUAL_DEPARTURE || e.getEventType() == EventTypeEnumeration.ESTIMATED_DEPARTURE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Delivery without departure transit event"));
            var arrivalEvent = deliveryAnonymized.getTransitEvents().stream()
                .filter(e -> e.getEventType() == EventTypeEnumeration.ACTUAL_ARRIVAL || e.getEventType() == EventTypeEnumeration.ESTIMATED_ARRIVAL)
                .findFirst();
 
            var builder = ReportedAnonymizedDelivery.builder()
                .quantity(deliveryAnonymized.getDeliveryQuantity().getValue())
                .measurementUnit(deliveryAnonymized.getDeliveryQuantity().getUnit())
                .lastUpdatedOnDateTime(deliveryAnonymized.getLastUpdatedOnDateTime())
                .dateOfDeparture(departureEvent.getDateTimeOfEvent())
                .departureType(departureEvent.getEventType())
                .originBpnsAnonymized(deliveryAnonymized.getOriginBpnsAnonymized())
                .destinationBpnsAnonymized(deliveryAnonymized.getDestinationBpnsAnonymized());
            arrivalEvent.ifPresent(event -> builder.dateOfArrival(event.getDateTimeOfEvent()).arrivalType(event.getEventType()));
            deliveries.add(builder.build());
        }
        return deliveries;
    }
 
    private Set<ReportedAnonymizedStock> mapStocks(ItemStockAnonymizedSamm samm) {
        var stocks = new HashSet<ReportedAnonymizedStock>();
        for (var allocatedStock : samm.getAllocatedStocksAnonymized()) {
            stocks.add(ReportedAnonymizedStock.builder()
                .quantity(allocatedStock.getQuantityOnAllocatedStock().getValue())
                .measurementUnit(allocatedStock.getQuantityOnAllocatedStock().getUnit())
                .stockLocationBpnsAnonymized(allocatedStock.getStockLocationBPNSAnonymized())
                .isBlocked(allocatedStock.getIsBlocked())
                .lastUpdatedOnDateTime(allocatedStock.getLastUpdatedOnDateTime())
                .build());
        }
        return stocks;
    }
 
    private Set<ReportedAnonymizedProduction> mapProductions(PlannedProductionOutputAnonymized samm) {
        var productions = new HashSet<ReportedAnonymizedProduction>();
        for (var output : samm.getAllocatedPlannedProductionOutputs()) {
            productions.add(ReportedAnonymizedProduction.builder()
                .quantity(output.getPlannedProductionQuantity().getValue())
                .measurementUnit(output.getPlannedProductionQuantity().getUnit())
                .productionSiteBpnsAnonymized(output.getProductionSiteBpnsAnonymized())
                .estimatedTimeOfCompletion(output.getEstimatedTimeOfCompletion())
                .lastUpdatedOnDateTime(output.getLastUpdatedOnDateTime())
                .materialGlobalAssetIdAnonymized(samm.getMaterialGlobalAssetIdAnonymized())
                .build());
        }
        return productions;
    }
 
    private List<ChildAggregatedData> mapChildItems(JsonNode childItemsNode, AggregatedData parent) {
        var children = new ArrayList<ChildAggregatedData>();
        for (JsonNode childNode : elements(childItemsNode)) {
            var child = ChildAggregatedData.builder()
                .externalMaterialNumber(getText(childNode, "materialNumber"))
                .externalMaterialName(getText(childNode, "materialName"))
                .parentData(parent)
                .productions(new HashSet<>())
                .deliveries(new HashSet<>())
                .stocks(new HashSet<>())
                .build();
 
            mapAspectItems(childNode.get("items"), child);
            child.setChildData(mapChildItems(childNode.get("childItems"), child));
            children.add(child);
        }
        return children;
    }

    private static Iterable<JsonNode> elements(JsonNode node) {
        return node != null && node.isArray() ? node : List.<JsonNode>of();
    }
 
    private static String getText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
