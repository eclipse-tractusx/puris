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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.demand.domain.model.Demand;
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
