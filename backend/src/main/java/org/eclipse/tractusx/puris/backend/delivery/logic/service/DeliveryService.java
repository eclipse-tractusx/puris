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

import org.eclipse.tractusx.puris.backend.delivery.domain.model.Delivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.DeliveryRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

public abstract class DeliveryService<T extends Delivery> {

    final protected DeliveryRepository<T> repository;

    protected DeliveryService(DeliveryRepository<T> repository) {
        this.repository = repository;
    }

    public abstract boolean validate(T delivery);

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
        if (day.isPresent() && direction.isPresent() && ownMaterialNumber.isPresent() && bpns.isPresent() && bpnl.isPresent()) {
            Stream<T> stream = repository.getForOwnMaterialNumberAndPartnerBPNLAndBPNS(ownMaterialNumber.get(), bpnl.get(), bpns.get()).stream();
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
            if (direction.get() == DirectionCharacteristic.INBOUND) {
                stream = stream.filter(delivery -> delivery.getDestinationBpns().equals(bpns.get()));
            } else {
                stream = stream.filter(delivery -> delivery.getOriginBpns().equals(bpns.get()));
            }
            return stream.toList();
        }
        if (ownMaterialNumber.isPresent() && bpns.isPresent() && bpnl.isPresent()) {
            return repository.getForOwnMaterialNumberAndPartnerBPNLAndBPNS(ownMaterialNumber.get(), bpnl.get(), bpns.get());
        }
        if (ownMaterialNumber.isPresent() && bpns.isPresent()) {
            return repository.getForOwnMaterialNumberAndBPNS(ownMaterialNumber.get(), bpns.get());
        }
        if (ownMaterialNumber.isPresent() && bpnl.isPresent()) {
            return repository.getForOwnMaterialNumberAndPartnerBPNL(ownMaterialNumber.get(), bpnl.get());
        }
        if (ownMaterialNumber.isPresent()) {
            return repository.getForOwnMaterialNumber(ownMaterialNumber.get());
        }

        return List.of();
    }

    public final double getSumOfQuantities(List<T> deliveries) {
        double sum = 0;
        for (T delivery : deliveries) {
            sum += delivery.getQuantity();
        }
        return sum;
    }

    public final List<Double> getQuantityForDays(String material, String partnerBpnl, String siteBpns, DirectionCharacteristic direction, int numberOfDays) {
        List<Double> deliveryQtys = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<T> deliveries = findAllByFilters(Optional.of(material), Optional.of(siteBpns), Optional.of(partnerBpnl), Optional.of(date), Optional.of(direction));
            deliveryQtys.add(getSumOfQuantities(deliveries));

            localDate = localDate.plusDays(1);
        }
        return deliveryQtys;
    }

    public final T update(T delivery) {
        if (delivery.getUuid() == null || repository.findById(delivery.getUuid()).isEmpty()) {
            return null;
        }
        if (validate(delivery)) {
            return repository.save(delivery);
        } else {
            return null;
        }
    }

    public final void delete(UUID id) {
        repository.deleteById(id);
    }
}
