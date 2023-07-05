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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.controller.exception.RequestIdNotFoundException;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.*;
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
import org.springframework.beans.factory.annotation.Value;
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

    private static final OkHttpClient CLIENT = new OkHttpClient();

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

    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;

    // @Value("${partner.bpnl}")
    // private String partnerBpnl;

    @Value("${partner.bpns}")
    private String partnerBpns;

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
            } else {
                log.info("Found requested Material: " + existingMaterial.getMaterialNumberCustomer());
            }
            boolean ordersProducts =
                    existingMaterial.getOrderedByPartners()
                            .stream().anyMatch(
                                    partner -> partner.getBpnl().equals(requestingPartnerBpnl));
            log.info("Requesting entity orders this Material? " + ordersProducts);
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

            log.info("Found Stocks for this Partner? " + productStocks.size());

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

        String edr = edcAdapterService.initializeProxyCall(partnerIdsUrl,
                requestDto.getHeader().getRespondAssetId(), filterProperties);

        ObjectNode edrNode = null;
        try {
            edrNode = objectMapper.readValue(edr, ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // prepare interface object
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setRequestId(requestDto.getHeader().getRequestId());
        //messageHeaderDto.setRespondAssetId("product-stock-response-api");
        messageHeaderDto.setContractAgreementId("some cid");
        messageHeaderDto.setSender("BPNL1234567890ZZ"); // PLATO's BPNL
        messageHeaderDto.setSenderEdc(ownEdcIdsUrl);
        // set receiver per partner
        messageHeaderDto.setReceiver("http://sokrates-controlplane:8084/api/v1/ids"); //Fallback
        messageHeaderDto.setReceiver(requestDto.getHeader().getSenderEdc());
        messageHeaderDto.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeaderDto.setCreationDate(new Date());

        ResponseDto responseDto = new ResponseDto();
        responseDto.setHeader(messageHeaderDto);
        responseDto.setPayload(resultProductStocks);

        // TODO extract from edr
        String authToken = null;
        String authMethod = null;
        String requestUrl = null;
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        objectMapper.valueToTree(responseDto).toString()))
                .url(requestUrl)
                .build();

        log.info(String.format("Request body of EDC Request: %s", objectMapper.valueToTree(responseDto).toString()));
        try {
            CLIENT.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Update status - also only MessageContentErrorDtos would be completed
        // quickfix: don´t persist request
        //requestDto.setState(DT_RequestStateEnum.ERROR);
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
