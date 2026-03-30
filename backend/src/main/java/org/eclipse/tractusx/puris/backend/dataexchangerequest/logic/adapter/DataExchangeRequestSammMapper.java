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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.adapter;

import java.util.ArrayList;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.OwnDataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.ReportedDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataExchangeRequestSammMapper {
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;

    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;

    public OwnDataExchangeRequestSamm ownDataExchangeRequestToSamm(OwnDataExchangeRequest request) {
        var builder = OwnDataExchangeRequestSamm.builder();

        return builder
                .notificationId(request.getNotification().getNotificationId())
                .criticality(request.getCriticality())
                .desiredStartDateTime(request.getDesiredStartDateTime())
                .desiredEndDateTime(request.getDesiredEndDateTime())
                .requestedTypes(request.getRequestedTypes() != null ? new ArrayList<>(request.getRequestedTypes()): null)
                .text(request.getText())
                .timestamp(request.getTimestamp())
                .relatedDataExchangeRequestId(request.getRelatedDataExchangeRequest() != null ? request.getRelatedDataExchangeRequest().getUuid().toString() : null)
                .build();
    }

    public ReportedDataExchangeRequest sammToReportedDataExchangeRequest(DataExchangeRequestSamm samm) {
        var notification = reportedDemandAndCapacityNotificationService.findByNotificationId(samm.getNotificationId());

        if (notification == null) {
            throw new IllegalArgumentException("Referenced notification does not exist: " + samm.getNotificationId());
        }

        var builder = ReportedDataExchangeRequest.builder();
        return builder
                .notification(notification)
                .criticality(samm.getCriticality())
                .desiredStartDateTime(samm.getDesiredStartDateTime())
                .desiredEndDateTime(samm.getDesiredEndDateTime())
                .requestedTypes(samm.getRequestedTypes() != null ? new ArrayList<>(samm.getRequestedTypes()) : null)
                .text(samm.getText())
                .build();
    }

    public ReportedDataExchangeRequest findRelatedReportedDataExchangeRequest(OwnDataExchangeRequestSamm samm) {
        if (samm.getRelatedDataExchangeRequestId() == null) {
            return null;
        }
        return reportedDataExchangeRequestService.findById(UUID.fromString(samm.getRelatedDataExchangeRequestId()));
    }
}
