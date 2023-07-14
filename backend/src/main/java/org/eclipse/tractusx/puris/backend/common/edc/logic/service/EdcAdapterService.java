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
package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.okhttp.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.CreateAssetDto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EDCRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.model.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Layer of EDC Adapter. Builds and sends requests to a productEDC.
 * The EDC connection is configured using the application.properties file.
 */
@Service
@Slf4j
public class EdcAdapterService {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    @Autowired
    private OrderRepository orderRepository;

    @Value("${edc.controlplane.host}")
    private String edcHost;

    @Value("${edc.controlplane.data.port}")
    private Integer dataPort;

    @Value("${edc.backend.url}")
    private String backendUrl;

    @Value("${edc.controlplane.key}")
    private String edcApiKey;

    @Value("${server.port}")
    private String serverPort;

    @Value("${edc.dataplane.public.port}")
    private String dataplanePort;

    @Value("${minikube.ip}")
    private String minikubeIp;

    @Value("${request.serverendpoint}")
    private String requestServerEndpointURL;

    @Value("${response.serverendpoint}")
    private String responseServerEndpointURL;

    private ObjectMapper objectMapper;

    @Autowired
    private EDCRequestBodyBuilder edcRequestBodyBuilder;

    @Autowired
    private AuthCodeService authCodeService;

    public EdcAdapterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Publish an order at own EDC.
     *
     * @param orderId id of the order to publish.
     * @return true, if order was published.
     * @throws IOException if the connection to the EDC failed.
     */
    public boolean publishOrderAtEDC(String orderId) throws IOException {
        var order = orderRepository.findByOrderId(orderId);
        if (order.isPresent()) {
            var orderUrl = "http://" + minikubeIp + ":" + serverPort + "/catena/orders/order/id/" + orderId;
            var assetBody = edcRequestBodyBuilder.buildAssetRequestBody(orderUrl, orderId);
            var policyBody = edcRequestBodyBuilder.buildPolicyRequestBody(orderId);
            var contractBody = edcRequestBodyBuilder.buildContractRequestBody(orderId);
            var success = sendEdcRequest(assetBody, "/data/assets").isSuccessful();
            success &= sendEdcRequest(policyBody, "/data/policydefinitions").isSuccessful();
            success &= sendEdcRequest(contractBody, "/data/contractdefinitions").isSuccessful();
            return success;
        }
        return false;
    }

    /**
     * Publish an Asset (ContractDefinition) using an {@link CreateAssetDto} with a public policy.
     *
     * @param createAssetDto asset creation dto to use.
     * @return true, if ContractDefinition has been created successfully
     * @throws IOException   if REST calls for creation could not be sent
     * @throws JSONException if createAssetDto could not be parsed into JsonNode
     */
    public boolean publishAssetAtEDC(CreateAssetDto createAssetDto) throws IOException {

        String assetId = createAssetDto.getAssetDto().getPropertiesDto().getId();

        boolean success = true;
        JsonNode assetBody = objectMapper.valueToTree(createAssetDto);
        JsonNode policyBody =
                edcRequestBodyBuilder.buildPolicyRequestBody(assetId);
        log.info(String.format("Policy Body: %s", policyBody.asText()));
        JsonNode contractBody = edcRequestBodyBuilder.buildContractRequestBody(assetId);
        log.info(String.format("Contract Body: %s", contractBody.asText()));

        success = success && sendEdcRequest(assetBody, "/data/assets").isSuccessful();
        log.info(String.format("Creation of asset was successfull: %b", success));
        success = success && sendEdcRequest(policyBody, "/data/policydefinitions").isSuccessful();
        log.info(String.format("Creation of policy was successfull: %b", success));
        success = success && sendEdcRequest(contractBody, "/data/contractdefinitions").isSuccessful();
        log.info(String.format("Created Contract Definition (%b) for Asset %s", success,
                objectMapper.writeValueAsString(createAssetDto)));

        return success;
    }


