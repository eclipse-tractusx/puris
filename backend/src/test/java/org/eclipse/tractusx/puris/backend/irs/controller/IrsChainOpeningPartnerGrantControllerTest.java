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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.ModelMapperConfig;
import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningPartnerGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningPartnerGrantService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
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

@WebMvcTest(IrsChainOpeningPartnerGrantController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class, ModelMapperConfig.class})
public class IrsChainOpeningPartnerGrantControllerTest {
    @Autowired
    private MockMvc mockMvc;
 
    @MockitoBean
    private IrsChainOpeningPartnerGrantService irsChainOpeningPartnerGrantService;
 
    @MockitoBean
    private IrsRequestService irsRequestService;
 
    private static final String PARTNER_GRANTS_PATH = "/irs/partner-grants";
 
    private static final UUID GRANT_UUID = UUID.randomUUID();
 
    private static final UUID NOTIFICATION_UUID = UUID.randomUUID();
 
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
 
    private static final String GLOBAL_ASSET_ID = "urn:uuid:9b1f2a70-1c2d-4f2e-9a3b-7c5d8e1f2a34";
    private static final String SOURCE_DISRUPTION_ID = "0f3a7c21-5b8e-4d1a-9f6c-2e4b8a7d1c05";
 
    private static final String CUSTOMER_BPNL = "BPNL1234567890ZZ";
 
    private static final String UPSTREAM_SUPPLIER_BPNL = "BPNL2222222222RR";
 
    private static final String PURIS_USE_CASE = "PURIS_ITEM_STOCK_ANONYMIZED_RECURSIVE";
 
    private static final Instant VALID_FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant VALID_TO = Instant.parse("2026-09-30T00:00:00Z");
 
    @Test
    @WithMockApiKey
    void getAllPartnerGrants_AsAdmin_ReturnsAllFieldsAsDto() throws Exception {
        // given
        IrsChainOpeningPartnerGrant grant = getSyncedPartnerGrant();
 
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsChainOpeningPartnerGrantService.findAll()).thenReturn(List.of(grant));
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(PARTNER_GRANTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].uuid").value(GRANT_UUID.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].globalAssetId").value(GLOBAL_ASSET_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sourceDisruptionId").value(SOURCE_DISRUPTION_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].requesterBpn").value(CUSTOMER_BPNL))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].validFrom").value(VALID_FROM.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].validTo").value(VALID_TO.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].useCase").value(PURIS_USE_CASE))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].syncStatus").value("SYNCED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].allowedBpnls", Matchers.contains(UPSTREAM_SUPPLIER_BPNL)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reportedNotificationIds",
                Matchers.contains(NOTIFICATION_ID.toString())));
 
        verify(irsChainOpeningPartnerGrantService).findAll();
    }

    @Test
    @WithMockApiKey
    void getAllPartnerGrants_ReferencesNotifications() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsChainOpeningPartnerGrantService.findAll()).thenReturn(List.of(getSyncedPartnerGrant()));
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(PARTNER_GRANTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reportedNotificationIds", Matchers.not(Matchers.hasItem(NOTIFICATION_UUID.toString()))));
    }
 
    @Test
    @WithMockApiKey
    void getAllPartnerGrants_WhenAdapterDisabled_Returns403WithReason() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(false);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(PARTNER_GRANTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value(Matchers.containsString("IRS adapter is disabled")));
 
        verify(irsChainOpeningPartnerGrantService, never()).findAll();
    }
 
    @Test
    @WithMockUser(roles = "PURIS_USER")
    void getAllPartnerGrants_AsNonAdmin_Returns403() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(PARTNER_GRANTS_PATH)).andExpect(MockMvcResultMatchers.status().isForbidden());
 
        verify(irsChainOpeningPartnerGrantService, never()).findAll();
    }
    
    private IrsChainOpeningPartnerGrant getSyncedPartnerGrant() {
        Set<ReportedDemandAndCapacityNotification> notifications = new LinkedHashSet<>();
        notifications.add(notification(NOTIFICATION_UUID, NOTIFICATION_ID, UPSTREAM_SUPPLIER_BPNL));
 
        return IrsChainOpeningPartnerGrant.builder()
            .uuid(GRANT_UUID)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .requesterBpn(CUSTOMER_BPNL)
            .validFrom(VALID_FROM)
            .validTo(VALID_TO)
            .useCase(PURIS_USE_CASE)
            .syncStatus(IrsGrantSyncStatusEnumeration.SYNCED)
            .reportedNotifications(notifications)
            .build();
    }
 
    private static ReportedDemandAndCapacityNotification notification(UUID uuid, UUID notificationId, String partnerBpnl) {
        Partner partner = new Partner();
        partner.setBpnl(partnerBpnl);
 
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(uuid);
        notification.setNotificationId(notificationId);
        notification.setPartner(partner);
 
        return notification;
    }
}