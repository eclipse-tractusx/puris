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
package org.eclipse.tractusx.puris.backend.irs.controller;

import java.util.List;
import java.util.UUID;

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
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IrsJobController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class})
public class IrsJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IrsJobRepository irsJobRepository;

    @MockitoBean
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Test
    @WithMockApiKey
    void getAllJobs_adapterEnabled_returnsJobs() throws Exception {
        IrsJob job = new IrsJob();
        job.setUuid(UUID.randomUUID());
        job.setJobId(UUID.randomUUID());
        job.setState(IrsJobStateEnumeration.RUNNING);
        job.setRequestStatus(IrsQueuedRequestStatusEnumeration.PENDING);

        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsJobRepository.findAll()).thenReturn(List.of(job));

        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].uuid").value(job.getUuid().toString()))
            .andExpect(jsonPath("$[0].state").value("RUNNING"));

        verify(irsJobRepository).findAll();
    }

    @Test
    @WithMockApiKey
    void getAllJobs_adapterDisabled_returnsForbiddenAndSkipsRepository() throws Exception {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);

        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobRepository);
    }

    @Test
    void getAllJobs_withoutAuthHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(irsJobRepository);
        verifyNoInteractions(irsAdapterConfiguration);
    }

    @Test
    @WithMockApiKey(apiKey = "wrong-key")
    void getAllJobs_withWrongApiKey_returnsForbidden() throws Exception {
        mockMvc.perform(get("/irs/jobs"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(irsJobRepository);
        verifyNoInteractions(irsAdapterConfiguration);
    }

}
