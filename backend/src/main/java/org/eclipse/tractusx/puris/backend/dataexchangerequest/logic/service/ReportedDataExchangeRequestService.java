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

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportedDataExchangeRequestService  extends DataExchangeRequestService<ReportedDataExchangeRequest> {
    private final ReportedDataExchangeRequestRepository repository;
    protected final Function<ReportedDataExchangeRequest, Boolean> validator;

    public ReportedDataExchangeRequestService(ReportedDataExchangeRequestRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final ReportedDataExchangeRequest create(ReportedDataExchangeRequest reportedDataExchangeRequest) {
        if (reportedDataExchangeRequest == null || !validator.apply(reportedDataExchangeRequest)) {  
            throw new IllegalArgumentException("Invalid data exchange request");
        }
        if (repository.findAll().stream().filter(existing -> existing.equals(reportedDataExchangeRequest)).findFirst().isPresent()) {
            throw new KeyAlreadyExistsException("Notification already exists");
        }
        return repository.save(reportedDataExchangeRequest);
    }

    public boolean validate(ReportedDataExchangeRequest dataExchangeRequest) {
        return basicValidation(dataExchangeRequest);
    }
}
