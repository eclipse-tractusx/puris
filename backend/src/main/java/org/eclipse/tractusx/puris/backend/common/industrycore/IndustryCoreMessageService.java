/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.common.industrycore;
import java.util.Date;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IndustryCoreMessageService {
    public static final String MESSAGE_HEADER_VERSION = "3.0.0";

    private final PartnerService partnerService;
    private final ObjectMapper objectMapper;

    public JsonNode createMessage(Partner receiver, IndustryCoreMessageContext context, Object samm) {
        return createMessage(receiver, context, samm, null);
    }

    public JsonNode createMessage(Partner receiver, IndustryCoreMessageContext context, Object samm, UUID relatedMessageId) {
        var body = objectMapper.createObjectNode();
        body.set("header", createHeader(receiver, context, relatedMessageId));
        body.set("content", buildContent(context, samm));
        return body;
    }

    private JsonNode buildContent(IndustryCoreMessageContext context, Object samm) {
        JsonNode sammNode = objectMapper.convertValue(samm, JsonNode.class);
        if (context.getContentKey() == null) {
            return sammNode;
        }
        var wrapper = objectMapper.createObjectNode();
        wrapper.set(context.getContentKey(), sammNode);
        return wrapper;
    }

    public ObjectNode createHeader(Partner receiver, IndustryCoreMessageContext context, UUID relatedMessageId) {
        var ownBpnl = partnerService.getOwnPartnerEntity().getBpnl();
        var header = objectMapper.createObjectNode();
        header.put("messageId", UUID.randomUUID().toString());
        header.put("context", context.getValue());
        header.put("sentDateTime", new Date().toString());
        header.put("senderBpn", ownBpnl);
        header.put("receiverBpn", receiver.getBpnl());
        header.put("version", MESSAGE_HEADER_VERSION);
        if (relatedMessageId != null) {
            header.put("relatedMessageId", relatedMessageId.toString());
        }
        return header;
    }

    public <T> T validateAndParse(JsonNode body, IndustryCoreMessageContext expectedContext, String senderBpn, Class<T> contentType) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            throw new IllegalArgumentException("Body is missing");
        }
        validate(body.get("header"), senderBpn, expectedContext);
        return parseContent(body.get("content"), expectedContext, contentType);
    }

    private <T> T parseContent(JsonNode content, IndustryCoreMessageContext context, Class<T> contentType) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            throw new IllegalArgumentException("Content is missing");
        }
        JsonNode payload = context.getContentKey() == null ? content : content.get(context.getContentKey());
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("Missing content payload");
        }
        try {
            return objectMapper.treeToValue(payload, contentType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid content payload", e);
        }
    }

    public void validate(JsonNode header, String senderBpn, IndustryCoreMessageContext expectedContext) {
        if (header == null || header.isMissingNode() || header.isNull()) {
            throw new IllegalArgumentException("Header is missing");
        }

        String sender = textOrThrow(header, "senderBpn");
        String receiverBpn = textOrThrow(header, "receiverBpn");
        String version = textOrThrow(header, "version");
        String context = textOrThrow(header, "context");

        if (!expectedContext.getValue().equals(context)) {
            throw new IllegalArgumentException("Unexpected context: " + context);
        }

        if (!partnerService.getOwnPartnerEntity().getBpnl().equals(receiverBpn)) {
            throw new IllegalArgumentException("receiverBpn does not match own BPNL");
        }
        if (senderBpn == null || !senderBpn.equals(sender)) {
            throw new IllegalArgumentException("senderBpn does not match edc-bpn header");
        }
        if (!MESSAGE_HEADER_VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported header version: " + version);
        }
    }

    private static String textOrThrow(JsonNode header, String field) {
        var node = header.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing/invalid header field: " + field);
        }
        return node.asText();
    }
}
