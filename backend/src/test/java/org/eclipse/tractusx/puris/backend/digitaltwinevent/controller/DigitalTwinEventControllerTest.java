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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@WebMvcTest(DigitalTwinEventControllerTest.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class,
    TestConfig.class, IndustryCoreMessageService.class})
public class DigitalTwinEventControllerTest {
    private static final String OWN_BPNL = "BPNL4444444444XX";

    private static final String PARTNER_BPNL = "BPNL1234567890ZZ";

    private static final String CONNECT_TO_PARENT = "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VariablesService variablesService;

    @MockitoBean
    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        Partner ownPartner = new Partner();
        ownPartner.setBpnl(OWN_BPNL);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
    }

    @ParameterizedTest
    @CsvSource({
        "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0, ConnectToParent",
        "IndustryCore-DigitalTwinEvent-ConnectToChild:3.0.0, ConnectToChild",
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
        "IndustryCore-DigitalTwinEvent-ConnectToParent:3.0.0\n"
    })
    void contextNotMatchingPattern_post_returnsBadRequest(String context) throws Exception {
        postViaEdc(message(context).toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Context does not match IndustryCore-DigitalTwinEvent-<Operation>:3.0.0"));
    }

    @Test
    void headerWithoutContext_post_returnsBadRequest() throws Exception {
        ObjectNode body = message(CONNECT_TO_PARENT);
        header(body).remove("context");

        postViaEdc(body.toString())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Context does not match IndustryCore-DigitalTwinEvent-<Operation>:3.0.0"));
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

    private static ObjectNode header(ObjectNode body) {
        return (ObjectNode) body.get("header");
    }
}
