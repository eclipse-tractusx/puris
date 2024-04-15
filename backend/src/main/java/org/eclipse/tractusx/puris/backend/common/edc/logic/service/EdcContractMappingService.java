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

package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.EdcContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.EdcContractMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EdcContractMappingService {

    @Autowired
    private EdcContractMappingRepository repository;

    public EdcContractMapping find(String partnerBpnl) {
        return repository.findById(partnerBpnl).orElse(null);
    }

    public EdcContractMapping create(EdcContractMapping edcContractMapping) {
        if (repository.findById(edcContractMapping.getPartnerBpnl()).isPresent()) {
            return null;
        }
        return repository.save(edcContractMapping);
    }

    public EdcContractMapping update(EdcContractMapping edcContractMapping) {
        if (!repository.findById(edcContractMapping.getPartnerBpnl()).isPresent()) {
            return null;
        }
        return repository.save(edcContractMapping);
    }

    public void delete (String partnerBpnl) {
        repository.deleteById(partnerBpnl);
    }

    public void delete(EdcContractMapping edcContractMapping) {
        delete(edcContractMapping.getPartnerBpnl());
    }
}
