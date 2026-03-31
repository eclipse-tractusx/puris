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

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.OwnDemandAndCapacityNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestSammMapper {
    @Autowired
    private OwnDemandAndCapacityNotificationService ownDemandAndCapacityNotificationService;

    public DataExchangeRequestSamm ownDataExchangeRequestToSamm(OwnDataExchangeRequest request) {
        var builder = DataExchangeRequestSamm.builder();

        return builder
                .id(request.getUuid())
                .sourceDisruptionId(request.getNotification().getSourceDisruptionId())
                .criticality(request.getCriticality())
                .desiredStartDateTime(request.getDesiredStartDateTime())
                .desiredEndDateTime(request.getDesiredEndDateTime())
                .requestedTypes(request.getRequestedTypes() != null ? new ArrayList<>(request.getRequestedTypes()): null)
                .text(request.getText())
                .timestamp(request.getTimestamp())
                .build();
    }

    public ReportedDataExchangeRequest sammToReportedDataExchangeRequest(String bpnl,DataExchangeRequestSamm samm) {
        var notification = ownDemandAndCapacityNotificationService.findByBpnlAndSourceDisruptionId(bpnl, samm.getSourceDisruptionId());
        if (notification == null) {
            log.error("No matching notification found for BPNL {} and source disruption ID {}", bpnl, samm.getSourceDisruptionId());
            return null;
        }
        log.info("Found matching notification with ID {} for BPNL {} and source disruption ID {}", samm.getId(), bpnl, samm.getSourceDisruptionId());
        var builder = ReportedDataExchangeRequest.builder();
        return builder
                .uuid(samm.getId())
                .notification(notification)
                .criticality(samm.getCriticality())
                .desiredStartDateTime(samm.getDesiredStartDateTime())
                .desiredEndDateTime(samm.getDesiredEndDateTime())
                .requestedTypes(samm.getRequestedTypes() != null ? new ArrayList<>(samm.getRequestedTypes()) : null)
                .text(samm.getText())
                .timestamp(samm.getTimestamp())
                .build();
    }
}
