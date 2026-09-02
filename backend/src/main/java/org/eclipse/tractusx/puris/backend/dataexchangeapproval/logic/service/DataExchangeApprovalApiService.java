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

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageContext;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter.DataExchangeApprovalSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsChainOpeningRootGrantRepository;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningPartnerGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningRootGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsJobService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
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
    private IrsChainOpeningRootGrantService irsChainOpeningRootGrantService;
    @Autowired
    private IrsChainOpeningPartnerGrantService irsChainOpeningGrantService;
    @Autowired
    private IrsChainOpeningRootGrantRepository irsChainOpeningRootGrantRepository;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private IrsJobService irsJobService;

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
        ReportedDataExchangeApproval existingApproval = null;
        existingApproval = reportedDataExchangeApprovalService.findByApprovalId(approval.getApprovalId());

        if (existingApproval != null) {
            log.info("Updating existing Approval");
            approval.setUuid(existingApproval.getUuid());
            if (reportedDataExchangeApprovalService.update(approval) == null) {
                log.error("Error updating Approval");
                return null;
            }
            if (approval.getDataExchangeRequest().getRelatedDataExchangeRequest() != null) {
                irsChainOpeningGrantService.onRelatedApprovalReceived(approval);
            } else if (approval.isFinalized()) {
                createJobsForRootGrants(approval.getDataExchangeRequest().getNotification());
            }
            return approval;
        }
        try {
            log.info("Creating new Approval");
            ReportedDataExchangeApproval created = reportedDataExchangeApprovalService.create(approval);
            if (created.getDataExchangeRequest().getRelatedDataExchangeRequest() == null) {
                // create root grants for the notification if the approval is for a root request
                irsChainOpeningRootGrantService.syncGrantsForNotification(created.getDataExchangeRequest().getNotification());
                if (created.isFinalized()) {
                    createJobsForRootGrants(created.getDataExchangeRequest().getNotification());
                }
            } else {
                // add the notification to the affected materials' parent materials' grants if the approval is for a related request
                irsChainOpeningGrantService.onRelatedApprovalReceived(created);
            }
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
            irsChainOpeningGrantService.createGrantsForApproval(approval);
            log.info("Successfully sent Data Exchange Approval to partner " + partner.getBpnl());
        } catch (Exception e) {
            log.error("Error in ReportedDataExchangeApproval for partner " + partner.getBpnl(), e);
        }
        
    }

    /**
     * Creates and sends a new IRS job for every root grant that has the given notification among
     * its reportedNotifications. A grant whose globalAssetId does not resolve to a known material,
     * or whose material is not eligible for an IRS job, is skipped and logged — it does not prevent
     * jobs from being created for the other grants.
     *
     * @param notification the notification whose root grants should each get a new IRS job
     */
    private void createJobsForRootGrants(ReportedDemandAndCapacityNotification notification) {
        String sourceDisruptionId = notification.getSourceDisruptionId().toString();
        List<IrsChainOpeningRootGrant> grants = irsChainOpeningRootGrantRepository.findAllByReportedNotifications_Uuid(notification.getUuid());

        for (IrsChainOpeningRootGrant grant : grants) {
            Material material = materialService.findByMaterialNumberCx(grant.getGlobalAssetId());
            if (material == null) {
                log.error("No material found for globalAssetId {} while creating IRS job for sourceDisruptionId {}",
                    grant.getGlobalAssetId(), sourceDisruptionId);
                continue;
            }

            IrsJob irsJob = new IrsJob();
            irsJob.setMaterial(material);
            irsJob.setSourceDisruptionId(grant.getSourceDisruptionId());

            try {
                irsJobService.createAndSend(irsJob);
            } catch (IllegalArgumentException e) {
                log.error("Failed to create IRS job for globalAssetId {}, sourceDisruptionId {}",
                    grant.getGlobalAssetId(), sourceDisruptionId, e);
            }
        }
    }

    private JsonNode createDataExchangeApprovalBody(OwnDataExchangeApproval approval) {
        var samm = sammMapper.ownDataExchangeApprovalToSamm(approval);
        return messageService.createMessage(approval.getDataExchangeRequest().getNotification().getPartner(), IndustryCoreMessageContext.DATA_EXCHANGE_APPROVAL_CONTEXT, samm);
    }
}
