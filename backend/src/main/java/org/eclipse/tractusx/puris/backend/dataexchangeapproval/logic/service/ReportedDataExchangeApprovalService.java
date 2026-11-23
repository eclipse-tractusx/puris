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
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.ReportedDataExchangeApprovalRepository;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdateAction;
import org.eclipse.tractusx.puris.backend.common.liveupdate.LiveUpdatePublisher;
import org.springframework.stereotype.Service;

@Service
public class ReportedDataExchangeApprovalService extends DataExchangeApprovalService<ReportedDataExchangeApproval, ReportedDataExchangeApprovalRepository>{
    
    public ReportedDataExchangeApprovalService(ReportedDataExchangeApprovalRepository repository, LiveUpdatePublisher liveUpdatePublisher) {
        super(repository, liveUpdatePublisher);
    }

    public final List<ReportedDataExchangeApproval> findAllByDataExchangeRequest_RelatedDataExchangeRequest_Uuid(UUID relatedDataExchangeRequestUuid) {
        return repository.findAllByDataExchangeRequest_RelatedDataExchangeRequest_Uuid(relatedDataExchangeRequestUuid);
    }

    public final ReportedDataExchangeApproval create(ReportedDataExchangeApproval reportedDataExchangeApproval) {
        if (reportedDataExchangeApproval == null || !validator.apply(reportedDataExchangeApproval)) {  
            throw new IllegalArgumentException("Invalid reported data exchange approval");
        }
        UUID requestUuid = reportedDataExchangeApproval.getDataExchangeRequest().getUuid();
        if (repository.findByDataExchangeRequest_Uuid(requestUuid).isPresent()) {
            throw new KeyAlreadyExistsException("Reported Data exchange approval already exists for data exchange request with uuid: " + requestUuid);
        }
        if (repository.findByApprovalId(reportedDataExchangeApproval.getApprovalId()).isPresent()) {
            throw new KeyAlreadyExistsException(String.format("A reported data exchange aproval for approval id %s' already exists", reportedDataExchangeApproval.getApprovalId()));
        }
        if (reportedDataExchangeApproval.getApprovalId() == null) {
            reportedDataExchangeApproval.setApprovalId(UUID.randomUUID().toString());
        } else if (repository.findByApprovalId(reportedDataExchangeApproval.getApprovalId()).isPresent()) {
            throw new KeyAlreadyExistsException(String.format("A reported data exchange aproval for approval id %s' already exists", reportedDataExchangeApproval.getApprovalId()));
        }

        return saveAndPublish(reportedDataExchangeApproval, LiveUpdateAction.CREATED);
    }

    public final ReportedDataExchangeApproval update(ReportedDataExchangeApproval reportedDataExchangeApproval) {
        if (!validator.apply(reportedDataExchangeApproval)) {
            throw new IllegalArgumentException("Invalid request");
        }
        if (reportedDataExchangeApproval.getUuid() == null || repository.findById(reportedDataExchangeApproval.getUuid()).isEmpty()) {
            return null;
        }
        return saveAndPublish(reportedDataExchangeApproval, LiveUpdateAction.UPDATED);
    }

    @Override
    public boolean validate(ReportedDataExchangeApproval dataExchangeApproval) {
        return dataExchangeApproval != null &&
        basicValidation(dataExchangeApproval) &&
        dataExchangeApproval.getDataExchangeRequest() != null &&
        dataExchangeApproval.getDataExchangeRequest().getUuid() != null;
    }
}
