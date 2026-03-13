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

    protected boolean basicValidation(DataExchangeRequest dataExchangeRequest) {
        return dataExchangeRequest.getCriticality() != null &&
            dataExchangeRequest.getText() != null &&
            dataExchangeRequest.getDesiredStartDateTime() != null &&
            dataExchangeRequest.getDesiredEndDateTime() != null &&
            dataExchangeRequest.getNotification() != null &&
            (dataExchangeRequest.getUuid() == null || dataExchangeRequest.getTimestamp() != null) &&
            dataExchangeRequest.getRequestedTypes() != null &&
            !dataExchangeRequest.getRequestedTypes().isEmpty() &&
            validateDesiredDates(dataExchangeRequest, dataExchangeRequest.getNotification());
    }
    private boolean validateDesiredDates(DataExchangeRequest ownDataExchangeRequest, ReportedDemandAndCapacityNotification notification) {
        if (!ownDataExchangeRequest.getDesiredStartDateTime().before(ownDataExchangeRequest.getDesiredEndDateTime())) {
            return false;
        }
        if (ownDataExchangeRequest.getDesiredStartDateTime().before(notification.getStartDateOfEffect())) {
            return false;
        }
        if (ownDataExchangeRequest.getDesiredEndDateTime().before(notification.getStartDateOfEffect())) {
            return false;
        }
        if (notification.getExpectedEndDateOfEffect() != null) {
            if (ownDataExchangeRequest.getDesiredStartDateTime().after(notification.getExpectedEndDateOfEffect())) {
                return false;
            }
            if (ownDataExchangeRequest.getDesiredEndDateTime().after(notification.getExpectedEndDateOfEffect())) {
                return false;
            }
        }
        return true;
    }
}
