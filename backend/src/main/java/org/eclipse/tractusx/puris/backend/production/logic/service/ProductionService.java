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
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ProductionRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductionService {
    public final ProductionRepository repository;

    protected final Function<OwnProduction, Boolean> validator;

    public ProductionService(ProductionRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final List<OwnProduction> findAll() {
        return repository.findAll();
    }

    public final List<OwnProduction> findAllByPartnerId(UUID partnerId) {
        return repository.findAll().stream().filter(production -> production.getPartner().getUuid().equals(partnerId))
                .toList();
    }

    public final OwnProduction findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final OwnProduction create(OwnProduction production) {
        if (production.getUuid() != null && repository.findById(production.getUuid()).isPresent()) {
            return null;
        }
        if (!validator.apply(production)) {
            return null;
        }
        return repository.save(production);
    }

    public final List<OwnProduction> createAll(List<OwnProduction> productions) {
        if (productions.stream().anyMatch(production -> !validator.apply(production))) {
            return null;
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> productions.stream().anyMatch(production -> production.equals(existing)))) {
            return null;
        }
        return repository.saveAll(productions);
    }

    public final OwnProduction update(OwnProduction production) {
        if (production.getUuid() == null || repository.findById(production.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(production);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public boolean validate(OwnProduction production) {
        return production.getQuantity() > 0 && production.getMeasurementUnit() != null
                && production.getEstimatedTimeOfCompletion() != null && production.getMaterial() != null
                && production.getProductionSiteBpns() != null;
    }
}
