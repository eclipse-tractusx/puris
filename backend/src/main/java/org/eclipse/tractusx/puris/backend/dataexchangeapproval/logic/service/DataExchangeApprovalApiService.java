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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageContext;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter.DataExchangeApprovalSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeApprovalApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;
    @Autowired
    private IndustryCoreMessageService messageService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DataExchangeApprovalSammMapper sammMapper;
    @Autowired
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;
    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;
    @Autowired
    private ExecutorService executorService;

    public ReportedDataExchangeApproval handleIncomingDataExchangeApproval(String bpnl, DataExchangeApprovalSamm samm) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        var approval = sammMapper.sammToReportedDataExchangeApproval(samm);
        if (approval == null) {
            log.error("Error mapping incoming Approval");
            return null;
        }

        OwnDataExchangeRequest ownRequest = approval.getDataExchangeRequest();
        if (!partner.getBpnl().equals(ownRequest.getNotification().getPartner().getBpnl())) {
            log.error("Partner {} is not the recipient of request {}", bpnl, ownRequest.getRequestId());
            return null;
        }

        ReportedDataExchangeApproval existingApproval = null;
        existingApproval = reportedDataExchangeApprovalService.findByApprovalId(approval.getApprovalId());

        if (existingApproval != null) {
            log.info("Updating existing Approval");
            approval.setUuid(existingApproval.getUuid());
            if (reportedDataExchangeApprovalService.update(approval) == null) {
                log.error("Error updating Approval");
                return null;
            }
            finalizeOriginApprovalIfComplete(approval);
            return approval;
        }
        try {
            log.info("Creating new Approval");
            ReportedDataExchangeApproval created = reportedDataExchangeApprovalService.create(approval);
            finalizeOriginApprovalIfComplete(created);
            return created;
        } catch (KeyAlreadyExistsException e) {
            log.error("Approval already exists", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Approval: {}", approval.toString());
            return null;
        }
    }

    public void sendDataExchangeApproval(OwnDataExchangeApproval approval, Partner partner) {
        var body = createDataExchangeApprovalBody(approval);
        try {
            edcAdapterService.doDataExchangeApprovalPostRequest(partner, body);
            log.info("Successfully sent Data Exchange Approval to partner " + partner.getBpnl()); 
        } catch (Exception e) {
            log.error("Error in ReportedDataExchangeApproval for partner " + partner.getBpnl(), e);
        }
    }

    private void finalizeOriginApprovalIfComplete(ReportedDataExchangeApproval incoming) {
        ReportedDataExchangeRequest origin = incoming.getDataExchangeRequest().getRelatedDataExchangeRequest();
        if (origin == null) {
            return;
        }
        OwnDataExchangeApproval originApproval = ownDataExchangeApprovalService.findByDataExchangeRequest_Uuid(origin.getUuid());
        if (originApproval == null) {
            log.warn("No own approval found for origin request {}", origin.getRequestId());
            return;
        }
        if (originApproval.isFinalized()) {
            return;
        }

        List<OwnDataExchangeRequest> siblings = ownDataExchangeRequestService.findByRelatedDataExchangeRequest(origin);
        if (siblings.isEmpty()) {
            log.warn("Origin request {} has an unfinalized approval but no forwarded requests", origin.getRequestId());
            return;
        }

        boolean allApproved = siblings.stream().allMatch(r -> reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(r.getUuid()) != null);
        if (!allApproved) {
            return;
        }

        originApproval.setFinalized(true);
        if (ownDataExchangeApprovalService.update(originApproval) == null) {
            log.error("Failed to finalize approval {}", originApproval.getApprovalId());
            return;
        }
        Partner customer = origin.getNotification().getPartner();
        executorService.submit(() -> sendDataExchangeApproval(originApproval, customer));
    }

    private JsonNode createDataExchangeApprovalBody(OwnDataExchangeApproval approval) {
        var samm = sammMapper.ownDataExchangeApprovalToSamm(approval);
        return messageService.createMessage(approval.getDataExchangeRequest().getNotification().getPartner(), IndustryCoreMessageContext.DATA_EXCHANGE_APPROVAL_CONTEXT, samm);
    }
}
