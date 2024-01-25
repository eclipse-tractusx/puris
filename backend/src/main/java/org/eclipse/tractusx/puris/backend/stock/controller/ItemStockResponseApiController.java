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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockResponseDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestMessageService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockResponseApiService;
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
public class ItemStockResponseApiController {
    @Autowired
    private ItemStockRequestMessageService itemStockRequestMessageService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ItemStockResponseApiService itemStockResponseApiService;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Validator validator;
    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;


    @Operation(description = "This endpoint receives the item stock response messages. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "The response message was accepted"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "422", description = "The related message id does not match any open request")
    })
    @PostMapping("response")
    public ResponseEntity<ResponseReactionMessageDto> postMapping(@RequestBody ItemStockResponseDto responseDto) {
        if(!validator.validate(responseDto).isEmpty()) {
            log.warn("Rejected invalid message body");
            return ResponseEntity.status(400).body(new ResponseReactionMessageDto(responseDto.getHeader().getMessageId()));
        }

        log.info("Received response: \n" + responseDto);
        if (responseDto.getHeader() == null || responseDto.getHeader().getMessageId() == null || responseDto.getHeader().getRelatedMessageId() == null
            || responseDto.getHeader().getReceiverBpn() == null || responseDto.getHeader().getSenderBpn() == null
            || !bpnlPattern.matcher(responseDto.getHeader().getSenderBpn()).matches()
            || !bpnlPattern.matcher(responseDto.getHeader().getReceiverBpn()).matches()) {
            return ResponseEntity.status(400).body(new ResponseReactionMessageDto(responseDto.getHeader().getMessageId()));
        }

        Partner partner = partnerService.findByBpnl(responseDto.getHeader().getSenderBpn());
        if (partner == null) {
            log.error("Unknown partner in response dto: \n" + responseDto);
            return ResponseEntity.status(400).body(new ResponseReactionMessageDto(responseDto.getHeader().getMessageId()));
        }
        var initialRequest = itemStockRequestMessageService.find(
            new ItemStockRequestMessage.Key(responseDto.getHeader().getRelatedMessageId(),
                responseDto.getHeader().getReceiverBpn(),responseDto.getHeader().getSenderBpn()));
        if (initialRequest == null || initialRequest.getState() != DT_RequestStateEnum.Requested) {
            log.error("Response dto does not match any open request: \n" + responseDto);
            return ResponseEntity.status(422).body(new ResponseReactionMessageDto(responseDto.getHeader().getMessageId()));
        }
        executorService.submit(() -> itemStockResponseApiService.consumeResponse(responseDto, partner, initialRequest));
        return ResponseEntity.status(200).body(new ResponseReactionMessageDto(responseDto.getHeader().getMessageId()));
    }

    private record ResponseReactionMessageDto(UUID messageId) {
    }
}
