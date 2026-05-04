/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.adapter.DemandAndCapacityNotificationSammMapper;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.DemandAndCapacityNotificationSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemandAndCapacityNotifcationRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;
    @Autowired
    private MessageHeaderService messageHeaderService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DemandAndCapacityNotificationSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public static final String DEMAND_AND_CAPACITY_NOTIFICATION_CONTEXT = "CX-DemandAndCapacityNotificationAPI-Receive:2.0.0";

    public ReportedDemandAndCapacityNotification handleIncomingNotification(String bpnl, DemandAndCapacityNotificationSamm samm) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        var notification = sammMapper.sammToReportedDemandAndCapacityNotification(samm, partner);
        var existingNotification = reportedDemandAndCapacityNotificationService.findByNotificationId(notification.getNotificationId());
        if (existingNotification != null) {
            log.info("Updating existing Notification");
            notification.setUuid(existingNotification.getUuid());
            if (reportedDemandAndCapacityNotificationService.update(notification) == null) {
                log.error("Error updating Notification");
                return null;
            }
            return notification;
        }
        try {
            log.info("Creating new Notification");
            return reportedDemandAndCapacityNotificationService.create(notification);
        } catch (KeyAlreadyExistsException e) {
            log.error("Notification already exists", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Notification: {}", notification.toString());
            return null;
        }
    }

    public void sendDemandAndCapacityNotification(OwnDemandAndCapacityNotification notification){
        var partner = notification.getPartner();
        var body = createNotificationRequestBody(notification);
        try {
            edcAdapterService.doNotificationPostRequest(partner, body);
            log.info("Successfully sent Notification to partner " + partner.getBpnl()); 
        } catch (Exception e) {
            log.error("Error in ReportedNotificationRequest for partner " + partner.getBpnl(), e);
        }
    }

    private JsonNode createNotificationRequestBody(OwnDemandAndCapacityNotification notification) {
        var body = objectMapper.createObjectNode();
        body.set("header", messageHeaderService.createHeader(notification.getPartner(), DEMAND_AND_CAPACITY_NOTIFICATION_CONTEXT));
        var content = objectMapper.createObjectNode();
        body.set("content", content);
        var samm = sammMapper.ownNotificationToSamm(notification);
        content.set("demandAndCapacityNotification", objectMapper.convertValue(samm, JsonNode.class));
        return body;
    }
}
