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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.SuccessfulRequestDto;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockResponse;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockResponseApiServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * This class contains the REST controller of the product-stock-response api.
 */
@RestController
@RequestMapping("product-stock")
@Slf4j
public class ProductStockResponseApiController {

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductStockResponseApiServiceImpl productStockResponseApiService;

    @PostMapping("response")
    @Operation(summary = "This endpoint receives the responses to the consumer's requests.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = ProductStockResponse.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Product Stock Response was accepted",
        content = @Content(examples = @ExampleObject(value = "{\n" +
            "  \"requestId\": \"48878d48-6f1d-47f5-8ded-a441d0d879df\"\n" +
            "}"))),
        @ApiResponse(responseCode = "400", description = "Response body malformed"),
        @ApiResponse(responseCode = "401", description = "Not authorized"),
        @ApiResponse(responseCode = "422", description = "The request ID does not match any open request")
    })
    @CrossOrigin
    public ResponseEntity<Object> postResponse(@RequestBody String body) {
        ProductStockResponse productStockResponse = null;
        try {
            productStockResponse = objectMapper.readValue(body, ProductStockResponse.class);
            log.info(objectMapper.readTree(objectMapper.writeValueAsString(productStockResponse)).toPrettyString());
        } catch (Exception e) {
            log.error("Failed to deserialize body of incoming message", e);
            return ResponseEntity.status(HttpStatusCode.valueOf(400)).build();
        }
        
        if (productStockResponse.getHeader() == null || productStockResponse.getHeader().getRequestId() == null) {
            log.error("No RequestId provided!");
            return ResponseEntity.status(400).build();
        }

        UUID requestId = productStockResponse.getHeader().getRequestId();

        ProductStockRequest productStockRequestFound = productStockRequestService.findRequestByHeaderUuid(requestId);
        if (productStockRequestFound == null) {
            log.error("Request id " +requestId +  " not found");
            return ResponseEntity.status(422).build();
        } else {
            log.info("Got response for request Id " + requestId);
        }

        if(!productStockRequestFound.getHeader().getReceiver().equals(productStockResponse.getHeader().getSender())) {
            log.error("Request receiver " + productStockRequestFound.getHeader().getReceiver() + " does not match "
                + " response sender " + productStockResponse.getHeader().getSender());
            return ResponseEntity.status(401).build();
        }

        productStockRequestService.updateState(productStockRequestFound, DT_RequestStateEnum.Completed);
        productStockResponseApiService.consumeResponse(productStockResponse);

        // if the request has been correctly taken over, return 202
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).body(new SuccessfulRequestDto(requestId));
    }

}
