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
            throw new KeyAlreadyExistsException("Data exchange request already exists");
        }
        return repository.save(reportedDataExchangeRequest);
    }

    public final ReportedDataExchangeRequest update(ReportedDataExchangeRequest reportedDataExchangeRequest) {
        if (!validator.apply(reportedDataExchangeRequest)) {
            throw new IllegalArgumentException("Invalid request");
        }
        if (reportedDataExchangeRequest.getUuid() == null || repository.findById(reportedDataExchangeRequest.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(reportedDataExchangeRequest);
    }

    public boolean validate(ReportedDataExchangeRequest dataExchangeRequest) {
        return validateWithDetails(dataExchangeRequest).isEmpty();
    }

    public List<String> validateWithDetails(ReportedDataExchangeRequest dataExchangeRequest) {
        List<String> errors = new ArrayList<>();
        errors.addAll(basicValidation(dataExchangeRequest));
        if (dataExchangeRequest.getNotification() == null) {
            errors.add("Missing notification.");
        }
        errors.addAll(validateDesiredDates(dataExchangeRequest));

        return errors;
    }

    private List<String> validateDesiredDates(ReportedDataExchangeRequest reportedDataExchangeRequest) {
        List<String> errors = new ArrayList<>();
        if (!reportedDataExchangeRequest.getDesiredStartDateTime().before(reportedDataExchangeRequest.getDesiredEndDateTime())) {
            errors.add("desiredStartDateTime must be before desiredEndDateTime.");
        }
        if (reportedDataExchangeRequest.getDesiredStartDateTime().before(reportedDataExchangeRequest.getNotification().getStartDateOfEffect())) {
            errors.add("desiredStartDateTime must not be before notification startDateOfEffect.");
        }
        if (reportedDataExchangeRequest.getDesiredEndDateTime().before(reportedDataExchangeRequest.getNotification().getStartDateOfEffect())) {
            errors.add("desiredEndDateTime must not be before notification startDateOfEffect.");
        }
        if (reportedDataExchangeRequest.getNotification().getExpectedEndDateOfEffect() != null) {
            if (reportedDataExchangeRequest.getDesiredStartDateTime().after(reportedDataExchangeRequest.getNotification().getExpectedEndDateOfEffect())) {
                errors.add("desiredStartDateTime must not be after notification expectedEndDateOfEffect.");
            }
            if (reportedDataExchangeRequest.getDesiredEndDateTime().after(reportedDataExchangeRequest.getNotification().getExpectedEndDateOfEffect())) {
                errors.add("desiredEndDateTime must not be after notification expectedEndDateOfEffect.");
            }
        }
        return errors;
    }
}
