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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.OwnProductionRepository;
import org.springframework.stereotype.Service;

@Service
public class OwnProductionService extends ProductionService<OwnProduction> {
    private final OwnProductionRepository repository;

    private final PartnerService partnerService;

    protected final Function<OwnProduction, Boolean> validator;

    public OwnProductionService(OwnProductionRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final OwnProduction create(OwnProduction production) {
        if (!validator.apply(production)) {
            
            throw new IllegalArgumentException("Invalid production");
        }
        if (repository.findAll().stream().anyMatch(prod -> prod.equals(production))) {
            throw new KeyAlreadyExistsException("Production already exists");
        }
        return repository.save(production);
    }

    public final List<OwnProduction> createAll(List<OwnProduction> productions) {
        if (productions.stream().anyMatch(production -> !validator.apply(production))) {
            throw new IllegalArgumentException("Invalid production");
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> productions.stream().anyMatch(production -> production.equals(existing)))) {
            throw new KeyAlreadyExistsException("Production already exists");
        }
        return repository.saveAll(productions);
    }

    public boolean validate(OwnProduction production) {
        return validateWithDetails(production).isEmpty();
    }

    public List<String> validateWithDetails(OwnProduction production) {
        List<String> errors = new ArrayList<>();
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();

        if (production.getQuantity() <= 0) {
            errors.add("Quantity must be greater than 0.");
        }
        if (production.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (production.getLastUpdatedOnDateTime() == null) {
            errors.add("Missing lastUpdatedOnTime.");
        } else if (production.getLastUpdatedOnDateTime().after(new Date())) {
            errors.add("lastUpdatedOnDateTime cannot be in the future.");
        }
        if (production.getEstimatedTimeOfCompletion() == null) {
            errors.add("Missing estimated time of completion.");
        }
        if (production.getMaterial() == null) {
            errors.add("Missing material.");
        }
        if (production.getPartner() == null) {
            errors.add("Missing partner.");
        }
        if (production.getPartner().equals(ownPartnerEntity)) {
            errors.add("Partner cannot be the same as own partner entity.");
        }
        if (production.getProductionSiteBpns() == null) {
            errors.add("Missing production site BPNS.");
        }
        if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(production.getProductionSiteBpns()))) {
            errors.add("Production site BPNS must match one of the own partner entity's site BPNS.");
        }
        if (!((production.getCustomerOrderNumber() != null && production.getCustomerOrderPositionNumber() != null) || 
            (production.getCustomerOrderNumber() == null && production.getCustomerOrderPositionNumber() == null && production.getSupplierOrderNumber() == null))) {
            errors.add("If an order position reference is given, customer order number and customer order position number must be set.");
        }

        return errors;
    }
}
