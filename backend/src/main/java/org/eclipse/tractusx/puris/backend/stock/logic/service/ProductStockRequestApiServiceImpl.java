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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.*;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestApiService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * according to the API specification.
 */
@Component
@Slf4j
public class ProductStockRequestApiServiceImpl implements RequestApiService {

    @Autowired
    private RequestService requestService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${edc.dataplane.public.port}")
    String dataPlanePort;

    @Value("${edc.controlplane.host}")
    String dataPlaneHost;

    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;

    @Value("${own.bpnl}")
    private String ownBPNL;

    @Value("${edc.applydataplaneworkaround}")
    private boolean applyDataplaneWorkaround;



    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public void handleRequest(RequestDto requestDto) {

        log.info(String.format("param requestDto %s", requestDto));
        requestDto.setState(DT_RequestStateEnum.WORKING);

        Request requestEntity = requestService.findRequestByHeaderUuid(requestDto.getHeader().getRequestId());
        requestEntity = requestService.updateState(requestEntity, DT_RequestStateEnum.WORKING);

        String partnerIdsUrl = requestDto.getHeader().getSenderEdc();

        List<MessageContentDto> resultProductStocks = new ArrayList<>();

        String requestingPartnerBpnl = requestDto.getHeader().getSender();

        for (ProductStockRequestForMaterialDto productStockRequestDto : requestDto.getPayload()) {

            // Check if product is known
            Material existingMaterial =
                    materialService.findProductByMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
            if (existingMaterial == null) {
                MessageContentErrorDto messageContentErrorDto = new MessageContentErrorDto();
                messageContentErrorDto.setMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
                messageContentErrorDto.setError("PURIS-01");
                messageContentErrorDto.setMessage("Material is unknown.");
                resultProductStocks.add(messageContentErrorDto);
                log.warn(String.format("No Material found for ID Customer %s in request %s",
                        productStockRequestDto.getMaterialNumberCustomer(),
                        requestDto.getHeader().getRequestId()));
                continue;
            } else {
                log.info("Found requested Material: " + existingMaterial.getMaterialNumberCustomer());
            }
            boolean ordersProducts =
                    existingMaterial.getOrderedByPartners()
                            .stream().anyMatch(
                                    partner -> partner.getBpnl().equals(requestingPartnerBpnl));
            log.info("Requesting entity orders this Material? " + ordersProducts);
            if (!ordersProducts) {
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
                continue;
            }

            List<ProductStock> productStocks =
                    productStockService
                            .findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
                                    productStockRequestDto.getMaterialNumberCustomer(),
                                    requestingPartnerBpnl);

            ProductStock productStock = null;
            if (productStocks.size() == 0) {
                MessageContentErrorDto messageContentErrorDto = new MessageContentErrorDto();
                messageContentErrorDto.setMaterialNumberCustomer(productStockRequestDto.getMaterialNumberCustomer());
                messageContentErrorDto.setError("PURIS-03");
                messageContentErrorDto.setMessage("No Product Stock found.");
                resultProductStocks.add(messageContentErrorDto);
                log.warn("No Product Stocks of Material " + productStockRequestDto.getMaterialNumberCustomer() 
                + " found for " + requestDto.getHeader().getSender());
                continue;
            } else productStock = productStocks.get(0);


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
        }


        var data = edcAdapterService.getContractForResponseApi(partnerIdsUrl);
        if(data == null) {
            log.error("Failed to contract response api from " + partnerIdsUrl);
            requestEntity = requestService.updateState(requestEntity, DT_RequestStateEnum.ERROR);
            log.info("Request status: \n" + requestEntity.toString());
            return;
        }
        String authKey = data[0];
        String authCode = data[1];
        String endpoint = data[2];
        String contractId = data[3];
        // prepare interface object
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setRequestId(requestDto.getHeader().getRequestId());
        messageHeaderDto.setContractAgreementId(contractId);
        messageHeaderDto.setSender(ownBPNL); 
        messageHeaderDto.setSenderEdc(ownEdcIdsUrl);
        // set receiver per partner
        messageHeaderDto.setReceiver(requestDto.getHeader().getSenderEdc());
        messageHeaderDto.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeaderDto.setCreationDate(new Date());

        ResponseDto responseDto = new ResponseDto();
        responseDto.setHeader(messageHeaderDto);
        responseDto.setPayload(resultProductStocks);

        if (applyDataplaneWorkaround) {
            log.info("Applying Dataplane Address Workaround");
            endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
        }
        try {
            String requestBody = objectMapper.writeValueAsString(responseDto);
            var response = edcAdapterService.sendDataPullRequest(
                    endpoint, authKey, authCode, requestBody);
            log.info(response.body().string());
            response.body().close();
            requestEntity = requestService.updateState(requestEntity, DT_RequestStateEnum.COMPLETED);
        } catch (Exception e) {
            log.error("Failed to send response to " + responseDto.getHeader().getReceiver(), e);
            requestEntity = requestService.updateState(requestEntity, DT_RequestStateEnum.ERROR);
        } finally {
            log.info("Request status: \n" + requestEntity.toString());
        }

    }
}
