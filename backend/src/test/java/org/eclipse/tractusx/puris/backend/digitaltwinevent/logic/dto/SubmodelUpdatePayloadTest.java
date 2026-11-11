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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class SubmodelUpdatePayloadTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String CATENA_X_ID = "urn:uuid:4f3c5b2a-1e7d-4a8b-9c6f-2d1e0b9a8c7f";
    private static final String SEMANTIC_ID = "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @ParameterizedTest
    @EnumSource(SubmodelEventType.class)
    void eventType_isReadFromItsSpecValue(SubmodelEventType eventType) throws Exception {
        ObjectNode content = content();
        event(content).put("eventType", eventType.getValue());

        SubmodelUpdatePayload payload = objectMapper.treeToValue(content, SubmodelUpdatePayload.class);

        assertEquals(eventType, payload.getListOfEvents().get(0).getEventType());
        assertEquals(Set.of(), VALIDATOR.validate(payload));
    }

    @Test
    void unknownEventType_isRejected() {
        ObjectNode content = content();
        event(content).put("eventType", "ModifySubmodel");

        assertThrows(JsonProcessingException.class, () -> objectMapper.treeToValue(content, SubmodelUpdatePayload.class));
    }

    @Test
    void eventWithoutSubmodelSemanticId_isInvalid() throws Exception {
        ObjectNode content = content();
        event(content).remove("submodelSemanticId");

        assertSingleViolation(objectMapper.treeToValue(content, SubmodelUpdatePayload.class),
            "listOfEvents[0].submodelSemanticId");
    }

    @Test
    void tooLongSubmodelSemanticId_isInvalid() throws Exception {
        ObjectNode content = content();
        event(content).put("submodelSemanticId", "x".repeat(2049));

        assertSingleViolation(objectMapper.treeToValue(content, SubmodelUpdatePayload.class),
            "listOfEvents[0].submodelSemanticId");
    }

    @Test
    void missingListOfEvents_isInvalid() throws Exception {
        ObjectNode content = content();
        content.remove("listOfEvents");

        assertSingleViolation(objectMapper.treeToValue(content, SubmodelUpdatePayload.class), "listOfEvents");
    }

    private ObjectNode content() {
        ObjectNode content = objectMapper.createObjectNode();
        content.putArray("listOfEvents").addObject()
            .put("eventType", "UpdateSubmodel")
            .put("catenaXId", CATENA_X_ID)
            .put("submodelSemanticId", SEMANTIC_ID);
        return content;
    }

    private static ObjectNode event(ObjectNode content) {
        return (ObjectNode) content.get("listOfEvents").get(0);
    }

    private static <T> void assertSingleViolation(T payload, String expectedPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(payload);
        assertEquals(1, violations.size(), () -> violations.toString());
        assertEquals(expectedPath, violations.iterator().next().getPropertyPath().toString());
    }
}
