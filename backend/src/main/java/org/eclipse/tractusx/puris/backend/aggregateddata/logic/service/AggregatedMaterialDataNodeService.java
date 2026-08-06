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

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedMaterialData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedMaterialDataNode;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.repository.AggregatedMaterialDataNodeRepository;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedAnonymizedDelivery;
import org.springframework.stereotype.Service;

@Service
public class AggregatedMaterialDataNodeService {
    private final AggregatedMaterialDataNodeRepository repository;
 
    public AggregatedMaterialDataNodeService(AggregatedMaterialDataNodeRepository repository) {
        this.repository = repository;
    }
 
    public AggregatedMaterialDataNode findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }
 
    public List<AggregatedMaterialDataNode> findAll() {
        return repository.findAll();
    }

    public AggregatedMaterialDataNode create(AggregatedMaterialDataNode node) {
        if (!validate(node)) {
            throw new IllegalArgumentException("Invalid aggregated material data node");
        }
        if (node.getUuid() != null && repository.existsById(node.getUuid())) {
            throw new KeyAlreadyExistsException("Aggregated material data node already exists");
        }
        return repository.save(node);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public boolean validate(AggregatedMaterialDataNode node) {
        if (node == null || node.getAggregatedMaterialData() == null) {
            return false;
        }
        return validateSubtree(node, node.getAggregatedMaterialData(), node.getParentNode());
    }
 
    boolean validateSubtree(AggregatedMaterialDataNode node, AggregatedMaterialData expectedAggregate, AggregatedMaterialDataNode expectedParent) {
        return node != null
            && isNotBlank(node.getExternalMaterialNumber())
            && isNotBlank(node.getExternalMaterialName())
            && node.getMeasurementUnit() != null
            && node.getAggregatedMaterialData() == expectedAggregate
            && node.getParentNode() == expectedParent
            && validateDeliveries(node)
            && validateStocks(node)
            && validateProductions(node)
            && validateChildren(node, expectedAggregate);
    }
 
    private boolean validateChildren(AggregatedMaterialDataNode node, AggregatedMaterialData expectedAggregate) {
        return node.getChildMaterialData() == null || node.getChildMaterialData().stream().allMatch(child -> validateSubtree(child, expectedAggregate, node));
    }
 
    private boolean validateDeliveries(AggregatedMaterialDataNode node) {
        return node.getDeliveries() == null || node.getDeliveries().stream().allMatch(this::isValidDelivery);
    }

    private boolean isValidDelivery(ReportedAnonymizedDelivery delivery) {
        return delivery.getQuantity() > 0
            && delivery.getMeasurementUnit() != null
            && delivery.getLastUpdatedOnDateTime() != null
            && delivery.getDateOfDeparture() != null
            && delivery.getDepartureType() != null
            && delivery.getOriginBpnsAnonymized() != null
            && delivery.getDestinationBpnsAnonymized() != null
            && isArrivalDateValid(delivery);
    }

    private static boolean isArrivalDateValid(ReportedAnonymizedDelivery delivery) {
        if ( delivery.getDateOfArrival() == null) {
            return delivery.getArrivalType() == null;
        }
        return delivery.getArrivalType() != null && delivery.getDateOfArrival().toInstant().isAfter(delivery.getDateOfDeparture().toInstant());
    }

    private boolean validateStocks(AggregatedMaterialDataNode node) {
        return node.getStocks() == null
            || node.getStocks().stream().allMatch(stock ->
                stock.getQuantity() > 0
                && stock.getMeasurementUnit() != null
                && stock.getStockLocationBpnsAnonymized() != null
                && stock.getLastUpdatedOnDateTime() != null);
    }

    private boolean validateProductions(AggregatedMaterialDataNode node) {
        return node.getProductions() == null
            || node.getProductions().stream().allMatch(production ->
                production.getQuantity() > 0
                && production.getMeasurementUnit() != null
                && production.getProductionSiteBpnsAnonymized() != null
                && production.getEstimatedTimeOfCompletion() != null
                && production.getLastUpdatedOnDateTime() != null);
    }
 
    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
