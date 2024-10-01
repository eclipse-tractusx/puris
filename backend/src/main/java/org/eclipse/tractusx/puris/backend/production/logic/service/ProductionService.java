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
package org.eclipse.tractusx.puris.backend.production.logic.service;

import org.eclipse.tractusx.puris.backend.production.domain.model.Production;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ProductionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

public abstract class ProductionService<T extends Production>  {

    protected final ProductionRepository<T> repository;

    protected ProductionService(ProductionRepository<T> repository) {
        this.repository = repository;
    }

    public final List<T> findAll() {
        return repository.findAll();
    }

    public final T findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }    

    public final List<T> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(production -> production.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<T> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(production -> production.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<T> findAllByFilters(
        Optional<String> ownMaterialNumber,
        Optional<String> bpnl,
        Optional<String> bpns,
        Optional<Date> dayOfCompletion) {
        if (ownMaterialNumber.isPresent() && bpnl.isPresent() && bpns.isPresent() && dayOfCompletion.isPresent()) {
            var resultList = repository.getForOwnMaterialNumberAndPartnerBPNLAndBPNS(ownMaterialNumber.get(), bpnl.get(), bpns.get());
            LocalDate localEstimatedTimeOfCompletion = Instant.ofEpochMilli(dayOfCompletion.get().getTime())
                .atOffset(ZoneOffset.UTC)
                .toLocalDate();
            
            return resultList.stream()
                .filter(production -> {
                    LocalDate productionEstimatedTimeOfCompletion = Instant.ofEpochMilli(production.getEstimatedTimeOfCompletion().getTime())
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate();
                    return productionEstimatedTimeOfCompletion.getDayOfMonth() == localEstimatedTimeOfCompletion.getDayOfMonth()
                        && productionEstimatedTimeOfCompletion.getMonth() == localEstimatedTimeOfCompletion.getMonth() &&
                        productionEstimatedTimeOfCompletion.getYear() == localEstimatedTimeOfCompletion.getYear();
                }).toList();
        }
        if (ownMaterialNumber.isPresent() && bpnl.isPresent() && bpns.isPresent()) {
            return repository.getForOwnMaterialNumberAndPartnerBPNLAndBPNS(ownMaterialNumber.get(), bpnl.get(), bpns.get());
        }
        if (ownMaterialNumber.isPresent() && bpnl.isPresent()) {
            return repository.getForOwnMaterialNumberAndPartnerBPNL(ownMaterialNumber.get(), bpnl.get());
        }
        if (ownMaterialNumber.isPresent() && bpns.isPresent()) {
            return repository.getForOwnMaterialNumberAndBPNS(ownMaterialNumber.get(), bpns.get());
        }
        if (ownMaterialNumber.isPresent()) {
            return repository.getForOwnMaterialNumber(ownMaterialNumber.get());
        }
        return List.of();

    }

    public final List<Double> getQuantityForDays(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<Double> quantities = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<T> productions = findAllByFilters(Optional.of(material), Optional.of(partnerBpnl), Optional.of(siteBpns), Optional.of(date));
            double productionQuantity = getSumOfQuantities(productions);
            quantities.add(productionQuantity);

            localDate = localDate.plusDays(1);
        }
        return quantities;
    }

    public abstract boolean validate(T production);

    public final T update(T production) {
        if (production.getUuid() == null || repository.findById(production.getUuid()).isEmpty()) {
            return null;
        }
        if (validate(production)) {
            return repository.save(production);
        } else {
            return null;
        }
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    private double getSumOfQuantities(List<T> productions) {
        double sum = 0;
        for (T production : productions) {
            sum += production.getQuantity();
        }
        return sum;
    }
}
