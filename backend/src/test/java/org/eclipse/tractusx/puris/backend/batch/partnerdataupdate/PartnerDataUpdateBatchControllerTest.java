/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate;

import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.controller.PartnerDataUpdateBatchController;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto.PartnerDataUpdateBatchRunDto;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchProcessService;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.eclipse.tractusx.puris.backend.common.ModelMapperConfig;
import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@WebMvcTest(PartnerDataUpdateBatchController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class, ModelMapperConfig.class})
public class PartnerDataUpdateBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerDataUpdateBatchService batchService;

    @MockitoBean
    private PartnerDataUpdateBatchProcessService processService;

    @Autowired
    private ModelMapper modelMapper;

    @Test
    @WithMockApiKey
    void history_AsAdmin_Returns200() throws Exception {
        PartnerDataUpdateBatchRun run = PartnerDataUpdateBatchRun.builder()
            .startTime(OffsetDateTime.now(ZoneOffset.UTC))
            .status(BatchRunStatusEnum.IN_PROGRESS)
            .build();

        Pageable pageable = PageRequest.of(0, 10);
        List<PartnerDataUpdateBatchRun> list = List.of(run);
        
        Page<PartnerDataUpdateBatchRun> mockPage = new PageImpl<>(list, pageable, list.size());

        when(batchService.findAll(any(Pageable.class))).thenReturn(mockPage);
        mockMvc.perform(get("/admin/batch/partner-data-update"))
            .andExpect(status().isOk());
    }

    @Test
    void history_AsUser_Returns401() throws Exception {
        // no api key header -> unauthorized/forbidden
        mockMvc.perform(get("/admin/batch/partner-data-update")).andExpect(status().is(401));
    }
}

