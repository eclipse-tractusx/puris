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
package org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class FeedbackPayloadTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String CATENA_X_ID = "urn:uuid:4f3c5b2a-1e7d-4a8b-9c6f-2d1e0b9a8c7f";
    private static final String OTHER_CATENA_X_ID = "urn:uuid:9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
    private static final String ITEM_ERROR = "Unknown Catena-X ID";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void validFeedback_isDeserialized() throws Exception {
        FeedbackPayload payload = objectMapper.treeToValue(content(), FeedbackPayload.class);

        assertEquals(FeedbackStatus.ERROR, payload.getStatus());
        assertEquals(FeedbackStatus.OK, payload.getListOfItems().get(0).getStatus());
        assertNull(payload.getListOfItems().get(0).getStatusMessage());
        assertEquals(FeedbackStatus.ERROR, payload.getListOfItems().get(1).getStatus());
        assertEquals(ITEM_ERROR, payload.getListOfItems().get(1).getStatusMessage());
        assertEquals(Set.of(), VALIDATOR.validate(payload));
    }

    @Test
    void itemErrorMessage_isAcceptedAsStatusMessage() throws Exception {
        ObjectNode content = content();
        ObjectNode failedItem = item(content, 1);
        failedItem.remove("statusMessage");
        failedItem.put("errorMessage", ITEM_ERROR);

        FeedbackPayload payload = objectMapper.treeToValue(content, FeedbackPayload.class);

        assertEquals(ITEM_ERROR, payload.getListOfItems().get(1).getStatusMessage());
    }

    @Test
    void itemFeedback_isWrittenWithStatusMessage() {
        ItemFeedback item = new ItemFeedback(CATENA_X_ID, FeedbackStatus.ERROR, ITEM_ERROR);

        ObjectNode json = objectMapper.valueToTree(item);

        assertEquals(ITEM_ERROR, json.get("statusMessage").asText());
        assertNull(json.get("errorMessage"));
    }

    @Test
    void unknownStatus_isRejected() {
        ObjectNode content = content().put("status", "WARNING");

        assertThrows(JsonProcessingException.class, () -> objectMapper.treeToValue(content, FeedbackPayload.class));
    }

    @Test
    void missingStatus_isInvalid() throws Exception {
        ObjectNode content = content();
        content.remove("status");

        assertSingleViolation(objectMapper.treeToValue(content, FeedbackPayload.class), "status");
    }

    @Test
    void itemWithoutCatenaXId_isInvalid() throws Exception {
        ObjectNode content = content();
        item(content, 0).remove("catenaXId");

        assertSingleViolation(objectMapper.treeToValue(content, FeedbackPayload.class), "listOfItems[0].catenaXId");
    }

    @Test
    void tooLongStatusMessage_isInvalid() throws Exception {
        ObjectNode content = content().put("statusMessage", "x".repeat(2049));

        assertSingleViolation(objectMapper.treeToValue(content, FeedbackPayload.class), "statusMessage");
    }

    private ObjectNode content() {
        ObjectNode content = objectMapper.createObjectNode()
            .put("status", "ERROR")
            .put("statusMessage", "One item could not be processed.");
        ArrayNode items = content.putArray("listOfItems");
        items.addObject().put("catenaXId", CATENA_X_ID).put("status", "OK");
        items.addObject().put("catenaXId", OTHER_CATENA_X_ID).put("status", "ERROR").put("statusMessage", ITEM_ERROR);
        return content;
    }

    private static ObjectNode item(ObjectNode content, int index) {
        return (ObjectNode) content.get("listOfItems").get(index);
    }

    private static <T> void assertSingleViolation(T payload, String expectedPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(payload);
        assertEquals(1, violations.size(), () -> violations.toString());
        assertEquals(expectedPath, violations.iterator().next().getPropertyPath().toString());
    }
}
