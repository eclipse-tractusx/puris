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
package org.eclipse.tractusx.puris.backend.common.edc.controller;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller for forwarding/building requests to a productEDC.
 */
@RestController
@RequestMapping("edc")
@Slf4j
public class EdcController {

    @Autowired private EdcAdapterService edcAdapter;


    /**
     * Get the catalog from an EDC.
     *
     * @param dspUrl url of the edc to get catalog from.
     * @return catalog of the requested edc.
     */
    @GetMapping("/catalog")
    @CrossOrigin
    public ResponseEntity<String> getEDCCatalog(@RequestParam String dspUrl) {
        try {
            var catalog = edcAdapter.getCatalog(dspUrl);
            return ResponseEntity.ok(catalog.toPrettyString());
        } catch (IOException e) {
            log.warn(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get assets from own EDC.
     *
     * @param assetId optional parameter if only a specific asset should be retrieved.
     * @return response from own EDC.
     */
    @GetMapping("/assets")
    @CrossOrigin
    public ResponseEntity<String> getAssets(@RequestParam String assetId) {
        try {
            var result = edcAdapter.sendGetRequest(List.of("v3", "assets", assetId));
            var stringData = result.body().string();
            result.body().close();
            return ResponseEntity.ok(stringData);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
