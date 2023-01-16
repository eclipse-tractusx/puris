/*
 * Copyright (c) 2022,2023 Volkswagen AG
 * Copyright (c) 2022,2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.service;

import org.eclipse.tractusx.puris.backend.model.ExternalConnector;
import org.eclipse.tractusx.puris.backend.model.repo.ExternalConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service Class, managing external connectors.
 */
@Service
public class ExternalConnectorService {

    private final ExternalConnectorRepository externalConnectorRepository;

    private final EdcAdapter edcAdapter;

    @Autowired
    public ExternalConnectorService(ExternalConnectorRepository externalConnectorRepository, EdcAdapter edcAdapter) {
        this.externalConnectorRepository = externalConnectorRepository;
        this.edcAdapter = edcAdapter;
    }

    /**
     * Save a new external connector.
     *
     * @param externalConnector external connector to be persisted.
     * @return true, iff url of connector is valid and persisting it succeeded.
     */
    public boolean persistConnector(ExternalConnector externalConnector) {
        if (checkUrl(externalConnector.getUrl())) {
            externalConnectorRepository.saveAndFlush(externalConnector);
            return true;
        }
        return false;
    }

    /**
     * Get a list of all external connectors.
     *
     * @return list of all external connectors.
     */
    public List<ExternalConnector> getAll() {
        return externalConnectorRepository.findAll();
    }

    /**
     * Verify that url can be used to get the catalog of an EDC.
     *
     * @param url url to verify.
     * @return true, if url can be used to get the catalog of an EDC.
     */
    private boolean checkUrl(String url) {
        try {
            edcAdapter.getCatalog(url);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

}
