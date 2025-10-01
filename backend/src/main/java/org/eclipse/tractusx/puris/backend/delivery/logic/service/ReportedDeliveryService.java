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

package org.eclipse.tractusx.puris.backend.delivery.logic.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.ReportedDeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class ReportedDeliveryService extends DeliveryService<ReportedDelivery> {
    private final ReportedDeliveryRepository repository;

    private final PartnerService partnerService;

    protected final Function<ReportedDelivery, Boolean> validator;

    private Partner ownPartnerEntity;

    public ReportedDeliveryService(ReportedDeliveryRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final List<ReportedDelivery> findAllByReportedId(UUID reportedId) {
        return repository.findAll().stream().filter(delivery -> delivery.getPartner().getUuid().equals(reportedId))
            .toList();
    }

    public final ReportedDelivery create(ReportedDelivery delivery) {
        if (delivery.getUuid() != null && repository.findById(delivery.getUuid()).isPresent()) {
            return null;
        }
        if (!validator.apply(delivery)) {
            return null;
        }
        return repository.save(delivery);
    }

    public final List<ReportedDelivery> createAll(List<ReportedDelivery> deliveries) {
        if (deliveries.stream().anyMatch(delivery -> !validator.apply(delivery))) {
            return null;
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> deliveries.stream().anyMatch(delivery -> delivery.equals(existing)))) {
            return null;
        }
        return repository.saveAll(deliveries);
    }

    public boolean validate(ReportedDelivery delivery) {
        return 
            delivery.getQuantity() >= 0 &&
            delivery.getMeasurementUnit() != null &&
            delivery.getMaterial() != null &&
            delivery.getPartner() != null &&
            validateResponsibility(delivery) &&
            validateTransitEvent(delivery) &&
            ((
                delivery.getCustomerOrderNumber() != null && 
                delivery.getCustomerOrderPositionNumber() != null 
            ) || (
                delivery.getCustomerOrderNumber() == null && 
                delivery.getCustomerOrderPositionNumber() == null &&
                delivery.getSupplierOrderNumber() == null
            ));
    }

    private boolean validateTransitEvent(ReportedDelivery delivery) {
        var now = new Date().getTime();
        return
            delivery.getDepartureType() != null &&
            (delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE || delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE) &&
            delivery.getArrivalType() != null &&
            (delivery.getArrivalType() == EventTypeEnumeration.ESTIMATED_ARRIVAL || delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL) &&
            !(delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE && delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL) &&
            delivery.getDateOfDeparture().getTime() < delivery.getDateOfArrival().getTime() && 
            (delivery.getArrivalType() != EventTypeEnumeration.ACTUAL_ARRIVAL || delivery.getDateOfArrival().getTime() < now) &&
            (delivery.getDepartureType() != EventTypeEnumeration.ACTUAL_DEPARTURE || delivery.getDateOfDeparture().getTime() < now);
    }

    private boolean validateResponsibility(ReportedDelivery delivery) {
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        return delivery.getIncoterm() != null && switch (delivery.getIncoterm().getResponsibility()) {
            case CUSTOMER ->
                delivery.getMaterial().isProductFlag() &&
                ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
            case SUPPLIER ->
                delivery.getMaterial().isMaterialFlag() &&
                delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
            case PARTIAL ->
                (
                    delivery.getMaterial().isMaterialFlag() &&
                    ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())) &&
                    delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))
                    
                ) || (
                    delivery.getMaterial().isProductFlag() &&
                    delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())) &&
                    ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))
                );
        };
    }
}
