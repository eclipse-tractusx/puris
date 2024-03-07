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
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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

    private Response sendDtrPostRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl() + "/api/v3.0").newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
            .post(body)
            .url(urlBuilder.build())
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
        log.info("Registration of item-stock request api successful " + (result = registerApiAsset(DT_ApiMethodEnum.REQUEST)));
        if (!result) return false;
        log.info("Registration of item-stock response api successful " + (result = registerApiAsset(DT_ApiMethodEnum.RESPONSE)));
        if (!result) return false;
        log.info("Registration of item-stock status-request api successful " + (result = registerApiAsset(DT_ApiMethodEnum.STATUS_REQUEST)));
        if (!result) return false;
        if (variablesService.isUseFrameworkPolicy()) {
            log.info("Registration of framework agreement policy successful " + (result = createFrameWorkPolicy()));
            if (!result) return false;
        } else {
            log.info("Skipping registration of framework agreement policy");
        }
        log.info("Registration of DTR Asset successful " + (result = registerDtrAsset()));
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
        result &= createDtrContractDefinitionForPartner(partner);
        result &= createContractDefinitionForPartner(partner, DT_ApiMethodEnum.REQUEST);
        result &= createContractDefinitionForPartner(partner, DT_ApiMethodEnum.STATUS_REQUEST);
        return result & createContractDefinitionForPartner(partner, DT_ApiMethodEnum.RESPONSE);
    }

    /**
     * Registers a contract definition using a policy that
     * allows only the given partner's BPNL for the given Api Method.
     * Prior to this method you must always have successfully completed the
     * createPolicyDefinitionForPartner - method first.
     *
     * @param partner   the partner
     * @param apiMethod the api method
     * @return true, if registration ran successfully
     */
    private boolean createContractDefinitionForPartner(Partner partner, DT_ApiMethodEnum apiMethod) {
        var body = edcRequestBodyBuilder.buildContractDefinitionWithBpnRestrictedPolicy(partner, apiMethod);
        try (var response = sendPostRequest(body, List.of("v2", "contractdefinitions"))) {
            if (!response.isSuccessful()) {
                log.warn("Contract definition registration failed for partner " + partner.getBpnl() + " and "
                    + apiMethod);
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Contract definition registration failed for partner " + partner.getBpnl() + " and " + apiMethod);
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
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register DTR Asset", e);
            return false;
        }
    }

    /**
     * Util method to register an API asset to your control plane.
     *
     * @param apiMethod the api method to register.
     * @return true if successful.
     */
    private boolean registerApiAsset(DT_ApiMethodEnum apiMethod) {
        var body = edcRequestBodyBuilder.buildCreateItemStockAssetBody(apiMethod);
        try (var response = sendPostRequest(body, List.of("v3", "assets"))) {
            if (!response.isSuccessful()) {
                log.warn("Asset registration failed");
                if (response.body() != null) {
                    log.warn("Response: \n" + response.body().string());
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to register api asset " + apiMethod.CX_TAXO, e);
            return false;
        }
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
        try (var response = sendPostRequest(edcRequestBodyBuilder.buildBasicCatalogRequestBody(dspUrl, null), List.of("v2", "catalog", "request"))) {
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
    public String getAllNegotiations() throws IOException {
        var requestBody = edcRequestBodyBuilder.buildNegotiationsRequestBody();
        try (var response = sendPostRequest(requestBody, List.of("v2", "contractnegotiations", "request"))) {
            return response.body().string();
        }
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
    public JsonNode initiateProxyPullTransfer(Partner partner, String contractId, String assetId) throws IOException {
        var body = edcRequestBodyBuilder.buildProxyPullRequestBody(partner, contractId, assetId);
        try (var response = sendPostRequest(body, List.of("v2", "transferprocesses"))) {
            String data = response.body().string();
            return objectMapper.readTree(data);
        }
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
     * @return The response body as String
     * @throws IOException If the connection to your control plane fails
     */
    public String getAllTransfers() throws IOException {
        var requestBody = edcRequestBodyBuilder.buildTransfersRequestBody();
        try (var response = sendPostRequest(requestBody, List.of("v2", "transferprocesses", "request"))) {
            String data = response.body().string();
            return data;
        }
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

    /**
     * Tries to negotiate for the given api Method with the given partner
     * and also tries to initiate the transfer of the edr token to the given endpoint.
     * <p>
     * It will return a String array of length 4. The authKey is stored under index 0, the
     * authCode under index 1, the endpoint under index 2 and the contractId under index 3.
     *
     * @param partner   the partner
     * @param apiMethod the api method
     * @return A String array or null, if negotiation or transfer have failed or the authCode did not arrive
     */
    public String[] getContractForItemStockApi(Partner partner, DT_ApiMethodEnum apiMethod) {
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
                    if (("https://w3id.org/catenax/taxonomy#" + apiMethod.CX_TAXO).equals(dctTypeObject.get("@id").asText())) {
                        if (apiMethod.TYPE.equals(entry.get("asset:prop:type").asText())) {
                            if ("1.0".equals(entry.get("https://w3id.org/catenax/ontology/common#version").asText())) {
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
            if (targetCatalogEntry == null) {
                log.error("Could not find api asset " + apiMethod + " at partner " + partner.getBpnl() + " 's catalog");
                return null;
            }
            String assetApiId = targetCatalogEntry.get("@id").asText();
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
                log.error("Failed to obtain " + assetApiId + " from " + partner.getEdcUrl());
                return null;
            }

            // Initiate transfer of edr
            var transferResp = initiateProxyPullTransfer(partner, contractId, assetApiId);
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
                    log.info("Successfully negotiated for " + assetApiId + " with " + partner.getEdcUrl());
                    return new String[]{edr_Dto.authKey(), edr_Dto.authCode(), edr_Dto.endpoint(), contractId};
                }
            }
            log.warn("did not receive authCode");
            log.error("Failed to obtain " + assetApiId + " from " + partner.getEdcUrl());
            return null;

        } catch (Exception e) {
            log.error("Failed to get contract for " + apiMethod + " from " + partner.getBpnl(), e);
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
