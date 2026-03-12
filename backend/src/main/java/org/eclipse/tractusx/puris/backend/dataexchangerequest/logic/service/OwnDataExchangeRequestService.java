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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service;
import java.util.function.Function;
import javax.management.openmbean.KeyAlreadyExistsException;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class OwnDataExchangeRequestService extends DataExchangeRequestService<OwnDataExchangeRequest> {
    private final OwnDataExchangeRequestRepository repository;
    protected final Function<OwnDataExchangeRequest, Boolean> validator;

    public OwnDataExchangeRequestService(OwnDataExchangeRequestRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final OwnDataExchangeRequest create(OwnDataExchangeRequest ownDataExchangeRequest) {
        if (ownDataExchangeRequest == null || !validator.apply(ownDataExchangeRequest)) {  
            throw new IllegalArgumentException("Invalid data exchange request");
        }
        if (repository.findAll().stream().filter(existing -> existing.equals(ownDataExchangeRequest)).findFirst().isPresent()) {
            throw new KeyAlreadyExistsException("Notification already exists");
        }
        return repository.save(ownDataExchangeRequest);
    }

    public boolean validate(OwnDataExchangeRequest dataExchangeRequest) {
        return basicValidation(dataExchangeRequest);
    }
}
