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

import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.adapter.DataExchangeRequestSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestApiService {
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;
    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;
    @Autowired
    private DataExchangeRequestSammMapper sammMapper;

    public ReportedDataExchangeRequest handleIncomingDataExchangeRequest(UUID notificationId, DataExchangeRequestSamm samm) {
        ReportedDemandAndCapacityNotification notification =
                reportedDemandAndCapacityNotificationService.findByNotificationId(notificationId);
        if (notification == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        var request = sammMapper.sammToReportedDataExchangeRequest(samm);
        var existingRequest = reportedDataExchangeRequestService.findById(request.getUuid());
        if (existingRequest != null) {
            log.info("Updating existing Request");
            request.setUuid(existingRequest.getUuid());
            if (reportedDataExchangeRequestService.update(request) == null) {
                log.error("Error updating Request");
                return null;
            }
            return request;
        }
        try {
            log.info("Creating new Request");
            return reportedDataExchangeRequestService.create(request);
        } catch (KeyAlreadyExistsException e) {
            log.error("Request already exists", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Request: {}", request.toString());
            return null;
        }
    }
    
}
