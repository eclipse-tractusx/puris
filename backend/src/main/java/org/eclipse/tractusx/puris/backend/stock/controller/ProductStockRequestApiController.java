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
package org.eclipse.tractusx.puris.backend.stock.controller;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.api.controller.exception.RequestIdAlreadyUsedException;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfullRequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestApiService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequestForMaterial;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.RequestMarshallingService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("product-stock")
public class ProductStockRequestApiController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RequestService requestService;

    @Autowired
    RequestApiService requestApiService;

    @Autowired
    RequestMarshallingService requestMarshallingService;

    @PostMapping("request")
    public ResponseEntity<Object> postRequest(@RequestBody String requestBody) {
        log.info("product-stock/request called: \n" + requestBody);

        ProductStockRequestDto productStockRequestDto = null;
        try {
            productStockRequestDto = requestMarshallingService.transformToProductStockRequestDto(requestBody);
        } catch (Exception e) {
            log.error("Failed to deserialize body of incoming message", e);
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).build();
        }

        if (productStockRequestDto.getHeader() == null || productStockRequestDto.getHeader().getRequestId() == null) {
            log.error("No RequestId provided!");
            return ResponseEntity.status(422).build();
        }

        UUID requestId = productStockRequestDto.getHeader().getRequestId();

        ProductStockRequest productStockRequestFound =
                requestService.findRequestByHeaderUuid(requestId);
                

        if (productStockRequestFound != null) {
            throw new RequestIdAlreadyUsedException(requestId);
        }

        productStockRequestDto.setState(DT_RequestStateEnum.RECEIPT);
        try {
            ProductStockRequest newProductStockRequestEntity = new ProductStockRequest();
            newProductStockRequestEntity.setHeader(modelMapper.map(productStockRequestDto.getHeader(), MessageHeader.class));
            List<ProductStockRequestForMaterial> payload = new ArrayList<>();
            for (var payloadItem : productStockRequestDto.getPayload()) {
                payload.add(modelMapper.map(payloadItem, ProductStockRequestForMaterial.class));
            }
            newProductStockRequestEntity.setPayload(payload);
            newProductStockRequestEntity.setState(productStockRequestDto.getState());
            requestService.createRequest(newProductStockRequestEntity);
            log.info("Persisted incoming request " + productStockRequestDto.getHeader().getRequestId());
        } catch (Exception e) {
            log.warn("Failed to persist incoming request " + productStockRequestDto.getHeader().getRequestId());
        }

        // handling the request and responding should be done asynchronously.
        final RequestDto threadRequestDto = new RequestDto(
                productStockRequestDto.getState(),
                productStockRequestDto.getUuid(),
                productStockRequestDto.getHeader(),
                productStockRequestDto.getPayload()
        );

        Thread respondAsyncThread = new Thread(() -> {
            requestApiService.handleRequest(threadRequestDto);
        });
        respondAsyncThread.start();

        // if the request has been correctly taken over, return 200
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(new SuccessfullRequestDto(requestId));
        
    }

}
