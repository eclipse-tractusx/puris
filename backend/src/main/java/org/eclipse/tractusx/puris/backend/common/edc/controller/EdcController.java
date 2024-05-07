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
import okhttp3.Response;
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

    private static final String CATALOG = "catalog";
    private static final String ASSETS = "assets";
    private static final String CONTRACTNEGOTIATIONS = "contractnegotiations";
    private static final String TRANSFERS = "transfers";


    /**
     * Get the catalog from an EDC.
     *
     * @param dspUrl      url of the edc to get catalog from.
     * @param partnerBpnl bpnl of the partner to get the catalog from.
     * @return catalog of the requested edc.
     */
    @GetMapping(CATALOG)
    public ResponseEntity<String> getCatalog(@RequestParam String dspUrl, @RequestParam String partnerBpnl) {
        try {
            if (!PatternStore.URL_PATTERN.matcher(dspUrl).matches()) {
                return ResponseEntity.badRequest().build();
            }
            var catalogResponse = edcAdapter.getCatalogResponse(dspUrl, partnerBpnl, null);
            if (catalogResponse != null && catalogResponse.isSuccessful()) {
                var responseString = catalogResponse.body().string();
                catalogResponse.body().close();
                return ResponseEntity.ok(responseString);
            } else {
                if (catalogResponse != null) {
                    log.warn(statusCodeMessageGenerator(CATALOG, catalogResponse.code()));
                    if (catalogResponse.body() != null) {
                        catalogResponse.body().close();
                    }
                } else {
                    log.warn(noResponseMessageGenerator(CATALOG));
                }
                return ResponseEntity.badRequest().build();
            }
        } catch (IOException e) {
            log.error(exceptionMessageGenerator(CATALOG), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get assets from own EDC.
     *
     * @param assetId optional parameter if only a specific asset should be retrieved.
     * @return response from own EDC.
     */
    @GetMapping(ASSETS)
    public ResponseEntity<String> getAssets(@RequestParam String assetId) {
        try {
            if (!PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING.matches(assetId)) {
                return ResponseEntity.badRequest().build();
            }
            var assetsResponse = edcAdapter.sendGetRequest(List.of("v3", ASSETS, assetId));
            if (assetsResponse != null && assetsResponse.isSuccessful()) {
                var stringData = assetsResponse.body().string();
                assetsResponse.body().close();
                return ResponseEntity.ok(stringData);
            } else {
                if (assetsResponse != null) {
                    log.warn(statusCodeMessageGenerator(ASSETS, assetsResponse.code()));
                    if (assetsResponse.body() != null) {
                        assetsResponse.body().close();
                    }
                } else {
                    log.warn(noResponseMessageGenerator(ASSETS));
                }
                return ResponseEntity.badRequest().build();
            }
        } catch (IOException e) {
            log.error(exceptionMessageGenerator(ASSETS), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all contract negotiations in the history
     * of your control plane.
     *
     * @return contract negotiation data
     */
    @GetMapping(CONTRACTNEGOTIATIONS)
    public ResponseEntity<String> getContractNegotiations() {
        try {
            Response negotiationsResponse = edcAdapter.getAllNegotiations();
            if (negotiationsResponse != null && negotiationsResponse.isSuccessful()) {
                String responseString = negotiationsResponse.body().string();
                negotiationsResponse.body().close();
                return ResponseEntity.ok(responseString);
            } else {
                if (negotiationsResponse != null) {
                    log.warn(statusCodeMessageGenerator(CONTRACTNEGOTIATIONS, negotiationsResponse.code()));
                    if (negotiationsResponse.body() != null) {
                        negotiationsResponse.body().close();
                    }
                } else {
                    log.warn(noResponseMessageGenerator(CONTRACTNEGOTIATIONS));
                }
                return ResponseEntity.internalServerError().build();
            }

        } catch (Exception e) {
            log.error(exceptionMessageGenerator(CONTRACTNEGOTIATIONS), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all transfers in the history
     * of your control plane.
     *
     * @return transfer data
     */
    @GetMapping(TRANSFERS)
    public ResponseEntity<JsonNode> getTransfers() {
        try {
            Response transfersResponse = edcAdapter.getAllTransfers();
            if (transfersResponse != null && transfersResponse.isSuccessful()) {
                String data = transfersResponse.body().string();
                var responseObject = objectMapper.readTree(data);
                for (var item : responseObject) {
                    // The response from the control plane does not contain
                    // an edc:connectorId field, if your side was involved as PROVIDER
                    // in a transfer. Because we want to show the other party's
                    // BPNL in the frontend in any case, we retrieve the BPNL via
                    // the contractAgreement and insert it into the JSON data.
                    String myRole = item.get("type").asText();
                    if ("PROVIDER".equals(myRole)) {
                        String contractId = item.get("contractId").asText();
                        var contractObject = objectMapper.readTree(edcAdapter.getContractAgreement(contractId));
                        String partnerBpnl = contractObject.get("consumerId").asText();
                        ((ObjectNode) item).put("connectorId", partnerBpnl);
                    }
                }
                return ResponseEntity.ok(responseObject);
            } else {
                if (transfersResponse != null) {
                    log.warn(statusCodeMessageGenerator(TRANSFERS, transfersResponse.code()));
                    if (transfersResponse.body() != null) {
                        transfersResponse.body().close();
                    }
                }
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error(exceptionMessageGenerator(TRANSFERS), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String exceptionMessageGenerator(String endpointName) {
        return "Exception in " + endpointName + " endpoint ";
    }

    private String statusCodeMessageGenerator(String endpointName, int code) {
        return endpointName + " endpoint received status code " + code;
    }

    private String noResponseMessageGenerator(String endpointName) {
        return endpointName + "  endpoint received no response";
    }

}
