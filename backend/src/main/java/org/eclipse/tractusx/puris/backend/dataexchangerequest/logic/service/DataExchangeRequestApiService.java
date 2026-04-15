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
import java.util.Date;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.adapter.DataExchangeRequestSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DataExchangeRequestSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public static final String DATA_EXCHANGE_REQUEST_CONTEXT = "CX-DataExchangeRequestReceiveAPI-Receive:1.0.0";
    public static final String MESSAGE_HEADER_VERSION = "3.0.0";

    public ReportedDataExchangeRequest handleIncomingDataExchangeRequest(String bpnl, DataExchangeRequestSamm samm) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        var request = sammMapper.sammToReportedDataExchangeRequest(bpnl, samm);
        if (request == null) {
            log.error("Error mapping incoming Request");
            return null;
        }
        ReportedDataExchangeRequest existingRequest = null;
        existingRequest = reportedDataExchangeRequestService.findByRequestId(request.getRequestId());

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

    public void sendDataExchangeRequest(OwnDataExchangeRequest request, Partner partner) {
        var body = createDataExchangeRequestBody(request);
        try {
            edcAdapterService.doDataExchangePostRequest(partner, body);
            log.info("Successfully sent Data Exchange Request to partner " + partner.getBpnl()); 
        } catch (Exception e) {
            log.error("Error in ReportedDataExchangeRequest for partner " + partner.getBpnl(), e);
        }
    }

    private JsonNode createDataExchangeRequestBody(OwnDataExchangeRequest request) {
        var ownPartnerEntity = partnerService.getOwnPartnerEntity();
        var body = objectMapper.createObjectNode();
        var header = objectMapper.createObjectNode();
        body.set("header", header);
        header.put("senderBpn", ownPartnerEntity.getBpnl());
        header.put("receiverBpn", request.getNotification().getPartner().getBpnl());
        header.put("context", DATA_EXCHANGE_REQUEST_CONTEXT);
        header.put("messageId", UUID.randomUUID().toString());
        header.put("sentDateTime", new Date().toString());
        header.put("version", MESSAGE_HEADER_VERSION);
        var samm = sammMapper.ownDataExchangeRequestToSamm(request);
        body.set("content", objectMapper.convertValue(samm, JsonNode.class));
        return body;
    }
    
}
