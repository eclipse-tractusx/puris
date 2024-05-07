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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.SubmodelType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EdrDto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
        boolean result;
        log.info("Registration of framework agreement policy successful {}", (result = createContractPolicy()));
        boolean assetRegistration;
        log.info("Registration of DTR Asset successful {}", (assetRegistration = registerDtrAsset()));
        result &= assetRegistration;
        log.info("Registration of ItemStock 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getItemStockSubmodelApiAssetId(),
            variablesService.getItemStockSubmodelEndpoint(),
            SubmodelType.ITEM_STOCK.URN_SEMANTIC_ID
        )));
        log.info("Registration of Planned Production 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getProductionSubmodelApiAssetId(),
            variablesService.getProductionSubmodelEndpoint(),
            SubmodelType.PRODUCTION.URN_SEMANTIC_ID
        )));
        log.info("Registration of Short Term Material Demand 1.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getDemandSubmodelApiAssetId(),
            variablesService.getDemandSubmodelEndpoint(),
            SubmodelType.DEMAND.URN_SEMANTIC_ID
        )));
        log.info("Registration of Delivery Information 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getDeliverySubmodelApiAssetId(),
            variablesService.getDeliverySubmodelEndpoint(),
            SubmodelType.DELIVERY.URN_SEMANTIC_ID
        )));
        result &= assetRegistration;
        log.info("Registration of PartTypeInformation 1.0.0 submodel successful {}", (assetRegistration = registerPartTypeInfoSubmodelAsset()));
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
        boolean result = createBpnlAndMembershipPolicyDefinitionForPartner(partner);
        result &= createSubmodelContractDefinitionForPartner(SubmodelType.ITEM_STOCK.URN_SEMANTIC_ID, variablesService.getItemStockSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(SubmodelType.PRODUCTION.URN_SEMANTIC_ID, variablesService.getProductionSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(SubmodelType.DEMAND.URN_SEMANTIC_ID, variablesService.getDemandSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(SubmodelType.DELIVERY.URN_SEMANTIC_ID, variablesService.getDeliverySubmodelApiAssetId(), partner);
        result &= createDtrContractDefinitionForPartner(partner);
        return createSubmodelContractDefinitionForPartner(SubmodelType.PART_TYPE_INFORMATION.URN_SEMANTIC_ID, variablesService.getPartTypeSubmodelApiAssetId(), partner) && result;
    }

    private boolean createSubmodelContractDefinitionForPartner(String semanticId, String assetId, Partner partner) {
        var body = edcRequestBodyBuilder.buildSubmodelContractDefinitionWithBpnRestrictedPolicy(assetId, partner);
        try (var response = sendPostRequest(body, List.of("v2", "contractdefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Contract definition registration failed for partner " + partner.getBpnl() + " and {} Submodel", semanticId);
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Contract definition registration failed for partner " + partner.getBpnl() + " and {} Submodel", semanticId);
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

    private boolean registerPartTypeInfoSubmodelAsset() {
        var body = edcRequestBodyBuilder.buildPartTypeInfoSubmodelRegistrationBody();
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("Asset registration failed for PartTypeInfoSubmodel");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            log.info("Asset registration successful for PartTypeInfoSubmodel");
            return true;
        } catch (Exception e) {
            log.error("Failed to register PartTypeInfoSubmodel");
            return false;
        }
    }

    private boolean registerSubmodelAsset(String assetId, String endpoint, String semanticId) {
        var body = edcRequestBodyBuilder.buildSubmodelRegistrationBody(assetId, endpoint, semanticId);
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("{} Submodel Asset registration failed", semanticId);
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register {} Submodel", semanticId, e);
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

    private JsonNode getSubmodelFromPartner(MaterialPartnerRelation mpr, SubmodelType type, DirectionCharacteristic direction, int retries) {
        if (retries < 0) {
            return null;
        }
        Partner partner = mpr.getPartner();
        SubmodelData submodelData = switch (type) {
            case DTR -> throw new IllegalArgumentException("DTR not supported");
            case ITEM_STOCK -> fetchSubmodelDataByDirection(mpr, SubmodelType.ITEM_STOCK.URN_SEMANTIC_ID, direction);
            case PRODUCTION -> fetchSubmodelDataByDirection(mpr, SubmodelType.PRODUCTION.URN_SEMANTIC_ID, direction);
            case DEMAND -> fetchSubmodelDataByDirection(mpr, SubmodelType.DEMAND.URN_SEMANTIC_ID, direction);
            case DELIVERY -> fetchSubmodelDataByDirection(mpr, SubmodelType.DELIVERY.URN_SEMANTIC_ID, direction);
            case PART_TYPE_INFORMATION -> fetchPartTypeSubmodelData(mpr);
        };
        boolean failed = true;
        try {
            String assetId = submodelData.assetId();
            String partnerDspUrl = submodelData.dspUrl();
            String submodelContractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
            if (submodelContractId == null) {
                log.info("Need Contract for " + type + " with " + partner.getBpnl());
                if (negotiateForSubmodel(mpr, type, direction)) {
                    submodelContractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
                } else {
                    log.error("Failed to contract for " + type + " with " + partner.getBpnl());
                    return getSubmodelFromPartner(mpr, type, direction, --retries);
                }
            }
            if (!partner.getEdcUrl().equals(partnerDspUrl)) {
                log.warn("Divering Edc Urls for Partner: " + partner.getBpnl() + " and type " + type);
                log.warn("General Partner EdcUrl: " + partner.getEdcUrl());
                log.warn("URL from AAS: " + partnerDspUrl);
            }
            // Request EdrToken
            var transferResp = initiateProxyPullTransfer(partner, submodelContractId, assetId, partnerDspUrl);
            String transferId = transferResp.get("@id").asText();
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                transferResp = getTransferState(transferId);
                if ("STARTED".equals(transferResp.get("edc:state").asText())) {
                    break;
                }
            }
            EdrDto edrDto = null;
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
                return doSubmodelRequest(type, mpr, direction, --retries);
            }
            if (!submodelData.href().startsWith(edrDto.endpoint())) {
                log.warn("Diverging URLs in ItemStock Submodel request");
                log.warn("href: " + submodelData.href());
                log.warn("Data plane base URL from EDR: " + edrDto.endpoint());
            }
            try (var response = getProxyPullRequest(submodelData.href, edrDto.authKey(), edrDto.authCode(), new String[]{type.REPRESENTATION})) {
                if (response.isSuccessful()) {
                    String responseString = response.body().string();
                    failed = false;
                    return objectMapper.readTree(responseString);
                }
            }
        } catch (Exception e) {
            log.error("Error in Submodel Transfer Request for " + type + " at " + partner.getBpnl(), e);
        } finally {
            if (failed) {
                log.warn("Invalidating Contract data for " + type + " with " + partner.getBpnl());
                edcContractMappingService.putContractId(partner, type, submodelData.assetId(), submodelData.dspUrl(), null);
            }
        }
        return getSubmodelFromPartner(mpr, type, direction, --retries);
    }

    public JsonNode doSubmodelRequest(SubmodelType type, MaterialPartnerRelation mpr, DirectionCharacteristic direction, int retries) {
        if (retries < 0) {
            return null;
        }
        var data = getSubmodelFromPartner(mpr, type, direction, 1);
        if (data == null) {
            return doSubmodelRequest(type, mpr, direction, --retries);
        }
        return data;
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
            edcContractMappingService.putDtrContractData(partner, assetId, contractId);
            return true;
        } catch (Exception e) {
            log.error("Error in Negotiation for DTR of " + partner.getBpnl(), e);
            return false;
        }
    }

    private SubmodelData fetchSubmodelDataByDirection(MaterialPartnerRelation mpr, String semanticId, DirectionCharacteristic direction) {
        String manufacturerPartId = switch (direction) {
            case INBOUND -> mpr.getMaterial().getOwnMaterialNumber();
            case OUTBOUND -> mpr.getPartnerMaterialNumber();
        };
        String manufacturerId = switch (direction) {
            case INBOUND -> variablesService.getOwnBpnl();
            case OUTBOUND -> mpr.getPartner().getBpnl();
        };
        return fetchSubmodelData(mpr, semanticId, manufacturerPartId, manufacturerId);
    }

    private SubmodelData fetchPartTypeSubmodelData(MaterialPartnerRelation mpr) {
        return fetchSubmodelData(mpr, "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation",
            mpr.getPartnerMaterialNumber(), mpr.getPartner().getBpnl());
    }

    private record SubmodelData(String assetId, String dspUrl, String href) {
    }

    private SubmodelData fetchSubmodelData(MaterialPartnerRelation mpr, String semanticId, String manufacturerPartId, String manufacturerId) {
        JsonNode submodelDescriptors = getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, 1);
        for (var submodelDescriptor : submodelDescriptors) {
            var semanticIdObject = submodelDescriptor.get("semanticId");
            var keys = semanticIdObject.get("keys");
            for (var key : keys) {
                var keyType = key.get("type").asText();
                var keyValue = key.get("value").asText();
                if ("GlobalReference".equals(keyType) && semanticId.equals(keyValue)) {
                    var endpoints = submodelDescriptor.get("endpoints");
                    var endpoint = endpoints.get(0);
                    var interfaceObject = endpoint.get("interface").asText();
                    if ("SUBMODEL-3.0".equals(interfaceObject)) {
                        var protocolInformationObject = endpoint.get("protocolInformation");
                        String href = protocolInformationObject.get("href").asText();
                        String subProtocolBodyData = protocolInformationObject.get("subprotocolBody").asText();
                        var subProtocolElements = subProtocolBodyData.split(";");
                        String assetId = subProtocolElements[0].replace("id=", "");
                        String dspUrl = subProtocolElements[subProtocolElements.length - 1].replace("dspEndpoint=", "");
                        if (!urlPattern.matcher(dspUrl).matches()) {
                            log.error("Found invalid URL Submodel Descriptor: " + dspUrl);
                        }
                        return new SubmodelData(assetId, dspUrl, href);
                    }
                }
            }
        }
        return null;
    }

    private JsonNode getAasSubmodelDescriptors(String manufacturerPartId, String manufacturerId, MaterialPartnerRelation mpr, int retries) {
        if (retries < 0) {
            log.error("AasSubmodelDescriptors Request failed for " + manufacturerPartId + " and " + manufacturerId);
            return null;
        }
        boolean failed = true;
        Partner partner = mpr.getPartner();
        try {
            var dtrContractData = edcContractMappingService.getDtrAssetAndContractId(partner);
            String assetId = dtrContractData[0];
            String contractId = dtrContractData[1];
            if (contractId == null || assetId == null) {
                if (!negotiateForPartnerDtr(partner)) {
                    return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
                }
                dtrContractData = edcContractMappingService.getDtrAssetAndContractId(partner);
                assetId = dtrContractData[0];
                contractId = dtrContractData[1];
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
            EdrDto edrDto = null;
            // Await arrival of edr
            for (int i = 0; i < 100; i++) {
                edrDto = edrService.findByTransferId(transferId);
                if (edrDto != null) {
                    log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
                    break;
                }
                Thread.sleep(100);
            }
            if (edrDto == null) {
                log.error("Failed to obtain EDR data for " + assetId + " with " + partner.getEdcUrl());
                return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
            }
            HttpUrl.Builder urlBuilder = HttpUrl.parse(edrDto.endpoint()).newBuilder()
                .addPathSegment("api")
                .addPathSegment("v3")
                .addPathSegment("lookup")
                .addPathSegment("shells");
            String query = "{\"name\":\"manufacturerPartId\",\"value\":\"" + manufacturerPartId + "\"}";
            query += ",{\"name\":\"digitalTwinType\",\"value\":\"PartType\"}";
            query += ",{\"name\":\"manufacturerId\",\"value\":\"" + manufacturerId + "\"}";
            String encodedQuery = Base64.getEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8));
            urlBuilder.addQueryParameter("assetIds", encodedQuery);
            var request = new Request.Builder()
                .get()
                .header(edrDto.authKey(), edrDto.authCode())
                .url(urlBuilder.build())
                .build();
            try (var response = CLIENT.newCall(request).execute()) {
                var bodyString = response.body().string();
                var jsonResponse = objectMapper.readTree(bodyString);
                var resultArray = jsonResponse.get("result");
                if (resultArray != null && resultArray.isArray() && !resultArray.isEmpty()) {
                    if (resultArray.size() > 1) {
                        log.warn("Found more than one result for query " + query);
                        log.info(resultArray.toPrettyString());
                    }
                    String aasId = resultArray.get(0).asText();
                    urlBuilder = HttpUrl.parse(edrDto.endpoint()).newBuilder()
                        .addPathSegment("api")
                        .addPathSegment("v3")
                        .addPathSegment("shell-descriptors");
                    String base64AasId = Base64.getEncoder().encodeToString(aasId.getBytes(StandardCharsets.UTF_8));
                    urlBuilder.addPathSegment(base64AasId);
                    request = new Request.Builder()
                        .get()
                        .header(edrDto.authKey(), edrDto.authCode())
                        .url(urlBuilder.build())
                        .build();
                    try (var response2 = CLIENT.newCall(request).execute()) {
                        var body2String = response2.body().string();
                        var aasJson = objectMapper.readTree(body2String);
                        var submodelDescriptors = aasJson.get("submodelDescriptors");
                        if (submodelDescriptors != null) {
                            failed = false;
                            return submodelDescriptors;
                        } else {
                            log.warn("No SubmodelDescriptors found in DTR shell-descriptors response:\n" + aasJson.toPrettyString());
                        }
                    }
                } else {
                    if (resultArray != null) {
                        if (resultArray.isArray() && resultArray.isEmpty()) {
                            log.warn("Empty Result array received");
                        } else {
                            log.warn("Unexpected Response for DTR lookup with query " + query + "\n" + resultArray.toPrettyString());
                        }
                    } else {
                        log.warn("No Result Array received in DTR lookup response: \n" + jsonResponse.toPrettyString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in AasSubmodelDescriptor Request for " + mpr + " and manufacturerPartId " + manufacturerPartId, e);
            return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
        } finally {
            if (failed) {
                log.warn("Invalidating DTR contract data");
                edcContractMappingService.putDtrContractData(partner, null, null);
            }
        }
        return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
    }

    /**
     * Tries to negotiate for a partner's Submodel API.<p>
     * If successful, the contractId as well as the assetId
     * are stored to the EdcContractMapping of that partner
     * and can be retrieved from there to be used in later
     * transfer requests for that api asset.
     *
     * @param mpr       The mpr indicating the partner and material in question
     * @param type      The SubmodelType (DTR is not supported)
     * @param direction May be null if the SubmodelType is not direction-sensitive
     * @return true, if a contract was successfully negotiated
     */

    private boolean negotiateForSubmodel(MaterialPartnerRelation mpr, SubmodelType type, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        SubmodelData submodelData = switch (type) {
            case DTR -> throw new IllegalArgumentException("DTR not supported");
            case ITEM_STOCK -> fetchSubmodelDataByDirection(mpr, SubmodelType.ITEM_STOCK.URN_SEMANTIC_ID, direction);
            case PRODUCTION -> fetchSubmodelDataByDirection(mpr, SubmodelType.PRODUCTION.URN_SEMANTIC_ID, direction);
            case DEMAND -> fetchSubmodelDataByDirection(mpr, SubmodelType.DEMAND.URN_SEMANTIC_ID, direction);
            case DELIVERY -> fetchSubmodelDataByDirection(mpr, SubmodelType.DELIVERY.URN_SEMANTIC_ID, direction);
            case PART_TYPE_INFORMATION -> fetchPartTypeSubmodelData(mpr);
        };
        try {
            var responseNode = getCatalog(submodelData.dspUrl());
            var catalogArray = responseNode.get("dcat:dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            JsonNode targetCatalogEntry = null;
            for (var entry : catalogArray) {
                var semanticId = entry.get("aas-semantics:semanticId");
                if (semanticId == null) {
                    continue;
                }
                String idString = semanticId.get("@id").asText();
                if (idString == null) {
                    continue;
                }
                if (type.URN_SEMANTIC_ID.equals(idString) && submodelData.assetId.equals(entry.get("edc:id").asText())) {
                    if (targetCatalogEntry == null) {
                        if (testContractPolicyConstraints(entry)) {
                            targetCatalogEntry = entry;
                        } else {
                            log.error("Contract Negotiation for " + type + " Submodel asset with partner " + partner.getBpnl() + " has " +
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
            if (targetCatalogEntry == null) {
                log.error("Could not find asset for " + type + " Submodel at partner " + partner.getBpnl() + "'s catalog");
                log.warn("CATALOG CONTENT \n" + catalogArray.toPrettyString());
                return false;
            }
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
                log.error("Failed to obtain " + type + " from " + partner.getEdcUrl() + ", last negotiation state: \n"
                    + negotiationState.toPrettyString());
                return false;
            }
            edcContractMappingService.putContractId(partner, type, submodelData.assetId(), submodelData.dspUrl(), contractId);
            log.info("Got contract for " + type + " Submodel api with partner " + partner.getBpnl());
            return true;

        } catch (Exception e) {
            log.error("Error in negotiation for " + type, e);
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
    public String getCxIdFromPartTypeInformation(MaterialPartnerRelation mpr) {
        var data =  getSubmodelFromPartner(mpr, SubmodelType.PART_TYPE_INFORMATION, null, 1);
        return data.get("catenaXId").asText();
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
