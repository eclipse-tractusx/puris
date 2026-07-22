/*
Copyright (c) 2026 Volkswagen AG

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
package org.eclipse.tractusx.puris.backend.aggregateddata.logic.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public abstract class AggregatedDataService<TEntity extends AggregatedData, TRepository extends JpaRepository<TEntity, UUID>> {
    protected final TRepository repository;
    protected final PartnerService partnerService;

    protected final Function<TEntity, Boolean> validator;

    public AggregatedDataService(TRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final TEntity create(TEntity aggregatedData) {
        if (!validator.apply(aggregatedData)) {
            throw new IllegalArgumentException("Invalid aggregated data");
        }
        if (aggregatedData.getUuid() != null && repository.findById(aggregatedData.getUuid()).isPresent()) {
            throw new KeyAlreadyExistsException("Aggregated data already exists");
        }
        return repository.save(aggregatedData);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public abstract boolean validate(TEntity aggregatedData);

    protected final boolean validateCommonFields(AggregatedData aggregatedData) {
        boolean deliveriesValid = aggregatedData.getDeliveries() == null ||
                aggregatedData.getDeliveries().stream().allMatch(delivery ->
                        delivery.getMeasurementUnit() != null &&
                        delivery.getLastUpdatedOnDateTime() != null &&
                        delivery.getDateOfDeparture() != null &&
                        delivery.getDepartureType() != null &&
                        delivery.getOriginBpnsAnonymized() != null &&
                        delivery.getDestinationBpnsAnonymized() != null);

        boolean stocksValid = aggregatedData.getStocks() == null ||
                aggregatedData.getStocks().stream().allMatch(stock ->
                        stock.getMeasurementUnit() != null &&
                        stock.getStockLocationBpnsAnonymized() != null &&
                        stock.getLastUpdatedOnDateTime() != null);

        boolean productionsValid = aggregatedData.getProductions() == null ||
                aggregatedData.getProductions().stream().allMatch(production ->
                        production.getMeasurementUnit() != null &&
                        production.getProductionSiteBpnsAnonymized() != null &&
                        production.getEstimatedTimeOfCompletion() != null &&
                        production.getLastUpdatedOnDateTime() != null);

        boolean childrenValid = aggregatedData.getChildData() == null ||
                aggregatedData.getChildData().stream().allMatch(child ->
                        child.getExternalMaterialNumber() != null && !child.getExternalMaterialNumber().isBlank() &&
                        child.getExternalMaterialName() != null && !child.getExternalMaterialName().isBlank() &&
                        child.getParentData() == aggregatedData &&
                        validateCommonFields(child));

        return deliveriesValid && stocksValid && productionsValid && childrenValid;
    }
}

