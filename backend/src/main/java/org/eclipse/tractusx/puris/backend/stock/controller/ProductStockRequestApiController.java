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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfulRequestDto;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestApiServiceImpl;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * This class contains the REST controller of the product-stock-response api.
 */
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

    @PostMapping("request")
    @Operation(summary = "This endpoint receives the product stock requests from a consumer.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = ProductStockRequest.class)))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Product Stock request was accepted"),
        @ApiResponse(responseCode = "400", description = "Request body malformed"),
        @ApiResponse(responseCode = "422", description = "A request with the same ID already exists")
    })
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

        productStockRequest.setState(DT_RequestStateEnum.Received);
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
    @Operation(summary = "This endpoint allows the consumer to OPTIONALLY check the current status of a request it already made.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema = @Schema(implementation = StatusRequestSchema.class)
        ))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status request was successful",
            content = @Content(
                examples = {@ExampleObject
                    (name = "Status response sample",
                        value = "{\"requestId\": \"48878d48-6f1d-47f5-8ded-a441d0d879df\",\n" +
                            "  \"requestState\": \"Working\"}")}
            )),
        @ApiResponse(responseCode = "400", description = "Request body malformed"),
        @ApiResponse(responseCode = "401", description = "Not authorized"),
        @ApiResponse(responseCode = "422", description = "The request ID is not known")
    })
    public ResponseEntity<Object> getRequest(@RequestBody JsonNode body) {
        try {
            MessageHeaderDto header = objectMapper.convertValue(body.get("header"), MessageHeaderDto.class);
            var request = productStockRequestService.findRequestByHeaderUuid(header.getRequestId());
            if (request == null) {
                return ResponseEntity.status(422).build();
            }
            String senderBpnl = request.getHeader().getSender();
            if (!senderBpnl.equals(header.getSender())) {
                return ResponseEntity.status(401).build();
            }
            var requestStatus = request.getState();
            var jsonResponseBody = objectMapper.createObjectNode();
            jsonResponseBody.put("requestId", header.getRequestId().toString());
            jsonResponseBody.put("requestState", requestStatus.name());
            return ResponseEntity.status(200).body(jsonResponseBody);
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * This class serves only as a template for the schema in the Swagger-UI
     */
    private static class StatusRequestSchema {
        public MessageHeaderDto header;
        public JsonNode content;
    }


}
