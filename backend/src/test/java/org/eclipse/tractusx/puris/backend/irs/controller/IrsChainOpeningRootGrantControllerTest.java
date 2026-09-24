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
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsChainOpeningRootGrant;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsGrantSyncStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsChainOpeningRootGrantService;
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

@WebMvcTest(IrsChainOpeningRootGrantController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class, ModelMapperConfig.class})
public class IrsChainOpeningRootGrantControllerTest {
    @Autowired
    private MockMvc mockMvc;
 
    @MockitoBean
    private IrsChainOpeningRootGrantService irsChainOpeningRootGrantService;
 
    @MockitoBean
    private IrsRequestService irsRequestService;
 
    private static final String ROOT_GRANTS_PATH = "/irs/root-grants";
 
    private static final UUID GRANT_UUID = UUID.randomUUID();

    private static final UUID NOTIFICATION_UUID = UUID.randomUUID();
 
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
 
    private static final String GLOBAL_ASSET_ID = "urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df";
    private static final String SOURCE_DISRUPTION_ID = "b6f5c8b4-6d4a-4a9c-9a0e-6d2b4a1f0c33";
 
    private static final String OWN_BPNL = "BPNL4444444444XX";
    private static final String SUPPLIER_BPNL = "BPNL1234567890ZZ";
 
    private static final Instant VALID_FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant VALID_TO = Instant.parse("2026-09-30T00:00:00Z");
 
    @Test
    @WithMockApiKey
    void getAllRootGrants_AsAdmin_ReturnsAllFieldsAsDto() throws Exception {
        // given
        IrsChainOpeningRootGrant grant = getSyncedRootGrant();
 
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
        when(irsChainOpeningRootGrantService.findAll()).thenReturn(List.of(grant));
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(ROOT_GRANTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].uuid").value(GRANT_UUID.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].globalAssetId").value(GLOBAL_ASSET_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sourceDisruptionId").value(SOURCE_DISRUPTION_ID))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].requesterBpn").value(OWN_BPNL))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].validFrom").value(VALID_FROM.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].validTo").value(VALID_TO.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].syncStatus").value("SYNCED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].allowedBpnlSet", Matchers.contains(SUPPLIER_BPNL)));
 
        verify(irsChainOpeningRootGrantService).findAll();
    }
 
    @Test
    @WithMockApiKey
    void getAllRootGrants_WhenAdapterDisabled_Returns403WithReason() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(false);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(ROOT_GRANTS_PATH))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value(Matchers.containsString("IRS adapter is disabled")));
 
        verify(irsChainOpeningRootGrantService, never()).findAll();
    }
 
    @Test
    @WithMockUser(roles = "PURIS_USER")
    void getAllRootGrants_AsNonAdmin_Returns403() throws Exception {
        // when
        when(irsRequestService.isEnabled()).thenReturn(true);
 
        // then
        mockMvc.perform(MockMvcRequestBuilders.get(ROOT_GRANTS_PATH)).andExpect(MockMvcResultMatchers.status().isForbidden());
 
        verify(irsChainOpeningRootGrantService, never()).findAll();
    }
 
    private IrsChainOpeningRootGrant getSyncedRootGrant() {
        Set<ReportedDemandAndCapacityNotification> notifications = new LinkedHashSet<>();
        notifications.add(notification(NOTIFICATION_UUID, NOTIFICATION_ID, SUPPLIER_BPNL));
 
        return IrsChainOpeningRootGrant.builder()
            .uuid(GRANT_UUID)
            .globalAssetId(GLOBAL_ASSET_ID)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .requesterBpn(OWN_BPNL)
            .validFrom(VALID_FROM)
            .validTo(VALID_TO)
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
