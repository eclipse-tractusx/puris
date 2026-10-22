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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/

package org.eclipse.tractusx.puris.backend.irs.controller;

import java.util.List;
import java.util.NoSuchElementException;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.ModelMapperConfig;
import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJobStateEnumeration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestStatusEnumeration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsJobService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IrsJobController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class, ModelMapperConfig.class})
public class IrsJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IrsJobService irsJobService;

    @MockitoBean
    private IrsAdapterConfiguration irsAdapterConfiguration;

    private static final String SOURCE_DISRUPTION_ID = "urn:uuid:3b4edc05-e214-47a1-b0c2-1d831cdd9ba9";

    @Test
    @WithMockApiKey
    void getAllJobs_adapterEnabled_returnsJobs() throws Exception {
        IrsJob job = new IrsJob();
        job.setUuid(UUID.randomUUID());
        job.setJobId(UUID.randomUUID());
        job.setState(IrsJobStateEnumeration.RUNNING);
        job.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);
        Material material = new Material();
        material.setOwnMaterialNumber("MAT-123");
        job.setMaterial(material);

        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobService.findAll()).thenReturn(List.of(job));

        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].uuid").value(job.getUuid().toString()))
            .andExpect(jsonPath("$[0].state").value("RUNNING"))
            .andExpect(jsonPath("$[0].ownMaterialNumber").value("MAT-123"));

        verify(irsJobService).findAll();
    }

    @Test
    @WithMockApiKey
    void getAllJobs_adapterDisabled_returnsForbiddenAndSkipsRepository() throws Exception {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);

        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobService);
    }

    @Test
    void getAllJobs_withoutAuthHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(irsJobService);
        verifyNoInteractions(irsAdapterConfiguration);
    }

    @Test
    @WithMockApiKey(apiKey = "wrong-key")
    void getAllJobs_withWrongApiKey_returnsForbidden() throws Exception {
        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobService);
        verifyNoInteractions(irsAdapterConfiguration);
    }

    @Test
    @WithMockApiKey
    void createJob_validRootGrant_returnsCreatedJob() throws Exception {
        UUID rootGrantUuid = UUID.randomUUID();
        Material material = new Material();
        material.setOwnMaterialNumber("MAT-123");
        IrsJob saved = new IrsJob();
        saved.setUuid(UUID.randomUUID());
        saved.setMaterial(material);
        saved.setSourceDisruptionId(SOURCE_DISRUPTION_ID);
        saved.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);

        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobService.createAndSendForRootGrant(rootGrantUuid)).thenReturn(saved);

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(rootGrantUuid)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uuid").value(saved.getUuid().toString()))
            .andExpect(jsonPath("$.ownMaterialNumber").value("MAT-123"))
            .andExpect(jsonPath("$.sourceDisruptionId").value(SOURCE_DISRUPTION_ID))
            .andExpect(jsonPath("$.requestStatus").value("PENDING"));

        verify(irsJobService).createAndSendForRootGrant(rootGrantUuid);
    }

    @Test
    @WithMockApiKey
    void createJob_missingRootGrant_returnsBadRequest() throws Exception {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(irsJobService);
    }

    @Test
    @WithMockApiKey
    void createJob_unknownRootGrant_returnsNotFound() throws Exception {
        UUID rootGrantUuid = UUID.randomUUID();
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobService.createAndSendForRootGrant(rootGrantUuid))
            .thenThrow(new NoSuchElementException("IRS root grant does not exist."));

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(rootGrantUuid)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockApiKey
    void createJob_materialNotEligible_returnsBadRequest() throws Exception {
        UUID rootGrantUuid = UUID.randomUUID();
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobService.createAndSendForRootGrant(rootGrantUuid))
            .thenThrow(new IllegalArgumentException("Material must be a product to be used for an IRS job."));

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(rootGrantUuid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockApiKey
    void createJob_jobAlreadyQueued_returnsConflict() throws Exception {
        UUID rootGrantUuid = UUID.randomUUID();
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobService.createAndSendForRootGrant(rootGrantUuid))
            .thenThrow(new KeyAlreadyExistsException("An IRS job for this material and source disruption is already queued."));

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(rootGrantUuid)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockApiKey
    void createJob_adapterDisabled_returnsForbidden() throws Exception {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);

        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(UUID.randomUUID())))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobService);
    }

    @Test
    @WithMockUser(roles = "PURIS_USER")
    void createJob_asNonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(post("/irs/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootGrantBody(UUID.randomUUID())))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobService);
    }

    private static String rootGrantBody(UUID rootGrantUuid) {
        return "{\"rootGrantUuid\":\"" + rootGrantUuid + "\"}";
    }

}
