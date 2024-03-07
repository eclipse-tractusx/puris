/*
 * Copyright (c) 2022,2024 Volkswagen AG
 * Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controller for forwarding/building requests to a productEDC.
 */
@RestController
@RequestMapping("edc")
@Slf4j
public class EdcController {

    @Autowired
    private EdcAdapterService edcAdapter;
    @Autowired
    private ObjectMapper objectMapper;


    /**
     * Get the catalog from an EDC.
     *
     * @param dspUrl url of the edc to get catalog from.
     * @return catalog of the requested edc.
     */
    @GetMapping("/catalog")
    public ResponseEntity<String> getCatalog(@RequestParam String dspUrl) {
        try {
            if (!PatternStore.URL_PATTERN.matcher(dspUrl).matches()) {
                return ResponseEntity.badRequest().build();
            }
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
    public ResponseEntity<String> getAssets(@RequestParam String assetId) {
        try {
            if (!PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING.matches(assetId)) {
                return ResponseEntity.badRequest().build();
            }
            var result = edcAdapter.sendGetRequest(List.of("v3", "assets", assetId));
            var stringData = result.body().string();
            result.body().close();
            return ResponseEntity.ok(stringData);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all contract negotiations in the history
     * of your control plane.
     *
     * @return contract negotiation data
     */
    @GetMapping("/contractnegotiations")
    public ResponseEntity<String> getContractNegotiations() {
        try {
            String data = edcAdapter.getAllNegotiations();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all transfers in the history
     * of your control plane.
     *
     * @return transfer data
     */
    @GetMapping("/transfers")
    public ResponseEntity<JsonNode> getTransfers() {
        try {
            String data = edcAdapter.getAllTransfers();
            var responseObject = objectMapper.readTree(data);
            for (var item : responseObject) {
                // The response from the control plane does not contain
                // an edc:connectorId field, if your side was involved as PROVIDER
                // in a transfer. Because we want to show the other party's
                // BPNL in the frontend in any case, we retrieve the BPNL via
                // the contractAgreement and insert it into the JSON data.
                String myRole = item.get("edc:type").asText();
                if ("PROVIDER".equals(myRole)) {
                    String contractId = item.get("edc:contractId").asText();
                    var contractObject = objectMapper.readTree(edcAdapter.getContractAgreement(contractId));
                    String partnerBpnl = contractObject.get("edc:consumerId").asText();
                    ((ObjectNode) item).put("edc:connectorId", partnerBpnl);
                }
            }
            return ResponseEntity.ok(responseObject);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
