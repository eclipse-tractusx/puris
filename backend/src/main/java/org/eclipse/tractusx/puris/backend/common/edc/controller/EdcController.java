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

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for forwarding/building requests to a productEDC.
 */
@RestController
@RequestMapping("edc")
public class EdcController {

    @Autowired private EdcAdapterService edcAdapter;

    /**
     * Publish an order at the edc.
     *
     * @param orderId id of the order to be published at the edc.
     * @return OK if order was published, else false.
     */
    @GetMapping("/publish")
    @CrossOrigin
    public ResponseEntity<String> publishAtEDC(@RequestParam String orderId) {
        try {
            var success = edcAdapter.publishOrderAtEDC(orderId);
            return ResponseEntity.ok("Success: " + success);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Get the catalog from an EDC.
     *
     * @param idsUrl ids url of the edc to get catalog from.
     * @return catalog of the requested edc.
     */
    @GetMapping("/catalog")
    @CrossOrigin
    public ResponseEntity<String> getEDCCatalog(@RequestParam String idsUrl) {
        if (!isValidURI(idsUrl)) {
            return ResponseEntity.badRequest().body("Malformed URL!");
        }
        try {
            var catalog = edcAdapter.getCatalog(idsUrl);
            return ResponseEntity.ok(catalog);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Start a negotiation with another EDC.
     *
     * @param orderId ID of the asset to request.
     * @param connectorAddress address of the edc to start negotiating with.
     * @return response of the own EDC.
     */
    @GetMapping("/startNegotiation")
    @CrossOrigin
    public ResponseEntity<String> startNegotiation(@RequestParam String orderId, @RequestParam String connectorAddress, 
            @RequestParam String contractDefinitionId) {
        try {
            var result = edcAdapter.startNegotiation(connectorAddress, contractDefinitionId, orderId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Start a data transfer with another EDC.
     *
     * @param orderId id of the asset to transfer.
     * @param connectorAddress address of the requested EDC.
     * @param transferId id used for the transfer process.
     * @param contractId id of the negotiated contract.
     * @return response of the own EDC.
     */
    @GetMapping("/startTransfer")
    @CrossOrigin
    public ResponseEntity<String> startTransfer(
            @RequestParam String orderId,
            @RequestParam String connectorAddress,
            @RequestParam String transferId,
            @RequestParam String contractId) {
        try {
            var result = edcAdapter.startTransfer(transferId, connectorAddress, contractId, orderId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Get negotiations from own EDC.
     *
     * @param negotiationId optional parameter if only a specific negotiation should be retrieved.
     * @return response from own EDC.
     */
    @GetMapping("/negotiations")
    @CrossOrigin
    public ResponseEntity<String> getNegotiations(@RequestParam(required = false) String negotiationId) {
        try {
            var result = edcAdapter.getFromEdc(negotiationId, "data", "contractnegotiations");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
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
    public ResponseEntity<String> getAssets(@RequestParam(required = false) String assetId) {
        try {
            var result = edcAdapter.getFromEdc(assetId, "data", "assets");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Delete an asset from the EDC.
     *
     * @param assetId id of the asset to delete.
     * @return response from own EDC.
     */
    @DeleteMapping("/assets/{assetId}")
    @CrossOrigin
    public ResponseEntity<String> deleteAsset(@PathVariable String assetId) {
        try {
            var result = edcAdapter.deleteAsset(assetId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Get transfers from the EDC.
     *
     * @param transferId optional parameter if only a specific transfer should be retrieved.
     * @return response from own EDC.
     */
    @GetMapping("/transfers")
    @CrossOrigin
    public ResponseEntity<String> getTransfers(@RequestParam(required = false) String transferId) {
        try {
            var result = edcAdapter.getFromEdc(transferId, "data", "transferprocess");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Utility method to check if a URI is valid.
     *
     * @param uriString input string to validate.
     * @return true iff string is a valid URI.
     */
    private static boolean isValidURI(String uriString) {
        try {
            URL url = new URL(uriString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
