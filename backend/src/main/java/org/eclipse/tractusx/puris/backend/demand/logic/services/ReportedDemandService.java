/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)

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
package org.eclipse.tractusx.puris.backend.demand.logic.services;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.ReportedDemandRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class ReportedDemandService extends DemandService<ReportedDemand, ReportedDemandRepository> {

    public ReportedDemandService(ReportedDemandRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        super(repository, partnerService, mprService);
    }

    @Override
    public boolean validate(ReportedDemand demand) {
        return basicValidation(demand).isEmpty() && validateReportedDemand(demand).isEmpty();
    }

    public List<String> validateWithDetails(ReportedDemand demand) {
        List<String> validationErrors = new ArrayList<>();
        validationErrors.addAll(basicValidation(demand));
        validationErrors.addAll(validateReportedDemand(demand));
        return validationErrors;
    }
}
