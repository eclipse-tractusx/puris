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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter;

import java.util.ArrayList;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeApprovalSammMapper {
    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;

    public DataExchangeApprovalSamm ownDataExchangeApprovalToSamm (OwnDataExchangeApproval approval) {
        var builder = DataExchangeApprovalSamm.builder();

        return builder
            .approvalId(approval.getApprovalId())
            .dataExchangeRequestId(approval.getDataExchangeRequest().getRequestId())
            .timestamp(approval.getTimestamp())
            .approvedTypes(approval.getApprovedTypes() != null ? new ArrayList<>(approval.getApprovedTypes()) : null)
            .finalized(approval.isFinalized())
            .build();
    }

    public ReportedDataExchangeApproval sammToReportedDataExchangeApproval (String bpnl, DataExchangeApprovalSamm samm) {
        var dataExchangeRequest = ownDataExchangeRequestService.findByRequestId(samm.getDataExchangeRequestId());
        if (dataExchangeRequest == null) {
            log.error("No matching data exchange request found for ID {}", samm.getDataExchangeRequestId());
            return null;
        }
        return ReportedDataExchangeApproval.builder()
            .approvalId(samm.getApprovalId())
            .dataExchangeRequest(dataExchangeRequest)
            .timestamp(samm.getTimestamp())
            .approvedTypes(samm.getApprovedTypes() != null ? new ArrayList<>(samm.getApprovedTypes()) : null)
            .isFinalized(samm.isFinalized())
            .build();
    }
}
