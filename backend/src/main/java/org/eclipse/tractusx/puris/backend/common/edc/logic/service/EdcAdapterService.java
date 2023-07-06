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
import org.eclipse.tractusx.puris.backend.stock.controller.DataPullController;
import org.eclipse.tractusx.puris.backend.stock.logic.service.DatapullAuthCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
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
    private DatapullAuthCodeService datapullAuthCodeService;

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

    public boolean publishRequestAndResponseAssetAtEDC(String serverOwnerId) throws IOException {

        String[] assetIds = {"asset.properties.asset:request-api." + serverOwnerId,
                "asset.properties.asset:response-api." + serverOwnerId};
        String[] serverURLs = {requestServerEndpointURL, responseServerEndpointURL};
        boolean success = true;
        for (int i = 0; i < 2; i++) {
            String assetId = assetIds[i];
            String endpointURL = serverURLs[i];
            var assetBody = edcRequestBodyBuilder.buildAssetRequestBody(endpointURL, assetId);
            var policyBody = edcRequestBodyBuilder.buildPolicyRequestBody(assetId);
            var contractBody = edcRequestBodyBuilder.buildContractRequestBody(assetId);
            success = success && sendEdcRequest(assetBody, "/data/assets").isSuccessful();
            success = success && sendEdcRequest(policyBody, "/data/policydefinitions").isSuccessful();
            success = success && sendEdcRequest(contractBody, "/data/contractdefinitions").isSuccessful();
            log.info(endpointURL);
        }
        return success;
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
        var request = new Request.Builder()
                .get()
                .url(new HttpUrl.Builder()
                        .scheme("http")
                        .host(edcHost)
                        .port(dataPort)
                        .addPathSegment("data")
                        .addPathSegment("catalog")
                        .addEncodedQueryParameter("providerUrl", idsUrl)
                        .build()
                )
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .build();
        var response = CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }
        return response.body().string();
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
        log.info(String.format("catalog request url: %s", httpUrl));

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

    /**
     * Get received data from a transfer from edc backend application.
     *
     * @param transferId id of the transfer.
     * @return received data from transfer.
     * @throws IOException if the connection to the backend application failed.
     */
    public String getFromBackend(String transferId) throws IOException {
        var request = new Request.Builder()
                .get()
                .url(new URL(String.format("%s/%s", backendUrl, transferId)))
                .header("Accept", "application/octet-stream")
                .build();
        log.info(String.format("BackendUrl: %s", request.urlString()));
        var response = CLIENT.newCall(request).execute();
        return response.body().string();
    }

    /**
     * Start a negotitation with another EDC.
     *
     * @param connectorAddress ids url of the negotiation counterparty.
     * @param orderId          id of the negotiations target asset.
     * @return response body received from the EDC.
     * @throws IOException if the connection to the EDC failed.
     */
    public String startNegotiation(String connectorAddress, String orderId) throws IOException {
        var negotiationRequestBody = edcRequestBodyBuilder.buildNegotiationRequestBody(connectorAddress, orderId);
        var response = sendEdcRequest(negotiationRequestBody, "/data/contractnegotiations");

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
        log.info("TransferRequestBody:\n" + transferNode.toPrettyString());
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

        log.info(String.format("Request body of EDC Request: %s", requestBody));
        return CLIENT.newCall(request).execute();
    }

    public Response sendEdcRequest(String urlSuffix) throws IOException {
        Request request = new Request.Builder()
                .header("X-Api-Key", edcApiKey)
                .header("Content-Type", "application/json")
                .url("http://" + edcHost + ":" + dataPort + urlSuffix)
                .build();
        log.info(String.format("Send Request to url: %s", request.urlString()));

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
     * 
     * @param partnerIdsUrl
     * @param partnersAssetId
     * @param filterProperties
     * @return the authCode of a consumer pull request
     */
    public String initializeProxyCall(String partnerIdsUrl,
                                      String partnersAssetId, Map<String,
            String> filterProperties) {

        String catalog = null;
        ObjectNode catalogNode = null;
        try {
            catalog = getCatalog(partnerIdsUrl, Optional.of(filterProperties));
            catalogNode = objectMapper.readValue(catalog, ObjectNode.class);

            log.info(String.format("Got catalog for idsUrl %s with filters %s: %s", partnerIdsUrl
                    , filterProperties, catalog));
        } catch (IOException e) {
            // quickfix: don´t persist request
            //requestDto.setState(DT_RequestStateEnum.ERROR);

            //correspondingRequest = requestService.updateState(correspondingRequest,
            //        DT_RequestStateEnum.ERROR);

            log.error(String.format("Catalog for %s could not be reached.", partnerIdsUrl));
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        // we expect only one offer for us
        log.info(String.format("Received catalog: %s", catalog));
        log.info(String.format("Catalog: %s", catalogNode.get("contractOffers").toString()));
        JsonNode contractOfferJson = catalogNode.get("contractOffers").get(0);
        String contractDefinitionId = contractOfferJson.get("id").asText();

        try {
            String negotiationResponseString =
                    startNegotiation(partnerIdsUrl + "/data",
                            contractDefinitionId,
                            partnersAssetId);

            ObjectNode negotiationResponse = objectMapper.readValue(negotiationResponseString, ObjectNode.class);
            log.info(String.format("Negotiation Response answer: %s",
                    negotiationResponse.toString()));

            String negotiationId = negotiationResponse.get("id").asText();
            log.info(String.format("Contract Id: %s", negotiationId));


            boolean negotiationDone = false;
            String negotiationStateResultString = null;
            ObjectNode negotiationStateResult = null;
            do {
                Thread.sleep(2000);
                negotiationStateResultString = getNegotiationState(negotiationId);
                negotiationStateResult = objectMapper.readValue(negotiationStateResultString, ObjectNode.class);

                log.info(String.format("Negotiation State answer: %s",
                        negotiationStateResult.toString()));

                if (negotiationStateResult.get("state").asText().equals("CONFIRMED")) {
                    negotiationDone = true;
                } else if (negotiationStateResult.get("state").asText().equals("ERROR")) {
                    throw new RuntimeException(String.format("Negotiation Result: Error for " +
                            "Negotiation ID %S", negotiationId));
                } else if (negotiationStateResult.get("state").asText().equals("DECLINED")) {
                    throw new RuntimeException(String.format("Negotiation Result: DECLINED for " +
                            "Negotiation ID %S", negotiationId));
                }

            } while (!negotiationDone);

            String contractAgreementId = negotiationStateResult.get("contractAgreementId").asText();
            log.info(String.format("Contract Agreement ID: %s", contractAgreementId));

            String transferId = UUID.randomUUID().toString();
            String transferResultString = startTransfer(transferId,
                    partnerIdsUrl + "/data",
                    contractAgreementId,
                    partnersAssetId);

            ObjectNode transferResult = objectMapper.readValue(transferResultString, ObjectNode.class);
            log.info(String.format("Init Transfer answer: %s",
                    transferResult.toString()));

            String transferResponseId = transferResult.get("id").asText();
            log.info(String.format("Transfer ID created by me = %s ; transferResponseId = %s",
                    transferId, transferResponseId));

            boolean transferCompleted = false;
            do {
                log.info(String.format("Check State of transfer"));
                String transferAnswer = getTransferState(transferResponseId);
                log.info(transferAnswer);
                ObjectNode transferState = objectMapper.readValue(transferAnswer,
                        ObjectNode.class);
                // quickfix. String concatenation needed fo impl. cast
                String state = transferState.get("state").asText();
                log.info(String.format("Current State: %s", state));

                if (state.equals("COMPLETED")) {
                    transferCompleted = true;
                } else if (state.equals("ERROR")) {
                    throw new RuntimeException("Transfer failed");
                }
                Thread.sleep(5000);
            } while (!transferCompleted);


            String edr = null;
            for (int i = 0 ; i < 4; i++) {
                Thread.sleep(500);
                log.info(String.format("Query Response Endpoint"));
                var searchResult = datapullAuthCodeService.findByTransferId(transferId);
                if (searchResult.isPresent()) {
                    edr = datapullAuthCodeService.findByTransferId(transferId).get().getAuthCode();
                    break;
                }
            } 
            
            log.info(String.format("Backend Application answer: %s", edr));
            return edr;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            // quickfix: don´t persist request
            //requestDto.setState(DT_RequestStateEnum.ERROR);
            /*
            requestService.updateState(correspondingRequest, DT_RequestStateEnum.ERROR);
             */
            throw new RuntimeException(e);
        }
    }

}