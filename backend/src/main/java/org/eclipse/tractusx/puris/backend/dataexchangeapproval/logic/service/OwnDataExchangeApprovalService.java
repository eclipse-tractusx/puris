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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.OwnDataExchangeApprovalRepository;
import org.springframework.stereotype.Service;

@Service
public class OwnDataExchangeApprovalService extends DataExchangeApprovalService<OwnDataExchangeApproval, OwnDataExchangeApprovalRepository> {
    
    public OwnDataExchangeApprovalService(OwnDataExchangeApprovalRepository repository) {
        super(repository);
    }

    public final OwnDataExchangeApproval create(OwnDataExchangeApproval ownDataExchangeApproval) {
        if (ownDataExchangeApproval == null || !validator.apply(ownDataExchangeApproval)) {  
            throw new IllegalArgumentException("Invalid data exchange approval");
        }
        UUID requestUuid = ownDataExchangeApproval.getDataExchangeRequest().getUuid();
        if (repository.findByDataExchangeRequest_Uuid(requestUuid).isPresent()) {
            throw new KeyAlreadyExistsException("Data exchange approval already exists for data exchange request with uuid: " + requestUuid);
        }
        return repository.save(ownDataExchangeApproval);
    }

    @Override
    public boolean validate(OwnDataExchangeApproval dataExchangeApproval) {
        return dataExchangeApproval != null &&
        basicValidation(dataExchangeApproval) &&
        dataExchangeApproval.getDataExchangeRequest() != null &&
        dataExchangeApproval.getDataExchangeRequest().getUuid() != null;
    }
}
