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
package org.eclipse.tractusx.puris.backend.delivery.logic.adapter;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Delivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.DeliveryInformation;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Location;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.OrderPositionReference;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Position;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.TransitEvent;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.TransitLocations;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeliveryInformationSammMapper {
    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private MaterialService materialService;

    public DeliveryInformation ownDeliveryToSamm(List<OwnDelivery> deliveryList) {
        if (deliveryList == null || deliveryList.isEmpty()) {
            log.warn("Can't map empty list");
            return null;
        }
        Partner partner = deliveryList.get(0).getPartner();
        if (deliveryList.stream().anyMatch(deli -> !deli.getPartner().equals(partner))) {
            log.warn("Can't map delivery list with different partners");
            return null;
        }
        Material material = deliveryList.get(0).getMaterial();
        if (deliveryList.stream().anyMatch(deli -> !deli.getMaterial().equals(material))) {
            log.warn("Can't map delivery list with different materials");
            return null;
        }
        var groupedByPositionAttributes = deliveryList
                .stream()
                .collect(Collectors.groupingBy(prod -> new PositionsMappingHelper(
                    prod.getCustomerOrderNumber(),
                    prod.getSupplierOrderNumber(),
                    prod.getCustomerOrderPositionNumber()
                )));
        DeliveryInformation samm = new DeliveryInformation();
        var matNumberCx = material.isProductFlag() ? material.getMaterialNumberCx() : mprService.find(material, partner).getPartnerCXNumber();
        samm.setMaterialGlobalAssetId(matNumberCx);

        var posList = new HashSet<Position>();
        samm.setPositions(posList);
        for (var mappingHelperListEntry : groupedByPositionAttributes.entrySet()) {
            var key = mappingHelperListEntry.getKey();
            var delivery = mappingHelperListEntry.getValue().get(0);
            Position position = new Position();
            
            posList.add(position);
            if (key.customerOrderId != null || key.customerOrderPositionId != null) {
                OrderPositionReference opr = new OrderPositionReference(
                    delivery.getSupplierOrderNumber(),
                    delivery.getCustomerOrderNumber(),
                    delivery.getCustomerOrderPositionNumber()
                );
                position.setOrderPositionReference(opr);
            }
            var deliveries = new HashSet<Delivery>();
            position.setDeliveries(deliveries);
            for (var v : mappingHelperListEntry.getValue()) {
                ItemQuantityEntity itemQuantityEntity = new ItemQuantityEntity(v.getQuantity(), v.getMeasurementUnit());
                Set<TransitEvent> events = new HashSet<TransitEvent>();
                events.add(new TransitEvent(v.getDateOfDeparture(), v.getDepartureType()));
                events.add(new TransitEvent(v.getDateOfArrival(), v.getArrivalType()));
                TransitLocations locations = new TransitLocations(new Location(v.getOriginBpna(), v.getOriginBpns()), new Location(v.getDestinationBpna(), v.getDestinationBpns()));
                Delivery newDelivery = new Delivery(
                        itemQuantityEntity, new Date(), events, locations, v.getTrackingNumber(), v.getIncoterm());
                deliveries.add(newDelivery);
            }
        }
        return samm;
    }

    public List<ReportedDelivery> sammToReportedDeliveries(DeliveryInformation samm, Partner partner) {
        String matNbrCatenaX = samm.getMaterialGlobalAssetId();
        ArrayList<ReportedDelivery> outputList = new ArrayList<>();
        var mpr = mprService.findByPartnerAndPartnerCXNumber(partner, matNbrCatenaX);
        Material material = materialService.findByMaterialNumberCx(matNbrCatenaX);
        if (material == null && mpr == null) {
            log.warn("Could not identify material with CatenaXNbr " + matNbrCatenaX);
            return outputList;
        }
        if (material == null) {
            material = mpr.getMaterial();
        }

        for (var position : samm.getPositions()) {
            String supplierOrderNumber = null, customerOrderPositionNumber = null, customerOrderNumber = null;
            if (position.getOrderPositionReference() != null) {
                supplierOrderNumber = position.getOrderPositionReference().getSupplierOrderId();
                customerOrderNumber = position.getOrderPositionReference().getCustomerOrderId();
                customerOrderPositionNumber = position.getOrderPositionReference().getCustomerOrderPositionId();
            }
            for (var delivery : position.getDeliveries()) {
                var builder = ReportedDelivery.builder();
                var arrivalEvent = delivery.getTransitEvents().stream()
                    .filter(e -> e.getEventType().equals(EventTypeEnumeration.ACTUAL_ARRIVAL) || e.getEventType().equals(EventTypeEnumeration.ESTIMATED_ARRIVAL))
                    .findFirst().get();
                var departureEvent = delivery.getTransitEvents().stream()
                    .filter(e -> e.getEventType().equals(EventTypeEnumeration.ACTUAL_DEPARTURE) || e.getEventType().equals(EventTypeEnumeration.ESTIMATED_DEPARTURE))
                    .findFirst().get();
                var newDelivery = builder
                    .material(material)
                    .partner(partner)
                    .customerOrderNumber(customerOrderNumber)
                    .customerOrderPositionNumber(customerOrderPositionNumber)
                    .supplierOrderNumber(supplierOrderNumber)
                    .quantity(delivery.getDeliveryQuantity().getValue())
                    .measurementUnit(delivery.getDeliveryQuantity().getUnit())
                    .arrivalType(arrivalEvent.getEventType())
                    .dateOfArrival(arrivalEvent.getDateTimeOfEvent())
                    .departureType(departureEvent.getEventType())
                    .dateOfDeparture(departureEvent.getDateTimeOfEvent())
                    .originBpns(delivery.getTransitLocations().getOrigin().getBpnsProperty())
                    .originBpna(delivery.getTransitLocations().getOrigin().getBpnaProperty())
                    .destinationBpns(delivery.getTransitLocations().getDestination().getBpnsProperty())
                    .destinationBpna(delivery.getTransitLocations().getDestination().getBpnaProperty())
                    .trackingNumber(delivery.getTrackingNumber())
                    .incoterm(delivery.getIncoterm())
                    .build();
                outputList.add(newDelivery);
            }
        }
        return outputList;
    }

    private record PositionsMappingHelper(String customerOrderId, String supplierOrderId, String customerOrderPositionId) {}
}
