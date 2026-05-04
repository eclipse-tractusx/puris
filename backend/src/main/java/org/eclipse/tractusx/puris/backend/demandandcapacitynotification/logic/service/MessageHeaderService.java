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
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service;
import java.util.Date;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageHeaderService {
    public static final String MESSAGE_HEADER_VERSION = "3.0.0";

    private final PartnerService partnerService;
    private final ObjectMapper objectMapper;

    public ObjectNode createHeader(Partner receiver, String context) {
        return createHeader(receiver, context, null);
    }

    public ObjectNode createHeader(Partner receiver, String context, UUID relatedMessageId) {
        var ownBpnl = partnerService.getOwnPartnerEntity().getBpnl();
        var header = objectMapper.createObjectNode();
        header.put("messageId", UUID.randomUUID().toString());
        header.put("context", context);
        header.put("sentDateTime", new Date().toString());
        header.put("senderBpn", ownBpnl);
        header.put("receiverBpn", receiver.getBpnl());
        header.put("version", MESSAGE_HEADER_VERSION);
        if (relatedMessageId != null) {
            header.put("relatedMessageId", relatedMessageId.toString());
        }
        return header;
    }

    public void validate(JsonNode header, String expectedContext, String edcBpnHeader) {
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
