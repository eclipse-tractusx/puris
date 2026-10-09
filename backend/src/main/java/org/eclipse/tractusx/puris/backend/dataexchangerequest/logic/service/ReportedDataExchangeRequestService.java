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

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.ReportedDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class ReportedDataExchangeRequestService extends DataExchangeRequestService<ReportedDataExchangeRequest, ReportedDataExchangeRequestRepository> {

    public ReportedDataExchangeRequestService(ReportedDataExchangeRequestRepository repository, PartnerService partnerService,
            MaterialPartnerRelationService mprService) {
        super(repository, partnerService, mprService);
    }

    public final ReportedDataExchangeRequest create(ReportedDataExchangeRequest reportedDataExchangeRequest) {
        if (reportedDataExchangeRequest == null) {
            throw new IllegalArgumentException("Missing data exchange request.");
        }
        if (reportedDataExchangeRequest.getRequestId() == null) {
            reportedDataExchangeRequest.setRequestId(UUID.randomUUID().toString());
        }
        List<String> errors = validateWithDetails(reportedDataExchangeRequest);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid data exchange request: " + String.join(" ", errors));
        }
        if (repository.findByRequestId(reportedDataExchangeRequest.getRequestId()).isPresent()) {
            throw new KeyAlreadyExistsException(String.format("A reported data exchange request for request id '%s' already exists", reportedDataExchangeRequest.getRequestId()));
        }
        return repository.save(reportedDataExchangeRequest);
    }

    public final ReportedDataExchangeRequest update(ReportedDataExchangeRequest reportedDataExchangeRequest) {
        if (reportedDataExchangeRequest == null) {
            throw new IllegalArgumentException("Missing data exchange request.");
        }
        List<String> errors = validateWithDetails(reportedDataExchangeRequest);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + String.join(" ", errors));
        }
        if (reportedDataExchangeRequest.getUuid() == null || repository.findById(reportedDataExchangeRequest.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(reportedDataExchangeRequest);
    }
    
    @Override
    public List<String> validateWithDetails(ReportedDataExchangeRequest dataExchangeRequest) {
        if (dataExchangeRequest == null) {
            return List.of("Missing data exchange request.");
        }
        List<String> errors = new ArrayList<>();
        errors.addAll(basicValidation(dataExchangeRequest));
        errors.addAll(validateMaterials(dataExchangeRequest));
        errors.addAll(validateSites(dataExchangeRequest, dataExchangeRequest.getPartner(), partnerService.getOwnPartnerEntity()));
        OwnDemandAndCapacityNotification notification = dataExchangeRequest.getNotification();
        if (notification != null) {
            errors.addAll(validateDesiredDates(dataExchangeRequest, notification.getStartDateOfEffect(), notification.getExpectedEndDateOfEffect()));
        }
        return errors;
    }
}
