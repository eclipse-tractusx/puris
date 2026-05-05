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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public JsonNode createMessage(Partner receiver, MessageContext context, Object samm) {
        return createMessage(receiver, context, samm, null);
    }

    public JsonNode createMessage(Partner receiver, MessageContext context, Object samm, UUID relatedMessageId) {
        var body = objectMapper.createObjectNode();
        body.set("header", createHeader(receiver, context, relatedMessageId));
        body.set("content", buildContent(context, samm));
        return body;
    }

    private JsonNode buildContent(MessageContext context, Object samm) {
        JsonNode sammNode = objectMapper.convertValue(samm, JsonNode.class);
        if (context.getContentKey() == null) {
            return sammNode;
        }
        var wrapper = objectMapper.createObjectNode();
        wrapper.set(context.getContentKey(), sammNode);
        return wrapper;
    }

    public ObjectNode createHeader(Partner receiver, MessageContext context, UUID relatedMessageId) {
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

    public <T> T validateAndParse(JsonNode body, MessageContext expectedContext, String edcBpnHeader, Class<T> contentType) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is missing");
        }
        validate(body.get("header"), expectedContext, edcBpnHeader);
        return parseContent(body.get("content"), expectedContext, contentType);
    }

    private <T> T parseContent(JsonNode content, MessageContext context, Class<T> contentType) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is missing");
        }
        JsonNode payload = context.getContentKey() == null ? content : content.get(context.getContentKey());
        if (payload == null || payload.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing content payload");
        }
        try {
            return objectMapper.treeToValue(payload, contentType);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid content payload");
        }
    }

    public void validate(JsonNode header, MessageContext expectedContext, String edcBpnHeader) {
        if (header == null || header.isMissingNode() || header.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header is missing");
        }

        String senderBpn = textOrThrow(header, "senderBpn");
        String receiverBpn = textOrThrow(header, "receiverBpn");
        String context = textOrThrow(header, "context");
        String version = textOrThrow(header, "version");

        if (!partnerService.getOwnPartnerEntity().getBpnl().equals(receiverBpn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverBpn does not match own BPNL");
        }
        if (edcBpnHeader == null || !edcBpnHeader.equals(senderBpn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderBpn does not match edc-bpn header");
        }
        if (!MESSAGE_HEADER_VERSION.equals(version)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported header version: " + version);
        }
    }

    private static String textOrThrow(JsonNode header, String field) {
        var node = header.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing/invalid header field: " + field);
        }
        return node.asText();
    }
}
