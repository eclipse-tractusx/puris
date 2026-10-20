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
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageContext;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UniqueIdPushConnectTochildPayload;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.service.ConnectToChildService;
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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/digital-twin-events")
@Slf4j
public class DigitalTwinEventController {
    @Autowired
    private IndustryCoreMessageService messageService;

    @Autowired
    private PartnerService partnerService;
    
    @Autowired 
    private ConnectToChildService connectToChildService;

    @Autowired
    private Validator validator;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;

    private final Pattern contextPattern = PatternStore.DIGITAL_TWIN_EVENT_CONTEXT_PATTERN;

    @Operation(summary = "Receives Digital Twin Events 3.0. This endpoint is meant to be accessed by partners via EDC only.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK: The Digital Twin event has been processed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Bad Request: invalid edc-bpn header, message header or context", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized. Missing API key.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions or invalid API key", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not Found: the sender is not a known partner", content = @Content),
        @ApiResponse(responseCode = "501", description = "Handling of the operation is not implemented yet", content = @Content),
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
        Partner sender = partnerService.findByBpnl(bpnl);
        if (sender == null) {
            log.warn("Rejecting Digital Twin Event. Unknown partner {}", bpnl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Unknown partner " + bpnl));
        }
        try {
            IndustryCoreMessageContext context = getContext(body);
            messageService.validate(body.get("header"), bpnl, context);
            return switch (context) {
                case DIGITAL_TWIN_EVENT_CONNECT_TO_PARENT_CONTEXT -> handleConnectToParent(sender, body);
                case DIGITAL_TWIN_EVENT_CONNECT_TO_CHILD_CONTEXT -> handleConnectToChild(sender, body);
                case DIGITAL_TWIN_EVENT_SUBMODEL_UPDATE_CONTEXT -> handleSubmodelUpdate(sender, body);
                case DIGITAL_TWIN_EVENT_FEEDBACK_CONTEXT -> handleFeedback(sender, body);
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

    private ResponseEntity<?> handleConnectToParent(Partner sender, JsonNode body) {
        return notImplemented("ConnectToParent", sender, body);
    }

    /**
     * Stores the parent items of a ConnectToChild event as relations of the own child materials to the parent
     * materials of the sender.
     *
     * @throws IllegalArgumentException if the content does not match the OpenAPI specification or a parent item does not belong to the sender
     */
    private ResponseEntity<?> handleConnectToChild(Partner sender, JsonNode body) {
        UniqueIdPushConnectTochildPayload payload = messageService.validateAndParse(body, IndustryCoreMessageContext.DIGITAL_TWIN_EVENT_CONNECT_TO_CHILD_CONTEXT, sender.getBpnl(),
            UniqueIdPushConnectTochildPayload.class);
        Set<ConstraintViolation<UniqueIdPushConnectTochildPayload>> errors = validator.validate(payload);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid content: " + errors.stream()
                .map(error -> error.getPropertyPath() + " " + error.getMessage())
                .collect(Collectors.joining("; ")));
        }
        try {
            connectToChildService.handleConnectToChild(sender, payload);
        } catch (NoSuchElementException e) {
            log.warn("Rejecting ConnectToChild event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()));
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleSubmodelUpdate(Partner sender, JsonNode body) {
        return notImplemented("SubmodelUpdate", sender, body);
    }

    private ResponseEntity<?> handleFeedback(Partner sender, JsonNode body) {
        return notImplemented("Feedback", sender, body);
    }

    private ResponseEntity<ProblemDetail> notImplemented(String operation, Partner sender, JsonNode body) {
        log.info("Received {} event from {}. Handling is not implemented yet. Full message: {}", operation, sender.getBpnl(), body.toPrettyString() );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, operation + " is not implemented yet"));
    }

    private ResponseEntity<ProblemDetail> badRequest(String detail) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail));
    }

    private static final String EXAMPLE_MESSAGE = "{\n" +
        "  \"header\": {\n" +
        "    \"messageId\": \"urn:uuid:3e7c5d2a-1b4f-4a8e-9d6c-2f0e1a3b5c7d\",\n" +
        "    \"context\": \"IndustryCore-DigitalTwinEvent-ConnectToChild:3.0.0\",\n" +
        "    \"sentDateTime\": \"2025-01-15T10:30:00.000Z\",\n" +
        "    \"senderBpn\": \"BPNL4444444444XX\",\n" +
        "    \"receiverBpn\": \"BPNL1234567890ZZ\",\n" +
        "    \"version\": \"3.0.0\"\n" +
        "  },\n" +
        "  \"content\": {\n" +
        "    \"digitalTwinType\": \"PartType\",\n" +
        "    \"information\": \"Usage as Planned information for parts supplied by the receiver.\",\n" +
        "    \"listOfItems\": [\n" +
        "      {\n" +
        "        \"catenaXId\": \"urn:uuid:5d8e2f1a-6b3c-4d7e-8f9a-1b2c3d4e5f6a\",\n" +
        "        \"parentItems\": [\n" +
        "          {\n" +
        "            \"catenaXId\": \"urn:uuid:9a3f1b2c-7d4e-4f5a-8b6c-0e1d2f3a4b5c\",\n" +
        "            \"businessPartner\": \"BPNL4444444444XX\",\n" +
        "            \"quantity\": {\n" +
        "              \"value\": 2.0,\n" +
        "              \"unit\": \"unit:piece\"\n" +
        "            },\n" +
        "            \"validityPeriod\": {\n" +
        "              \"validFrom\": \"2025-01-01T00:00:00.000Z\"\n" +
        "            },\n" +
        "            \"createdOn\": \"2025-01-15T10:30:00.000Z\",\n" +
        "            \"lastModifiedOn\": \"2025-01-15T10:30:00.000Z\"\n" +
        "          }\n" +
        "        ]\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
    "}";
}
