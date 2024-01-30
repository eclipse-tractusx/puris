/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialPartnerRelationsController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class})
public class MaterialPartnerRelationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private MaterialPartnerRelationService mprService;

    private final String materialNumber = "MNR-7307-AU340474.001";
    private final String bpnl = "BPNL2222222222RR";
    private final String edcUrl = "https://example.com";
    private final String bpna = "BPNA1234567890AA";
    private final String bpns = "BPNS1234567890SS";
    private final Partner partner = new Partner("TestPartner", edcUrl, bpnl, bpns, "TestSite", bpna, "Test Street", "Test City", "DE");
    private final Material material = Material.builder()
        .ownMaterialNumber(materialNumber)
        .materialFlag(true)
        .materialNumberCx(String.valueOf(UUID.randomUUID()))
        .name("TestMaterial")
        .productFlag(true)
        .build();

    @Test
    @WithMockApiKey
    public void createMaterialPartnerRelationTest() throws Exception {
        // given
        String partnerMaterialNumber = "MNR-8101-ID146955.001";
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, partnerMaterialNumber,
            true, true);



        // when
        when(materialService.findByOwnMaterialNumber(materialNumber)).thenReturn(material);
        when(partnerService.findByBpnl(bpnl)).thenReturn(partner);
        when(mprService.find(material, partner)).thenReturn(null);
        when(mprService.create(any(MaterialPartnerRelation.class))).thenReturn(newMpr);

        // then
        mockMvc.perform(post("/materialpartnerrelations")
                .param("ownMaterialNumber", materialNumber)
                .param("partnerBpnl", bpnl)
                .param("partnerMaterialNumber", partnerMaterialNumber)
                .param("partnerSupplies", "true")
                .param("partnerBuys", "true"))
            .andExpect(status().isOk());

        verify(materialService).findByOwnMaterialNumber(materialNumber);
        verify(partnerService).findByBpnl(bpnl);
        verify(mprService).find(material, partner);
        verify(mprService).create(any(MaterialPartnerRelation.class));
    }

    @Test
    @WithMockApiKey
    public void createMaterialPartnerRelationTestPatternMatchShouldFail() throws Exception {
        // given
        String bpnlWrong = "WrongPattern";
        Partner partner = new Partner();
        partner.setBpnl(bpnlWrong);
        partner.setEdcUrl(edcUrl);
        partner.setName("TestPartner");

        String partnerMaterialNumber = "MNR-8101-ID146955.002";
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, partnerMaterialNumber,
            true, true);

        // when
        when(materialService.findByOwnMaterialNumber(materialNumber)).thenReturn(material);
        when(partnerService.findByBpnl(bpnlWrong)).thenReturn(partner);
        when(mprService.find(material, partner)).thenReturn(null);
        when(mprService.create(any(MaterialPartnerRelation.class))).thenReturn(newMpr);

        // then
        mockMvc.perform(post("/materialpartnerrelations")
                .param("ownMaterialNumber", materialNumber)
                .param("partnerBpnl", bpnlWrong)
                .param("partnerMaterialNumber", partnerMaterialNumber)
                .param("partnerSupplies", "true")
                .param("partnerBuys", "true"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockApiKey
    public void updateMaterialPartnerRelationTest() throws Exception {
        // given
        String partnerMaterialNumber = "MNR-8101-ID146955.001";
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, partnerMaterialNumber,
            true, true);


        String partnerMaterialNumber2 = "MNR-8101-ID146955.002";
        MaterialPartnerRelation newMpr2 = new MaterialPartnerRelation(material, partner, partnerMaterialNumber2,
            true, true);

        // when
        when(partnerService.findByBpnl(anyString())).thenReturn(partner);
        when(materialService.findByOwnMaterialNumber(anyString())).thenReturn(material);
        when(mprService.find(any(Material.class), any(Partner.class))).thenReturn(newMpr);
        when(mprService.update(any(MaterialPartnerRelation.class))).thenReturn(newMpr2);

        // then
        mockMvc.perform(put("/materialpartnerrelations")
                .param("ownMaterialNumber", materialNumber)
                .param("partnerBpnl", bpnl)
                .param("partnerMaterialNumber", partnerMaterialNumber2)
                .param("partnerSupplies", "true")
                .param("partnerBuys", "true")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(mprService).find(material, partner);
        verify(mprService).update(any(MaterialPartnerRelation.class));
    }

    @Test
    @WithMockApiKey
    public void updateMaterialPartnerRelationTestWrongPatternShouldFail() throws Exception {
        // given
        String bpnlWrong = "WrongPattern";
        Partner partner = new Partner();
        partner.setBpnl(bpnlWrong);
        partner.setEdcUrl(edcUrl);
        partner.setName("TestPartner");

        String partnerMaterialNumber = "MNR-8101-ID146955.001";
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, partnerMaterialNumber,
            true, true);


        String partnerMaterialNumber2 = "MNR-8101-ID146955.002";
        MaterialPartnerRelation newMpr2 = new MaterialPartnerRelation(material, partner, partnerMaterialNumber2,
            true, true);


        // when
        when(partnerService.findByBpnl(anyString())).thenReturn(partner);
        when(materialService.findByOwnMaterialNumber(anyString())).thenReturn(material);
        when(mprService.find(any(Material.class), any(Partner.class))).thenReturn(newMpr);
        when(mprService.update(any(MaterialPartnerRelation.class))).thenReturn(newMpr2);

        // then
        mockMvc.perform(put("/materialpartnerrelations")
                .param("ownMaterialNumber", materialNumber)
                .param("partnerBpnl", bpnlWrong)
                .param("partnerMaterialNumber", partnerMaterialNumber2)
                .param("partnerSupplies", "true")
                .param("partnerBuys", "true")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError());
    }
}
