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
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Named.named;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.ParentData;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UniqueIdPushConnectTochildPayload;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UsageAsPlannedItem;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.service.ConnectToChildService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@WebMvcTest(DigitalTwinEventController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class,
    TestConfig.class, IndustryCoreMessageService.class})
public class DigitalTwinEventControllerTest {
    private static final String OWN_BPNL = "BPNL4444444444XX";

    private static final String PARTNER_BPNL = "BPNL1234567890ZZ";

    private static final String CONNECT_TO_PARENT = "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0";

    private static final String CONNECT_TO_CHILD = "IndustryCore-DigitalTwinEvent-ConnectToChild:3.0.0";

    private static final String CHILD_CATENA_X_ID = "urn:uuid:5d8e2f1a-6b3c-4d7e-8f9a-1b2c3d4e5f6a";

    private static final String PARENT_CATENA_X_ID = "urn:uuid:9a3f1b2c-7d4e-4f5a-8b6c-0e1d2f3a4b5c";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VariablesService variablesService;

    @MockitoBean
    private PartnerService partnerService;

    @MockitoBean
    private ConnectToChildService connectToChildService;

    private Partner partner;

    @BeforeEach
    void setUp() {
        Partner ownPartner = new Partner();
        ownPartner.setBpnl(OWN_BPNL);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        partner = new Partner();
        partner.setBpnl(PARTNER_BPNL);
        when(partnerService.findByBpnl(PARTNER_BPNL)).thenReturn(partner);
    }

    @ParameterizedTest
    @CsvSource({
        "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0, ConnectToParent",
        "IndustryCore-DigitalTwinEvent-SubmodelUpdate:3.0.0, SubmodelUpdate",
        "IndustryCore-DigitalTwinEvent-Feedback:3.0.0, Feedback"
    })
    void validMessage_post_isRoutedToHandlerOfOperation(String context, String operation) throws Exception {
        postViaEdc(message(context).toString())
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.detail").value(operation + " is not implemented yet"));
    }

