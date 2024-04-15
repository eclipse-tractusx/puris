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
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.SubmodelType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDRDto;
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

    private Pattern urlPattern = PatternStore.URL_PATTERN;

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
        boolean result = true;
        if (variablesService.isUseFrameworkPolicy()) {
            log.info("Registration of framework agreement policy successful {}", (result = createFrameWorkPolicy()));
            if (!result) return false;
        } else {
            log.info("Skipping registration of framework agreement policy");
        }
        boolean assetRegistration;
        log.info("Registration of DTR Asset successful {}", (assetRegistration = registerDtrAsset()));
        result &= assetRegistration;
        log.info("Registration of ItemStock 2.0.0 submodel successful {}", (assetRegistration = registerItemStockSubmodel()));
        result &= assetRegistration;
        log.info("Registration of PartTypeInformation 1.0.0 submodel successful {}", (assetRegistration = registerPartTypeAsset()));
        result &= assetRegistration;
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
        boolean result = createPolicyDefinitionForPartner(partner);
        result &= createItemStockSubmodelContractDefinitionForPartner(partner);
        result &= createDtrContractDefinitionForPartner(partner);
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
     * Registers a policy definition that allows only the given partner's
     * BPNL.
     *
     * @param partner the partner
     * @return true, if registration ran successfully
     */
    private boolean createPolicyDefinitionForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildBpnRestrictedPolicy(partner);
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
            log.error("Failed to register policy definition for partner " + partner.getBpnl(), e);
            return false;
        }
    }

    /**
     * Registers the framework agreement policy definition
     *
     * @return true, if registration ran successfully
     */
    private boolean createFrameWorkPolicy() {
        var body = edcRequestBodyBuilder.buildFrameworkAgreementPolicy();
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

    private boolean registerPartTypeAsset() {
        var body = edcRequestBodyBuilder.buildPartTypeInfoRegistrationBody();
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("Asset registration failed for PartTypeAsset");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            log.info("Asset registration successful for PartTypeAsset");
            return true;
        } catch (Exception e) {
            log.error("Failed to register PartTypeAsset");
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

    private boolean getPartnerAasForMaterial(MaterialPartnerRelation mpr) {
        ContractMapping contractMapping = edcContractMappingService.getContractMapping(mpr.getPartner(), SubmodelType.DTR);
        if (contractMapping.getContractId() == null || contractMapping.getAssetId() == null) {
            if (!negotiateForPartnerDtr(mpr.getPartner()))
                return false;
        }
        return initiateDtrTransferForLookupApi("manufacturerPartId", mpr.getPartnerMaterialNumber(), mpr);
    }

    private boolean getPartnerAasForProduct(MaterialPartnerRelation mpr) {
        ContractMapping contractMapping = edcContractMappingService.getContractMapping(mpr.getPartner(), SubmodelType.DTR);
        if (contractMapping.getContractId() == null || contractMapping.getAssetId() == null) {
            if (!negotiateForPartnerDtr(mpr.getPartner())) {
                return false;
            }
        }
        return initiateDtrTransferForLookupApi("customerPartId", mpr.getPartnerMaterialNumber(), mpr);
    }

    public JsonNode doItemStockSubmodelRequest(MaterialPartnerRelation mpr, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        Material material = mpr.getMaterial();
        String materialNumber = switch (direction) {
            case INBOUND -> material.getMaterialNumberCx();
            case OUTBOUND -> mpr.getPartnerCXNumber();
        };
        boolean failed = true;
        ContractMapping contractMapping = edcContractMappingService.getContractMapping(mpr.getPartner(), SubmodelType.ITEMSTOCK);

        try {
            String href = edcContractMappingService.getHrefMapping(partner, SubmodelType.ITEMSTOCK, materialNumber);
            if (href == null) {
                log.info("Need href for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
                switch (direction) {
                    case OUTBOUND -> getPartnerAasForMaterial(mpr);
                    case INBOUND -> getPartnerAasForProduct(mpr);
                }
                href = edcContractMappingService.getHrefMapping(partner, SubmodelType.ITEMSTOCK, materialNumber);
            }
            String itemStockContractId = contractMapping.getContractId();
            String assetId = contractMapping.getAssetId();
            String partnerItemStockEdcUrl = contractMapping.getPartnerDspUrl();

            if (itemStockContractId == null || assetId == null || partnerItemStockEdcUrl == null) {
                log.info("Need Contract for ItemStock Submodel 2.0.0 with " + partner.getBpnl());
                if (negotiateForPartnerItemStockSubmodel(mpr, direction)) {
                    contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.ITEMSTOCK);
                    itemStockContractId = contractMapping.getContractId();
                    assetId = contractMapping.getAssetId();
                    partnerItemStockEdcUrl = contractMapping.getPartnerDspUrl();
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
                EDRDto edrDto = null;
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
        } finally {
            if (failed) {
                // Invalidate stored data
                edcContractMappingService.invalidateContractMapping(partner, SubmodelType.ITEMSTOCK);
                edcContractMappingService.invalidateHrefMapping(partner, SubmodelType.ITEMSTOCK, materialNumber);
                log.warn("ItemStock Submodel request for material " + material.getOwnMaterialNumber() + " at partner " +
                    partner.getBpnl() + " failed. Invalidating stored contract data. You may want to retry this request");
            }
        }
        return null;
    }

    private boolean negotiateForPartnerItemStockSubmodel(MaterialPartnerRelation mpr, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        try {
            ContractMapping contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.ITEMSTOCK);
            String itemStockEdcUrl = contractMapping.getPartnerDspUrl();
            String itemStockAssetId = contractMapping.getAssetId();
            if (itemStockEdcUrl == null || itemStockAssetId == null) {
                log.info("Need ItemStock Submodel Edc URL or AssetId from " + mpr.getPartner().getBpnl());
                switch (direction) {
                    case INBOUND -> getPartnerAasForProduct(mpr);
                    case OUTBOUND -> getPartnerAasForMaterial(mpr);
                }
                contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.ITEMSTOCK);
                itemStockEdcUrl = contractMapping.getPartnerDspUrl();
                itemStockAssetId = contractMapping.getAssetId();
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
                                        if (variablesService.isUseFrameworkPolicy()) {
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
                                            targetCatalogEntry = entry;
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
            contractMapping.setContractId(contractId);
            contractMapping.setAssetId(assetId);
            edcContractMappingService.saveContractMapping(partner, SubmodelType.ITEMSTOCK, contractMapping);
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
            log.info("Got contract for DTR api with partner {}", partner.getBpnl());
            var contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.DTR);
            contractMapping.setContractId(contractId);
            contractMapping.setAssetId(assetId);
            edcContractMappingService.saveContractMapping(partner, SubmodelType.DTR, contractMapping);

            return true;
        } catch (Exception e) {
            log.error("Error in Negotiation for DTR of " + partner.getBpnl(), e);
            return false;
        }
    }

    private boolean initiateDtrTransferForLookupApi(String specificAssetIdType, String specificAssetIdValue,
                                                    MaterialPartnerRelation mpr) {
        Partner partner = mpr.getPartner();
        var contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.DTR);
        boolean needPartTypeInformation = mpr.isPartnerSuppliesMaterial();
        boolean foundItemStockInformation = false;
        boolean foundPartTypeInformation = false;
        try {
            String contractId = contractMapping.getContractId();
            String assetId = contractMapping.getAssetId();
            if (contractId == null || assetId == null) {
                if (!negotiateForPartnerDtr(partner)) {
                    return false;
                }
                contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.DTR);
                contractId = contractMapping.getContractId();
                assetId = contractMapping.getAssetId();
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
            EDRDto edrDto = null;
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
                        for (var submodelDescriptor : submodelDescriptors) {
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
                                            continue;
                                        }
                                        String materialNumber = findMaterialNumber.get();
                                        String subProtocolBodyData = protocolInformationObject.get("subprotocolBody").asText();
                                        var subProtocolElements = subProtocolBodyData.split(";");
                                        String id = subProtocolElements[0].replace("id=", "");
                                        String dspUrl = subProtocolElements[subProtocolElements.length - 1].replace("dspEndpoint=", "");
                                        if (!urlPattern.matcher(dspUrl).matches()) {
                                            log.error("Found invalid URL in ItemStockSubmodel Descriptor: " + dspUrl);
                                            continue;
                                        }
                                        var itemStockContractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.ITEMSTOCK);
                                        itemStockContractMapping.setAssetId(id);
                                        itemStockContractMapping.setPartnerDspUrl(dspUrl);
                                        edcContractMappingService.saveContractMapping(partner, SubmodelType.ITEMSTOCK, itemStockContractMapping);
                                        edcContractMappingService.saveHrefMapping(partner, SubmodelType.ITEMSTOCK, materialNumber, href);
                                        log.info("Updated ItemStock Submodel contractMapping for Partner " + partner.getBpnl());
                                        foundItemStockInformation = true;
                                    }
                                }
                                if (needPartTypeInformation &&
                                    "GlobalReference".equals(keyType) && "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation".equals(keyValue)) {
                                    var endpoints = submodelDescriptor.get("endpoints");
                                    var endpoint = endpoints.get(0);
                                    var interfaceObject = endpoint.get("interface").asText();
                                    if ("SUBMODEL-3.0".equals(interfaceObject)) {
                                        var protocolInformationObject = endpoint.get("protocolInformation");
                                        String href = protocolInformationObject.get("href").asText();
                                        var pathSegments = HttpUrl.parse(href).encodedPathSegments();
                                        String materialNumber = pathSegments.get(pathSegments.size() - 1);
                                        String subProtocolBodyData = protocolInformationObject.get("subprotocolBody").asText();
                                        var subProtocolElements = subProtocolBodyData.split(";");
                                        String id = subProtocolElements[0].replace("id=", "");
                                        String dspUrl = subProtocolElements[subProtocolElements.length - 1].replace("dspEndpoint=", "");
                                        if (!urlPattern.matcher(dspUrl).matches()) {
                                            log.error("Found invalid URL in PartTypeSubmodel Descriptor: " + dspUrl);
                                            continue;
                                        }
                                        var partTypeContractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.PARTTYPE);
                                        partTypeContractMapping.setAssetId(id);
                                        partTypeContractMapping.setPartnerDspUrl(dspUrl);
                                        edcContractMappingService.saveContractMapping(partner, SubmodelType.PARTTYPE, partTypeContractMapping);
                                        edcContractMappingService.saveHrefMapping(partner, SubmodelType.PARTTYPE, mpr.getPartnerMaterialNumber(), href);
                                        log.info("Updated PartType Submodel contractMapping and HREF for Partner " + partner.getBpnl()
                                        + " and partner materialNumber: " + href);
                                        foundPartTypeInformation = true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error in shell-descriptors aas request ", e);
                        return false;
                    }
                }
            } catch (Exception e) {
                log.error("Error in lookup api request", e);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error in transfer request for DTR at Partner " + partner.getBpnl());
            return false;
        } finally {
            boolean failed = !foundItemStockInformation || (needPartTypeInformation && !foundPartTypeInformation);
            if (failed) {
                // Invalidate failing dtr contract information
                edcContractMappingService.invalidateContractMapping(partner, SubmodelType.DTR);
                log.warn("DTR Lookup request for {} {} at partner {} failed. Invalidating contract data. You may want " +
                    "to retry this request", specificAssetIdType, specificAssetIdValue, partner.getBpnl());
            }
        }
    }

    /**
     * Tries to negotiate for a partner's PartType API.<p>
     * If successful, the contractId as well as the assetId
     * are stored to the EdcContractMapping of that partner
     * and can be retrieved from there to be used in later
     * transfer requests for that api asset.
     *
     * @param mpr The MaterialPartnerRelation
     * @return true, if contract was agreed
     */
    private boolean negotiateForPartTypeApi(MaterialPartnerRelation mpr) {
        Partner partner = mpr.getPartner();
        ContractMapping contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.PARTTYPE);
        try {
            String partnerDSPUrl = contractMapping.getPartnerDspUrl();
            String partTypeAssetId = contractMapping.getAssetId();
            if (partnerDSPUrl == null || partTypeAssetId == null) {
                getPartnerAasForMaterial(mpr);
                contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.PARTTYPE);
                partnerDSPUrl = contractMapping.getPartnerDspUrl();
                partTypeAssetId = contractMapping.getAssetId();
            }

            var responseNode = getCatalog(partnerDSPUrl);
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
                                if (partTypeAssetId.equals(entry.get("edc:id").asText())) {
                                    if (targetCatalogEntry == null) {
                                        if (variablesService.isUseFrameworkPolicy()) {
                                            if (testFrameworkAgreementConstraint(entry)) {
                                                targetCatalogEntry = entry;
                                            } else {
                                                log.error("Contract Negotiation for PartTypeInformation Submodel asset with partner " + partner.getBpnl() + " has " +
                                                    "been aborted. This partner's contract policy does not match the policy " +
                                                    "supported by this application. \n Supported Policy: " + variablesService.getPurisFrameworkAgreement() +
                                                    "\n Received offer from Partner: \n" + entry.toPrettyString());
                                                break;
                                            }
                                        }
                                    } else {
                                        targetCatalogEntry = entry;
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
            contractMapping.setContractId(contractId);
            edcContractMappingService.saveContractMapping(partner, SubmodelType.PARTTYPE, contractMapping);
            log.info("Got contract for PartType api with partner " + partner.getBpnl());
            return true;
        } catch (Exception e) {
            log.error("Error in Negotiation for PartType of " + partner.getBpnl(), e);
            return false;
        }
    }

    /**
     * This method will return the partnerCXId from the supplier partner and
     * for the material that are contained in the given MaterialPartnerRelation.
     * <p>
     * If the partner is not a supplier for that material, we can't expect to find a
     * result a that partner's PartType Submodel API.
     *
     * @param mpr the MaterialPartnerRelation
     * @return the partner's CXid for that material
     */
    public String getPartTypeInformationFromPartner(MaterialPartnerRelation mpr) {
        var edrDto = initiatePartTypeTransferFromPartner(mpr);
        if (edrDto == null) {
            return null;
        }
        String href = edcContractMappingService.getHrefMapping(mpr.getPartner(), SubmodelType.PARTTYPE, mpr.getPartnerMaterialNumber());
        if (href == null) {
            if (mpr.isPartnerSuppliesMaterial()) {
                log.info("Retrieving PartType Submodel HREF for MATERIAL " + mpr.getMaterial().getOwnMaterialNumber()
                    + " from " + mpr.getPartner().getBpnl());
                if (getPartnerAasForMaterial(mpr)) {
                    href = edcContractMappingService.getHrefMapping(mpr.getPartner(), SubmodelType.PARTTYPE, mpr.getPartnerMaterialNumber());
                } else {
                    log.warn("PartType fetch failed");
                    return null;
                }
            }
        }
        try (var response = getProxyPullRequest(href, edrDto.authKey(), edrDto.authCode(),
            new String[]{"$value"})) {
            if (response != null && response.isSuccessful()) {
                var body = objectMapper.readTree(response.body().string());
                var cxId = body.get("catenaXId").asText();
                return cxId;
            }

        } catch (Exception e) {
            log.error("Error in PartType information request with partner " + mpr.getPartner().getBpnl() + " for " +
                mpr.getMaterial().getOwnMaterialNumber(), e);
        }
        return null;
    }

    /**
     * This method will obtain the EDR data needed to send an
     * EDC transfer request to a partner's PartType information
     * api.<p>
     * <p>
     * It will return a String array of length 4. The authKey is stored under index 0, the
     * authCode under index 1, the endpoint under index 2 and the contractId under index 3.
     *
     * @param mpr A MaterialPartnerRelation
     * @return A String array or null, if negotiation failed or the EDR data did not arrive
     */
    private EDRDto initiatePartTypeTransferFromPartner(MaterialPartnerRelation mpr) {
        Partner partner = mpr.getPartner();
        ContractMapping contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.PARTTYPE);
        boolean failed = true;
        try {
            String contractId = contractMapping.getContractId();
            String assetId = contractMapping.getAssetId();
            if (contractId == null || assetId == null) {
                log.info("Need contract for PartType with partner " + partner.getBpnl());
                negotiateForPartTypeApi(mpr);
                contractMapping = edcContractMappingService.getContractMapping(partner, SubmodelType.PARTTYPE);
                contractId = contractMapping.getContractId();
                assetId = contractMapping.getAssetId();
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
                EDRDto edrDto = edrService.findByTransferId(transferId);
                if (edrDto != null) {
                    log.info("Successfully negotiated for " + assetId + " with " + partner.getEdcUrl());
                    failed = false;
                    return edrDto;
                }
            }
            log.warn("Did not receive EDR");
            log.error("Failed to request transfer for " + assetId + " from " + partner.getEdcUrl());
        } catch (Exception e) {
            log.error("Error during EDC request for PartType information api from partner " + partner.getBpnl(), e);
        } finally {
            if (failed) {
                edcContractMappingService.invalidateContractMapping(partner, SubmodelType.PARTTYPE);
                log.warn("Invalidating PartType contract data with partner " + partner.getBpnl() +
                    "You may want to do a retry. ");
            }
        }
        return null;
    }

    /**
     * Helper method to check whether you and the contract offer from the other party have the
     * same framework agreement policy.
     *
     * @param catalogEntry the catalog item containing the desired api asset
     * @return true, if the policy matches yours, otherwise false
     */
    private boolean testFrameworkAgreementConstraint(JsonNode catalogEntry) {
        try {
            var policyObject = catalogEntry.get("odrl:hasPolicy");
            if (policyObject == null) {
                return false;
            }
            var permissionObject = policyObject.get("odrl:permission");
            if (permissionObject == null) {
                return false;
            }
            var constraintObject = permissionObject.get("odrl:constraint");
            if (constraintObject == null) {
                return false;
            }
            var leftOperandObject = constraintObject.get("odrl:leftOperand");
            if (!variablesService.getPurisFrameworkAgreement().equals(leftOperandObject.asText())) {
                return false;
            }
            var operatorObject = constraintObject.get("odrl:operator");
            if (!"odrl:eq".equals(operatorObject.get("@id").asText())) {
                return false;
            }
            var rightOperandObject = constraintObject.get("odrl:rightOperand");
            if (!"active".equals(rightOperandObject.asText())) {
                return false;
            }
        } catch (Exception e) {
            log.error("Failed in Framework Agreement Test ", e);
            return false;
        }
        return true;
    }
}
