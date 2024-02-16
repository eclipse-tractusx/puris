/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockStatusRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

@RestController
@RequestMapping("item-stock")
@Slf4j
/**
 * This class offers endpoints for the ItemStock-request and
 * -status-request api assets.
 */
public class ItemStockRequestApiController {

    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;
    @Autowired
    private ItemStockRequestMessageService itemStockRequestMessageService;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Validator validator;
    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    @Operation(summary = "This endpoint receives the item stock request messages. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "The request was accepted"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "422", description = "A request with same Id already exists")
    })
    @PostMapping("request")
    public ResponseEntity<RequestReactionMessageDto> postMapping(@RequestBody ItemStockRequestMessageDto requestMessageDto) {
        if (!validator.validate(requestMessageDto).isEmpty()) {
            log.warn("Rejected invalid message body");
            return ResponseEntity.status(400).body(new RequestReactionMessageDto(requestMessageDto.getHeader().getMessageId()));
        }
        log.info("Got Request\n" + requestMessageDto);
        ItemStockRequestMessage requestMessage = ItemStockRequestMessageDto.convertToEntity(requestMessageDto);
        var createdRequestMessage = itemStockRequestMessageService.create(requestMessage);
        log.info("Created RequestMessageEntity:\n" + requestMessage);
        if (createdRequestMessage == null) {
            // Validation failed or messageId was used before in combination with these partner bpnl's
            log.warn("Received invalid request\n" + requestMessageDto);
            return ResponseEntity.status(422).body(new RequestReactionMessageDto(requestMessageDto.getHeader().getMessageId()));
        }
        switch (requestMessageDto.getContent().getDirection()) {
            case OUTBOUND ->
                executorService.submit(() -> itemStockRequestApiService.handleRequestFromCustomer(requestMessageDto, createdRequestMessage));
            case INBOUND ->
                executorService.submit(() -> itemStockRequestApiService.handleRequestFromSupplier(requestMessageDto, createdRequestMessage));
            default -> {
                log.warn("Missing direction in request \n" + requestMessageDto);
            }
        }
        return ResponseEntity.status(202).body(new RequestReactionMessageDto(requestMessageDto.getHeader().getMessageId()));
    }

    @Operation(summary = "This endpoint receives the item stock status request messages. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status request was successful. "),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "422", description = "Unknown Message Id requested")
    })
    @PostMapping("status")
    public ResponseEntity<StatusReactionMessageDto> getStatus(@RequestBody ItemStockStatusRequestMessageDto statusRequest) {
        if (!validator.validate(statusRequest).isEmpty()) {
            // Bad Request
            log.warn("Rejected invalid message body");
            return ResponseEntity.status(400).build();
        }
        var relatedMessage = itemStockRequestMessageService.find(new ItemStockRequestMessage.Key(statusRequest.getHeader().getMessageId(),
            statusRequest.getHeader().getSenderBpn(), statusRequest.getHeader().getReceiverBpn()));
        if (relatedMessage == null) {
            return ResponseEntity.status(422).build();
        }
        return ResponseEntity.status(200).body(new StatusReactionMessageDto(statusRequest.getHeader().getRelatedMessageId(), relatedMessage.getState()));
    }

    private record RequestReactionMessageDto(UUID messageId) {
    }

    private record StatusReactionMessageDto(UUID messageId, DT_RequestStateEnum requestState) {
    }

}
