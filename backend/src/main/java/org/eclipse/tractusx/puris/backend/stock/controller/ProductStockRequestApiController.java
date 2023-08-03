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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfulRequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestApiServiceImpl;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("product-stock")
public class ProductStockRequestApiController {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductStockRequestService productStockRequestService;

    @Autowired
    ProductStockRequestApiServiceImpl requestApiService;

    @Autowired
    EdcAdapterService edcAdapterService;

    @PostMapping("request")
    public ResponseEntity<Object> postRequest(@RequestBody String requestBody) {
        log.info("product-stock/request called: \n" + requestBody);

        ProductStockRequest productStockRequest = null;
        try {
            productStockRequest = objectMapper.readValue(requestBody, ProductStockRequest.class);
        } catch (Exception e) {
            log.error("Malformed request received");
            return ResponseEntity.status(400).build();
        }

        if (productStockRequest.getHeader() == null || productStockRequest.getHeader().getRequestId() == null) {
            log.error("No RequestId provided!");
            return ResponseEntity.status(400).build();
        }

        if (productStockRequest.getHeader().getSender() == null) {
            log.error("No sender attribute in header");
            return ResponseEntity.status(400).build();
        }

        UUID requestId = productStockRequest.getHeader().getRequestId();

        ProductStockRequest productStockRequestFound =
                productStockRequestService.findRequestByHeaderUuid(requestId);
                

        if (productStockRequestFound != null) {
            log.error("RequestId already in use");
            return ResponseEntity.status(422).build();
        }

        productStockRequest.setState(DT_RequestStateEnum.RECEIPT);
        productStockRequestService.createRequest(productStockRequest);

        final ProductStockRequest requestForAsyncThread = productStockRequest;
        Thread respondAsyncThread = new Thread(() -> {
            requestApiService.handleRequest(requestForAsyncThread);
        });
        respondAsyncThread.start();

        // if the request has been correctly taken over, return 202
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).body(new SuccessfulRequestDto(requestId));
        
    }

    @GetMapping("request")
    public ResponseEntity<Object> getRequest(@RequestBody JsonNode body) {
        try {
            MessageHeaderDto header = objectMapper.convertValue(body.get("header"), MessageHeaderDto.class);
            var request = productStockRequestService.findRequestByHeaderUuid(header.getRequestId());
            var requestStatus = request.getState();
            var jsonResponseBody = objectMapper.createObjectNode();
            jsonResponseBody.put("requestId", header.getRequestId().toString());
            jsonResponseBody.put("requestState", requestStatus.STATUSTEXT);
            return ResponseEntity.status(200).body(jsonResponseBody);
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }


}
