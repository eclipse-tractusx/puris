/*
 * Copyright (c) 2022 Volkswagen AG
 * Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.JsonLdUtils;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private final ObjectMapper objectMapper;
    @Autowired
    private EdcRequestBodyBuilder edcRequestBodyBuilder;

    @Autowired
    private EdcContractMappingService edcContractMappingService;

    @Autowired
    private JsonLdUtils jsonLdUtils;

    private final Pattern urlPattern = PatternStore.URL_PATTERN;

    public EdcAdapterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Util method for issuing a GET request to the management api of your control plane.
     * Any caller of this method has the responsibility to close
     * the returned Response object after using it.
     *
     * @param pathSegments The path segments
     * @param queryParams  The query parameters to use
     * @return The response
     * @throws IOException If the connection to your control plane fails
     */
    public Response sendGetRequest(List<String> pathSegments, Map<String, String> queryParams) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getEdcManagementUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        var request = new Request.Builder()
            .get()
            .url(urlBuilder.build())
            .header("X-Api-Key", variablesService.getEdcApiKey())
            .build();
        return CLIENT.newCall(request).execute();
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
        return sendGetRequest(pathSegments, new HashMap<>());
    }

    /**
     * Util method for issuing a POST request to the management api of your control plane.
     * Any caller of this method has the responsibility to close
     * the returned Response object after using it.
     *
     * @param requestBody  The request body, not null
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
            AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID
        )));
        result &= assetRegistration;
        log.info("Registration of Planned Production 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getProductionSubmodelApiAssetId(),
            variablesService.getProductionSubmodelEndpoint(),
            AssetType.PRODUCTION_SUBMODEL.URN_SEMANTIC_ID
        )));
        result &= assetRegistration;
        log.info("Registration of Short Term Material Demand 1.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getDemandSubmodelApiAssetId(),
            variablesService.getDemandSubmodelEndpoint(),
            AssetType.DEMAND_SUBMODEL.URN_SEMANTIC_ID
        )));
        result &= assetRegistration;
        log.info("Registration of Delivery Information 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getDeliverySubmodelApiAssetId(),
            variablesService.getDeliverySubmodelEndpoint(),
            AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID
        )));
        result &= assetRegistration;
        log.info("Registration of Demand and Capacity Notification 2.0.0 asset successful {}", (assetRegistration = registerNotificationAsset(
            variablesService.getNotificationApiAssetId(),
            variablesService.getNotificationEndpoint()
        )));
        log.info("Registration of Days of Supply 2.0.0 submodel successful {}", (assetRegistration = registerSubmodelAsset(
            variablesService.getDaysOfSupplySubmodelApiAssetId(),
            variablesService.getDaysOfSupplySubmodelEndpoint(),
            AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID
        )));
        log.info("Registration of PartTypeInformation 1.0.0 submodel successful {}", (assetRegistration = registerPartTypeInfoSubmodelAsset()));
        result &= assetRegistration;
        return result;
    }


    /**
     * Utility method to register policy- and contract-definitions for both the
     * REQUEST and the RESPONSE-Api specifically for the given partner.
     *
     * @param partner The partner
     * @return true, if all registrations ran successfully
     */
    public boolean createPolicyAndContractDefForPartner(Partner partner) {
        boolean result = createBpnlAndMembershipPolicyDefinitionForPartner(partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID, variablesService.getItemStockSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.PRODUCTION_SUBMODEL.URN_SEMANTIC_ID, variablesService.getProductionSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.DEMAND_SUBMODEL.URN_SEMANTIC_ID, variablesService.getDemandSubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID, variablesService.getDeliverySubmodelApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.NOTIFICATION.URN_SEMANTIC_ID, variablesService.getNotificationApiAssetId(), partner);
        result &= createSubmodelContractDefinitionForPartner(AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID, variablesService.getDaysOfSupplySubmodelApiAssetId(), partner);
        result &= createDtrContractDefinitionForPartner(partner);
        return createSubmodelContractDefinitionForPartner(AssetType.PART_TYPE_INFORMATION_SUBMODEL.URN_SEMANTIC_ID, variablesService.getPartTypeSubmodelApiAssetId(), partner) && result;
    }

    private boolean createSubmodelContractDefinitionForPartner(String semanticId, String assetId, Partner partner) {
        var body = edcRequestBodyBuilder.buildSubmodelContractDefinitionWithBpnRestrictedPolicy(assetId, partner);
        try (var response = sendPostRequest(body, List.of("v3", "contractdefinitions"))) {
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
        try (var response = sendPostRequest(body, List.of("v3", "contractdefinitions"))) {
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
     * @param partner The partner to create the policy for
     * @return true, if registration ran successfully
     */
    private boolean createBpnlAndMembershipPolicyDefinitionForPartner(Partner partner) {
        var body = edcRequestBodyBuilder.buildBpnAndMembershipRestrictedPolicy(partner);
        try (var response = sendPostRequest(body, List.of("v3", "policydefinitions"))) {
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
        try (var response = sendPostRequest(body, List.of("v3", "policydefinitions"))) {
            if (!response.isSuccessful()) {
                if (response.code() == 409) {
                    log.info("Framework agreement policy definition already existed");
                    return true;
                }
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
        return sendAssetRegistrationRequest(body, "DTR");
    }

    private boolean registerPartTypeInfoSubmodelAsset() {
        var body = edcRequestBodyBuilder.buildPartTypeInfoSubmodelRegistrationBody();
        return sendAssetRegistrationRequest(body, variablesService.getPartTypeSubmodelApiAssetId());
    }

    private boolean registerSubmodelAsset(String assetId, String endpoint, String semanticId) {
        var body = edcRequestBodyBuilder.buildSubmodelRegistrationBody(assetId, endpoint, semanticId);
        return sendAssetRegistrationRequest(body, assetId);
    }

    private boolean registerNotificationAsset(String assetId, String endpoint) {
        var body = edcRequestBodyBuilder.buildNotificationRegistrationBody(assetId, endpoint);
        return sendAssetRegistrationRequest(body, assetId);
    }

    private boolean sendAssetRegistrationRequest(JsonNode body, String assetId) {
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                if (response.code() == 409) {
                    log.info("Asset {} already existed", assetId);
                    return true;
                }
                log.warn("Asset registration failed for {}", assetId);
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register {} Asset", assetId, e);
            return false;
        }
    }

    /**
     * Retrieve the response to an unfiltered catalog request from the partner
     * with the given dspUrl
     *
     * @param dspUrl      The dspUrl of your partner
     * @param partnerBpnl The bpnl of your partner
     * @param filter      Map of key (leftOperand) and values (rightOperand) to use as filterExpression with equal operand
     * @return The response containing the full catalog, if successful
     */
    public Response getCatalogResponse(String dspUrl, String partnerBpnl, Map<String, String> filter) throws IOException {
        return sendPostRequest(edcRequestBodyBuilder.buildBasicCatalogRequestBody(dspUrl, partnerBpnl, filter), List.of("v3", "catalog", "request"));
    }

    /**
     * Retrieve an (unfiltered) catalog from the partner with the
     * given dspUrl
     *
     * @param dspUrl      The dspUrl of your partner
     * @param partnerBpnl The bpnl of your partner
     * @param filter      Map of key (leftOperand) and values (rightOperand) to use as filterExpression with equal operand
     * @return The full catalog
     * @throws IOException If the connection to the partners control plane fails
     */
    public JsonNode getCatalog(String dspUrl, String partnerBpnl, Map<String, String> filter) throws IOException {
        try (var response = getCatalogResponse(dspUrl, partnerBpnl, filter)) {
            JsonNode responseNode = objectMapper.readTree(response.body().string());
            log.debug("Got Catalog response {}", responseNode.toPrettyString());
            return responseNode;
        }

    }

    /**
     * Helper method for contracting a certain asset as specified in the catalog item from
     * a specific Partner.
     * <p>
     * Uses the dspUrl of the partner.
     *
     * @param partner     The Partner to negotiate with
     * @param catalogItem An excerpt from a catalog.
     * @return The JSON response to your contract offer.
     * @throws IOException If the connection to the partners control plane fails
     */
    private JsonNode initiateNegotiation(Partner partner, JsonNode catalogItem) throws IOException {
        return initiateNegotiation(partner, catalogItem, null);
    }

    /**
     * Helper method for contracting a certain asset as specified in the catalog item from
     * a specific Partner.
     *
     * @param partner     The Partner to negotiate with
     * @param catalogItem An excerpt from a catalog.
     * @param dspUrl      The dspUrl if a specific (not from MAD Partner) needs to be used, if null, the partners edcUrl is taken
     * @return The JSON response to your contract offer.
     * @throws IOException If the connection to the partners control plane fails
     */
    private JsonNode initiateNegotiation(Partner partner, JsonNode catalogItem, String dspUrl) throws IOException {
        // use dspUrl as provided, if set - else use partner
        dspUrl = dspUrl != null && !dspUrl.isEmpty() ? dspUrl : partner.getEdcUrl();
        var requestBody = edcRequestBodyBuilder.buildAssetNegotiationBody(partner, catalogItem, dspUrl);
        try (Response response = sendPostRequest(requestBody, List.of("v3", "contractnegotiations"))) {
            JsonNode responseNode = objectMapper.readTree(response.body().string());
            log.debug("Result from negotiation {}", responseNode.toPrettyString());
            return responseNode;
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
        try (var response = sendGetRequest(List.of("v3", "contractnegotiations", negotiationId))) {
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
        return sendPostRequest(requestBody, List.of("v3", "contractnegotiations", "request"));
    }

    /**
     * Sends a request to the own control plane in order to initiate a transfer of
     * a previously negotiated asset.
     *
     * @param partner    The partner
     * @param contractId The contract id
     * @return The response object
     * @throws IOException If the connection to your control plane fails
     */
    public JsonNode initiateProxyPullTransfer(Partner partner, String contractId, String partnerEdcUrl) throws IOException {
        var body = edcRequestBodyBuilder.buildProxyPullRequestBody(partner, contractId, partnerEdcUrl);
        try (var response = sendPostRequest(body, List.of("v3", "transferprocesses"))) {
            String data = response.body().string();
            JsonNode result = objectMapper.readTree(data);
            log.debug("Got response from Proxy pull transfer init: {}", result.toPrettyString());
            return result;
        }
    }

    public JsonNode initiateProxyPullTransfer(Partner partner, String contractId) throws IOException {
        return initiateProxyPullTransfer(partner, contractId, partner.getEdcUrl());
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
        try (var response = sendGetRequest(List.of("v3", "transferprocesses", transferId))) {
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
        log.debug("GetAllTransfers Request: {}", requestBody.toPrettyString());
        return sendPostRequest(requestBody, List.of("v3", "transferprocesses", "request"));
    }

    /**
     * Sends a request to the own control plane in order to receive
     * the contract agreement with the given contractAgreementId
     *
     * @param contractAgreementId The contractAgreement's Id
     * @return The contractAgreement
     * @throws IOException If the connection to your control plane fails
     */
    public String getContractAgreement(String contractAgreementId) throws IOException {
        try (var response = sendGetRequest(List.of("v3", "contractagreements", contractAgreementId))) {
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
            RequestBody requestBody = RequestBody.create(requestBodyString, MediaType.parse("application/json"));
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

    private JsonNode postNotificationToPartner(Partner partner, AssetType type, JsonNode payload, int retries) {
        if (retries < 0) {
            return null;
        }
        boolean failed = true;
        String partnerDspUrl = partner.getEdcUrl();
        var assetId = switch (type) {
            case NOTIFICATION -> variablesService.getNotificationApiAssetId();
            default -> throw new IllegalArgumentException("Unsupported type " + type);
        };
        try {
            String contractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
            if (contractId == null) {
                log.info("Need Contract for " + type + " with " + partner.getBpnl());
                if (negotiateContractForNotification(partner, type)) {
                    contractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
                } else {
                    log.error("Failed to contract for " + type + " with " + partner.getBpnl());
                    return postNotificationToPartner(partner, type, payload, --retries);
                }
            }
            // Request EdrToken
            var transferResp = initiateProxyPullTransfer(partner, contractId, partnerDspUrl);
            log.debug("Transfer Request {}", transferResp.toPrettyString());
            String transferId = transferResp.get("@id").asText();
            // try proxy pull and terminate request
            try {
                EdrDto edrDto = getAndAwaitEdrDto(transferId);
                log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
                if (edrDto == null) {
                    log.error("Failed to obtain EDR data for " + assetId + " with " + partner.getEdcUrl());
                    return doNotificationPostRequest(type, partner, payload, --retries);
                }
                try (var response = postProxyPullRequest(edrDto.endpoint(), edrDto.authKey(), edrDto.authCode(), new ObjectMapper().writeValueAsString(payload))) {
                    if (response.isSuccessful()) {
                        String responseString = response.body().string();
                        failed = false;
                        return objectMapper.readTree(responseString);
                    }
                    log.info("Failed to post Notification to Partner.");
                }
            } finally {
                if (transferId != null) {
                    terminateTransfer(transferId);
                }
            }
        } catch (Exception e) {
            log.error("Error in Transfer Request for " + type + " at " + partner.getBpnl(), e);
        } finally {
            if (failed) {
                log.warn("Invalidating Contract data for " + type + " with " + partner.getBpnl());
                edcContractMappingService.putContractId(partner, type, assetId, partnerDspUrl, null);
            }
        }
        return postNotificationToPartner(partner, type, payload, --retries);
    }

    private JsonNode getSubmodelFromPartner(MaterialPartnerRelation mpr, AssetType type, DirectionCharacteristic direction, int retries) {
        if (retries < 0) {
            return null;
        }
        Partner partner = mpr.getPartner();
        SubmodelData submodelData = switch (type) {
            case DTR -> throw new IllegalArgumentException("DTR not supported");
            case ITEM_STOCK_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID, direction);
            case PRODUCTION_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.PRODUCTION_SUBMODEL.URN_SEMANTIC_ID, direction);
            case DEMAND_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.DEMAND_SUBMODEL.URN_SEMANTIC_ID, direction);
            case DELIVERY_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID, direction);
            case NOTIFICATION -> throw new IllegalArgumentException("DemandAndCapacityNotification not supported");
            case DAYS_OF_SUPPLY -> fetchSubmodelDataByDirection(mpr, AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID, direction);
            case PART_TYPE_INFORMATION_SUBMODEL -> fetchPartTypeSubmodelData(mpr);
        };
        boolean failed = true;
        try {
            String assetId = submodelData.assetId();
            String partnerDspUrl = submodelData.dspUrl();
            String submodelContractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
            if (submodelContractId == null) {
                log.info("Need Contract for " + type + " with " + partner.getBpnl());
                if (negotiateContractForSubmodel(mpr, type, direction)) {
                    submodelContractId = edcContractMappingService.getContractId(partner, type, assetId, partnerDspUrl);
                } else {
                    log.error("Failed to contract for " + type + " with " + partner.getBpnl());
                    return getSubmodelFromPartner(mpr, type, direction, --retries);
                }
            }
            if (!partner.getEdcUrl().equals(partnerDspUrl)) {
                log.warn("Diverging Edc Urls for Partner: " + partner.getBpnl() + " and type " + type);
                log.warn("General Partner EdcUrl: " + partner.getEdcUrl());
                log.warn("URL from AAS: " + partnerDspUrl);
            }
            // Request EdrToken
            var transferResp = initiateProxyPullTransfer(partner, submodelContractId, partnerDspUrl);
            log.debug("Transfer Request {}", transferResp.toPrettyString());
            String transferId = transferResp.get("@id").asText();
            // try proxy pull and terminate request
            try {
                EdrDto edrDto = getAndAwaitEdrDto(transferId);
                log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
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
            } finally {
                if (transferId != null) {
                    terminateTransfer(transferId);
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

    /**
     * Get the EDR via edr api and retry multiple times in case the EDR has not yet been available
     *
     * @param transferProcessId to get the EDR for, not null
     * @return edr received, or null if not yet available
     * @throws InterruptedException if thread was not able to sleep
     */
    private @Nullable EdrDto getAndAwaitEdrDto(String transferProcessId) throws InterruptedException, IOException {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            JsonNode transferResp = getTransferState(transferProcessId);
            if ("STARTED".equals(transferResp.get("state").asText())) {
                break;
            }
        }
        EdrDto edrDto = null;
        // retry, if Data Space Protocol / Data Plane Provisioning communication needs time to prepare
        for (int i = 0; i < 100; i++) {
            edrDto = getEdrForTransferProcessId(transferProcessId, 2);
            if (edrDto != null) {
                break;
            }
            Thread.sleep(100);
        }
        return edrDto;
    }

    public JsonNode doSubmodelRequest(AssetType type, MaterialPartnerRelation mpr, DirectionCharacteristic direction, int retries) {
        if (retries < 0) {
            return null;
        }
        var data = getSubmodelFromPartner(mpr, type, direction, 1);
        if (data == null) {
            return doSubmodelRequest(type, mpr, direction, --retries);
        }
        return data;
    }

    public JsonNode doNotificationPostRequest(AssetType type, Partner partner, JsonNode body, int retries) {
        if (retries < 0) {
            return null;
        }
        var data = postNotificationToPartner(partner, type, body, retries);
        if (data == null) {
            return doNotificationPostRequest(type, partner, body, --retries);
        }
        return data;
    }

    private boolean negotiateForPartnerDtr(Partner partner) {
        try {
            Map<String, String> equalFilters = new HashMap<>();
            equalFilters.put(EdcRequestBodyBuilder.CX_COMMON_NAMESPACE + "version", "3.0");
            equalFilters.put(
                "'" + EdcRequestBodyBuilder.DCT_NAMESPACE + "type'.'@id'",
                EdcRequestBodyBuilder.CX_TAXO_NAMESPACE + "DigitalTwinRegistry"
            );
            var responseNode = getCatalog(partner.getEdcUrl(), partner.getBpnl(), equalFilters);
            responseNode = jsonLdUtils.expand(responseNode);

            var catalogArray = responseNode.get(EdcRequestBodyBuilder.DCAT_NAMESPACE + "dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            if (catalogArray.size() > 1) {
                log.warn("Ambiguous catalog entries found! Will take the first\n" + catalogArray.toPrettyString());
                // potential constraint check in future
            }
            JsonNode targetCatalogEntry = catalogArray.get(0);
            if (targetCatalogEntry == null) {
                log.error("Could not find asset for DigitalTwinRegistry at partner " + partner.getBpnl() + "'s catalog");
                return false;
            }
            String assetId = targetCatalogEntry.get("@id").asText();
            log.debug("Found contract offer for asset {}", assetId);
            JsonNode negotiationResponse = initiateNegotiation(partner, targetCatalogEntry);
            String negotiationId = negotiationResponse.get("@id").asText();
            log.info("Started negotiation with id {}", negotiationId);
            // Await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var responseObject = getNegotiationState(negotiationId);
                if ("FINALIZED".equals(responseObject.get("state").asText())) {
                    contractId = responseObject.get("contractAgreementId").asText();
                    log.info("Contracted DTR with contractAgreementId {}", contractId);
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

    private record EdrDto(String authKey, String authCode, String endpoint){
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

    /**
     * Queries the dtr of a partner for the given mpr / material and returns submodel descriptors
     * <p>
     * Method assumes that the query at dtr only finds one shell (else take first entry)
     *
     * @param manufacturerPartId material number of the supplier party
     * @param manufacturerId     bpnl of the supplier party
     * @param mpr                containing the mapping between material and partner to lookup at dtr
     * @param retries            number of times to retry in case the shell could not (yet) been found
     * @return array of submodelDescriptors of the found shell
     */
    private JsonNode getAasSubmodelDescriptors(String manufacturerPartId, String manufacturerId, MaterialPartnerRelation mpr, int retries) {
        if (retries < 0) {
            log.error("AasSubmodelDescriptors Request failed for " + manufacturerPartId + " and " + manufacturerId);
            return null;
        }

        // A criticalFailure indicates that the connection to the partner's DTR could not be established at all
        // or delivers a completely unexpected response. This is assumed to be true at first, and will be set to false
        // if a response was received that contains the expected answer or at least an empty result.
        boolean criticalFailure = true;
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
            var transferResp = initiateProxyPullTransfer(partner, contractId);
            String transferId = transferResp.get("@id").asText();
            try {
                EdrDto edrDto = getAndAwaitEdrDto(transferId);
                if (edrDto == null) {
                    log.error("Failed to obtain EDR data for " + assetId + " with " + partner.getEdcUrl());
                    return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
                } else {
                    log.info("Received EDR data for " + assetId + " with " + partner.getEdcUrl());
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
                                criticalFailure = false;
                                return submodelDescriptors;
                            } else {
                                log.warn("No SubmodelDescriptors found in DTR shell-descriptors response:\n" + aasJson.toPrettyString());
                            }
                        }
                    } else {
                        if (resultArray != null) {
                            if (resultArray.isArray() && resultArray.isEmpty()) {
                                log.warn("Empty Result array received");
                                criticalFailure = false;
                            } else {
                                log.warn("Unexpected Response for DTR lookup with query " + query + "\n" + resultArray.toPrettyString());
                            }
                        } else {
                            log.warn("No Result Array received in DTR lookup response: \n" + jsonResponse.toPrettyString());
                        }
                    }
                }

            } finally {
                if (transferId != null) {
                    terminateTransfer(transferId);
                }
            }
        } catch (Exception e) {
            log.error("Error in AasSubmodelDescriptor Request for " + mpr + " and manufacturerPartId " + manufacturerPartId, e);
            return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
        } finally {
            if (criticalFailure) {
                log.warn("Invalidating DTR contract data");
                edcContractMappingService.putDtrContractData(partner, null, null);
            }
        }
        return getAasSubmodelDescriptors(manufacturerPartId, manufacturerId, mpr, --retries);
    }

    /**
     * Requests an EDR for the communication from edc
     * <p>
     * The edc already handles the expiry as configured in the provider data plane and refreshs the token before
     * answering.
     *
     * @param transferProcessId to get the EDR for
     * @return unpersisted EdrDto.
     */
    private EdrDto getEdrForTransferProcessId(String transferProcessId, int retries) {
        if (retries < 0) return null;
        boolean failed = true;
        try (Response response = sendGetRequest(
            List.of("v3", "edrs", transferProcessId, "dataaddress"),
            Map.of("auto_refresh", "true"))
        ) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode responseObject = objectMapper.readTree(response.body().string());

                String dataPlaneEndpoint = responseObject.get("endpoint").asText();
                String authToken = responseObject.get("authorization").asText();
                if (dataPlaneEndpoint != null && authToken != null) {
                    EdrDto edr = new EdrDto("Authorization", authToken, dataPlaneEndpoint);
                    log.debug("Requested EDR successfully: {}", edr);
                    failed = false;
                    return edr;
                }
            }
        } catch (Exception e) {
            log.error("EDR token for transfer process with ID {} could not be obtained", transferProcessId);
        } finally {
            if (failed && --retries >= 0) {
                try {
                    Thread.sleep(100);
                } catch (Exception e1) {
                    log.error("Sleep interrupted", e1);
                }
            }
        }
        return getEdrForTransferProcessId(transferProcessId, retries);

    }

    /**
     * Terminate the transfer with reason "Transfer done.
     *
     * @param transferProcessId to terminate
     */
    private void terminateTransfer(String transferProcessId) {

        JsonNode body = edcRequestBodyBuilder.buildTransferProcessTerminationBody("Transfer done.");

        try (Response response = sendPostRequest(body, List.of("v3", "transferprocesses", transferProcessId, "terminate"))) {

            JsonNode resultNode = objectMapper.readTree(response.body().string());
            if (!response.isSuccessful()) {
                log.error(
                    "Transfer process with id {} could not be termianted; status code {}, reason: {}",
                    transferProcessId,
                    response.code(),
                    resultNode.get("message").asText("MESSAGE NOT FOUND")
                );
            } else {
                log.info("Terminated transfer process with id {}.", transferProcessId);
            }
        } catch (IOException e) {
            log.error("Error while trying to terminate transfer: ", e);
        }
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

    private boolean negotiateContractForSubmodel(MaterialPartnerRelation mpr, AssetType type, DirectionCharacteristic direction) {
        Partner partner = mpr.getPartner();
        SubmodelData submodelData = switch (type) {
            case DTR -> throw new IllegalArgumentException("DTR not supported");
            case ITEM_STOCK_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID, direction);
            case PRODUCTION_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.PRODUCTION_SUBMODEL.URN_SEMANTIC_ID, direction);
            case DEMAND_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.DEMAND_SUBMODEL.URN_SEMANTIC_ID, direction);
            case DELIVERY_SUBMODEL -> fetchSubmodelDataByDirection(mpr, AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID, direction);
            case NOTIFICATION -> throw new IllegalArgumentException("DemandAndCapacityNotification not supported");
            case DAYS_OF_SUPPLY -> fetchSubmodelDataByDirection(mpr, AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID, direction);
            case PART_TYPE_INFORMATION_SUBMODEL -> fetchPartTypeSubmodelData(mpr);
        };
        Map<String, String> equalFilters = new HashMap<>();
        equalFilters.put(EdcRequestBodyBuilder.CX_COMMON_NAMESPACE + "version", "3.0");
        equalFilters.put(
            "'" + EdcRequestBodyBuilder.DCT_NAMESPACE + "type'.'@id'",
            EdcRequestBodyBuilder.CX_TAXO_NAMESPACE + "Submodel"
        );
        equalFilters.put("'" + EdcRequestBodyBuilder.AAS_SEMANTICS_NAMESPACE + "semanticId'.'@id'", type.URN_SEMANTIC_ID);
        return negotiateContract(partner, submodelData.assetId(), type, submodelData.dspUrl(), equalFilters);
    }

    public boolean negotiateContractForNotification(Partner partner, AssetType type) {
        Map<String, String> equalFilters = new HashMap<>();
        equalFilters.put(EdcRequestBodyBuilder.CX_COMMON_NAMESPACE + "version", "1.0");
        equalFilters.put(
            "'" + EdcRequestBodyBuilder.DCT_NAMESPACE + "type'.'@id'",
            EdcRequestBodyBuilder.CX_TAXO_NAMESPACE + "DemandAndCapacityNotificationApi"
        );
        return negotiateContract(partner, variablesService.getNotificationApiAssetId(), type, partner.getEdcUrl(), equalFilters);
    }

    public boolean negotiateContract(Partner partner, String assetId, AssetType type, String dspUrl, Map<String, String> equalFilters) {
        try {
            var responseNode = getCatalog(dspUrl, partner.getBpnl(), equalFilters);
            responseNode = jsonLdUtils.expand(responseNode);
            var catalogArray = responseNode.get(EdcRequestBodyBuilder.DCAT_NAMESPACE + "dataset");
            // If there is exactly one asset, the catalogContent will be a JSON object.
            // In all other cases catalogContent will be a JSON array.
            // For the sake of uniformity we will embed a single object in an array.
            if (catalogArray.isObject()) {
                catalogArray = objectMapper.createArrayNode().add(catalogArray);
            }
            JsonNode targetCatalogEntry = null;
            if (!catalogArray.isEmpty()) {
                if (catalogArray.size() > 1) {
                    log.debug("Muliple contract offers found! Will take the first with supported policy \n" + catalogArray.toPrettyString());
                }

                for (JsonNode entry : catalogArray) {
                    if (testContractPolicyConstraints(entry)) {
                        targetCatalogEntry = entry;
                        break;
                    } else {
                        log.info(
                            "Contract offer did not match Framework Policy {} and Contract Policy {}:\n{}",
                            variablesService.getPurisFrameworkAgreementWithVersion(),
                            variablesService.getPurisPurposeWithVersion(),
                            entry.toPrettyString()
                        );
                    }
                }
            }

            if (targetCatalogEntry == null) {
                log.error("Could not find asset for " + type + " at partner " + partner.getBpnl() + "'s catalog");
                log.warn("CATALOG CONTENT \n" + catalogArray.toPrettyString());
                return false;
            }
            JsonNode negotiationResponse = initiateNegotiation(partner, targetCatalogEntry, dspUrl);
            String negotiationId = negotiationResponse.get("@id").asText();
            // Await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var responseObject = getNegotiationState(negotiationId);
                if ("FINALIZED".equals(responseObject.get("state").asText())) {
                    contractId = responseObject.get("contractAgreementId").asText();
                    break;
                }
            }
            if (contractId == null) {
                var negotiationState = getNegotiationState(negotiationId);
                log.error("Failed to obtain " + type + " from " + partner.getEdcUrl() + ", last negotiation state: \n"
                    + negotiationState.toPrettyString());
                return false;
            }
            log.info("Putting new ContractId" + contractId + "for " + type + " api with partner " + partner.getBpnl());
            edcContractMappingService.putContractId(partner, type, assetId, dspUrl, contractId);
            log.info("Got contract for " + type + " api with partner " + partner.getBpnl());
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
        var data = getSubmodelFromPartner(mpr, AssetType.PART_TYPE_INFORMATION_SUBMODEL, null, 1);
        return data.get("catenaXId").asText();
    }

    /**
     * Helper method to check whether you and the contract offer from the other party have the
     * same framework agreement policy. The given catalogEntry must be expanded!
     *
     * @param catalogEntry the catalog item containing the desired api asset in expanded form
     * @return true, if the policy matches yours, otherwise false
     */
    public boolean testContractPolicyConstraints(JsonNode catalogEntry) {
        log.debug("Testing constraints in the following catalogEntry: \n{}", catalogEntry.toPrettyString());
        var constraint = Optional.ofNullable(catalogEntry.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "hasPolicy"))
            .filter(policy -> policy.isArray() && policy.size() == 1)
            .map(policy -> policy.get(0))
            .map(policy -> policy.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "permission"))
            .filter(permission -> permission.isArray() && permission.size() == 1)
            .map(permission -> permission.get(0))
            .map(permission -> permission.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "constraint"))
            .filter(constr -> constr.isArray() && constr.size() == 1)
            .map(constr -> constr.get(0))
            .map(con -> con.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "and"));
        if (constraint.isEmpty()) {
            log.debug("Constraint mismatch: we expect to have a constraint in permission node.");
            return false;
        }

        for (String rule : new String[] {"obligation", "prohibition"}) {
            var policy = catalogEntry.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "hasPolicy").get(0);
            var ruleNode = policy.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + rule);
            boolean test = ruleNode == null || (ruleNode.isArray() && ruleNode.isEmpty());
            if (!test) {
                log.warn("Unexpected {} found, rejecting: {}", rule, catalogEntry.toPrettyString());
                return false;
            }
        }

        boolean result = true;

        if (constraint.get().isArray() && constraint.get().size() == 2) {
            Optional<JsonNode> frameworkAgreementConstraint = Optional.empty();
            Optional<JsonNode> purposeConstraint = Optional.empty();

            for (JsonNode con : constraint.get()) { // Iterate over array elements and find the nodes
                JsonNode leftOperandNode = con.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "leftOperand");
                leftOperandNode = leftOperandNode.get(0);
                leftOperandNode = leftOperandNode.get("@id");
                if (leftOperandNode != null && (EdcRequestBodyBuilder.CX_POLICY_NAMESPACE + "FrameworkAgreement").equals(leftOperandNode.asText())) {
                    frameworkAgreementConstraint = Optional.of(con);
                }
                if (leftOperandNode != null && (EdcRequestBodyBuilder.CX_POLICY_NAMESPACE + "UsagePurpose").equals(leftOperandNode.asText())) {
                    purposeConstraint = Optional.of(con);
                }
            }

            if (frameworkAgreementConstraint.isEmpty() || purposeConstraint.isEmpty()) {
                log.debug(
                    "Not all constraints have been found: FrameworkAgreement constraint found: {}, " +
                        "UsagePurpose constraint found: {}",
                    frameworkAgreementConstraint.isPresent(),
                    purposeConstraint.isPresent()
                );
                return false;
            }

            result = result && testSingleConstraint(
                frameworkAgreementConstraint,
                EdcRequestBodyBuilder.CX_POLICY_NAMESPACE + "FrameworkAgreement",
                EdcRequestBodyBuilder.ODRL_NAMESPACE + "eq",
                variablesService.getPurisFrameworkAgreementWithVersion()
            );

            result = result && testSingleConstraint(
                purposeConstraint,
                EdcRequestBodyBuilder.CX_POLICY_NAMESPACE + "UsagePurpose",
                EdcRequestBodyBuilder.ODRL_NAMESPACE + "eq",
                variablesService.getPurisPurposeWithVersion()
            );

            JsonNode policy = catalogEntry.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "hasPolicy");
            JsonNode prohibition = policy.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "prohibition");
            JsonNode obligation = policy.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "obligation");
            result = result && (prohibition == null || (prohibition.isArray() && prohibition.isEmpty()));
            result = result && (obligation == null || (obligation.isArray() && obligation.isEmpty()));

        } else {
            log.info(
                "2 Constraints (Framework Agreement, Purpose) are expected but got {} constraints.",
                constraint.get().size()
            );
            return false;
        }

        return result;
    }

    private boolean testSingleConstraint(Optional<JsonNode> constraintToTest, String targetLeftOperand, String targetOperator, String targetRightOperand) {

        if (constraintToTest.isEmpty()) return false;

        JsonNode con = constraintToTest.get();

        JsonNode leftOperandNode = con.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "leftOperand");
        leftOperandNode = leftOperandNode == null ? null : leftOperandNode.get(0);
        leftOperandNode = leftOperandNode == null ? null : leftOperandNode.get("@id");
        if (leftOperandNode == null || !targetLeftOperand.equals(leftOperandNode.asText())) {
            String leftOperand = leftOperandNode == null ? "null" : leftOperandNode.asText();
            log.info("Left operand '{}' does not equal expected value '{}'.", leftOperand, targetLeftOperand);
            return false;
        }

        JsonNode operatorNode = con.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "operator");
        operatorNode = operatorNode == null ? null : operatorNode.get(0);
        operatorNode = operatorNode == null ? null : operatorNode.get("@id");
        if (operatorNode == null || !targetOperator.equals(operatorNode.asText())) {
            String operator = operatorNode == null ? "null" : operatorNode.asText();
            log.info("Operator '{}' does not equal expected value '{}'.", operator, targetOperator);
            return false;
        }

        JsonNode rightOperandNode = con.get(EdcRequestBodyBuilder.ODRL_NAMESPACE + "rightOperand");
        rightOperandNode = rightOperandNode == null ? null : rightOperandNode.get(0);
        rightOperandNode = rightOperandNode == null ? null : rightOperandNode.get("@value");
        if (rightOperandNode == null || !targetRightOperand.equals(rightOperandNode.asText())) {
            String rightOperand = rightOperandNode == null ? "null" : rightOperandNode.asText();
            log.info("Right operand '{}' does not equal expected value '{}'.", rightOperand, targetRightOperand);
            return false;
        }

        log.info("Contract Offer constraints can be fulfilled by PURIS FOSS application (passed).");

        return true;
    }
}
