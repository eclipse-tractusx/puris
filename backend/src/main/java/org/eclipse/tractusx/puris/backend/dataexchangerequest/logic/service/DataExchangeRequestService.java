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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.DataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class  DataExchangeRequestService<T extends DataExchangeRequest> {
    @Autowired
    protected DataExchangeRequestRepository<T> repository;

    public final List<T> findAll() {
        return repository.findAll();
    }

    public final T findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    protected List<String> basicValidation(DataExchangeRequest dataExchangeRequest) {
        List<String> errors = new ArrayList<>();

        addIfNull(errors, dataExchangeRequest.getCriticality(), "Missing criticality.");
        addIfNull(errors, dataExchangeRequest.getText(), "Missing text.");
        addIfNull(errors, dataExchangeRequest.getTimestamp(), "Missing timestamp.");
        addIfNull(errors, dataExchangeRequest.getDesiredStartDateTime(), "Missing desired start date and time.");
        addIfNull(errors, dataExchangeRequest.getDesiredEndDateTime(), "Missing desired end date and time.");
        addIfNull(errors, dataExchangeRequest.getNotification(), "Missing reference to demand and capacity notification.");
        if (dataExchangeRequest.getRequestedTypes() == null || dataExchangeRequest.getRequestedTypes().isEmpty()) {
            errors.add("Missing requested types.");
        }
        validateDesiredDate(errors, dataExchangeRequest.getDesiredStartDateTime(), "Desired start date and time", dataExchangeRequest.getNotification());

        validateDesiredDate(errors, dataExchangeRequest.getDesiredEndDateTime(), "Desired end date and time", dataExchangeRequest.getNotification());
        return errors;
    }
    private void addIfNull(List<String> errors, Object value, String message) {
        if (value == null) {
            errors.add(message);
        }
    }
    private void validateDesiredDate(List<String> errors, Date desiredDate, String fieldName, ReportedDemandAndCapacityNotification notification) {
        if (desiredDate == null || notification == null) {
            return;
        }
        if (notification.getStartDateOfEffect() != null && desiredDate.before(notification.getStartDateOfEffect())) {
            errors.add(fieldName + " must be after start date of effect of the reference notification.");
        }
        if (notification.getExpectedEndDateOfEffect() != null && desiredDate.after(notification.getExpectedEndDateOfEffect())) {
            errors.add(fieldName + " must be before expected end date of effect of the reference notification.");
        }
    }
}
