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
import java.util.Date;
import java.util.List;

import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.ReportedDemandRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
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
        return validateWithDetails(demand).isEmpty();
    }

    public List<String> validateWithDetails(ReportedDemand demand) {
        List<String> errors = new ArrayList<>();
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();

        if (demand.getMaterial() == null) {
            errors.add("Missing Material.");
        }
        if (demand.getPartner() == null) {
            errors.add("Missing Partner.");
        }
        if (!mprService.partnerOrdersProduct(demand.getMaterial(), demand.getPartner())) {
            errors.add("Cannot order specified material from Partner.");
        }
        if (demand.getQuantity() <= 0) {
            errors.add("Quantity must be greater than 0.");
        }
        if (demand.getMeasurementUnit() == null) {
            errors.add("Missing measurement unit.");
        }
        if (demand.getLastUpdatedOnDateTime() == null) {
            errors.add("Missing lastUpdatedOnTime.");
        } else if (demand.getLastUpdatedOnDateTime().after(new Date())) {
            errors.add("lastUpdatedOnDateTime cannot be in the future.");
        }
        if (demand.getDay() == null) {
            errors.add("Missing day.");
        }
        if (demand.getDemandCategoryCode() == null) {
            errors.add("Missing demand category code.");
        }
        if (demand.getDemandLocationBpns() == null) {
            errors.add("Missing demand location BPNS.");
        }
        if (demand.getPartner().equals(ownPartnerEntity)) {
            errors.add("Partner cannot be the same entity.");
        }
        if ((demand.getSupplierLocationBpns() != null  && 
            ownPartnerEntity.getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getSupplierLocationBpns())))
            || demand.getPartner().getSites().stream().noneMatch(site -> site.getBpns().equals(demand.getDemandLocationBpns()))) {
            errors.add("Invalid demand: supplier or demand location is not valid.");
        }
        return errors;
    }
}
