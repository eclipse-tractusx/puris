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

import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfullRequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.ResponseApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockResponseDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("product-stock")
@Slf4j
public class ProductStockResponseApiController {

    @Autowired
    RequestService requestService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ObjectMapper objectMapper;

    private ResponseApiService responseApiService;

    public ProductStockResponseApiController(ResponseApiService responseApiService) {
        this.responseApiService = responseApiService;
    }


    @PostMapping("response")
    public ResponseEntity<Object> postResponse(@RequestBody JsonNode requestBody) {
        log.info("product-stock/response called: \n" + requestBody.toPrettyString());
        ProductStockResponseDto productStockResponseDto = null;
        try {
            productStockResponseDto = objectMapper.treeToValue(requestBody, ProductStockResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to deserialize body of incoming message", e);
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).build();
        }
        
        if (productStockResponseDto.getHeader() == null || productStockResponseDto.getHeader().getRequestId() == null) {
            log.error("No RequestId provided!");
            return ResponseEntity.status(422).build();
        }

        UUID requestId = productStockResponseDto.getHeader().getRequestId();

        Request requestFound = requestService.findRequestByHeaderUuid(requestId);
        if (requestFound == null) {
            log.error("Request id " +requestId +  " not found");
            return ResponseEntity.status(422).build();
        } else {
            log.info("Got response for request Id " + requestId);
        }

        requestFound = requestService.updateState(requestFound, DT_RequestStateEnum.COMPLETED);
        responseApiService.consumeResponse(productStockResponseDto);

        // if the request has been correctly taken over, return 202
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).body(new SuccessfullRequestDto(requestId));
    }

}
