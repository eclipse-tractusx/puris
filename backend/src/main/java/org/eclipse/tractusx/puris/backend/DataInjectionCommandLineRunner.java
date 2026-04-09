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
package org.eclipse.tractusx.puris.backend;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInjectionCommandLineRunner implements CommandLineRunner {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private VariablesService variablesService;

    @Override
    public void run(String... args) {
        createOwnPartnerEntity();
    }

    /**
     * Generates and persists a Partner entity that holds all
     * relevant data about the owner of the running instance of
     * the PURIS application.
     */
    void createOwnPartnerEntity() {

        Partner mySelf = partnerService.findByBpnl(variablesService.getOwnBpnl());
        if (mySelf != null){
            log.info("Following partner has been configured for yourself (and not been changed): {}", mySelf);
            return;
        }
        if (variablesService.getOwnDefaultBpns() != null && variablesService.getOwnDefaultBpns().length() != 0) {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getEdcProtocolUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpns(),
                variablesService.getOwnDefaultSiteName(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry());
        } else {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getEdcProtocolUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry()
            );
        }
        mySelf = partnerService.create(mySelf);
        log.info("Successfully created own Partner Entity: {}", mySelf);
    }
}