    /**
     * Get catalog from an EDC.
     *
     * @param idsUrl url of the EDC to get catalog from.
     * @return catalog of the requested EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String getCatalog(String idsUrl) throws IOException {
        return getCatalog(idsUrl, Optional.empty());
    }

    /**
     * Get catalog from an EDC.
     *
     * @param idsUrl           url of the EDC to get catalog from.
     * @param filterProperties maps with key = asset property and value = filter value
     * @return catalog of the requested EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String getCatalog(String idsUrl, Optional<Map<String, String>> filterProperties) throws IOException {

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
        urlBuilder.scheme("http")
                .host(edcHost)
                .port(dataPort)
                .addPathSegment("data")
                .addPathSegment("catalog")
                .addEncodedQueryParameter("providerUrl", idsUrl + "/data");

        HttpUrl httpUrl = urlBuilder.build();

        // workaround EDC 0.3 takes filter=key=value, but HttpUrlBuilder encodes = to %3D
        // which is not recognized
        if (filterProperties.isPresent() && filterProperties.get().size() >= 1) {
            String url = urlBuilder.build().toString();

            for (Map.Entry<String, String> entry : filterProperties.get().entrySet()) {
                url = url + String.format("&filter=%s=%s", entry.getKey(), entry.getValue());
            }
            httpUrl = HttpUrl.parse(url);
        }
        log.debug(String.format("catalog request url: %s", httpUrl));

        var request = new Request.Builder()
                .get()
                .url(httpUrl)
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .build();
        var response = CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }
        return response.body().string();
    }

    public String startNegotiation(String connectorAddress,
                                   String contractDefinitionId, String assetId) throws IOException {
        var negotiationRequestBody =
                edcRequestBodyBuilder.buildNegotiationRequestBody(connectorAddress,
                        contractDefinitionId, assetId);
        var response = sendEdcRequest(negotiationRequestBody, "/data/contractnegotiations");

        return response.body().string();
    }

    public String getNegotiationState(String negotiationId) throws IOException {
        var response = sendEdcRequest("/data/contractnegotiations/" + negotiationId);
        return response.body().string();
    }

    /**
     * Start a data transfer with another EDC.
     *
     * @param transferId       id created for the transferprocess.
     * @param connectorAddress ids url of the transfer counterparty.
     * @param contractId       id of the negotiated contract.
     * @param orderId          id of the transfers target asset.
     * @return response body received from the EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String startTransfer(String transferId,
                                String connectorAddress,
                                String contractId,
                                String orderId) throws IOException {
        var transferNode = edcRequestBodyBuilder.buildTransferRequestBody(transferId, connectorAddress, contractId, orderId);
        log.debug("TransferRequestBody:\n" + transferNode.toPrettyString());
        var response = sendEdcRequest(transferNode, "/data/transferprocess");
        return response.body().string();
    }

    public String getTransferState(String transferId) throws IOException {
        var response = sendEdcRequest("/data/transferprocess/" + transferId);
        return response.body().string();
    }

    /**
     * Delete an asset from the own EDC.
     *
     * @param assetId id of the asset to delete.
     * @return response body received from the EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String deleteAsset(String assetId) throws IOException {
        var urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(edcHost)
                .port(dataPort);
        urlBuilder.addPathSegment("data");
        urlBuilder.addPathSegment("assets");
        urlBuilder.addPathSegment(assetId);
        var url = urlBuilder.build();
        var request = new Request.Builder()
                .url(url)
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .delete()
                .build();
        var response = CLIENT.newCall(request).execute();
        return response.body().string();
    }

    /**
     * Send a GET request to the own EDC.
     *
     * @param resourceId   (optional) id of the resource to request, will be left empty if null.
     * @param pathSegments varargs for the path segments of the request
     *                     (e.g "data", "assets" will be turned to /data/assets).
     * @return response body received from the EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String getFromEdc(String resourceId, String... pathSegments) throws IOException {
        var urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(edcHost)
                .port(dataPort);
        for (var seg : pathSegments) {
            urlBuilder.addPathSegment(seg);
        }
        if (resourceId != null) {
            urlBuilder.addPathSegment(resourceId);
        }
        var url = urlBuilder.build();
        var request = new Request.Builder()
                .get()
                .url(url)
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .build();
        var response = CLIENT.newCall(request).execute();
        return response.body().string();
    }

    /**
     * Util method for building a http POST request to the own EDC.
     *
     * @param requestBody requestBody to be sent to the EDC.
     * @param urlSuffix   path to POST data to
     * @return response received from the EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public Response sendEdcRequest(JsonNode requestBody, String urlSuffix) throws IOException {
        Request request = new Request.Builder()
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .url("http://" + edcHost + ":" + dataPort + urlSuffix)
                .build();

        log.debug(String.format("Request body of EDC Request: %s", requestBody));
        return CLIENT.newCall(request).execute();
    }

    public Response sendEdcRequest(String urlSuffix) throws IOException {
        Request request = new Request.Builder()
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .url("http://" + edcHost + ":" + dataPort + urlSuffix)
                .build();
        log.debug(String.format("Send Request to url: %s", request.urlString()));

        return CLIENT.newCall(request).execute();
    }

    public Response sendDataPullRequest(String url, String authCode, String requestBodyString){
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), requestBodyString);
            Request request = new Request.Builder()
                            .url(url)
                            .header("Authorization", authCode)
                            .post(requestBody)
                            .build();
            return CLIENT.newCall(request).execute();
        } catch (Exception e){
            log.error(url, e);
            throw new RuntimeException(e);
        }
        
    }

    /**
     * Tries to negotiate for the request api of the given partner, including the retrieval of the
     * authCode for a request. It will return a String array of length 2. The authCode is stored under 
     * index 0, the contractId under index 1. 
     * 
     * @param partnerIdsUrl
     * @return a String array or null, if negotiation or transfer have failed or the authCode did not arrive
     */
    public String[] getContractForRequestApi(String partnerIdsUrl) {
        return getContractForRequestOrResponseApiApi(partnerIdsUrl, "product-stock-request-api");
    }

