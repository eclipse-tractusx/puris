/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.production.logic.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ReportedProductionRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportedProductionService {
    private final ReportedProductionRepository repository;

    protected final Function<ReportedProduction, Boolean> validator;

    public ReportedProductionService(ReportedProductionRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final List<ReportedProduction> findAll() {
        return repository.findAll();
    }

    public final List<ReportedProduction> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(production -> production.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<ReportedProduction> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(production -> production.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<ReportedProduction> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpnl, Optional<String> bpns) {
        Stream<ReportedProduction> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(production -> production.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(production -> production.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (bpns.isPresent()) {
            stream = stream.filter(production -> production.getProductionSiteBpns().equals(bpns.get()));
        }
        return stream.toList();
    }

    public final ReportedProduction findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final ReportedProduction create(ReportedProduction production) {
        if (repository.findAll().stream().anyMatch(prod -> prod.equals(production))) {
            return null;
        }
        if (!validator.apply(production)) {
            return null;
        }
        return repository.save(production);
    }

    public final List<ReportedProduction> createAll(List<ReportedProduction> productions) {
        if (productions.stream().anyMatch(production -> !validator.apply(production))) {
            return null;
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> productions.stream().anyMatch(production -> production.equals(existing)))) {
            return null;
        }
        return repository.saveAll(productions);
    }

    public final ReportedProduction update(ReportedProduction production) {
        if (production.getUuid() == null || repository.findById(production.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(production);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public boolean validate(ReportedProduction production) {
        return 
            production.getQuantity() > 0 && 
            production.getMeasurementUnit() != null && 
            production.getEstimatedTimeOfCompletion() != null && 
            production.getMaterial() != null &&
            production.getPartner() != null &&
            production.getProductionSiteBpns() != null &&
            production.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(production.getProductionSiteBpns())) &&
            ((
                production.getCustomerOrderNumber() != null && 
                production.getCustomerOrderPositionNumber() != null
            ) || (
                production.getCustomerOrderNumber() == null && 
                production.getCustomerOrderPositionNumber() == null && 
                production.getSupplierOrderNumber() == null
            ));
    }
}
