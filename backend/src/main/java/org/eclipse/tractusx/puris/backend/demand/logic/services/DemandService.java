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
package org.eclipse.tractusx.puris.backend.demand.logic.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.demand.domain.model.Demand;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class DemandService<TEntity extends Demand, TRepository extends JpaRepository<TEntity, UUID> >  {
    protected final TRepository repository;
    protected final PartnerService partnerService;
    protected final MaterialPartnerRelationService mprService;

    protected final Function<TEntity, Boolean> validator;

    public DemandService(TRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.mprService = mprService;
        this.validator = this::validate;
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final List<TEntity> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(demand -> demand.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<TEntity> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(demand -> demand.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<TEntity> findAllByFilters(
        Optional<String> ownMaterialNumber,
        Optional<String> bpnl,
        Optional<String> demandLocationBpns) {
        var stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(demand -> demand.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(demand -> demand.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (demandLocationBpns.isPresent()) {
            stream = stream.filter(demand -> demand.getDemandLocationBpns().equals(demandLocationBpns.get()));
        }
        return stream.toList();
    }

    protected List<String> basicValidation(Demand demand) {
        List<String> errors = new ArrayList<>();
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();

        if (demand.getMaterial() == null) {
            errors.add("Missing Material.");
        }
        if (demand.getPartner() == null) {
            errors.add("Missing Partner.");
        }
        if (demand.getQuantity() < 0) {
            errors.add(String.format("Quantity '%s' must be greater than or equal to 0.", demand.getQuantity()));
        }
        if (demand.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (demand.getLastUpdatedOnDateTime() == null) {
            errors.add("Missing lastUpdatedOnTime.");
        } else if (demand.getLastUpdatedOnDateTime().after(new Date())) {
            errors.add(String.format("lastUpdatedOnDateTime '%s' must be in the past (system time: '%s').", demand.getLastUpdatedOnDateTime().toInstant().toString(), (new Date()).toInstant().toString()));
        }
        if (demand.getDay() == null) {
            errors.add("Missing day.");
        }
        if (demand.getDemandCategoryCode() == null) {
            errors.add("Missing demand category code.");
        }
        if (demand.getDemandLocationBpns() == null) {
            errors.add("Missing demand location BPNS.");
        }
        if (demand.getPartner().equals(ownPartnerEntity)) {
            errors.add(String.format("Partner cannot be the same as own partner entity '%s'.", demand.getPartner().getBpnl()));
        }
        return errors;
    }

    protected List<String> validateReportedDemand(Demand demand) {
        List<String> errors = new ArrayList<>();
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();
        if (!mprService.partnerOrdersProduct(demand.getMaterial(), demand.getPartner())) {
            errors.add(String.format("Partner '%s' is not configured to buy your material '%s'.", demand.getPartner().getBpnl(), demand.getMaterial().getOwnMaterialNumber()));
        }
        if ((demand.getSupplierLocationBpns() != null  && 
            ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getSupplierLocationBpns())))
            || demand.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getDemandLocationBpns()))) {
            errors.add(String.format("Either supplier location '%s' is not configured as your site or demand location '%s' is not configured as site for customer partner '%s'.", demand.getSupplierLocationBpns(), demand.getDemandLocationBpns(), demand.getPartner().getBpnl()));
        }
        return errors;
    }

    protected List<String> validateOwnDemand(Demand demand) {
        List<String> errors = new ArrayList<>();
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();
        if (!mprService.partnerSuppliesMaterial(demand.getMaterial(), demand.getPartner())) {
            errors.add(String.format("Partner '%s' is not configured to supply you the specified material '%s'.", demand.getPartner().getBpnl(), demand.getMaterial().getOwnMaterialNumber()));
        }
        if (ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getDemandLocationBpns()))) {
            errors.add(String.format("Demand location BPNS '%s' must match to one site configured for your own partner '%s' .", demand.getDemandLocationBpns(), ownPartnerEntity.getBpnl()));
        }
        if (demand.getSupplierLocationBpns() != null && 
            demand.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getSupplierLocationBpns()))) {
            errors.add(String.format("Expected supplier location BPNS '%s' must match to one site of the partner '%s' .", demand.getSupplierLocationBpns(), demand.getPartner().getBpnl()));
        }
        return errors;
    }

    public final TEntity create(TEntity demand) {
        if (!validator.apply(demand)) {
            throw new IllegalArgumentException("Invalid demand");
        }
        if (repository.findAll().stream().anyMatch(d -> d.equals(demand))) {
            throw new KeyAlreadyExistsException("Demand already exists");
        }
        return repository.save(demand);
    }

    public final TEntity update(TEntity demand) {
        if (demand.getUuid() == null || repository.findById(demand.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(demand);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public abstract boolean validate(TEntity demand);
}
