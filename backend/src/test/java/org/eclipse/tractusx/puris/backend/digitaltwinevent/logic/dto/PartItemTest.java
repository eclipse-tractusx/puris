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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class PartItemTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String MANUFACTURER_BPNL = "BPNL1234567890ZZ";
    private static final String CATENA_X_ID = "urn:uuid:4f3c5b2a-1e7d-4a8b-9c6f-2d1e0b9a8c7f";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void partTypeItem_withIdentificationOnly_isValid() throws Exception {
        PartTypeItem item = objectMapper.treeToValue(item(), PartTypeItem.class);

        assertTrue(VALIDATOR.validate(item).isEmpty());
    }

    @Test
    void partTypeItem_withManufacturerIdNoBpnl_isInvalid() throws Exception {
        PartTypeItem item = objectMapper.treeToValue(item().put("manufacturerId", "BPNS1234567890ZZ"), PartTypeItem.class);

        assertSingleViolation(item, "manufacturerId");
    }

    @Test
    void itemWithPartInstanceId_isDeducedAsSerializedPart() throws Exception {
        PartInstanceItem item = objectMapper.treeToValue(item().put("partInstanceId", "SN-0001"), PartInstanceItem.class);

        assertEquals("SN-0001", assertInstanceOf(SerializedPartItem.class, item).getPartInstanceId());
        assertTrue(VALIDATOR.validate(item).isEmpty());
    }

    @Test
    void itemWithBatchId_isDeducedAsBatch() throws Exception {
        PartInstanceItem item = objectMapper.treeToValue(item().put("batchId", "B-0001"), PartInstanceItem.class);

        assertEquals("B-0001", assertInstanceOf(BatchItem.class, item).getBatchId());
        assertTrue(VALIDATOR.validate(item).isEmpty());
    }

    @Test
    void itemWithJisNumber_isDeducedAsJis() throws Exception {
        ObjectNode node = item().put("jisNumber", "JIS-0001").put("jisCallDate", "2026-01-05T08:00:00Z");

        PartInstanceItem item = objectMapper.treeToValue(node, PartInstanceItem.class);

        assertNotNull(assertInstanceOf(JisItem.class, item).getJisCallDate());
        assertTrue(VALIDATOR.validate(item).isEmpty());
    }

    @Test
    void itemWithoutTypeProperty_isRejected() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.treeToValue(item(), PartInstanceItem.class));
    }

    @Test
    void itemWithTwoTypeProperties_isInvalid_inEitherOrder() throws Exception {
        assertSingleViolation(objectMapper.treeToValue(item().put("partInstanceId", "SN-0001").put("batchId", "B-0001"),
            PartInstanceItem.class), "singular");
        assertSingleViolation(objectMapper.treeToValue(item().put("batchId", "B-0001").put("partInstanceId", "SN-0001"),
            PartInstanceItem.class), "singular");
    }

    @Test
    void serializedPartWithoutManufacturerPartId_isInvalid() throws Exception {
        ObjectNode node = item().put("partInstanceId", "SN-0001");
        node.remove("manufacturerPartId");

        assertSingleViolation(objectMapper.treeToValue(node, PartInstanceItem.class), "manufacturerPartId");
    }

    private ObjectNode item() {
        return objectMapper.createObjectNode()
            .put("manufacturerId", MANUFACTURER_BPNL)
            .put("manufacturerPartId", "MPI-4711")
            .put("catenaXId", CATENA_X_ID);
    }

    private static <T> void assertSingleViolation(T item, String expectedPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(item);
        assertEquals(1, violations.size());
        assertEquals(expectedPath, violations.iterator().next().getPropertyPath().toString());
    }
}
