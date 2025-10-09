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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.OwnDemandRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OwnDemandService extends DemandService<OwnDemand, OwnDemandRepository> {
    public OwnDemandService(OwnDemandRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        super(repository, partnerService, mprService);
    }

    public final List<Double> getQuantityForDays(String material, Optional<String> partnerBpnl, Optional<String> siteBpns, int numberOfDays) {
        List<Double> quantities = new ArrayList<>();
        LocalDate localDate = LocalDate.now();
        List<OwnDemand> demands = findAllByFilters(Optional.of(material), partnerBpnl, siteBpns);
        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            LocalDate localDayDate = Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toLocalDate();
            List<OwnDemand> demandsForDate = demands.stream().filter(demand -> {
                LocalDate demandDayDate = Instant.ofEpochMilli(demand.getDay().getTime()).atOffset(ZoneOffset.UTC).toLocalDate();
                return demandDayDate.getDayOfMonth() == localDayDate.getDayOfMonth() && demandDayDate.getMonthValue() == localDayDate.getMonthValue();
            }).toList();
            double demandQuantity = getSumOfQuantities(demandsForDate);
            quantities.add(demandQuantity);

            localDate = localDate.plusDays(1);
        }
        return quantities;
    }

    @Override
    public boolean validate(OwnDemand demand) {
        return true;
    }

    public List<String> validateWithDetails(OwnDemand demand) {
        List<String> validationErrors = new ArrayList<>();
        validationErrors.addAll(basicValidation(demand));
        validationErrors.addAll(validateOwnDemand(demand));
        return validationErrors;
    }

    private final double getSumOfQuantities(List<OwnDemand> demands) {
        double sum = 0;
        for (OwnDemand demand : demands) {
            sum += demand.getQuantity();
        }
        return sum;
    }
}
