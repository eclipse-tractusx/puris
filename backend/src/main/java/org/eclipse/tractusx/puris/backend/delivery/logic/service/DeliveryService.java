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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.Delivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.DeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DeliveryService<T extends Delivery> {
    @Autowired
    protected DeliveryRepository<T> repository;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

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
            errors.add(String.format("Quantity '%d' must be greater than or equal to 0.", delivery.getQuantity()));
        }
        if (delivery.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (delivery.getLastUpdatedOnDateTime() == null) {
            errors.add("Missing lastUpdatedOnTime.");
        } else if (delivery.getLastUpdatedOnDateTime().after(new Date())) {
            errors.add(String.format("lastUpdatedOnDateTime '%s' must be in the past (system time: '%s').", delivery.getLastUpdatedOnDateTime().toInstant().toString(), (new Date()).toInstant().toString()));
        }
        if (delivery.getMaterial() == null) {
            errors.add("Missing material.");
        }
        if (delivery.getPartner() == null) {
            errors.add("Missing partner.");
        }
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
            errors.add(String.format("Partner cannot be the same as own partner entity '%s'.", delivery.getPartner().getBpnl()));
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
            errors.add("Delivery with estimated departure cannot have actual arrival.");
        }
        if (delivery.getDateOfDeparture() == null) {
            errors.add("Missing date of departure.");
        }
        if (delivery.getDateOfArrival() == null) {
            errors.add("Missing date of arrival.");
        }
        if (delivery.getDateOfArrival() != null && delivery.getDateOfDeparture() != null &&
            delivery.getDateOfDeparture().getTime() >= delivery.getDateOfArrival().getTime()) {
            errors.add(String.format("Date of departure '%s' must be before date of arrival '%s'.", delivery.getDateOfDeparture().toInstant().toString(), delivery.getDateOfArrival().toInstant().toString()));
        }
        if (delivery.getDateOfArrival() != null &&
            delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL && delivery.getDateOfArrival().getTime() >= now) {
            errors.add(String.format("Actual arrival date '%s' must be in the past (system time: '%s').", delivery.getDateOfArrival().toInstant().toString(), (new Date()).toInstant().toString()));
        }
        if (delivery.getDateOfDeparture() != null &&
            delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE && delivery.getDateOfDeparture().getTime() >= now) {
            errors.add(String.format("Actual departure date '%s' must be in the past (system time: '%s').", delivery.getDateOfDeparture().toInstant().toString(), (new Date()).toInstant().toString()));
        }

        return errors;
    }

    protected List<String> validateOwnResponsibility(Delivery delivery) {
        List<String> errors = new ArrayList<>();
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }
        var partnerSites = delivery.getPartner().getSites();
        MaterialPartnerRelation mpr = mprService.find(delivery.getPartner().getBpnl(), delivery.getMaterial().getOwnMaterialNumber());

        if (delivery.getIncoterm() == null) {
            errors.add("Missing Incoterm.");
        } else {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    if (!delivery.getMaterial().isProductFlag()) {
                        errors.add(String.format("Material '%s' must be configured as product via flag (incoterm '%s' with supplier responsibility).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue()));
                    }
                    errors.addAll(validateLocationsAsSupplier(delivery, mpr.getOwnProducingSites(), partnerSites));
                    break;
                case CUSTOMER:
                    if (!delivery.getMaterial().isMaterialFlag()) {
                        errors.add(String.format("Material '%s' must be configured as material via flag (incoterm '%s' with customer responsibility).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue()));
                    }
                    errors.addAll(validateLocationsAsCustomer(delivery, mpr.getOwnStockingSites(), partnerSites));
                    break;
                case PARTIAL:
                    boolean valid = false;
                    List<String> supplierPathErrors = Collections.emptyList();
                    List<String> customerPathErrors = Collections.emptyList();

                    if (delivery.getMaterial().isProductFlag()) {
                        supplierPathErrors = validateLocationsAsSupplier(delivery, mpr.getOwnProducingSites(), partnerSites);
                        if (supplierPathErrors.isEmpty()) {
                            valid = true;
                        }
                    }
                    if (delivery.getMaterial().isMaterialFlag()) {
                        customerPathErrors = validateLocationsAsCustomer(delivery, mpr.getOwnStockingSites(), partnerSites);
                        if (customerPathErrors.isEmpty()) {
                            valid = true;
                        }
                    }
                    if (!valid) {
                        errors.add(String.format("Responsibility conditions for material '%s' for partial responsibility (incoterm '%s') are not met. Either origin site '%s' does not match to own configured sites or destination site '%s' does not match to configured sites for partner '%s'. Additionally this behavior might not be applicable to the material configuration as product (%b) or material (%b).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue(), delivery.getOriginBpns(), delivery.getDestinationBpns(), delivery.getPartner().getBpnl(), delivery.getMaterial().isProductFlag(), delivery.getMaterial().isMaterialFlag()));
                        errors.addAll(supplierPathErrors);
                        errors.addAll(customerPathErrors);
                    }
                    break;
                default:
                    errors.add(String.format("Invalid incoterm responsibility for incoterm '%s'.", delivery.getIncoterm().getValue()));
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
        var ownSites = ownPartnerEntity.getSites();
        var partnerSites = delivery.getPartner().getSites();

        if (delivery.getIncoterm() == null) {
            errors.add("Missing Incoterm.");
        } else {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    if (!delivery.getMaterial().isMaterialFlag()) {
                        errors.add(String.format("Material '%s' must be configured as material via flag (incoterm '%s' with supplier responsibility).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue()));
                    }
                    errors.addAll(validateLocationsAsCustomer(delivery, ownSites, partnerSites));
                    break;
                case CUSTOMER:
                    if (!delivery.getMaterial().isProductFlag()) {
                        errors.add(String.format("Material '%s' must be configured as product via flag (incoterm '%s' with customer responsibility).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue()));
                    }
                    errors.addAll(validateLocationsAsSupplier(delivery, ownSites, partnerSites));
                    break;
                case PARTIAL:
                    boolean valid = false;
                    List<String> supplierPathErrors = Collections.emptyList();
                    List<String> customerPathErrors = Collections.emptyList();
                    if (delivery.getMaterial().isProductFlag()) {
                        supplierPathErrors  = validateLocationsAsSupplier(delivery, ownSites, partnerSites);
                        if (supplierPathErrors.isEmpty()) {
                            valid = true;
                        }
                    }
                    if (delivery.getMaterial().isMaterialFlag()) {
                        customerPathErrors = validateLocationsAsCustomer(delivery, ownSites, partnerSites);
                        if (customerPathErrors.isEmpty()) {
                            valid = true;
                        }
                    }
                    if (!valid) {
                        errors.add(String.format("Responsibility conditions for material '%s' for partial responsibility (incoterm '%s') are not met. Either origin site '%s' does not match to own configured sites or destination site '%s' does not match to configured sites for partner '%s'. Additionally this behavior might not be applicable to the material configuration as product (%b) or material (%b).", delivery.getMaterial().getOwnMaterialNumber(), delivery.getIncoterm().getValue(), delivery.getOriginBpns(), delivery.getDestinationBpns(), delivery.getPartner().getBpnl(), delivery.getMaterial().isProductFlag(), delivery.getMaterial().isMaterialFlag()));
                        errors.addAll(supplierPathErrors);
                        errors.addAll(customerPathErrors);
                    }
                    break;
                default:
                    errors.add("Invalid incoterm responsibility.");
                    break;
            }
        }
        return errors;
    }

    /**
     * Validates the location consistency for the supplier path of a Delivery.
     *
     * @param delivery     The Delivery being validated
     * @param ownSites     The set of sites configured for the own partner entity
     * @param partnerSites The set of sites configured for delivery partner entity
     * @return A list of validation error messages, an empty list means valid
     */
    protected List<String> validateLocationsAsSupplier(Delivery delivery, SortedSet<Site> ownSites, SortedSet<Site> partnerSites) {
        List<String> errors = new ArrayList<>();

        var ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();
        var partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
        
        if (!ownSite.isPresent()) {
            errors.add(String.format("Origin site '%s' must match one of the own partner entity's sites (incoterm '%s' with '%s' responsibility).", delivery.getOriginBpns(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        } else if (delivery.getOriginBpna() != null && ownSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getOriginBpna()))) {
            errors.add(String.format("Origin address '%s' is not configured for own site '%s' (incoterm '%s' with '%s' responsibility).", delivery.getOriginBpna(), delivery.getOriginBpns(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        }
        if (!partnerSite.isPresent()) {
            errors.add(String.format("Destination site '%s' must match one site of partner '%s' (incoterm '%s' with '%s' responsibility).", delivery.getDestinationBpns(), delivery.getPartner().getBpnl(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        } else if (delivery.getDestinationBpna() != null && partnerSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getDestinationBpna()))) {
            errors.add(String.format("Destination address '%s' is not configured for site '%s' of partner '%s' (incoterm '%s' with '%s' responsibility).", delivery.getDestinationBpna(), delivery.getDestinationBpns(), delivery.getPartner().getBpnl(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        }

        return errors;
    }

    /**
     * Validates the location consistency for the customer path of a Delivery.
     *
     * @param delivery     The Delivery being validated
     * @param ownSites     The set of sites configured for the own partner entity
     * @param partnerSites The set of sites configured for delivery partner entity
     * @return A list of validation error messages, an empty list means valid
     */
    protected List<String> validateLocationsAsCustomer(Delivery delivery, SortedSet<Site> ownSites, SortedSet<Site> partnerSites) {
        List<String> errors = new ArrayList<>();

        var ownSite = ownSites.stream().filter(site -> site.getBpns().equals(delivery.getDestinationBpns())).findFirst();
        var partnerSite = partnerSites.stream().filter(site -> site.getBpns().equals(delivery.getOriginBpns())).findFirst();

        if (!ownSite.isPresent()) {
            errors.add(String.format("Destination site '%s' must match one of the own partner entity's sites (incoterm '%s' with '%s' responsibility).", delivery.getDestinationBpns(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        } else if (delivery.getDestinationBpna() != null && ownSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getDestinationBpna()))) {
            errors.add(String.format("Destination address '%s' is not configured for own site '%s' (incoterm '%s' with '%s' responsibility).", delivery.getDestinationBpna(), delivery.getDestinationBpns(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        }
        if (!partnerSite.isPresent()) {
            errors.add(String.format("Origin site '%s' must match one site of partner '%s' (incoterm '%s' with '%s' responsibility).", delivery.getOriginBpns(), delivery.getPartner().getBpnl(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        } else if (delivery.getOriginBpna() != null && partnerSite.get().getAddresses().stream().noneMatch(address -> address.getBpna().equals(delivery.getOriginBpna()))) {
            errors.add(String.format("Origin address '%s' is not configured for site '%s' of partner '%s' (incoterm '%s' with '%s' responsibility).", delivery.getOriginBpna(), delivery.getOriginBpns(), delivery.getPartner().getBpnl(), delivery.getIncoterm().getValue(), delivery.getIncoterm().getResponsibility()));
        }

        return errors;
    }
    
}
