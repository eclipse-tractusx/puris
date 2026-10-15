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

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class OwnDataExchangeRequestService extends DataExchangeRequestService<OwnDataExchangeRequest, OwnDataExchangeRequestRepository> {

    public OwnDataExchangeRequestService(OwnDataExchangeRequestRepository repository, PartnerService partnerService,
            MaterialPartnerRelationService mprService) {
        super(repository, partnerService, mprService);
    }

    public final List<OwnDataExchangeRequest> findByRelatedDataExchangeRequest(ReportedDataExchangeRequest origin) {
        return repository.findByRelatedDataExchangeRequest_Uuid(origin.getUuid());
    }

    public final OwnDataExchangeRequest create(OwnDataExchangeRequest ownDataExchangeRequest) {
        if (ownDataExchangeRequest == null) {
            throw new IllegalArgumentException("Missing data exchange request.");
        }
        if (ownDataExchangeRequest.getRequestId() == null) {
            ownDataExchangeRequest.setRequestId(UUID.randomUUID().toString());
        }
        List<String> errors = validateWithDetails(ownDataExchangeRequest);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid data exchange request: " + String.join(" ", errors));
        }
        if (repository.findByRequestId(ownDataExchangeRequest.getRequestId()).isPresent()) {
            throw new KeyAlreadyExistsException("Data exchange request already exists");
        }
        return repository.save(ownDataExchangeRequest);
    }

    @Override
    public List<String> validateWithDetails(OwnDataExchangeRequest dataExchangeRequest) {
        if (dataExchangeRequest == null) {
            return List.of("Missing data exchange request.");
        }
        List<String> errors = new ArrayList<>();
        errors.addAll(basicValidation(dataExchangeRequest));
        errors.addAll(validateMaterials(dataExchangeRequest));
        errors.addAll(validateSites(dataExchangeRequest, partnerService.getOwnPartnerEntity(), dataExchangeRequest.getPartner()));
        ReportedDemandAndCapacityNotification notification = dataExchangeRequest.getNotification();
        if (notification != null) {
            errors.addAll(validateDesiredDates(dataExchangeRequest, notification.getStartDateOfEffect(), notification.getExpectedEndDateOfEffect()));
        }
        return errors;
    }
}
