/*
 * Copyright (c) 2026 Volkswagen AG
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.irs.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.ModelMapperConfig;
import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestTypeEnumeration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestQueueService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
    
@WebMvcTest(IrsQueuedRequestController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class, ModelMapperConfig.class})
public class IrsQueuedRequestControllerTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @MockitoBean
    private IrsRequestQueueService irsRequestQueueService;
 
    @MockitoBean
    private IrsRequestService irsRequestService;
 
    private static final String REQUESTS_PATH = "/irs/requests";
 
    private static final UUID REQUEST_UUID = UUID.randomUUID();
    private static final UUID LINKED_ENTITY_UUID = UUID.randomUUID();
 
    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");
    private static final Instant LAST_ATTEMPT_AT = Instant.parse("2026-09-21T10:05:00Z");
    private static final Instant NEXT_ATTEMPT_AT = Instant.parse("2026-09-21T10:10:00Z");
 
    @Test
    @WithMockApiKey
    void getAllQueuedRequests_AsAdmin_ReturnsAllFieldsAsDto() throws Exception {
        // given
        IrsQueuedRequest request = getFailedGrantUpdateRequest();
 
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsRequestQueueService.findAll()).thenReturn(List.of(request));
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(REQUESTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].uuid").value(REQUEST_UUID.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].method").value("PUT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].path").value("irs/chain-opening-grants/" + LINKED_ENTITY_UUID))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].queryParams").value("{\"bpn\":\"BPNL4444444444XX\"}"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].body").value("{\"key\":\"value\"}"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("FAILED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].attemptCount").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].maxAttempts").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].createdAt").value(CREATED_AT.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastAttemptAt").value(LAST_ATTEMPT_AT.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].nextAttemptAt").value(NEXT_ATTEMPT_AT.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastErrorMessage").value("HTTP 502: bad gateway"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("CHAIN_OPENING_ROOT_GRANT_UPDATE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].linkedEntityUuid").value(LINKED_ENTITY_UUID.toString()));
 
        verify(irsRequestQueueService).findAll();
    }
 
    @Test
    @WithMockApiKey
    void getAllQueuedRequests_WhenAdapterDisabled_Returns403WithReason() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(false);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(REQUESTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value(Matchers.containsString("IRS adapter is disabled")));
 
        verify(irsRequestQueueService, never()).findAll();
    }
 
    @Test
    @WithMockUser(roles = "PURIS_USER")
    void getAllQueuedRequests_AsNonAdmin_Returns403() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(REQUESTS_PATH)).andExpect(MockMvcResultMatchers.status().isForbidden());
 
        verify(irsRequestQueueService, never()).findAll();
    }
 
    private IrsQueuedRequest getFailedGrantUpdateRequest() {
        return IrsQueuedRequest.builder()
            .uuid(REQUEST_UUID)
            .method(IrsQueuedRequestMethodEnumeration.PUT)
            .path("irs/chain-opening-grants/" + LINKED_ENTITY_UUID)
            .queryParams("{\"bpn\":\"BPNL4444444444XX\"}")
            .body("{\"key\":\"value\"}")
            .status(IrsQueuedRequestStatusEnumeration.FAILED)
            .attemptCount(3)
            .maxAttempts(3)
            .createdAt(CREATED_AT)
            .lastAttemptAt(LAST_ATTEMPT_AT)
            .nextAttemptAt(NEXT_ATTEMPT_AT)
            .lastErrorMessage("HTTP 502: bad gateway")
            .type(IrsQueuedRequestTypeEnumeration.CHAIN_OPENING_ROOT_GRANT_UPDATE)
            .linkedEntityUuid(LINKED_ENTITY_UUID)
            .build();
    }
}