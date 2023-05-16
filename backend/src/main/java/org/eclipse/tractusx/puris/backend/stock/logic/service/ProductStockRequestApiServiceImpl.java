/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.controller.exception.RequestIdNotFoundException;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentErrorDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestApiService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiBusinessObjectEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_AssetTypeEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service implements the handling of a request for Product Stock
 * <p>
 * That means that one need to lookup
 * {@link org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock} and return it
 * according to the API speicfication.
 */
@Component
@Slf4j
public class ProductStockRequestApiServiceImpl implements RequestApiService {

    @Autowired
    private RequestService requestService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private ModelMapper modelMapper;

    private ObjectMapper objectMapper;

    public ProductStockRequestApiServiceImpl(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public void handleRequest(RequestDto requestDto) {

        log.info(String.format("param requestDto %s", requestDto));

        // quickfix: don´t persist request
        requestDto.setState(DT_RequestStateEnum.WORKING);
        /*
        //request has been created on post
        Request correspondingRequest = requestService.findByInternalUuid(requestDto.getUuid());

        // as soon as we're working on it, we need to set the state to working.
        log.info(String.format("found correspondingRequest %s", correspondingRequest));
        correspondingRequest = requestService.updateState(correspondingRequest, DT_RequestStateEnum.WORKING);
        */
        // determine Asset for partners Response API
        Map<String, String> filterProperties = new HashMap<>();
        // use shortcut with headers.responseAssetId, if given
        if (requestDto.getHeader().getRespondAssetId() != null) {
            filterProperties.put("asset:prop:id", requestDto.getHeader().getRespondAssetId());
        } else {
            filterProperties.put("asset:prop:usecase", DT_UseCaseEnum.PURIS.name());
            filterProperties.put("asset:prop:type", DT_AssetTypeEnum.API.name());
            filterProperties.put("asset:prop:apibusinessobject", DT_ApiBusinessObjectEnum.productStock.name());
            filterProperties.put("asset:prop:apimethod", DT_ApiMethodEnum.RESPONSE.name());
        }
        String partnerIdsUrl = requestDto.getHeader().getSenderEdc();
        //Partner requestingPartner = partnerService.findByBpnl();

        // contains either productStockSamms or messageContentError
        List<MessageContentDto> resultProductStocks = new ArrayList<>();

        String requestingPartnerBpnl = requestDto.getHeader().getSender();

        for (ProductStockRequestForMaterialDto productStockRequestDto : requestDto.getPayload()) {

            //if (messageContentDto instanceof ProductStockRequestForMaterialDto) {

            //ProductStockRequestForMaterialDto productStockRequestDto =
            //        (ProductStockRequestForMaterialDto) messageContentDto;

            // TODO determine data
            // Check if product is known
            Material existingMaterial =
                    materialService.findProductByMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
            if (existingMaterial == null) {
                // TODO MessageContentError: Material unknown
                MessageContentErrorDto messageContentErrorDto = new MessageContentErrorDto();
                messageContentErrorDto.setMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
                messageContentErrorDto.setError("PURIS-01");
                messageContentErrorDto.setMessage("Material is unknown.");
                resultProductStocks.add(messageContentErrorDto);
                log.warn(String.format("No Material found for ID Customer %s in request %s",
                        productStockRequestDto.getMaterialNumberCustomer(),
                        requestDto.getHeader().getRequestId()));
                //continue;
            }
            boolean ordersProducts =
                    existingMaterial.getOrderedByPartners()
                            .stream().anyMatch(
                                    partner -> partner.getBpnl().equals(requestingPartnerBpnl));

            if (!ordersProducts) {
                // TODO MessageContentError: Partner is not authorized
                MessageContentErrorDto messageContentErrorDto = new MessageContentErrorDto();
                messageContentErrorDto.setMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
                messageContentErrorDto.setError("PURIS-02");
                messageContentErrorDto.setMessage("Partner is not authorized.");
                resultProductStocks.add(messageContentErrorDto);
                log.warn(String.format("Partner %s is not an ordering Partner of Material " +
                                "found for ID Customer %s in request %s",
                        requestingPartnerBpnl,
                        productStockRequestDto.getMaterialNumberCustomer(),
                        requestDto.getHeader().getRequestId()));
            }

            List<ProductStock> productStocks =
                    productStockService
                            .findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
                                    productStockRequestDto.getMaterialNumberCustomer(),
                                    requestingPartnerBpnl);

            ProductStock productStock = null;
            if (productStocks.size() == 0) {
                // TODO no partner product stock given
                continue;
            } else productStock = productStocks.get(0);

            //TODO: is this one allocated stock or multiple
            if (productStocks.size() > 1) {
                List<ProductStock> distinctProductStocks =
                        productStocks.stream()
                                .filter(distinctByKey(p -> p.getAtSiteBpnl()))
                                .collect(Collectors.toList());
                if (distinctProductStocks.size() > 1) {
                    log.warn(String.format("More than one site is not yet supported per " +
                                    "partner. Product Stocks for material ID %s and partner %s in " +
                                    "request %s are accumulated",
                            productStockRequestDto.getMaterialNumberCustomer(),
                            requestingPartnerBpnl, requestDto.getHeader().getRequestId()));
                }

                double quantity = productStocks.stream().
                        mapToDouble(stock -> stock.getQuantity()).sum();
                productStock.setQuantity(quantity);

            }

            resultProductStocks.add(productStockSammMapper.toSamm(modelMapper.map(productStock,
                    ProductStockDto.class)));
            /*
            } else
                throw new IllegalStateException(String.format("Message Content is unknown: %s",
                        messageContentDto));
            */
        }


        // TODO: Init Transfer
        // EDC 0.1.2 hardwire catalog
        String catalog = null;
        ObjectNode catalogNode = null;
        try {
            catalog = edcAdapterService.getCatalog(partnerIdsUrl, Optional.of(filterProperties));
            catalogNode = objectMapper.readValue(catalog, ObjectNode.class);

            log.info(String.format("Got catalog for idsUrl %s with filters %s: %s", partnerIdsUrl
                    , filterProperties, catalog));
        } catch (IOException e) {
            // quickfix: don´t persist request
            requestDto.setState(DT_RequestStateEnum.ERROR);

            //correspondingRequest = requestService.updateState(correspondingRequest,
            //        DT_RequestStateEnum.ERROR);

            log.error(String.format("Catalog for %s for partner %s for request (external =%s ; " +
                            "internal=%s) could not be reached.", partnerIdsUrl,
                    requestDto.getHeader().getSender(), requestDto.getHeader().getRequestId(),
                    requestDto.getUuid()));
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
                    edcAdapterService.startNegotiation(partnerIdsUrl + "/data",
                            contractDefinitionId,
                            requestDto.getHeader().getRespondAssetId());

            ObjectNode negotiationResponse = objectMapper.readValue(negotiationResponseString, ObjectNode.class);
            log.info(String.format("Negotiation Response answer: %s",
                    negotiationResponse.toString()));

            String contractId = negotiationResponse.get("id").asText();
            log.info(String.format("Contract Id: %s", contractId));


            /*
            String contractNegotiationId = negotiationResponse.get("contract_agreement_id").toString();
            log.info(String.format("Contract Negotiation Id for Response (Request ID %s): %s",
                    requestDto.getHeader().getRequestId(),
                    contractNegotiationId));
             */

            boolean negotiationDone = false;
            String negotiationStateResultString = null;
            ObjectNode negotiationStateResult = null;
            do {
                Thread.sleep(2000);
                negotiationStateResultString = edcAdapterService.getNegotiationState(contractId);
                negotiationStateResult = objectMapper.readValue(negotiationStateResultString, ObjectNode.class);

                log.info(String.format("Negotiation State answer: %s",
                        negotiationStateResult.toString()));

                if (negotiationStateResult.get("state").asText().equals("CONFIRMED")) {
                    negotiationDone = true;
                } else if (negotiationStateResult.get("state").asText().equals("ERROR")) {
                    throw new RuntimeException(String.format("Negotiation Result: Error for " +
                            "Negotiation ID %S", contractId));
                } else if (negotiationStateResult.get("state").asText().equals("DECLINED")) {
                    throw new RuntimeException(String.format("Negotiation Result: DECLINED for " +
                            "Negotiation ID %S", contractId));
                }

            } while (!negotiationDone);

            String contractAgreementId = negotiationStateResult.get("contractAgreementId").asText();
            log.info(String.format("Contract Agreement ID: %s", contractAgreementId));

            String transferId = UUID.randomUUID().toString();
            String transferResultString = edcAdapterService.startTransfer(transferId,
                    partnerIdsUrl + "/data",
                    contractId,
                    requestDto.getHeader().getRespondAssetId());

            ObjectNode transferResult = objectMapper.readValue(transferResultString, ObjectNode.class);
            log.info(String.format("Init Transfer answer: %s",
                    transferResult.toString()));

            String transferResponseId = transferResult.get("id").asText();
            log.info(String.format("Transfer ID created by me = %s ; transferResponseId = %s",
                    transferId, transferResponseId));

            boolean transferCompleted = false;
            do {
                log.info(String.format("Check State of transfer"));
                String transferAnswer = edcAdapterService.getTransferState(transferResponseId);
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
                Thread.sleep(2000);
            } while (!transferCompleted);


            boolean edrReceived = false;
            do {
                log.info(String.format("Query Backend Application"));
                String backendAnswer = edcAdapterService.getFromBackend(transferResponseId);
                log.info(String.format("Backend Application answer: %s", backendAnswer));
                Thread.sleep(2000);
            } while (!edrReceived);

        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            // quickfix: don´t persist request
            requestDto.setState(DT_RequestStateEnum.ERROR);
            /*
            requestService.updateState(correspondingRequest, DT_RequestStateEnum.ERROR);
             */
            throw new RuntimeException(e);
        }
        // TODO: Does a request need a response-contract-agreement id so that I can determine the
        //  http proxy EndpointDataReference?

        // TODO: async wait for answer of backend

        // TODO: send response

        // Update status - also only MessageContentErrorDtos would be completed
        // quickfix: don´t persist request
        requestDto.setState(DT_RequestStateEnum.ERROR);
            /*
        requestService.updateState(correspondingRequest, DT_RequestStateEnum.COMPLETED);

             */
    }

    private Request findCorrespondingRequest(RequestDto requestDto) {
        UUID requestId = requestDto.getHeader().getRequestId();
        log.info(String.format("Find corresponding request with header id %s",
                requestDto.getHeader().getRequestId()));

        Request requestFound =
                requestService.findRequestByHeaderUuid(requestId);

        if (requestFound == null) {
            throw new RequestIdNotFoundException(requestId);
        } else return null;

    }
}
