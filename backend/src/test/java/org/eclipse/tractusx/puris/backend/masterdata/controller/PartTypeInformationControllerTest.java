/*
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.masterdata.controller;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.PartTypeInformationSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

@WebMvcTest(PartTypeInformationController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class})
class PartTypeInformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    PartnerService partnerService;

    @MockitoBean
    MaterialService materialService;

    @MockitoBean
    MaterialPartnerRelationService materialPartnerRelationService;

    @MockitoBean
    PartTypeInformationSammMapper sammMapper;

    String MATERIAL_OWN_MATERIAL_NUMBER = "some-number-4711";
    String MATERIAL_PARTNER_MATERIAL_NUMBER = "some-number-4711";
    String OWN_PARTNER_BPNL = "BPNL1234567890LE";
    String OTHER_PARTNER_BPNL = "BPNL0987654321LE";

    Material product;
    Partner ownPartner;
    Partner otherPartner;
    MaterialPartnerRelation mpr;

    Base64.Encoder encoder = Base64.getEncoder();
    
    
    void setup() {
        product = Material.builder()
            .ownMaterialNumber(MATERIAL_OWN_MATERIAL_NUMBER)
            .productFlag(true)
            .build();

        ownPartner = new Partner();
        ownPartner.setBpnl(OWN_PARTNER_BPNL);

        otherPartner = new Partner();
        otherPartner.setBpnl(OTHER_PARTNER_BPNL);

        mpr = new MaterialPartnerRelation(
            product,
            otherPartner,
            MATERIAL_PARTNER_MATERIAL_NUMBER,
            false,
            true
        );
    }
    

    @Test
    @WithMockApiKey
    void getPartTypeInformationSamm_GivenPathNotImplemented_Returns501() throws Exception {

        this.mockMvc.perform(
            get("/parttypeinformation/request/material-number/description")
        ).andExpect(status().isNotImplemented());
    }

    @Test
    @WithMockApiKey
    void resolveProduct_GivenSelfRequest_ResolvesProduct() throws Exception {

        String encodedMaterialNumber = encoder.encodeToString(MATERIAL_OWN_MATERIAL_NUMBER.getBytes());

        setup();

        when(partnerService.findByBpnl(OWN_PARTNER_BPNL)).thenReturn(ownPartner);
        when(materialService.findByOwnMaterialNumber(MATERIAL_OWN_MATERIAL_NUMBER)).thenReturn(product);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        this.mockMvc.perform(
            get("/parttypeinformation/"+ PartTypeInformationController.VERSION_1_0_0 +"/" +encodedMaterialNumber+"/submodel/$value" )
            .header("edc-bpn", OWN_PARTNER_BPNL)
        ).andExpect(status().isOk());

    }

    @Test
    @WithMockApiKey
    void resolveProduct_GivenPartnerRequest_ResolvesProduct() throws Exception {

        String encodedMaterialNumber = encoder.encodeToString(MATERIAL_OWN_MATERIAL_NUMBER.getBytes());

        setup();

        when(partnerService.findByBpnl(OTHER_PARTNER_BPNL)).thenReturn(otherPartner);
        when(materialService.findByOwnMaterialNumber(MATERIAL_OWN_MATERIAL_NUMBER)).thenReturn(product);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);        
        when(materialPartnerRelationService.find(any(Material.class), any(Partner.class))).thenReturn(mpr);

        this.mockMvc.perform(
            get("/parttypeinformation/"+ PartTypeInformationController.VERSION_2_0_0 +"/" +encodedMaterialNumber+"/submodel/$value" )
            .header("edc-bpn", OTHER_PARTNER_BPNL)
        ).andExpect(status().isOk());

    }
}
