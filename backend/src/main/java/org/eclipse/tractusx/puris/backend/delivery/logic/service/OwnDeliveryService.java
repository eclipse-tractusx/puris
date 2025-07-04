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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.OwnDeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class OwnDeliveryService extends DeliveryService<OwnDelivery> {
    private final OwnDeliveryRepository repository;

    private final PartnerService partnerService;

    protected final Function<OwnDelivery, Boolean> validator;

    private Partner ownPartnerEntity;

    public OwnDeliveryService(OwnDeliveryRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final List<OwnDelivery> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<OwnDelivery> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
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

    public boolean validate(OwnDelivery delivery) {
        return validateWithDetails(delivery).isEmpty();
    }

    public List<String> validateWithDetails(OwnDelivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }

        if (delivery.getQuantity() < 0) {
            errors.add("Quantity must be greater than or equal to 0.");
        }
        if (delivery.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (delivery.getMaterial() == null) {
            errors.add("Missing material.");
        }
        if (delivery.getPartner() == null) {
            errors.add("Missing partner.");
        }
        errors.addAll(validateResponsibility(delivery));
        errors.addAll(validateTransitEvent(delivery));
        if (delivery.getPartner().equals(ownPartnerEntity)) {
            errors.add("Partner cannot be the same as own partner entity.");
        }
        if (!((delivery.getCustomerOrderNumber() != null && delivery.getCustomerOrderPositionNumber() != null) || 
            (delivery.getCustomerOrderNumber() == null && delivery.getCustomerOrderPositionNumber() == null && delivery.getSupplierOrderNumber() == null))) {
            errors.add("Customer order number and position number must both be null or both be non-null, and supplier order number must be null if both are null.");
        }

        return errors;
    }

    private List<String> validateTransitEvent(OwnDelivery delivery) {
        List<String> errors = new ArrayList<>();
        var now = new Date().getTime();

        if (delivery.getDepartureType() == null) {
            errors.add("Missing departure type.");
        } else if (!(delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE || delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE)) {
            errors.add("Invalid departure type.");
        }
        if (delivery.getArrivalType() == null) {
            errors.add("Missing arrival type.");
        } else if (!(delivery.getArrivalType() == EventTypeEnumeration.ESTIMATED_ARRIVAL || delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL)) {
            errors.add("Invalid arrival type.");
        }
        if (delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE && delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL) {
            errors.add("Estimated departure cannot have actual arrival.");
        }
        if (delivery.getDateOfDeparture() == null) {
            errors.add("Missing date of departure.");
        }
        if (delivery.getDateOfArrival() == null) {
            errors.add("Missing date of arrival.");
        }
        if (delivery.getDateOfArrival() != null && delivery.getDateOfDeparture() != null &&
            delivery.getDateOfDeparture().getTime() >= delivery.getDateOfArrival().getTime()) {
            errors.add("Date of departure must be before date of arrival.");
        }
        if (delivery.getDateOfArrival() != null &&
            delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL && delivery.getDateOfArrival().getTime() >= now) {
            errors.add("Actual arrival date must be in the past.");
        }
        if (delivery.getDateOfDeparture() != null &&
            delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE && delivery.getDateOfDeparture().getTime() >= now) {
            errors.add("Actual departure date must be in the past.");
        }

        return errors;
    }

    private List<String> validateResponsibility(OwnDelivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }

        if (delivery.getIncoterm() == null) {
            errors.add("Missing Incoterm.");
        } else {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    if (!delivery.getMaterial().isProductFlag()) {
                        errors.add("Material must have product flag for supplier responsibility.");
                    }
                    if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))) {
                        errors.add("Origin BPNs must match one of the own partner entity's site BPNs for supplier responsibility.");
                    }
                    if (delivery.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))) {
                        errors.add("Destination BPNs must match one of the partner's site BPNs for supplier responsibility.");
                    }
                    break;
                case CUSTOMER:
                    if (!delivery.getMaterial().isMaterialFlag()) {
                        errors.add("Material must have material flag for customer responsibility.");
                    }
                    if (delivery.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))) {
                        errors.add("Origin BPNs must match one of the partner's site BPNs for customer responsibility.");
                    }
                    if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))) {
                        errors.add("Destination BPNs must match one of the own partner entity's site BPNs for customer responsibility.");
                    }
                    break;
                case PARTIAL:
                    if (!(delivery.getMaterial().isProductFlag() && 
                        ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) && 
                        delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))) &&
                        !(delivery.getMaterial().isMaterialFlag() && 
                        delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) && 
                        ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())))) {
                        errors.add("Responsibility conditions for partial responsibility are not met.");
                    }
                    break;
                default:
                    errors.add("Invalid incoterm responsibility.");
                    break;
            }
        }
        return errors;
    }
}