    @Test
    void messageWithoutHeader_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        body.remove("header");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Header is missing"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"senderBpn", "receiverBpn", "version"})
    void headerWithoutRequiredField_post_returnsBadRequest(String field) throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).remove(field);

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Missing/invalid header field: " + field));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "ConnectToParent",
        "IndustryCore-DigitalTwinEvent-ConnectToParent",
        "IndustryCore-DigitalTwinEvent-ConnectToParent:2.0.0",
        "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0",
        "IndustryCore-DigitalTwinEvent-:3.0.0",
        "IndustryCore-DigitalTwinEvent-Connect-To-Parent:3.0.0",
        "industrycore-digitaltwinevent-ConnectToParent:3.0.0",
        "CX-DataExchangeRequestAPI-RequestReceive:1.0.0",
        " IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0",
        "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0\n",
        "IndustryCore-DigitalTwinEvent-Delete:3.0.0",
        "IndustryCore-DigitalTwinEvent-connectToParent:3.0.0",
        "IndustryCore-DigitalTwinEvent-FEEDBACK:3.0.0",
        "IndustryCore-DigitalTwinEvent-ConnectToParentAndChild:3.0.0"
    })
    void contextNotMatchingPattern_post_returnsBadRequest(String context) throws Exception {
        postViaEdc(message(context).toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Context does not match IndustryCore-DigitalTwinEvent-<ConnectToParent | ConnectToChild | SubmodelUpdate | Feedback>:3.0.0"));
    }

    @Test
    void headerWithoutContext_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).remove("context");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Context does not match IndustryCore-DigitalTwinEvent-<ConnectToParent | ConnectToChild | SubmodelUpdate | Feedback>:3.0.0"));
    }

    @Test
    void senderNotMatchingEdcBpn_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).put("senderBpn", "BPNL0000000001AB");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("senderBpn does not match edc-bpn header"));
    }

    @Test
    void receiverNotOwnBpnl_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).put("receiverBpn", "BPNL0000000001AB");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("receiverBpn does not match own BPNL"));
    }

    @Test
    void unsupportedHeaderVersion_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).put("version", "2.0.0");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unsupported header version: 2.0.0"));
    }

    @Test
    void missingEdcBpn_post_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/digital-twin-events")
                .header("X-API-KEY", variablesService.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(message(CONNECT_TO_PARENT).toString()))
            .andExpect(status().isBadRequest());
    }

    private ResultActions postViaEdc(String body) throws Exception {
        return mockMvc.perform(post("/digital-twin-events")
            .header("X-API-KEY", variablesService.getApiKey())
            .header("edc-bpn", PARTNER_BPNL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    @Test
    void validConnectToChild_post_isHandledAndAnsweredWithOk() throws Exception {
        postViaEdc(connectToChildMessage().toString())
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        ArgumentCaptor<UniqueIdPushConnectTochildPayload> payload = ArgumentCaptor.forClass(UniqueIdPushConnectTochildPayload.class);
        verify(connectToChildService).handleConnectToChild(eq(partner), payload.capture());
        assertEquals("PartType", payload.getValue().getDigitalTwinType());
        UsageAsPlannedItem item = payload.getValue().getListOfItems().get(0);
        assertEquals(CHILD_CATENA_X_ID, item.getCatenaXId());
        ParentData parent = item.getParentItems().get(0);
        assertEquals(PARENT_CATENA_X_ID, parent.getCatenaXId());
        assertEquals(PARTNER_BPNL, parent.getBusinessPartner());
        assertEquals(2.0, parent.getQuantity().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, parent.getQuantity().getUnit());
        assertEquals(Date.from(Instant.parse("2026-01-01T00:00:00Z")), parent.getValidityPeriod().getValidFrom());
        assertNull(parent.getValidityPeriod().getValidTo());
        assertEquals(Date.from(Instant.parse("2026-10-09T08:00:00Z")), parent.getCreatedOn());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentsNotMatchingSpecification")
    void connectToChildNotMatchingSpecification_post_returnsBadRequest(Consumer<ObjectNode> invalidate, String expectedDetailStart) throws Exception {
        ObjectNode body = connectToChildMessage();
        invalidate.accept((ObjectNode) body.get("content"));

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(startsWith(expectedDetailStart)));

        verify(connectToChildService, never()).handleConnectToChild(any(), any());
    }

    static Stream<Arguments> contentsNotMatchingSpecification() {
        String invalid = "Invalid content: ";
        String parentPath = invalid + "listOfItems[0].parentItems[0].";
        return Stream.of(
            Arguments.of(named("digitalTwinType missing", (Consumer<ObjectNode>) content -> content.remove("digitalTwinType")),
                invalid + "digitalTwinType "),
            Arguments.of(named("PartInstance twin", (Consumer<ObjectNode>) content -> content.put("digitalTwinType", "PartInstance")),
                invalid + "digitalTwinType must be PartType, other Digital Twin types are not supported"),
            Arguments.of(named("information too long", (Consumer<ObjectNode>) content -> content.put("information", "x".repeat(1001))),
                invalid + "information "),
            Arguments.of(named("listOfItems missing", (Consumer<ObjectNode>) content -> content.remove("listOfItems")),
                invalid + "listOfItems "),
            Arguments.of(named("child catenaXId missing", (Consumer<ObjectNode>) content -> item(content).remove("catenaXId")),
                invalid + "listOfItems[0].catenaXId "),
            Arguments.of(named("child catenaXId no UUID", (Consumer<ObjectNode>) content -> item(content).put("catenaXId", "MNR-8101")),
                invalid + "listOfItems[0].catenaXId "),
            Arguments.of(named("parentItems missing", (Consumer<ObjectNode>) content -> item(content).remove("parentItems")),
                invalid + "listOfItems[0].parentItems "),
            Arguments.of(named("parent catenaXId missing", (Consumer<ObjectNode>) content -> parent(content).remove("catenaXId")),
                parentPath + "catenaXId "),
            Arguments.of(named("businessPartner missing", (Consumer<ObjectNode>) content -> parent(content).remove("businessPartner")),
                parentPath + "businessPartner "),
            Arguments.of(named("businessPartner no BPNL", (Consumer<ObjectNode>) content -> parent(content).put("businessPartner", "BPNS1234567890ZZ")),
                parentPath + "businessPartner "),
            Arguments.of(named("createdOn missing", (Consumer<ObjectNode>) content -> parent(content).remove("createdOn")),
                parentPath + "createdOn "),
            Arguments.of(named("quantity missing", (Consumer<ObjectNode>) content -> parent(content).remove("quantity")),
                parentPath + "quantity "),
            Arguments.of(named("quantity value missing", (Consumer<ObjectNode>) content -> ((ObjectNode) parent(content).get("quantity")).remove("value")),
                parentPath + "quantity.value "),
            Arguments.of(named("unknown unit", (Consumer<ObjectNode>) content -> ((ObjectNode) parent(content).get("quantity")).put("unit", "unit:banana")),
                "Invalid content payload"),
            Arguments.of(named("createdOn no date", (Consumer<ObjectNode>) content -> parent(content).put("createdOn", "yesterday")),
                "Invalid content payload")
        );
    }

    @Test
    void connectToChildForMaterialNotSuppliedToSender_post_returnsNotFound() throws Exception {
        String detail = "Child material " + CHILD_CATENA_X_ID + " is not supplied to " + PARTNER_BPNL;
        doThrow(new NoSuchElementException(detail))
            .when(connectToChildService).handleConnectToChild(eq(partner), any());

        postViaEdc(connectToChildMessage().toString())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void connectToChildWithParentOfOtherPartner_post_returnsBadRequest() throws Exception {
        String detail = "businessPartner of parent " + PARENT_CATENA_X_ID + " does not match the sender " + PARTNER_BPNL;
        doThrow(new IllegalArgumentException(detail))
            .when(connectToChildService).handleConnectToChild(eq(partner), any());

        postViaEdc(connectToChildMessage().toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(detail));
    }

    private ObjectNode message(String context) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode header = body.putObject("header");
        header.put("messageId", "3b4edc05-e214-47a1-b0c2-1d831cdd9ba9");
        header.put("context", context);
        header.put("sentDateTime", "2026-10-09T10:00:00+02:00");
        header.put("senderBpn", PARTNER_BPNL);
        header.put("receiverBpn", OWN_BPNL);
        header.put("version", "3.0.0");
        body.putObject("content");
        return body;
    }

    private ObjectNode connectToChildMessage() {
        ObjectNode body = message(CONNECT_TO_CHILD);
        ObjectNode content = body.putObject("content");
        content.put("digitalTwinType", "PartType");
        content.put("information", "Usage as Planned information for parts supplied by the receiver.");
        ObjectNode item = content.putArray("listOfItems").addObject();
        item.put("catenaXId", CHILD_CATENA_X_ID);
        ObjectNode parent = item.putArray("parentItems").addObject();
        parent.put("catenaXId", PARENT_CATENA_X_ID);
        parent.put("businessPartner", PARTNER_BPNL);
        ObjectNode quantity = parent.putObject("quantity");
        quantity.put("value", 2.0);
        quantity.put("unit", "unit:piece");
        ObjectNode validityPeriod = parent.putObject("validityPeriod");
        validityPeriod.put("validFrom", "2026-01-01T00:00:00.000Z");
        validityPeriod.putNull("validTo");
        parent.put("createdOn", "2026-10-09T10:00:00+02:00");
        parent.put("lastModifiedOn", "2026-10-09T10:00:00+02:00");
        return body;
    }

    private static ObjectNode header(ObjectNode body) {
        return (ObjectNode) body.get("header");
    }

    private static ObjectNode item(ObjectNode content) {
        return (ObjectNode) content.get("listOfItems").get(0);
    }

    private static ObjectNode parent(ObjectNode content) {
        return (ObjectNode) item(content).get("parentItems").get(0);
    }
}
