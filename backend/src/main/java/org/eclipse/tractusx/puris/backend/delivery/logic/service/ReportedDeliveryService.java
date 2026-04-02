/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.delivery.logic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.ReportedDeliveryRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class ReportedDeliveryService extends DeliveryService<ReportedDelivery> {
    private final ReportedDeliveryRepository repository;

    private final PartnerService partnerService;

    protected final Function<ReportedDelivery, Boolean> validator;

    public ReportedDeliveryService(ReportedDeliveryRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final List<ReportedDelivery> findAllByReportedId(UUID reportedId) {
        return repository.findAll().stream().filter(delivery -> delivery.getPartner().getUuid().equals(reportedId))
            .toList();
    }

    public final ReportedDelivery create(ReportedDelivery delivery) {
        if (delivery.getUuid() != null && repository.findById(delivery.getUuid()).isPresent()) {
            return null;
        }
        if (!validator.apply(delivery)) {
            return null;
        }
        return repository.save(delivery);
    }

    public final List<ReportedDelivery> createAll(List<ReportedDelivery> deliveries) {
        if (deliveries.stream().anyMatch(delivery -> !validator.apply(delivery))) {
            return null;
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> deliveries.stream().anyMatch(delivery -> delivery.equals(existing)))) {
            return null;
        }
        return repository.saveAll(deliveries);
    }

    public boolean validate(ReportedDelivery delivery) {
        return validateWithDetails(delivery).isEmpty();
    }

    public List<String> validateWithDetails(ReportedDelivery delivery) {
        List<String> validationErrors = new ArrayList<>();
        validationErrors.addAll(basicValidation(delivery));
        validationErrors.addAll(validateReportedResponsibility(delivery));
        return validationErrors;
    }
}
