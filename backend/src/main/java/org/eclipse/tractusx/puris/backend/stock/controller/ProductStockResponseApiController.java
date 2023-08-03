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

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfulRequestDto;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockResponse;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockResponseApiServiceImpl;
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

@RestController
@RequestMapping("product-stock")
@Slf4j
public class ProductStockResponseApiController {

    @Autowired
    ProductStockRequestService productStockRequestService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductStockResponseApiServiceImpl productStockResponseApiService;


    @PostMapping("response")
    public ResponseEntity<Object> postResponse(@RequestBody String body) {
        ProductStockResponse productStockResponse = null;
        try {
            productStockResponse = objectMapper.readValue(body, ProductStockResponse.class);
            log.info(objectMapper.readTree(objectMapper.writeValueAsString(productStockResponse)).toPrettyString());
        } catch (Exception e) {
            log.error("Failed to deserialize body of incoming message", e);
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).build();
        }
        
        if (productStockResponse.getHeader() == null || productStockResponse.getHeader().getRequestId() == null) {
            log.error("No RequestId provided!");
            return ResponseEntity.status(422).build();
        }

        UUID requestId = productStockResponse.getHeader().getRequestId();

        ProductStockRequest productStockRequestFound = productStockRequestService.findRequestByHeaderUuid(requestId);
        if (productStockRequestFound == null) {
            log.error("Request id " +requestId +  " not found");
            return ResponseEntity.status(422).build();
        } else {
            log.info("Got response for request Id " + requestId);
        }

        productStockRequestService.updateState(productStockRequestFound, DT_RequestStateEnum.COMPLETED);
        productStockResponseApiService.consumeResponse(productStockResponse);

        // if the request has been correctly taken over, return 202
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).body(new SuccessfulRequestDto(requestId));
    }

}
