/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated 
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Anthropic Claude (claude-opus-5-5)
 */
package org.eclipse.tractusx.puris.backend.digitaltwinevent.controller;
import java.util.regex.Pattern;
import java.util.Arrays;

import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageContext;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/digital-twin-events")
@Slf4j
public class DigitalTwinEventController {
    @Autowired
    private IndustryCoreMessageService messageService;

    @Autowired
    private PartnerService partnerService;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    private final Pattern contextPattern = PatternStore.DIGITAL_TWIN_EVENT_CONTEXT_PATTERN;

    @Operation(summary = "Receives Digital Twin Events 3.0. This endpoint is meant to be accessed by partners via EDC only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Bad Request: invalid edc-bpn header, message header or context", content = @Content),
        @ApiResponse(responseCode = "401", description = "Access forbidden. ", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access forbidden - self-access only", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not Found: a child material is unknown or not supplied to the sender", content = @Content),
        @ApiResponse(responseCode = "501", description = "Handling of the operation is not implemented yet", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> postDigitalTwinEvent(
        @RequestHeader("edc-bpn") String bpnl,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {@Content(examples = {
            @ExampleObject(EXAMPLE_MESSAGE)
        })})
        @RequestBody JsonNode body) {
        if (!bpnlPattern.matcher(bpnl).matches()) {
            log.warn("Rejecting Digital Twin Event. Invalid BPNL in edc-bpn header");
            return badRequest("Invalid BPNL in edc-bpn header");
        }
        if (partnerService.findByBpnl(bpnl) == null) {
            log.warn("Rejecting Digital Twin Event. Unknown partner {}", bpnl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Unknown partner " + bpnl));
        }
        try {
            IndustryCoreMessageContext context = getContext(body);
            messageService.validate(body.get("header"), bpnl, context);
            return switch (context) {
                case DIGITAL_TWIN_EVENT_CONNECT_TO_PARENT_CONTEXT -> handleConnectToParent(bpnl, body);
                case DIGITAL_TWIN_EVENT_CONNECT_TO_CHILD_CONTEXT -> handleConnectToChild(bpnl, body);
                case DIGITAL_TWIN_EVENT_SUBMODEL_UPDATE_CONTEXT -> handleSubmodelUpdate(bpnl, body);
                case DIGITAL_TWIN_EVENT_FEEDBACK_CONTEXT -> handleFeedback(bpnl, body);
                default -> throw new IllegalArgumentException("Unsupported operation in context " + context.getValue());
            };
        } catch (IllegalArgumentException e) {
            log.warn("Rejecting Digital Twin Event: {}", e.getMessage());
            return badRequest(e.getMessage());
        }
    }

    /**
     * Determines the context of a Digital Twin Event from its message header.
     *
     * @param body the request body
     * @return the context, one of the Digital Twin Event contexts of {@link IndustryCoreMessageContext}
     * @throws IllegalArgumentException if the header is missing, the context does not match
     */
    private IndustryCoreMessageContext getContext(JsonNode body) {
        JsonNode header = body.get("header");
        if (header == null || !header.isObject()) {
            throw new IllegalArgumentException("Header is missing");
        }
        String context = header.path("context").asText("");
        if (!contextPattern.matcher(context).matches()) {
            throw new IllegalArgumentException("Context does not match IndustryCore-DigitalTwinEvent-<ConnectToParent | ConnectToChild | SubmodelUpdate | Feedback>:3.0.0");
        }
        return Arrays.stream(IndustryCoreMessageContext.values())
            .filter(supported -> supported.getValue().equals(context))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Context does not match IndustryCore-DigitalTwinEvent-<ConnectToParent | ConnectToChild | SubmodelUpdate | Feedback>:3.0.0"));
    }

    private ResponseEntity<?> handleConnectToParent(String bpnl, JsonNode body) {
        return notImplemented("ConnectToParent", bpnl, body);
    }

    private ResponseEntity<?> handleConnectToChild(String bpnl, JsonNode body) {
        return notImplemented("ConnectToChild", bpnl, body);
    }

    private ResponseEntity<?> handleSubmodelUpdate(String bpnl, JsonNode body) {
        return notImplemented("SubmodelUpdate", bpnl, body);
    }

    private ResponseEntity<?> handleFeedback(String bpnl, JsonNode body) {
        return notImplemented("Feedback", bpnl, body);
    }

    private ResponseEntity<ProblemDetail> notImplemented(String operation, String bpnl, JsonNode body) {
        log.info("Received {} event from {}. Handling is not implemented yet. Full message: {}", operation, bpnl, body.toPrettyString() );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, operation + " is not implemented yet"));
    }

    private ResponseEntity<ProblemDetail> badRequest(String detail) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail));
    }

    private static final String EXAMPLE_MESSAGE = "{\n" +
        "  \"header\": {\n" +
        "    \"messageId\": \"3b4edc05-e214-47a1-b0c2-1d831cdd9ba9\",\n" +
        "    \"context\": \"IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0\",\n" +
        "    \"sentDateTime\": \"2026-10-09T10:00:00+02:00\",\n" +
        "    \"senderBpn\": \"BPNL1234567890ZZ\",\n" +
        "    \"receiverBpn\": \"BPNL4444444444XX\",\n" +
        "    \"version\": \"3.0.0\"\n" +
        "  },\n" +
        "  \"content\": {}\n" +
        "}";
}
