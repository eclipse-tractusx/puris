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
package org.eclipse.tractusx.puris.backend.masterdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.masterdata.controller.MaterialController;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialEntityDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(MaterialController.class)
@Import({ SecurityConfig.class, ApiKeyAuthenticationProvider.class })
public class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    private final ModelMapper modelMapper = new ModelMapper();
    private final String materialNumber = "MNR-7307-AU340474.001";
    private final MaterialEntityDto materialDto = new MaterialEntityDto(false, false, materialNumber, String.valueOf(UUID.randomUUID()),"TestMaterialDto");

    @Test
    @WithMockApiKey
    void createMaterialTest() throws Exception {
        // when
        Material createdMaterial =  modelMapper.map(materialDto,Material.class);
        when(materialService.findByOwnMaterialNumber(materialNumber)).thenReturn(null);
        when(materialService.create(createdMaterial)).thenReturn(createdMaterial);

        // then
        mockMvc.perform(post("/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(materialDto)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(materialService).create(createdMaterial);
    }

    @Test
    @WithMockApiKey
    void updateMaterialTest() throws Exception {
        // when
        Material existingMaterial = modelMapper.map(materialDto,Material.class);
        when(materialService.findByOwnMaterialNumber(materialNumber)).thenReturn(existingMaterial);
        when(materialService.update(existingMaterial)).thenReturn(existingMaterial);

        // then
        mockMvc.perform(put("/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(materialDto)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(materialService).update(existingMaterial);
    }

    @Test
    @WithMockApiKey
    void getMaterialTest() throws Exception {
        // when
        Material foundMaterial = modelMapper.map(materialDto, Material.class);
        when(materialService.findByOwnMaterialNumber(materialNumber)).thenReturn(foundMaterial);

        // then
        mockMvc.perform(get("/materials")
                .param("ownMaterialNumber", materialNumber))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.ownMaterialNumber").value(materialNumber));

        verify(materialService).findByOwnMaterialNumber(materialNumber);
    }


    @Test
    @WithMockApiKey
    void listMaterialsTest() throws Exception {
        // given
        MaterialEntityDto dto2 = new MaterialEntityDto();
        dto2.setOwnMaterialNumber("MNR-7307-AU340474.002");

        // when
        Material material1 = modelMapper.map(materialDto, Material.class);
        Material material2 = modelMapper.map(dto2, Material.class);
        List<Material> materialList = Arrays.asList(material1, material2);
        when(materialService.findAll()).thenReturn(materialList);

        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/materials/all")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].ownMaterialNumber").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].ownMaterialNumber").exists());

        verify(materialService).findAll();
    }

    private static String asJsonString(Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
