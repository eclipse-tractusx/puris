/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.stock.controller;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemStockRequestApiController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class})
class ItemStockRequestApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    ItemStockRequestApiService itemStockRequestApiService;

    @Test
    @WithMockApiKey
    void getItemStockSamm_GivenNotImplementPath_Returns501() throws Exception {

        this.mockMvc.perform(
                get("/item-stock/request/material-number/OUTBOUND/description")
        ).andExpect(status().isNotImplemented());
    }
}
