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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.Delivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.DeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DeliveryService<T extends Delivery> {
    @Autowired
    protected DeliveryRepository<T> repository;

    @Autowired
    private PartnerService partnerService;

    private Partner ownPartnerEntity;
    
    public final List<T> findAll() {
        return repository.findAll();
    }

    public final T findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final List<T> findAllByFilters(
        Optional<String> ownMaterialNumber,
        Optional<String> bpns,
        Optional<String> bpnl,
        Optional<Date> day,
        Optional<DirectionCharacteristic> direction) {
        Stream<T> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (direction.isPresent()) {
            if (ownPartnerEntity == null) {
                ownPartnerEntity = partnerService.getOwnPartnerEntity();
            }
            if (direction.get() == DirectionCharacteristic.INBOUND) {
                stream = stream.filter(delivery -> ownPartnerEntity.getSites().stream().anyMatch(site -> delivery.getDestinationBpns().equals(site.getBpns())));
            } else {
                stream = stream.filter(delivery -> ownPartnerEntity.getSites().stream().anyMatch(site -> delivery.getOriginBpns().equals(site.getBpns())));
            }
        }
        if (bpns.isPresent()) {
            stream = stream.filter(delivery -> delivery.getDestinationBpns().equals(bpns.get()) || delivery.getOriginBpns().equals(bpns.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (day.isPresent()) {
            LocalDate localDayDate = Instant.ofEpochMilli(day.get().getTime())
                .atOffset(ZoneOffset.UTC)
                .toLocalDate();
            stream = stream.filter(delivery -> {
                long time = direction.get() == DirectionCharacteristic.INBOUND
                    ? delivery.getDateOfArrival().getTime()
                    : delivery.getDateOfDeparture().getTime();
                LocalDate deliveryDayDate = Instant.ofEpochMilli(time)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();
                return deliveryDayDate.getDayOfMonth() == localDayDate.getDayOfMonth();
            });
        }
        return stream.toList();
    }

    public final double getSumOfQuantities(List<T> deliveries) {
        double sum = 0;
        for (T delivery : deliveries) {
            sum += delivery.getQuantity();
        }
        return sum;
    }

    public final List<Double> getQuantityForDays(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, DirectionCharacteristic direction, int numberOfDays) {
        List<Double> deliveryQtys = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<T> deliveries = findAllByFilters(Optional.of(material), siteBpns, partnerBpnl, Optional.of(date), Optional.of(direction));
            deliveryQtys.add(getSumOfQuantities(deliveries));

            localDate = localDate.plusDays(1);
        }
        return deliveryQtys;
    }

    public final T update(T delivery) {
        if (delivery.getUuid() == null || repository.findById(delivery.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(delivery);
    }

    public final void delete(UUID id) {
        repository.deleteById(id);
    }

    protected List<String> basicValidation(Delivery delivery) {
        List<String> errors = new ArrayList<>();

        if (delivery.getQuantity() < 0) {
            errors.add("Quantity must be greater than or equal to 0.");
        }
        if (delivery.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (delivery.getLastUpdatedOnDateTime() == null) {
            errors.add("Missing lastUpdatedOnTime.");
        } else if (delivery.getLastUpdatedOnDateTime().after(new Date())) {
            errors.add("lastUpdatedOnDateTime cannot be in the future.");
        }
        if (delivery.getMaterial() == null) {
            errors.add("Missing material.");
        }
        if (delivery.getPartner() == null) {
            errors.add("Missing partner.");
        }
        // errors.addAll(validateResponsibility(delivery));
        errors.addAll(validateTransitEvent(delivery));
        if (!((delivery.getCustomerOrderNumber() != null && delivery.getCustomerOrderPositionNumber() != null) || 
            (delivery.getCustomerOrderNumber() == null && delivery.getCustomerOrderPositionNumber() == null && delivery.getSupplierOrderNumber() == null))) {
            errors.add("If an order position reference is given, customer order number and customer order position number must be set.");
        }

        return errors;
    }

    protected List<String> validateOwnPartner(Delivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        if (delivery.getPartner().equals(ownPartnerEntity)) {
            errors.add("Partner cannot be the same as own partner entity.");
        }
        return errors;
    }

    protected List<String> validateTransitEvent(Delivery delivery) {
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

    protected List<String> validateOwnResponsibility(Delivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        var ownSites = ownPartnerEntity.getSites();
        var partnerSites = delivery.getPartner().getSites();

        if (delivery.getIncoterm() == null) {
            errors.add("Missing Incoterm.");
        } else {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    var ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();
                    var partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
                    if (!delivery.getMaterial().isProductFlag()) {
                        errors.add("Material must have product flag for supplier responsibility.");
                    }
                    if (!ownSite.isPresent()) {
                        errors.add("Origin BPNS must match one of the own partner entity's site BPNS for supplier responsibility.");
                    } else if (delivery.getOriginBpna() != null && ownSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getOriginBpna()))) {
                        errors.add("Origin BPNA must match one of the own partner entity's site' address BPNAs for supplier responsibility.");
                    }
                    if (!partnerSite.isPresent()) {
                        errors.add("Destination BPNS must match one of the partner's site BPNS for supplier responsibility.");
                    } else if (delivery.getDestinationBpna() != null && partnerSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getDestinationBpna()))) {
                        errors.add("Destination BPNA must match one of the own partner entity's site' address BPNAs for supplier responsibility.");
                    }
                    break;
                case CUSTOMER:
                    ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
                    partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();
                    if (!delivery.getMaterial().isMaterialFlag()) {
                        errors.add("Material must have material flag for customer responsibility.");
                    }
                    if (!ownSite.isPresent()) {
                        errors.add("Destination BPNS must match one of the own partner entity's site BPNS for customer responsibility.");
                    } else if (delivery.getDestinationBpna() != null && ownSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getDestinationBpna()))) {
                        errors.add("Destination BPNA must match one of the own partner entity's site' address BPNAs for customer responsibility.");
                    }
                    if (!partnerSite.isPresent()) {
                        errors.add("Origin BPNS must match one of the partner's site BPNS for customer responsibility.");
                    } else if (delivery.getOriginBpna() != null && partnerSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getOriginBpna()))) {
                        errors.add("Origin BPNA must match one of the own partner entity's site' address BPNAs for customer responsibility.");
                    }
                    break;
                case PARTIAL:
                    if (delivery.getMaterial().isProductFlag()) {
                        ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();
                        partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
                        if (ownSite.isPresent() && partnerSite.isPresent() && (
                                delivery.getOriginBpna() == null || 
                                ownSite.get().getAddresses().stream().anyMatch(address -> address.getBpna().equals(delivery.getOriginBpna()))
                            ) && (
                                delivery.getDestinationBpna() == null || 
                                partnerSite.get().getAddresses().stream().anyMatch(address -> address.getBpna().equals(delivery.getDestinationBpna())) 
                            )) {
                            return new ArrayList<>();
                        }
                    }
                    if (delivery.getMaterial().isMaterialFlag()) {
                        ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
                        partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();
                        if (ownSite.isPresent() && partnerSite.isPresent() && (
                                delivery.getDestinationBpna() == null || 
                                ownSite.get().getAddresses().stream().anyMatch(address -> address.getBpna().equals(delivery.getDestinationBpna()))
                            ) && (
                                delivery.getOriginBpna() == null || 
                                partnerSite.get().getAddresses().stream().anyMatch(address -> address.getBpna().equals(delivery.getOriginBpna())) 
                            )) {
                            return new ArrayList<>();
                        }
                    }
                    errors.add("Responsibility conditions for partial responsibility are not met.");
                    break;
                default:
                    errors.add("Invalid incoterm responsibility.");
                    break;
            }
        }
        return errors;
    }

    protected List<String> validateReportedResponsibility(Delivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }

        if (delivery.getIncoterm() == null) {
            errors.add("Missing Incoterm.");
        } else {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    if (!delivery.getMaterial().isMaterialFlag()) {
                        errors.add("Material must have material flag for supplier responsibility.");
                    }
                    if (delivery.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))) {
                        errors.add("Origin BPNA must match one of the partner entity's site' address BPNAs for supplier responsibility.");
                    }
                    if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))) {
                        errors.add("Destination BPNA must match one of the partner entity's site' address BPNAs for supplier responsibility.");
                    }

                    break;
                case CUSTOMER:
                    if (!delivery.getMaterial().isProductFlag()) {
                        errors.add("Material must have product flag for customer responsibility.");
                    }
                    if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))) {
                        errors.add("Site BPNS must match one of the own partner entity's site BPNS for customer responsibility.");
                    }
                    if (delivery.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))) {
                        errors.add("Site BPNA must match one of the partner entity's site' address BPNAs for customer responsibility.");
                    }

                    break;
                case PARTIAL:
                    if (delivery.getMaterial().isProductFlag()) {
                        if (delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())) &&
                            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))
                        ) {
                            return new ArrayList<>();
                        }
                    }
                    if (delivery.getMaterial().isMaterialFlag()) {
                        if (ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())) &&
                            delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns()))) {
                            return new ArrayList<>();
                        }
                    }
                    errors.add("Responsibility conditions for partial responsibility are not met.");
                    break;
                default:
                    errors.add("Invalid incoterm responsibility.");
                    break;
            }
        }
        return errors;
    }
}
