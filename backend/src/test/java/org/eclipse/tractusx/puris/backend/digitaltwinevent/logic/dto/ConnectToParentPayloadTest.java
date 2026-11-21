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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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

public class ConnectToParentPayloadTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String MANUFACTURER_BPNL = "BPNL1234567890ZZ";
    private static final String CATENA_X_ID = "urn:uuid:4f3c5b2a-1e7d-4a8b-9c6f-2d1e0b9a8c7f";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void partTypeContent_isDeserializedAsPartTypePayload() throws Exception {
        ConnectToParentPayload payload = objectMapper.treeToValue(content("PartType", item()), ConnectToParentPayload.class);

        ConnectToParentPartTypePayload partType = assertInstanceOf(ConnectToParentPartTypePayload.class, payload);
        assertEquals(DigitalTwinType.PART_TYPE, partType.getDigitalTwinType());
        assertEquals(MANUFACTURER_BPNL, partType.getListOfItems().get(0).getManufacturerId());
        assertEquals(Set.of(), VALIDATOR.validate(payload));
    }

    @Test
    void partInstanceContent_isDeserializedWithOneItemTypePerItem() throws Exception {
        ObjectNode content = content("PartInstance",
            item().put("partInstanceId", "SN-0001"),
            item().put("batchId", "B-0001"),
            item().put("jisNumber", "JIS-0001"));

        ConnectToParentPayload payload = objectMapper.treeToValue(content, ConnectToParentPayload.class);

        List<PartInstanceItem> items = assertInstanceOf(ConnectToParentPartInstancePayload.class, payload).getListOfItems();
        assertInstanceOf(SerializedPartItem.class, items.get(0));
        assertInstanceOf(BatchItem.class, items.get(1));
        assertInstanceOf(JisItem.class, items.get(2));
        assertEquals(Set.of(), VALIDATOR.validate(payload));
    }

    @Test
    void ambiguousPartInstanceItem_isInvalid() throws Exception {
        ObjectNode content = content("PartInstance", item().put("partInstanceId", "SN-0001").put("batchId", "B-0001"));

        assertSingleViolation(objectMapper.treeToValue(content, ConnectToParentPayload.class), "listOfItems[0].singular");
    }

    @Test
    void partInstanceItemWithoutTypeProperty_isRejected() {
        assertThrows(JsonProcessingException.class,
            () -> objectMapper.treeToValue(content("PartInstance", item()), ConnectToParentPayload.class));
    }

    @Test
    void invalidItem_isReportedWithItsPath() throws Exception {
        ObjectNode serializedPart = item().put("partInstanceId", "SN-0001");
        serializedPart.remove("manufacturerPartId");

        assertSingleViolation(objectMapper.treeToValue(content("PartInstance", serializedPart), ConnectToParentPayload.class),
            "listOfItems[0].manufacturerPartId");
    }

    @Test
    void contentOfOneTwinType_cannotBeReadAsTheOther() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.treeToValue(
            content("PartInstance", item().put("batchId", "B-0001")), ConnectToParentPartTypePayload.class));
        assertThrows(JsonProcessingException.class, () -> objectMapper.treeToValue(
            content("PartType", item()), ConnectToParentPartInstancePayload.class));
    }

    @Test
    void missingListOfItems_isInvalid() throws Exception {
        ObjectNode content = content("PartType");
        content.remove("listOfItems");

        assertSingleViolation(objectMapper.treeToValue(content, ConnectToParentPayload.class), "listOfItems");
    }

    @Test
    void tooLongInformation_isInvalid() throws Exception {
        ObjectNode content = content("PartType", item()).put("information", "x".repeat(1001));

        assertSingleViolation(objectMapper.treeToValue(content, ConnectToParentPayload.class), "information");
    }

    private ObjectNode content(String digitalTwinType, ObjectNode... items) {
        ObjectNode content = objectMapper.createObjectNode().put("digitalTwinType", digitalTwinType);
        ArrayNode listOfItems = content.putArray("listOfItems");
        for (ObjectNode item : items) {
            listOfItems.add(item);
        }
        return content;
    }

    private ObjectNode item() {
        return objectMapper.createObjectNode()
            .put("manufacturerId", MANUFACTURER_BPNL)
            .put("manufacturerPartId", "MPI-4711")
            .put("catenaXId", CATENA_X_ID);
    }

    private static <T> void assertSingleViolation(T payload, String expectedPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(payload);
        assertEquals(1, violations.size(), () -> violations.toString());
        assertEquals(expectedPath, violations.iterator().next().getPropertyPath().toString());
    }
}