    /**
     * Tries to negotiate for the response api of the given partner, including the retrieval of the
     * authCode for a request. It will return a String array of length 2. The authCode is stored under 
     * index 0, the contractId under index 1. 
     * @param partnerIdsUrl
     * @return a String array or null, if negotiation or transfer have failed or the authCode did not arrive
     */
    public String[] getContractForResponseApi(String partnerIdsUrl) {
        return getContractForRequestOrResponseApiApi(partnerIdsUrl, "product-stock-response-api");
    }

    /**
     * Tries to negotiate for the given api of the partner specified by the parameter
     * and also tries to initiate the transfer of the authCode token to the given endpoint. 
     * It will return a String array of length 2. The authCode is stored under index 0, the 
     * contractId under index 1. 
     * @param partnerIdsUrl counterparty's idsUrl
     * @param assetApi      id of the target asset
     * @return   a String array or null, if negotiation or transfer have failed or the authCode did not arrive
     */
    public String[] getContractForRequestOrResponseApiApi(String partnerIdsUrl, String assetApi) {
        try {
            HashMap<String, String> filter = new HashMap<>();
            filter.put("asset:prop:id", assetApi);
            String catalog = getCatalog(partnerIdsUrl, Optional.of(filter));
            JsonNode objectNode = objectMapper.readValue(catalog, ObjectNode.class);
            JsonNode contractOffer = objectNode.get("contractOffers").get(0);
            String contractDefinitionId = contractOffer.get("id").asText();
            String negotiationResponseString = startNegotiation(partnerIdsUrl + "/data", contractDefinitionId, assetApi);
            String negotiationId = objectMapper.readTree(negotiationResponseString).get("id").asText();

            // await confirmation of contract and contractId
            String contractId = null;
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                var negotiationState = getNegotiationState(negotiationId);
                var responseObject = objectMapper.readTree(negotiationState);
                if ("CONFIRMED".equals(responseObject.get("state").asText())) {
                    contractId = responseObject.get("contractAgreementId").asText();
                    break;
                }
            }
            if (contractId == null) {
                var negotiationState = getNegotiationState(negotiationId);
                log.warn("no contract id, last negotiation state: " + negotiationState);
                log.warn("Failed to obtain " + assetApi + " from " + partnerIdsUrl);
                return null;
            }

            // Initiate transfer of authCode
            String randomTransferID = UUID.randomUUID().toString();
            String transferResponse = startTransfer(randomTransferID, partnerIdsUrl + "/data", contractId, assetApi);
            String transferId = objectMapper.readTree(transferResponse).get("id").asText();
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                transferResponse = getTransferState(transferId);
                var transferResponseObject = objectMapper.readTree(transferResponse);
                if ("COMPLETED".equals(transferResponseObject.get("state").asText())) {
                    break;
                }
            }

            // await arrival of authCode ...
            for (int i = 0; i < 100; i++) {
                Thread.sleep(100);
                String authCode = authCodeService.findByTransferId(randomTransferID);
                if (authCode != null) {
                    log.info("Successfully negotiated for " + assetApi + " with " + partnerIdsUrl);
                    return new String [] {authCode, contractId};
                }
            }
            log.warn("did not receive authCode");
            log.warn("Failed to obtain " + assetApi + " from " + partnerIdsUrl);
            return null;
        } catch (Exception e){
            log.warn("ERROR");
            log.error("Failed to obtain " + assetApi + " from " + partnerIdsUrl, e);
            return null;
        }

    }

}