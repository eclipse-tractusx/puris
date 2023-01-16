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
package org.eclipse.tractusx.puris.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.model.ExternalConnector;
import org.eclipse.tractusx.puris.backend.service.ExternalConnectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing {@link ExternalConnector} entities. These will be
 * used by the frontend to implement a dropdown selector when requesting catalogs from
 * other EDCs.
 */
@RestController
@RequestMapping("externalConnector")
public class ExternalConnectorController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExternalConnectorService externalConnectorService;

    @Autowired
    public ExternalConnectorController(ExternalConnectorService externalConnectorService) {
        this.externalConnectorService = externalConnectorService;
    }

    /**
     * Create an external connector entity.
     *
     * @param content JSON string representing an external connector.
     * @return the created connector, if it was created.
     */
    @PostMapping("create")
    @CrossOrigin
    public ResponseEntity<ExternalConnector> createConnector(@RequestBody String content){
        try {
            var connector = MAPPER.readValue(content, ExternalConnector.class);
            if (externalConnectorService.persistConnector(connector)) {
                return ResponseEntity.ok(connector);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get a list of all created external connectors.
     *
     * @return list of all currently known external connectors.
     */
    @GetMapping("all")
    @CrossOrigin
    public List<ExternalConnector> getAll() {
        return externalConnectorService.getAll();
    }
}
