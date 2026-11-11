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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.AssertTrue;

/**
 * Item of a ConnectToParent message for digitalTwinType PartInstance.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(SerializedPartItem.class),
    @JsonSubTypes.Type(BatchItem.class),
    @JsonSubTypes.Type(JisItem.class)
})
public abstract class PartInstanceItem extends PartItem {
    private static final Set<String> TYPE_PROPERTIES = Set.of("partInstanceId", "batchId", "jisNumber");

    @JsonIgnore
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void setOtherProperty(String name, Object value) {
        otherProperties.put(name, value);
    }

    @JsonIgnore
    @AssertTrue(message = "must contain exactly one of partInstanceId, batchId or jisNumber")
    public boolean isSingular() {
        return otherProperties.keySet().stream().noneMatch(TYPE_PROPERTIES::contains);
    }
}
