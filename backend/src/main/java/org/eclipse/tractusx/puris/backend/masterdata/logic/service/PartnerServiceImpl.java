/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private VariablesService variablesService;

    private Pattern bpnlPattern = Pattern.compile("^BPNL[0-9]{10}[A-Z]{2}$");
    private Pattern bpnsPattern = Pattern.compile("^BPNS[0-9]{10}[A-Z]{2}$");
    private Pattern bpnaPattern = Pattern.compile("^BPNA[0-9]{10}[A-Z]{2}$");

    @Override
    public Partner create(Partner partner) {
        if (!testConstraints(partner)) {
            log.error("Could not create Partner " + partner.getBpnl() + " because of constraint violation");
            return null;
        }
        if (partner.getUuid() == null) {
            return partnerRepository.save(partner);
        }
        var searchResult = partnerRepository.findById(partner.getUuid());
        if (searchResult.isEmpty()) {
            if (partnerRepository.findFirstByBpnl(partner.getBpnl()).isEmpty()) {
                return partnerRepository.save(partner);
            }
        }
        log.error("Could not create Partner " + partner.getBpnl() + " because it already existed before");
        return null;
    }


    private boolean testConstraints(Partner partner) {
        // Each Partner needs a BPNL, a name, an edcUrl and a BPNA or a BPNS (containing a BPNA)
        boolean validData = bpnlPattern.matcher(partner.getBpnl()).matches();
        if (!validData) {
            log.error("Invalid BPNL: " + partner.getBpnl());
        }
        int addressCount = 0;
        try {
            new URL(partner.getEdcUrl()).toURI();
        } catch (Exception e) {
            validData = false;
            log.error("Invalid EDC URL: " + partner.getEdcUrl());
        }
        boolean nameExists = partner.getName() != null && !partner.getName().isEmpty();
        if (!nameExists) {
            log.error("Missing name of partner");
        }
        validData = validData && nameExists;
        for (var site : partner.getSites()) {
            boolean validBpns = bpnsPattern.matcher(site.getBpns()).matches();
            if (!validBpns) {
                log.error("Invalid BPNS: " + site.getBpns());
            }
            validData = validData && validBpns;
            for (var address : site.getAddresses()) {
                boolean validBpna = bpnaPattern.matcher(address.getBpna()).matches();
                if (!validBpna) {
                    log.error("Invalid BPNA: " + address.getBpna());
                }
                validData = validData && validBpna;
                addressCount++;
            }
        }
        for (var address : partner.getAddresses()) {
            boolean validBpna = bpnaPattern.matcher(address.getBpna()).matches();
            validData = validData && validBpna;
            addressCount++;
        }
        if (addressCount<1) {
            log.error("No BPNA given for Partner " + partner.getBpnl());
        }
        return validData && addressCount > 0;
    }

    @Override
    public Partner findByUuid(UUID partnerUuid) {
        return partnerRepository.findById(partnerUuid).orElse(null);
    }

    @Override
    public List<Partner> findAllCustomerPartnersForMaterialId(String ownMaterialNumber) {
        return mprService.findAllCustomersForOwnMaterialNumber(ownMaterialNumber);
    }

    @Override
    public List<Partner> findAllSupplierPartnersForMaterialId(String ownMaterialNumber) {
        var searchResult = materialRepository.findById(ownMaterialNumber);
        if (searchResult.isPresent()) {
            return mprService.findAllSuppliersForMaterial(searchResult.get());
        }
        return List.of();
    }

    @Override
    public List<Partner> findAll() {
        return partnerRepository.findAll();
    }

    @Override
    public Partner update(Partner partner) {
        if (!testConstraints(partner)) {
            log.error("Could not update Partner " + partner.getBpnl() + " because of constraint violation");
            return null;
        }

        Optional<Partner> existingPartner =
            partnerRepository.findById(partner.getUuid());
        if (existingPartner.isPresent()) {
            return partnerRepository.save(partner);
        }
        log.error("Could not update Partner " + partner.getBpnl() + " because it didn't exist before");
        return null;
    }

    @Override
    public Partner findByBpnl(String bpnl) {
        return partnerRepository.findFirstByBpnl(bpnl).orElse(null);
    }

    @Override
    public Partner findByBpns(String bpns) {
        return partnerRepository.findFirstBySites_Bpns(bpns).orElse(null);
    }

    @Override
    public Partner getOwnPartnerEntity() {
        return partnerRepository.findFirstByBpnl(variablesService.getOwnBpnl()).orElse(null);
    }
}
