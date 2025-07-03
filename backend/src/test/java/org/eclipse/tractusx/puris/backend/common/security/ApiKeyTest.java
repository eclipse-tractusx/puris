/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.common.security;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.controller.StockViewController;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedMaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedProductItemStockService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.CustomerSupplyService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.SupplierSupplyService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(StockViewController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class})
public class ApiKeyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService materialService;

    @MockitoBean
    private ProductItemStockService productItemStockService;

    @MockitoBean
    private MaterialItemStockService materialItemStockService;

    @MockitoBean
    private ItemStockRequestApiService itemStockRequestMessageService;

    @MockitoBean
    private ReportedMaterialItemStockService reportedMaterialItemStockService;

    @MockitoBean
    private ReportedProductItemStockService reportedProductItemStockService;

    @MockitoBean
    private CustomerSupplyService customerSupplyService;

    @MockitoBean
    private SupplierSupplyService supplierSupplyService;

    @MockitoBean
    private PartnerService partnerService;

    @MockitoBean
    private MaterialPartnerRelationService mprService;

    @MockitoBean
    private ModelMapper modelMapper;

    @Test
    void StockViewController_MaterialsRequestWithoutAuthHeader_ShouldReturn403() throws Exception {
        this.mockMvc.perform(
            get("/stockView/materials"))
                .andDo(print())
            .andExpect(status().is(401));
    }

    @Test
    void StockViewController_MaterialsRequestWithAuthHeader_ShouldReturn200() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
                    .header("X-API-KEY", "test")
            )
            .andExpect(status().is(200));
    }

    @Test
    @WithMockApiKey(apiKey = "test2")
    void StockViewController_MaterialsRequestWithWrongAnnotationAuth_ShouldReturn403() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
            )
            .andExpect(status().is(403));
    }

    @Test
    @WithMockApiKey
    void StockViewController_MaterialsRequestWithCorrectAnnotationAuth_ShouldReturn200() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
            )
            .andExpect(status().is(200));
    }

}
