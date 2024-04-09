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
package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.EdcContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service Layer of EDC Adapter. Builds and sends requests to a productEDC.
 * The EDC connection is configured using the application.properties file.
 */
@Service
@Slf4j
public class EdcAdapterService {
    private static final OkHttpClient CLIENT = new OkHttpClient();
    @Autowired
    private VariablesService variablesService;
    private ObjectMapper objectMapper;
    @Autowired
    private EdcRequestBodyBuilder edcRequestBodyBuilder;
    @Autowired
    private EndpointDataReferenceService edrService;

    @Autowired
    private EdcContractMappingService edcContractMappingService;

    private Pattern urnPattern = PatternStore.URN_OR_UUID_PATTERN;

    public EdcAdapterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Util method for issuing a GET request to the management api of your control plane.
     * Any caller of this method has the responsibility to close
     * the returned Response object after using it.
     *
     * @param pathSegments The path segments
     * @return The response
     * @throws IOException If the connection to your control plane fails
     */
    public Response sendGetRequest(List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getEdcManagementUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        var request = new Request.Builder()
            .get()
            .url(urlBuilder.build())
            .header("X-Api-Key", variablesService.getEdcApiKey())
            .build();
        return CLIENT.newCall(request).execute();
    }

    /**
     * Util method for issuing a POST request to the management api of your control plane.
     * Any caller of this method has the responsibility to close
     * the returned Response object after using it.
     *
     * @param requestBody  The request body
     * @param pathSegments The path segments
     * @return The response from your control plane
     * @throws IOException If the connection to your control plane fails
     */
    private Response sendPostRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getEdcManagementUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        var request = new Request.Builder()
            .post(body)
            .url(urlBuilder.build())
            .header("X-Api-Key", variablesService.getEdcApiKey())
            .header("Content-Type", "application/json")
            .build();
        return CLIENT.newCall(request).execute();
    }

    /**
     * Call this method at startup to register the necessary request and
     * response apis. In case you are using the framework agreement feature,
     * the framework agreement policy will be registered as well here.
     *
     * @return true if all registrations were successful, otherwise false
     */
    public boolean registerAssetsInitially() {
        boolean result;
        log.info("Registration of framework agreement policy successful " + (result = createFrameWorkPolicy()));
        log.info("Registration of DTR Asset successful " + (result &= registerDtrAsset()));
        log.info("Registration of ItemStock 2.0.0 submodel successful " + (result &= registerItemStockSubmodel()));
        return result;
    }


    /**
     * Utility method to register policy- and contract-definitions for both the
     * REQUEST and the RESPONSE-Api specifically for the given partner.
     *
     * @param partner the partner
     * @return true, if all registrations ran successfully
     */
    public boolean createPolicyAndContractDefForPartner(Partner partner) {
        boolean result = createBpnlAndMembershipPolicyDefinitionForPartner(partner);
        result &= createItemStockSubmodelContractDefinitionForPartner(partner);
        result &= createDtrContractDefinitionForPartner(partner);
        result &= registerPartTypeAssetForPartner(partner);
        return createPartTypeInfoContractDefForPartner(partner) && result;

    }

    private boolean createItemStockSubmodelContractDefinitionForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildItemStockSubmodelContractDefinitionWithBpnRestrictedPolicy(partner);
        try (var response = sendPostRequest(body, List.of("v2", "contractdefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Contract definition registration failed for partner " + partner.getBpnl() + " and ItemStock Submodel");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Contract definition registration failed for partner " + partner.getBpnl() + " and ItemStock Submodel");
            return false;
        }
    }

    private boolean createDtrContractDefinitionForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildDtrContractDefinitionForPartner(partner);
        try (var response = sendPostRequest(body, List.of("v2", "contractdefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Contract definition registration failed for partner " + partner.getBpnl() + " and DTR");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Contract definition registration failed for partner " + partner.getBpnl() + " and DTR", e);
            return false;
        }
    }

    private boolean createPartTypeInfoContractDefForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildPartTypeInfoContractDefinitionForPartner(partner);
        try (var response = sendPostRequest(body, List.of("v2", "contractdefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Contract definition registration failed for partner " + partner.getBpnl() + " and PartTypeInfo asset");
                return false;
            }
            log.info("Contract definition successful for PartTypeAsset and partner " + partner.getBpnl());
            return true;
        } catch (Exception e) {
            log.error("Contract definition registration failed for partner " + partner.getBpnl() + " and PartTypeInfo asset", e);
            return false;
        }
    }


    /**
     * Registers a policy definition that evaluates to true in case all the following conditions apply:
     * 1. The BPNL of the requesting connector is equal to the BPNL of the partner
     * 2. There's a CX membership credential present
     *
     * @param partner the partner to create the policy for
     * @return true, if registration ran successfully
     */
    private boolean createBpnlAndMembershipPolicyDefinitionForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildBpnAndMembershipRestrictedPolicy(partner);
        try (var response = sendPostRequest(body, List.of("v2", "policydefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Policy Registration failed");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register bpnl and membership policy definition for partner " + partner.getBpnl(), e);
            return false;
        }
    }

    /**
     * Registers the framework agreement policy definition
     *
     * @return true, if registration ran successfully
     */
    private boolean createContractPolicy() {
        var body = edcRequestBodyBuilder.buildFrameworkPolicy();
        try (var response = sendPostRequest(body, List.of("v2", "policydefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Framework Policy Registration failed");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register Framework Policy", e);
            return false;
        }
    }


    private boolean registerDtrAsset() {
        var body = edcRequestBodyBuilder.buildDtrRegistrationBody();
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("Asset registration failed for DTR");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register DTR Asset", e);
            return false;
        }
    }

    private boolean registerPartTypeAssetForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildPartTypeInfoRegistrationBody(partner);
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("Asset registration failed for PartTypeAsset and partner " + partner.getBpnl());
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            log.info("Asset registration successful for PartTypeAsset and partner " + partner.getBpnl());
            return true;
        } catch (Exception e) {
            log.error("Failed to register PartTypeAsset for partner " + partner.getBpnl());
            return false;
        }
    }

    private boolean registerItemStockSubmodel() {
        var body = edcRequestBodyBuilder.buildItemStockSubmodelRegistrationBody();
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("ItemStock Submodel Asset registration failed");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register ItemStock Submodel", e);
            return false;
        }
    }


    /**
     * Retrieve the response to an unfiltered catalog request from the partner
     * with the given dspUrl
     *
     * @param dspUrl The dspUrl of your partner
     * @return The response containing the full catalog, if successful
     */
    public Response getCatalogResponse(String dspUrl) throws IOException {
        return sendPostRequest(edcRequestBodyBuilder.buildBasicCatalogRequestBody(dspUrl, null), List.of("v2", "catalog", "request"));
    }

    /**
     * Retrieve an (unfiltered) catalog from the partner with the
     * given dspUrl
     *
     * @param dspUrl The dspUrl of your partner
     * @return The full catalog
     * @throws IOException If the connection to the partners control plane fails
     */
    public JsonNode getCatalog(String dspUrl) throws IOException {
        try (var response = getCatalogResponse(dspUrl)) {
            String stringData = response.body().string();
            return objectMapper.readTree(stringData);
        }

    }

    /**
     * Helper method for contracting a certain asset as specified in the catalog item from
     * a specific Partner.
     *
     * @param partner     The Partner to negotiate with
     * @param catalogItem An excerpt from a catalog.
     * @return The JSON response to your contract offer.
     * @throws IOException If the connection to the partners control plane fails
     */
    private JsonNode initiateNegotiation(Partner partner, JsonNode catalogItem) throws IOException {
        var requestBody = edcRequestBodyBuilder.buildAssetNegotiationBody(partner, catalogItem);
        try (var response = sendPostRequest(requestBody, List.of("v2", "contractnegotiations"))) {
            String responseString = response.body().string();
            return objectMapper.readTree(responseString);
        }
    }

    /**
     * Sends a request to the own control plane in order to receive
     * the current status of the previously initiated contractNegotiations as
     * specified by the parameter.
     *
     * @param negotiationId The id of the ongoing negotiation
     * @return The response body
     * @throws IOException If the connection to your control plane fails
     */
    public JsonNode getNegotiationState(String negotiationId) throws IOException {
        try (var response = sendGetRequest(List.of("v2", "contractnegotiations", negotiationId))) {
            String stringData = response.body().string();
            return objectMapper.readTree(stringData);
        }
    }

    /**
     * Sends a request to the own control plane in order to receive
     * a list of all negotiations.
     *
     * @return The response body as String
     * @throws IOException If the connection to your control plane fails
     */
    public Response getAllNegotiations() throws IOException {
        var requestBody = edcRequestBodyBuilder.buildNegotiationsRequestBody();
        return sendPostRequest(requestBody, List.of("v2", "contractnegotiations", "request"));
    }

    /**
     * Sends a request to the own control plane in order to initiate a transfer of
     * a previously negotiated asset.
     *
     * @param partner    The partner
     * @param contractId The contract id
     * @param assetId    The asset id
     * @return The response object
     * @throws IOException If the connection to your control plane fails
     */
    public JsonNode initiateProxyPullTransfer(Partner partner, String contractId, String assetId, String partnerEdcUrl) throws IOException {
        var body = edcRequestBodyBuilder.buildProxyPullRequestBody(partner, contractId, assetId, partnerEdcUrl);
        try (var response = sendPostRequest(body, List.of("v2", "transferprocesses"))) {
            String data = response.body().string();
            return objectMapper.readTree(data);
        }
    }

    public JsonNode initiateProxyPullTransfer(Partner partner, String contractId, String assetId) throws IOException {
        return initiateProxyPullTransfer(partner, contractId, assetId, partner.getEdcUrl());
    }

    /**
     * Sends a request to the own control plane in order to receive
     * the current status of the previously initiated transfer as specified by
     * the parameter.
     *
     * @param transferId The id of the transfer in question
     * @return The response from your Controlplane
     * @throws IOException If the connection to your control plane fails
     */
    public JsonNode getTransferState(String transferId) throws IOException {
        try (var response = sendGetRequest(List.of("v2", "transferprocesses", transferId))) {
            String data = response.body().string();
            return objectMapper.readTree(data);
        }
    }

    /**
     * Sends a request to the own control plane in order to receive
     * a list of all transfers.
     *
     * @return The response
     * @throws IOException If the connection to your control plane fails
     */
    public Response getAllTransfers() throws IOException {
        var requestBody = edcRequestBodyBuilder.buildTransfersRequestBody();
        return sendPostRequest(requestBody, List.of("v2", "transferprocesses", "request"));
    }

    /**
     * Sends a request to the own control plane in order to receive
     * the contract agreement with the given contractAgreementId
     *
     * @param contractAgreementId the contractAgreement's Id
     * @return the contractAgreement
     * @throws IOException If the connection to your control plane fails
     */
    public String getContractAgreement(String contractAgreementId) throws IOException {
        try (var response = sendGetRequest(List.of("v2", "contractagreements", contractAgreementId))) {
            return response.body().string();
        }
    }

    /**
     * Util method for sending a post request the given endpoint
     * in order to initiate a proxy pull request.
     * Any caller of this method has the responsibility to close
     * the returned Response object after using it.
     *
     * @param url               The URL of an endpoint you received to perform a pull request
     * @param authKey           The authKey to be used in the HTTP request header
     * @param authCode          The authCode to be used in the HTTP request header
     * @param requestBodyString The request body in JSON format as String
     * @return The response from the endpoint defined in the url (which is usually the other party's data plane), carrying the asset payload
     */
    public Response postProxyPullRequest(String url, String authKey, String authCode, String requestBodyString) {
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), requestBodyString);
            Request request = new Request.Builder()
                .url(url)
                .header(authKey, authCode)
                .post(requestBody)
                .build();
            return CLIENT.newCall(request).execute();
        } catch (Exception e) {
            log.error("Failed to send Proxy Pull request to " + url, e);
            throw new RuntimeException(e);
        }
    }

    public Response getProxyPullRequest(String url, String authKey, String authCode, String[] pathParams) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        for (var pathSegment : pathParams) {
            urlBuilder.addPathSegment(pathSegment);
        }
        try {
            var request = new Request.Builder()
                .get()
                .url(urlBuilder.build())
                .header(authKey, authCode)
                .build();
            return CLIENT.newCall(request).execute();
        } catch (Exception e) {
            log.error("ProxyPull GET Request failed ", e);
            return null;
        }
    }

    private boolean getPartnerAasForMaterial(Partner partner, String partnerMaterialNumber) {
        EdcContractMapping contractMapping = edcContractMappingService.find(partner.getBpnl());
        if (contractMapping != null) {
            if (contractMapping.getDtrContractId() == null || contractMapping.getDtrAssetId() == null) {
                negotiateForPartnerDtr(partner);
            }
            return initiateDtrTransferForLookupApi(partner, "manufacturerPartId", partnerMaterialNumber);
        }
        return false;
    }

    private boolean getPartnerAasForProduct(Partner partner, String partnerMaterialNumber) {
        EdcContractMapping contractMapping = edcContractMappingService.find(partner.getBpnl());
        if (contractMapping != null) {
            if (contractMapping.getDtrContractId() == null || contractMapping.getDtrAssetId() == null) {
                negotiateForPartnerDtr(partner);
            }
            return initiateDtrTransferForLookupApi(partner, "customerPartId", partnerMaterialNumber);
        }
        return false;
    }

    public JsonNode doItemStockSubmodelRequest(MaterialPartnerRelation mpr, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        Material material = mpr.getMaterial();
        String materialNumber = switch (direction) {
            case INBOUND -> material.getMaterialNumberCx();
            case OUTBOUND -> mpr.getPartnerCXNumber();
        };
        boolean failed = true;
        EdcContractMapping contractMapping = edcContractMappingService.find(partner.getBpnl());
        try {
            if (contractMapping != null) {
                String href = contractMapping.getMaterialToHrefMapping(materialNumber);
                if (href == null) {
                    log.info("Need href for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
                    switch (direction) {
                        case OUTBOUND -> getPartnerAasForMaterial(partner, mpr.getPartnerMaterialNumber());
                        case INBOUND -> getPartnerAasForProduct(partner, mpr.getPartnerMaterialNumber());
                    }
                    contractMapping = edcContractMappingService.find(partner.getBpnl());
                    href = contractMapping.getMaterialToHrefMapping(materialNumber);
                }
                String itemStockContractId = contractMapping.getItemStockContractId();
                String assetId = contractMapping.getItemStockAssetId();
                String partnerItemStockEdcUrl = contractMapping.getItemStockEdcProtocolUrl();

                if (itemStockContractId == null || assetId == null || partnerItemStockEdcUrl == null) {
                    log.info("Need Contract for ItemStock Submodel 2.0.0 with " + partner.getBpnl());
                    if (negotiateForPartnerItemStockSubmodel(mpr, direction)) {
                        contractMapping = edcContractMappingService.find(partner.getBpnl());
                        itemStockContractId = contractMapping.getItemStockContractId();
                        assetId = contractMapping.getItemStockEdcProtocolUrl();
                        partnerItemStockEdcUrl = contractMapping.getItemStockEdcProtocolUrl();
                        href = contractMapping.getMaterialToHrefMapping(materialNumber);
                    } else {
                        log.error("Failed to contract for ItemStock Submodel 2.0.0 with " + partner.getBpnl());
                        return null;
                    }
                }
                try {
                    if (!partnerItemStockEdcUrl.equals(partner.getEdcUrl())) {
                        log.warn("Diverging EDC URL's for partner " + partner.getBpnl());
                        log.warn("Edc URL from AAS " + partnerItemStockEdcUrl);
                        log.warn("General Edc Url of partner " + partner.getEdcUrl());
                    }
                    // Request EdrToken
                    var transferResp = initiateProxyPullTransfer(partner, itemStockContractId, assetId, partnerItemStockEdcUrl);
                    String transferId = transferResp.get("@id").asText();
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(100);
                        transferResp = getTransferState(transferId);
                        if ("STARTED".equals(transferResp.get("edc:state").asText())) {
                            break;
                        }
                    }
                    EDR_Dto edrDto = null;
                    // Await arrival of edr
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(100);
                        edrDto = edrService.findByTransferId(transferId);
                        if (edrDto != null) {
                            log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
                            break;
                        }
                    }
                    if (edrDto == null) {
                        log.error("Failed to obtain EDR data for " + assetId + " with " + partner.getEdcUrl());
                        return null;
                    }
                    var edrURL = edrDto.endpoint().endsWith("/") ? edrDto.endpoint() : edrDto.endpoint() + "/";
                    edrURL += materialNumber + "/" + direction;
                    if (!href.startsWith(edrURL)) {
                        log.warn("Diverging URLs in ItemStock Submodel request");
                        log.warn("href: " + href);
                        log.warn("URL from EDR: " + edrURL);
                    }
                    try (var response = getProxyPullRequest(href, edrDto.authKey(), edrDto.authCode(), new String[]{"$value"})) {
                        if (response.isSuccessful()) {
                            String responseString = response.body().string();
                            failed = false;
                            return objectMapper.readTree(responseString);
                        }
                    } catch (Exception e) {
                        log.error("Error in ItemStock Submodel 2.0.0 transfer request for partner " + partner.getBpnl(), e);
                    }


                } catch (Exception e) {
                    log.error("Error in ItemStock Submodel 2.0.0 transfer request for partner " + partner.getBpnl(), e);
                }
            }
        } finally {
            if (failed && contractMapping != null) {
                // invalidate stored data
                contractMapping.setItemStockAssetId(null);
                contractMapping.setItemStockContractId(null);
                contractMapping.setItemStockEdcProtocolUrl(null);
                contractMapping.putMaterialToHrefMapping(materialNumber, null);
                edcContractMappingService.update(contractMapping);
                log.warn("ItemStock Submodel request for material " + material.getOwnMaterialNumber() + " at partner " +
                    partner.getBpnl() + " failed. Invalidating contract data. You may want to retry this request");
            }
        }
        return null;
    }

    private boolean negotiateForPartnerItemStockSubmodel(MaterialPartnerRelation mpr, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        try {
            EdcContractMapping contractMapping = edcContractMappingService.find(mpr.getPartner().getBpnl());
            String itemStockEdcUrl = contractMapping.getItemStockEdcProtocolUrl();
            String itemStockAssetId = contractMapping.getItemStockAssetId();
            if (itemStockEdcUrl == null || itemStockAssetId == null) {
                log.info("Need ItemStock Submodel Edc URL for " + mpr.getPartner().getBpnl());
                switch (direction) {
                    case INBOUND -> getPartnerAasForProduct(mpr.getPartner(), mpr.getPartnerMaterialNumber());
                    case OUTBOUND -> getPartnerAasForMaterial(mpr.getPartner(), mpr.getPartnerMaterialNumber());
                }
                contractMapping = edcContractMappingService.find(mpr.getPartner().getBpnl());
                itemStockEdcUrl = contractMapping.getItemStockEdcProtocolUrl();
                itemStockAssetId = contractMapping.getItemStockAssetId();
            }
            var responseNode = getCatalog(itemStockEdcUrl);
            var catalogArray = responseNode.get("dcat:dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            JsonNode targetCatalogEntry = null;
            for (var entry : catalogArray) {
                var dctTypeObject = entry.get("dct:type");
                if (dctTypeObject != null) {
                    if (("https://w3id.org/catenax/taxonomy#Submodel").equals(dctTypeObject.get("@id").asText())) {
                        if ("3.0".equals(entry.get("https://w3id.org/catenax/ontology/common#version").asText())) {
                            var aasSemantics = entry.get("aas-semantics:semanticId");
                            if ("urn:samm:io.catenax.item_stock:2.0.0#ItemStock".equals(aasSemantics.get("@id").asText())) {
                                if (itemStockAssetId.equals(entry.get("edc:id").asText())) {
                                    if (targetCatalogEntry == null) {
                                        if (testFrameworkAgreementConstraint(entry)) {
                                            targetCatalogEntry = entry;
                                        } else {
                                            log.error("Contract Negotiation with partner " + partner.getBpnl() + " has " +
                                                "been aborted. This partner's contract policy does not match the policy " +
                                                "supported by this application. \n Supported Policy: " + variablesService.getPurisFrameworkAgreement() +
                                                "\n Received offer from Partner: \n" + entry.toPrettyString());
                                            break;
                                        }
                                    } else {
                                        log.warn("Ambiguous catalog entries found! \n" + catalogArray.toPrettyString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (targetCatalogEntry == null) {
                log.error("Could not find asset for ItemStock Submodel 2.0.0 at partner " + partner.getBpnl() + "'s catalog");
                log.warn("CATALOG CONTENT \n" + catalogArray.toPrettyString());
                return false;
            }
            String assetId = targetCatalogEntry.get("@id").asText();
            JsonNode negotiationResponse = initiateNegotiation(partner, targetCatalogEntry);
            String negotiationId = negotiationResponse.get("@id").asText();
            // Await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var responseObject = getNegotiationState(negotiationId);
                if ("FINALIZED".equals(responseObject.get("edc:state").asText())) {
                    contractId = responseObject.get("edc:contractAgreementId").asText();
                    break;
                }
            }
            if (contractId == null) {
                var negotiationState = getNegotiationState(negotiationId);
                log.warn("no contract id, last negotiation state: \n" + negotiationState.toPrettyString());
                log.error("Failed to obtain " + assetId + " from " + partner.getEdcUrl());
                return false;
            }
            contractMapping.setItemStockContractId(contractId);
            contractMapping.setItemStockAssetId(assetId);
            edcContractMappingService.update(contractMapping);
            log.info("Got contract for ItemStock Submodel 2.0.0 api with partner " + partner.getBpnl());
            return true;
        } catch (Exception e) {
            log.error("Error in Negotiation for ItemStock Submodel 2.0.0 of " + partner.getBpnl(), e);
            return false;
        }
    }

    private boolean negotiateForPartnerDtr(Partner partner) {
        try {
            var responseNode = getCatalog(partner.getEdcUrl());
            var catalogArray = responseNode.get("dcat:dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            JsonNode targetCatalogEntry = null;
            for (var entry : catalogArray) {
                var dctTypeObject = entry.get("dct:type");
                if (dctTypeObject != null) {
                    if (("https://w3id.org/catenax/taxonomy#DigitalTwinRegistry").equals(dctTypeObject.get("@id").asText())) {
                        if ("3.0".equals(entry.get("https://w3id.org/catenax/ontology/common#version").asText())) {
                            if (targetCatalogEntry == null) {
                                targetCatalogEntry = entry;
                            } else {
                                log.warn("Ambiguous catalog entries found! \n" + catalogArray.toPrettyString());
                            }
                        }
                    }
                }
            }
            if (targetCatalogEntry == null) {
                log.error("Could not find asset for DigitalTwinRegistry at partner " + partner.getBpnl() + "'s catalog");
                log.warn("CATALOG CONTENT \n" + catalogArray.toPrettyString());
                return false;
            }
            String assetId = targetCatalogEntry.get("@id").asText();
            JsonNode negotiationResponse = initiateNegotiation(partner, targetCatalogEntry);
            String negotiationId = negotiationResponse.get("@id").asText();
            // Await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var responseObject = getNegotiationState(negotiationId);
                if ("FINALIZED".equals(responseObject.get("edc:state").asText())) {
                    contractId = responseObject.get("edc:contractAgreementId").asText();
                    break;
                }
            }
            if (contractId == null) {
                var negotiationState = getNegotiationState(negotiationId);
                log.warn("no contract id, last negotiation state: \n" + negotiationState.toPrettyString());
                log.error("Failed to obtain " + assetId + " from " + partner.getEdcUrl());
                return false;
            }
            var contractMapping = edcContractMappingService.find(partner.getBpnl());
            contractMapping.setDtrContractId(contractId);
            contractMapping.setDtrAssetId(assetId);
            edcContractMappingService.update(contractMapping);
            log.info("Got contract for DTR api with partner " + partner.getBpnl());
            return true;
        } catch (Exception e) {
            log.error("Error in Negotiation for DTR of " + partner.getBpnl(), e);
            return false;
        }
    }

    private boolean initiateDtrTransferForLookupApi(Partner partner, String specificAssetIdType, String specificAssetIdValue) {
        var contractMapping = edcContractMappingService.find(partner.getBpnl());
        boolean failed = true;
        try {
            String contractId = contractMapping.getDtrContractId();
            String assetId = contractMapping.getDtrAssetId();
            if (contractId == null || assetId == null) {
                negotiateForPartnerDtr(partner);
                contractMapping = edcContractMappingService.find(partner.getBpnl());
                contractId = contractMapping.getDtrContractId();
                assetId = contractMapping.getDtrAssetId();
            }
            var transferResp = initiateProxyPullTransfer(partner, contractId, assetId);
            String transferId = transferResp.get("@id").asText();
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                transferResp = getTransferState(transferId);
                if ("STARTED".equals(transferResp.get("edc:state").asText())) {
                    break;
                }
            }
            EDR_Dto edrDto = null;
            // Await arrival of edr
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                edrDto = edrService.findByTransferId(transferId);
                if (edrDto != null) {
                    log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
                    break;
                }
            }
            if (edrDto == null) {
                log.error("Failed to obtain EDR data for " + assetId + " with " + partner.getEdcUrl());
                return false;
            }
            HttpUrl.Builder urlBuilder = HttpUrl.parse(edrDto.endpoint()).newBuilder()
                .addPathSegment("api")
                .addPathSegment("v3.0")
                .addPathSegment("lookup")
                .addPathSegment("shells");
            String query = "{\"name\":\"" + specificAssetIdType + "\",\"value\":\"" + specificAssetIdValue + "\"}";
            query += ",{\"name\":\"digitalTwinType\",\"value\":\"PartType\"}";
            urlBuilder.addQueryParameter("assetIds", Base64.getEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8)));
            var request = new Request.Builder()
                .get()
                .header(edrDto.authKey(), edrDto.authCode())
                .url(urlBuilder.build())
                .build();
            try (var response = CLIENT.newCall(request).execute()) {
                var bodyString = response.body().string();
                var jsonResponse = objectMapper.readTree(bodyString);
                var resultArray = jsonResponse.get("result");
                if (resultArray.isArray()) {
                    String aasId = resultArray.get(0).asText();
                    urlBuilder = HttpUrl.parse(edrDto.endpoint()).newBuilder()
                        .addPathSegment("api")
                        .addPathSegment("v3.0")
                        .addPathSegment("shell-descriptors");
                    String base64AasId = Base64.getEncoder().encodeToString(aasId.getBytes(StandardCharsets.UTF_8));
                    urlBuilder.addQueryParameter("aasIdentifier", base64AasId);
                    request = new Request.Builder()
                        .get()
                        .header(edrDto.authKey(), edrDto.authCode())
                        .url(urlBuilder.build())
                        .build();
                    try (var response2 = CLIENT.newCall(request).execute()) {
                        var body2String = response2.body().string();
                        var aasJson = objectMapper.readTree(body2String);
                        var resultObject = aasJson.get("result").get(0);
                        var submodelDescriptors = resultObject.get("submodelDescriptors");

                        var submodelDescriptor = submodelDescriptors.get(0);
                        var semanticId = submodelDescriptor.get("semanticId");
                        var keys = semanticId.get("keys");
                        for (var key : keys) {
                            var keyType = key.get("type").asText();
                            var keyValue = key.get("value").asText();
                            if ("GlobalReference".equals(keyType) && "urn:samm:io.catenax.item_stock:2.0.0#ItemStock".equals(keyValue)) {
                                var endpoints = submodelDescriptor.get("endpoints");
                                var endpoint = endpoints.get(0);
                                var interfaceObject = endpoint.get("interface").asText();
                                if ("SUBMODEL-3.0".equals(interfaceObject)) {
                                    var protocolInformationObject = endpoint.get("protocolInformation");
                                    String href = protocolInformationObject.get("href").asText();
                                    var findMaterialNumber = HttpUrl.parse(href).encodedPathSegments().stream().filter(segment -> urnPattern.matcher(segment).matches()).findFirst();
                                    if (findMaterialNumber.isEmpty()) {
                                        log.error("Failed to find materialNumber segment in HREF URL");
                                        return false;
                                    }
                                    String materialNumber = findMaterialNumber.get();
                                    String subProtocolBodyData = protocolInformationObject.get("subprotocolBody").asText();
                                    var subProtocolElements = subProtocolBodyData.split(";");
                                    String id = subProtocolElements[0].replace("id=", "");
                                    String dspUrl = subProtocolElements[subProtocolElements.length - 1].replace("dspEndpoint=", "");
                                    contractMapping.setItemStockAssetId(id);
                                    contractMapping.setItemStockPublicDataPlaneApiUrl(href);
                                    contractMapping.setItemStockEdcProtocolUrl(dspUrl);
                                    contractMapping.putMaterialToHrefMapping(materialNumber, href);
                                    edcContractMappingService.update(contractMapping);
                                    log.info("Updated contractMapping for Partner " + partner.getBpnl());
                                    failed = false;
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error in shell-descriptors aas request ", e);
                    }
                }
            } catch (Exception e) {
                log.error("Error in lookup api request", e);
            }
            return true;
        } catch (Exception e) {
            log.error("Error in transfer request for DTR at Partner " + partner.getBpnl());
            return false;
        } finally {
            if (failed && contractMapping != null) {
                // invalidate failing contract information
                contractMapping.setDtrContractId(null);
                contractMapping.setDtrAssetId(null);
                contractMapping.setItemStockEdcProtocolUrl(null);
                edcContractMappingService.update(contractMapping);
                log.warn("DTR Lookup request for " + specificAssetIdType + " " + specificAssetIdValue + " at partner " +
                    partner.getBpnl() + " failed. Invalidating contract data. You may want to retry this request");
            }
        }
    }

    /**
     * Tries to negotiate for the PartTypeInfo-Submodel asset with the given partner
     * and also tries to initiate the transfer of the edr token to the given endpoint.
     * <p>
     * It will return a String array of length 4. The authKey is stored under index 0, the
     * authCode under index 1, the endpoint under index 2 and the contractId under index 3.
     *
     * @param partner   the partner
     * @param apiMethod the api method
     * @return A String array or null, if negotiation or transfer have failed or the authCode did not arrive
     */
    public String[] getContractForPartTypeInfoSubmodel(Partner partner) {
        try {
            var responseNode = getCatalog(partner.getEdcUrl());
            var catalogArray = responseNode.get("dcat:dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            JsonNode targetCatalogEntry = null;
            for (var entry : catalogArray) {
                var dctTypeObject = entry.get("dct:type");
                if (dctTypeObject != null) {
                    if (("https://w3id.org/catenax/taxonomy#Submodel").equals(dctTypeObject.get("@id").asText())) {
                        if ("3.0".equals(entry.get("https://w3id.org/catenax/ontology/common#version").asText())) {
                            var semanticId = entry.get("aas-semantics:semanticId");
                            String idString = semanticId.get("@id").asText();
                            if ("urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation".equals(idString)) {
                                if (targetCatalogEntry == null) {
                                    if (testFrameworkAgreementConstraint(entry)) {
                                        targetCatalogEntry = entry;
                                    } else {
                                        log.error("Contract Negotiation for PartTypeInformation Submodel asset with partner " + partner.getBpnl() + " has " +
                                            "been aborted. This partner's contract policy does not match the policy " +
                                            "supported by this application. \n Supported Policy: " + variablesService.getPurisFrameworkAgreement() +
                                            "\n Received offer from Partner: \n" + entry.toPrettyString());
                                        break;
                                    }
                                } else {
                                    log.warn("Ambiguous catalog entries found! \n" + catalogArray.toPrettyString());
                                }
                            }
                        }
                    }
                }
            }
            if (targetCatalogEntry == null) {
                log.error("Could not find submodel asset for PartTypeInformation at partner " + partner.getBpnl() + " 's catalog");
                return null;
            }
            String assetId = targetCatalogEntry.get("@id").asText();
            JsonNode negotiationResponse = initiateNegotiation(partner, targetCatalogEntry);
            String negotiationId = negotiationResponse.get("@id").asText();
            // Await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var responseObject = getNegotiationState(negotiationId);
                if ("FINALIZED".equals(responseObject.get("edc:state").asText())) {
                    contractId = responseObject.get("edc:contractAgreementId").asText();
                    break;
                }
            }
            if (contractId == null) {
                var negotiationState = getNegotiationState(negotiationId);
                log.warn("no contract id, last negotiation state: \n" + negotiationState.toPrettyString());
                log.error("Failed to obtain " + assetId + " from " + partner.getEdcUrl());
                return null;
            }

            // Initiate transfer of edr
            var transferResp = initiateProxyPullTransfer(partner, contractId, assetId);
            String transferId = transferResp.get("@id").asText();
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                transferResp = getTransferState(transferId);
                if ("STARTED".equals(transferResp.get("edc:state").asText())) {
                    break;
                }
            }

            // Await arrival of edr
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                EDR_Dto edr_Dto = edrService.findByTransferId(transferId);
                if (edr_Dto != null) {
                    log.info("Successfully negotiated for " + assetId + " with " + partner.getEdcUrl());
                    return new String[]{edr_Dto.authKey(), edr_Dto.authCode(), edr_Dto.endpoint(), contractId};
                }
            }
            log.warn("did not receive authCode");
            log.error("Failed to obtain " + assetId + " from " + partner.getEdcUrl());
            return null;

        } catch (Exception e) {
            log.error("Failed to get contract for PartTypeInfo submodel asset from " + partner.getBpnl(), e);
            return null;
        }
    }

    /**
     * Helper method to check whether you and the contract offer from the other party have the
     * same framework agreement policy.
     *
     * @param catalogEntry the catalog item containing the desired api asset
     * @return true, if the policy matches yours, otherwise false
     */
    private boolean testContractPolicyConstraints(JsonNode catalogEntry) {
        var constraint = Optional.ofNullable(catalogEntry.get("odrl:hasPolicy"))
            .map(policy -> policy.get("odrl:permission"))
            .map(permission -> permission.get("odrl:constraint"));
        if (constraint.isEmpty()) return false;

        var leftOperand = constraint.map(cons -> cons.get("odrl:leftOperand"))
            .filter(operand -> variablesService.getPurisFrameworkAgreement().equals(operand.asText()));

        var operator = constraint.map(cons -> cons.get("odrl:operator"))
            .map(op -> op.get("@id"))
            .filter(operand -> "odrl:eq".equals(operand.asText()));

        var rightOperand = constraint.map(cons -> cons.get("odrl:rightOperand"))
            .filter(operand -> "active".equals(operand.asText()));

        if (leftOperand.isEmpty() || operator.isEmpty() || rightOperand.isEmpty()) return false;

        return true;
    }
}
