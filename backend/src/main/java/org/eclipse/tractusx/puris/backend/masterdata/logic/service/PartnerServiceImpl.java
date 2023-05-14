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

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;


    @Override
    public Partner create(Partner partner) {
        return partnerRepository.save(partner);
    }

    @Override
    public Partner findByUuid(UUID partnerUuid) {
        Optional<Partner> foundPartner = partnerRepository.findById(partnerUuid);

        if (!foundPartner.isPresent()) {
            return null;
        }
        return foundPartner.get();
    }

    @Override
    public List<Partner> findAllCustomerPartnersForMaterialId(UUID materialUuid) {
        return partnerRepository.findAllByActsAsCustomerFlagIsTrueAndOrdersProducts_Uuid(materialUuid);
    }

    @Override
    public Partner update(Partner partner) {
        Optional<Partner> existingPartner =
                partnerRepository.findById(partner.getUuid());

        if (existingPartner.isPresent()) {
            return existingPartner.get();
        } else return null;
    }

    @Override
    public Partner findByBpnl(String bpnl) {
        Optional<Partner> existingPartner =
                partnerRepository.findFirstByBpnl(bpnl);

        if (existingPartner.isPresent()) {
            return existingPartner.get();
        } else return null;
    }

    @Override
    public Partner findByBpns(String bpns) {
        Optional<Partner> existingPartner =
                partnerRepository.findFirstBySiteBpns(bpns);

        if (existingPartner.isPresent()) {
            return existingPartner.get();
        } else return null;
    }
}
