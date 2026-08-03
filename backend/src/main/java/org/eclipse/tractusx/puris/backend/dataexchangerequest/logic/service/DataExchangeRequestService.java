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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.DataExchangeRequestRepository;

public abstract class  DataExchangeRequestService<TEntity extends DataExchangeRequest, TRepository extends DataExchangeRequestRepository<TEntity>> {
    protected final TRepository repository;
    protected final Function<TEntity, Boolean> validator;

    public DataExchangeRequestService(TRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final TEntity findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).orElse(null);
    }

    protected List<String> basicValidation(DataExchangeRequest dataExchangeRequest) {
        List<String> errors = new ArrayList<>();
        if (dataExchangeRequest.getCriticality() == null) {
            errors.add("Missing criticality.");
        }
        if (dataExchangeRequest.getText() == null) {
            errors.add("Missing text.");
        }
        if (dataExchangeRequest.getDesiredStartDateTime() == null) {
            errors.add("Missing desiredStartDateTime.");
        }
        if (dataExchangeRequest.getDesiredEndDateTime() == null) {
            errors.add("Missing desiredEndDateTime.");
        }
        if (dataExchangeRequest.getUuid() != null && dataExchangeRequest.getTimestamp() == null) {
            errors.add("timestamp must be set when uuid is present.");
        }
        if (dataExchangeRequest.getRequestedTypes() == null) {
            errors.add("Missing requestedTypes.");
        } else if (dataExchangeRequest.getRequestedTypes().isEmpty()) {
            errors.add("requestedTypes must not be empty.");
        }
        return errors;
    }
    
    public abstract boolean validate(TEntity request);
}
