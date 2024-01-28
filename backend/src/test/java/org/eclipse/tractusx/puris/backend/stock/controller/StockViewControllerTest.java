/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.FrontendMaterialDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.*;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockViewController.class)
@Import({ SecurityConfig.class, ApiKeyAuthenticationProvider.class })
class StockViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private ProductItemStockService productItemStockService;

    @MockBean
    private MaterialItemStockService materialItemStockService;

    @MockBean
    private ItemStockRequestApiService itemStockRequestMessageService;

    @MockBean
    private ReportedMaterialItemStockService reportedMaterialItemStockService;

    @MockBean
    private ReportedProductItemStockService reportedProductItemStockService;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private MaterialPartnerRelationService mprService;

    @MockBean
    private ModelMapper modelMapper;

    @Test
    @WithMockApiKey
    void getMaterials_GivenTwoMaterials_ReturnsListOfMaterials() throws Exception{

        // given
        Material material1 = Material.builder()
            .ownMaterialNumber("MNR-4711")
            .materialFlag(true)
            .name("Test Material 1")
            .materialNumberCx("urn:uuid:ccfffbba-cfa0-49c4-bc9c-4e13d7a4ac7a")
            .build();
        Material material2 = Material.builder()
            .ownMaterialNumber("MNR-4712")
            .materialFlag(true)
            .name("Test Material 2")
            .build();
        List<Material> allMaterials = new ArrayList<>();
        allMaterials.add(material1);
        allMaterials.add(material2);
        when(materialService.findAllMaterials()).thenReturn(allMaterials);

        this.mockMvc.perform(
                get("/stockView/materials")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(result -> {
                String jsonResponse = result.getResponse().getContentAsString();
                ObjectMapper objectMapper = new ObjectMapper();

                List<FrontendMaterialDto> returnedMaterials = objectMapper.readValue(jsonResponse, new TypeReference<>() {
                });

                assertAll(
                    () -> assertNotNull(returnedMaterials),
                    () -> assertEquals(2, returnedMaterials.size())
                );

                assertAll(
                    () -> assertNotNull(returnedMaterials),
                    () -> assertEquals(2, returnedMaterials.size())
                );

                FrontendMaterialDto returnedMaterial = returnedMaterials.stream().filter(
                    frontendMaterialDto -> frontendMaterialDto.getOwnMaterialNumber().equals("MNR-4711")
                ).findFirst().get();
                assertAll(
                    () -> assertEquals("MNR-4711", returnedMaterial.getOwnMaterialNumber()),
                    () -> assertEquals("Test Material 1", returnedMaterial.getDescription())
                );
            });
    }

}
