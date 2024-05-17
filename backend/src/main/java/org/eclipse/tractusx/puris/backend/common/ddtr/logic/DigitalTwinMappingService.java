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

package org.eclipse.tractusx.puris.backend.common.ddtr.logic;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.ddtr.domain.model.DigitalTwinMapping;
import org.eclipse.tractusx.puris.backend.common.ddtr.domain.repository.DigitalTwinMappingRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class DigitalTwinMappingService {

    @Autowired
    private DigitalTwinMappingRepository repository;


    public DigitalTwinMapping create(Material material) {
        if(repository.findById(material.getOwnMaterialNumber()).isPresent()) {
            log.error("DTR Mapping for " + material.getOwnMaterialNumber() + " already exists");
            return null;
        }
        DigitalTwinMapping dtm = new DigitalTwinMapping();
        dtm.setOwnMaterialNumber(material.getOwnMaterialNumber());
        if (material.isProductFlag()) {
            dtm.setProductTwinId(UUID.randomUUID().toString());
        }
        return repository.save(dtm);
    }

    public DigitalTwinMapping update(Material material) {
        var searchResult = repository.findById(material.getOwnMaterialNumber());
        if (searchResult.isEmpty()) {
            log.error("DTR Mapping did not exist. Update failed for " + material.getOwnMaterialNumber());
            return null;
        }
        var dtm = searchResult.get();
        if (material.isProductFlag()) {
            dtm.setProductTwinId(UUID.randomUUID().toString());
        }
        return repository.save(dtm);
    }

    public DigitalTwinMapping get(String ownMaterialNumber) {
        return repository.findById(ownMaterialNumber).orElse(null);
    }

    public DigitalTwinMapping get(Material material) {
        return get(material.getOwnMaterialNumber());
    }
}
