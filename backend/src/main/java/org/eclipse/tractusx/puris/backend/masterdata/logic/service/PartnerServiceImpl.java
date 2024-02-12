/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
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

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private EdcAdapterService edcAdapterService;

    private final Pattern bpnlPattern = Pattern.compile("^BPNL[0-9a-zA-Z]{12}$");
    private final Pattern bpnsPattern = Pattern.compile("^BPNS[0-9a-zA-Z]{12}$");
    private final Pattern bpnaPattern = Pattern.compile("^BPNA[0-9a-zA-Z]{12}$");

    @Override
    public Partner create(Partner partner) {
        if (!testConstraints(partner)) {
            log.error("Could not create Partner " + partner.getBpnl() + " because of constraint violation");
            return null;
        }
        if (partner.getUuid() == null && partnerRepository.findFirstByBpnl(partner.getBpnl()).isEmpty()) {
            prepareApiAssetsForPartner(partner);
            return partnerRepository.save(partner);
        }
        log.error("Could not create Partner " + partner.getBpnl() + " because it already existed before");
        return null;
    }

    @Override
    public void prepareApiAssetsForPartner(Partner partner) {
        if (!variablesService.getOwnBpnl().equals(partner.getBpnl())) {
            executorService.submit(new RegistrationTask(partner));
        }
    }

    @AllArgsConstructor
    private class RegistrationTask implements Callable<Boolean> {
        private Partner partner;

        @Override
        public Boolean call() throws Exception {
            if (edcAdapterService.createPolicyAndContractDefForPartner(partner)) {
                log.info("Policy / ContractDef Registration successful for partner " + partner.getBpnl());
                return true;
            } else {
                log.warn("Policy / ContractDef Registration failed for partner " + partner.getBpnl());
                log.warn("Retrying in 3 seconds");
                Thread.sleep(3000);
                if (edcAdapterService.createPolicyAndContractDefForPartner(partner)) {
                    log.info("Retry successful");
                    return true;
                }
                log.warn("Retry failed");
                return false;
            }
        }
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
            if (site.getAddresses().isEmpty()) {
                validData = false;
                log.error("Site " + site.getBpns() + " has no addresses");
            }
        }
        for (var address : partner.getAddresses()) {
            boolean validBpna = bpnaPattern.matcher(address.getBpna()).matches();
            validData = validData && validBpna;
            addressCount++;
        }
        if (addressCount < 1) {
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
        return mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);
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
        if (existingPartner.isPresent() && existingPartner.get().getBpnl().equals(partner.getBpnl())) {
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
