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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.DataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;

public abstract class  DataExchangeRequestService<TEntity extends DataExchangeRequest, TRepository extends DataExchangeRequestRepository<TEntity>> {
    protected final TRepository repository;
    protected final PartnerService partnerService;
    protected final MaterialPartnerRelationService mprService;

    public DataExchangeRequestService(TRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.mprService = mprService;
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final TEntity findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).orElse(null);
    }

    public boolean validate(TEntity request) {
        return validateWithDetails(request).isEmpty();
    }

    public abstract List<String> validateWithDetails(TEntity request);

    protected List<String> basicValidation(DataExchangeRequest dataExchangeRequest) {
        List<String> errors = new ArrayList<>();
        if (dataExchangeRequest.getPartner() == null) {
            errors.add("Missing partner.");
        }
        if (dataExchangeRequest.getSourceDisruptionId() == null) {
            errors.add("Missing sourceDisruptionId.");
        }
        if (dataExchangeRequest.getLeadingRootCause() == null) {
            errors.add("Missing leadingRootCause.");
        }
        if (dataExchangeRequest.getEffect() == null) {
            errors.add("Missing effect.");
        }
        if (dataExchangeRequest.getMaterials() == null || dataExchangeRequest.getMaterials().isEmpty()) {
            errors.add("At least one affected material is required.");
        }
        if (dataExchangeRequest.getCriticality() == null) {
            errors.add("Missing criticality.");
        }
        if (dataExchangeRequest.getText() == null || dataExchangeRequest.getText().isBlank()) {
            errors.add("Missing text.");
        }
        if (dataExchangeRequest.getDesiredStartDateTime() == null) {
            errors.add("Missing desiredStartDateTime.");
        }
        if (dataExchangeRequest.getDesiredEndDateTime() == null) {
            errors.add("Missing desiredEndDateTime.");
        }
        if (dataExchangeRequest.getUuid() != null && dataExchangeRequest.getTimestamp() == null) {
            errors.add("timestamp must be set when uuid is present.");
        }
        if (dataExchangeRequest.getRequestedTypes() == null) {
            errors.add("Missing requestedTypes.");
        } else if (dataExchangeRequest.getRequestedTypes().isEmpty()) {
            errors.add("requestedTypes must not be empty.");
        }
        return errors;
    }

    /**
     * The desired window must be valid. If the request is linked to a notification, the requests dates must be within the notification's window. If the request is not linked to a notification, the dates are only checked for validity.
     */
    protected List<String> validateDesiredDates(DataExchangeRequest dataExchangeRequest, Date startDateOfEffect, Date expectedEndDateOfEffect) {
        List<String> errors = new ArrayList<>();
        Date desiredStart = dataExchangeRequest.getDesiredStartDateTime();
        Date desiredEnd = dataExchangeRequest.getDesiredEndDateTime();
        if (desiredStart == null || desiredEnd == null) {
            return errors;
        }
        if (!desiredStart.before(desiredEnd)) {
            errors.add("desiredStartDateTime must be before desiredEndDateTime.");
        }
        if (startDateOfEffect != null) {
            if (desiredStart.before(startDateOfEffect)) {
                errors.add("desiredStartDateTime must not be before notification startDateOfEffect.");
            }
            if (desiredEnd.before(startDateOfEffect)) {
                errors.add("desiredEndDateTime must not be before notification startDateOfEffect.");
            }
        }
        if (expectedEndDateOfEffect != null) {
            if (desiredStart.after(expectedEndDateOfEffect)) {
                errors.add("desiredStartDateTime must not be after notification expectedEndDateOfEffect.");
            }
            if (desiredEnd.after(expectedEndDateOfEffect)) {
                errors.add("desiredEndDateTime must not be after notification expectedEndDateOfEffect.");
            }
        }
        return errors;
    }

    protected List<String> validateMaterials(DataExchangeRequest dataExchangeRequest) {
        if (dataExchangeRequest.getMaterials() == null || dataExchangeRequest.getPartner() == null) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        for (Material material : dataExchangeRequest.getMaterials()) {
            if (material == null) {
                errors.add("Affected materials must not contain null.");
                continue;
            }
            if (mprService.find(material, dataExchangeRequest.getPartner()) == null) {
                errors.add(String.format("Material %s is not related to partner %s.",
                    material.getOwnMaterialNumber(), dataExchangeRequest.getPartner().getBpnl()));
            }
        }
        return errors;
    }

    protected List<String> validateSites(DataExchangeRequest dataExchangeRequest, Partner sender, Partner recipient) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateSitesBelongTo("affectedSitesSender", dataExchangeRequest.getAffectedSitesSender(), sender));
        errors.addAll(validateSitesBelongTo("affectedSitesRecipient", dataExchangeRequest.getAffectedSitesRecipient(), recipient));
        return errors;
    }

    private static List<String> validateSitesBelongTo(String field, List<Site> sites, Partner owner) {
        if (sites == null || sites.isEmpty() || owner == null) {
            return List.of();
        }
        Set<String> ownerBpns = owner.getSites() == null ? Set.of() : owner.getSites().stream().map(Site::getBpns).collect(Collectors.toSet());
        List<String> errors = new ArrayList<>();
        for (Site site : sites) {
            if (site == null || !ownerBpns.contains(site.getBpns())) {
                errors.add(String.format("%s contains site %s that does not belong to partner %s.", field, site == null ? null : site.getBpns(), owner.getBpnl()));
            }
        }
        return errors;
    }
}
