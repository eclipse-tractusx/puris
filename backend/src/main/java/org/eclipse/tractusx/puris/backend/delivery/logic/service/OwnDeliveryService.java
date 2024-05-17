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

package org.eclipse.tractusx.puris.backend.delivery.logic.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.DeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class OwnDeliveryService {
    public final DeliveryRepository repository;

    private final PartnerService partnerService;

    protected final Function<OwnDelivery, Boolean> validator;

    private Partner ownPartnerEntity;

    public OwnDeliveryService(DeliveryRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final List<OwnDelivery> findAll() {
        return repository.findAll();
    }

    public final List<OwnDelivery> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<OwnDelivery> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<OwnDelivery> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpns, Optional<String> bpnl) {
        Stream<OwnDelivery> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpns.isPresent()) {
            stream = stream.filter(delivery -> delivery.getDestinationBpns().equals(bpns.get()) || delivery.getOriginBpns().equals(bpns.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl.get()));
        }
        return stream.toList();
    }

    public final OwnDelivery findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final OwnDelivery create(OwnDelivery delivery) {
        if (!validator.apply(delivery)) {
            throw new IllegalArgumentException("Invalid delivery");
        }
        if (delivery.getUuid() != null && repository.findById(delivery.getUuid()).isPresent()) {
            throw new KeyAlreadyExistsException("Delivery already exists");
        }
        return repository.save(delivery);
    }

    public final List<OwnDelivery> createAll(List<OwnDelivery> deliveries) {
        if (deliveries.stream().anyMatch(delivery -> !validator.apply(delivery))) {
            throw new IllegalArgumentException("Invalid delivery");
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> deliveries.stream().anyMatch(delivery -> delivery.equals(existing)))) {
            throw new KeyAlreadyExistsException("delivery already exists");
        }
        return repository.saveAll(deliveries);
    }

    public final OwnDelivery update(OwnDelivery delivery) {
        if (delivery.getUuid() == null || repository.findById(delivery.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(delivery);
    }

    public final void delete(UUID id) {
        repository.deleteById(id);
    }

    public boolean validate(OwnDelivery delivery) {
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        return 
            delivery.getQuantity() > 0 && 
            delivery.getMeasurementUnit() != null &&
            delivery.getMaterial() != null &&
            delivery.getPartner() != null &&
            validateResponsibility(delivery) &&
            validateTransitEvent(delivery) &&
            !delivery.getPartner().equals(ownPartnerEntity) &&
            ((
                delivery.getCustomerOrderNumber() != null && 
                delivery.getCustomerOrderPositionNumber() != null
            ) || (
                delivery.getCustomerOrderNumber() == null && 
                delivery.getCustomerOrderPositionNumber() == null &&
                delivery.getSupplierOrderNumber() == null
            ));
    }

    private boolean validateTransitEvent(OwnDelivery delivery) {
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

    private boolean validateResponsibility(OwnDelivery delivery) {
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        return delivery.getIncoterm() != null && switch (delivery.getIncoterm().getResponsibility()) {
            case SUPPLIER ->
                delivery.getMaterial().isProductFlag() &&
                ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
            case CUSTOMER ->
                delivery.getMaterial().isMaterialFlag() &&
                delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
            case PARTIAL ->
                (
                    delivery.getMaterial().isProductFlag() &&
                    ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                    delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))
                ) || (
                    delivery.getMaterial().isMaterialFlag() &&
                    delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                    ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))
                );
        };
    }
}
