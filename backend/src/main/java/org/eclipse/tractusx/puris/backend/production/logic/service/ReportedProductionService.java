/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ReportedProductionRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportedProductionService extends ProductionService<ReportedProduction> {
    private final ReportedProductionRepository repository;

    protected final Function<ReportedProduction, Boolean> validator;

    public ReportedProductionService(ReportedProductionRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
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

    public boolean validate(ReportedProduction production) {
        return basicValidation(production).isEmpty() && validateReportedProduction(production).isEmpty();
    }

    public List<String> validateWithDetails(ReportedProduction production) {
        List<String> validationErrors = new ArrayList<>();
        validationErrors.addAll(basicValidation(production));
        validationErrors.addAll(validateReportedProduction(production));
        return validationErrors;
    }
}
