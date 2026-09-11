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
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.repository.AggregatedMaterialDataRepository;
import org.springframework.stereotype.Service;

@Service
public class AggregatedMaterialDataService  {
    private final AggregatedMaterialDataRepository repository;
    private final AggregatedMaterialDataNodeService nodeService;
 
    public AggregatedMaterialDataService(AggregatedMaterialDataRepository repository, AggregatedMaterialDataNodeService nodeService) {
        this.repository = repository;
        this.nodeService = nodeService;
    }
 
    public AggregatedMaterialData findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }
 
    public List<AggregatedMaterialData> findAll() {
        return repository.findAll();
    }
 
    public List<AggregatedMaterialData> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAllByMaterial_OwnMaterialNumber(ownMaterialNumber);
    }
 
    public AggregatedMaterialData create(AggregatedMaterialData aggregatedMaterialData) {
        if (!validate(aggregatedMaterialData)) {
            throw new IllegalArgumentException("Invalid aggregated material data");
        }
        if (aggregatedMaterialData.getUuid() != null && repository.existsById(aggregatedMaterialData.getUuid())) {
            throw new KeyAlreadyExistsException("Aggregated material data already exists");
        }
        return repository.save(aggregatedMaterialData);
    }
 
    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }
 
    public boolean validate(AggregatedMaterialData aggregatedMaterialData) {
        return aggregatedMaterialData != null && aggregatedMaterialData.getMaterial() != null && validateRootNodes(aggregatedMaterialData);
    }
 
    private boolean validateRootNodes(AggregatedMaterialData aggregatedMaterialData) {
        return aggregatedMaterialData.getChildMaterialData() == null
            || aggregatedMaterialData.getChildMaterialData().stream().allMatch(node ->
                node.getParentNode() == null
                && node.getAggregatedMaterialData() == aggregatedMaterialData
                && nodeService.validate(node));
    }
}
