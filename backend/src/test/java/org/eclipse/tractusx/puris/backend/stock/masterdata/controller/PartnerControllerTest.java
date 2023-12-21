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
package org.eclipse.tractusx.puris.backend.stock.masterdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.masterdata.controller.PartnerController;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Address;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.AddressDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.SiteDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(PartnerController.class)
@Import({ SecurityConfig.class, ApiKeyAuthenticationProvider.class })
public class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerService partnerService;
    private final ModelMapper modelMapper = new ModelMapper();

    @Test
    @WithMockApiKey
    void createPartnerTest() throws Exception {
        // given
        PartnerDto partnerDto = new PartnerDto();
        partnerDto.setBpnl("123");
        partnerDto.setName("TestName");
        partnerDto.setEdcUrl("https://exampleTest.com");


        // when
        Partner createdPartner = modelMapper.map(partnerDto, Partner.class);
        when(partnerService.findByBpnl("123")).thenReturn(null);
        when(partnerService.create(createdPartner)).thenReturn(createdPartner);

        // then
        mockMvc.perform(post("/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(partnerDto)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(partnerService).create(createdPartner);
    }

    @Test
    @WithMockApiKey
    void addAddressTest() throws Exception {
        // given
        String partnerBpnl = "BPNL2222222222RR";
        AddressDto addressDto = new AddressDto();
        addressDto.setBpna("testBpna");
        addressDto.setCountry("DE");
        addressDto.setStreetAndNumber("Grove Street");
        addressDto.setZipCodeAndCity("San Andreas");

        Partner existingPartner = new Partner();
        existingPartner.setBpnl(partnerBpnl);
        existingPartner.setName("TestName");
        existingPartner.setEdcUrl("testUrl.com");

        // when
        when(partnerService.findByBpnl(partnerBpnl)).thenReturn(existingPartner);
        Address newAddress = modelMapper.map(addressDto, Address.class);
        when(partnerService.update(existingPartner)).thenReturn(existingPartner);

        // then
        mockMvc.perform(put("/partners/putAddress")
                .param("partnerBpnl", partnerBpnl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(addressDto)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(partnerService).update(existingPartner);
    }

    @Test
    @WithMockApiKey
    void addSiteTest() throws Exception {
        // given
        String partnerBpnl = "BPNL2222222222RR";
        SiteDto siteDto = new SiteDto();
        siteDto.setName("WebsiteTest");
        siteDto.setBpns("testBpns");


        Partner existingPartner = new Partner();
        existingPartner.setBpnl(partnerBpnl);
        existingPartner.setName("TestName");
        existingPartner.setEdcUrl("testUrl.com");

        // when

        when(partnerService.findByBpnl(partnerBpnl)).thenReturn(existingPartner);
        Site newSite = modelMapper.map(siteDto, Site.class);
        when(partnerService.update(existingPartner)).thenReturn(existingPartner);

        // then
        mockMvc.perform(put("/partners/putSite")
                .param("partnerBpnl", partnerBpnl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(siteDto)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        verify(partnerService).update(existingPartner);
    }

    @Test
    @WithMockApiKey
    void getPartnerTest() throws Exception {
        // given
        String partnerBpnl = "BPNL2222222222RR";
        Partner partner = new Partner();
        partner.setBpnl(partnerBpnl);
        partner.setName("TestName");
        partner.setEdcUrl("example.com");

        // when
        when(partnerService.findByBpnl(partnerBpnl)).thenReturn(partner);
        PartnerDto partnerDto = modelMapper.map(partner, PartnerDto.class);

        // then
        mockMvc.perform(get("/partners")
                .param("partnerBpnl", partnerBpnl))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.bpnl").value(partnerBpnl));

        verify(partnerService).findByBpnl(partnerBpnl);
    }

    @Test
    @WithMockApiKey
    void listPartnersTest() throws Exception {
        // given
        Partner partner1 = new Partner();
        partner1.setBpnl("partner1Bpnl");
        Partner partner2 = new Partner();
        partner2.setBpnl("partner2Bpnl");
        List<Partner> partnerList = Arrays.asList(partner1, partner2);

        // when
        PartnerDto dto1 = modelMapper.map(partner1, PartnerDto.class);
        PartnerDto dto2 = modelMapper.map(partner2, PartnerDto.class);
        List<PartnerDto> dtoList = Arrays.asList(dto1, dto2);
        when(partnerService.findAll()).thenReturn(partnerList);

        // then
        mockMvc.perform(get("/partners/all")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].bpnl").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].bpnl").exists());

        verify(partnerService).findAll();
    }

    @Test
    @WithMockApiKey
    void getOwnSitesWithSitesTest() throws Exception {
        // given
        Partner ownPartnerEntity = new Partner();
        ownPartnerEntity.setBpnl("partnerBpnl");
        TreeSet<Site> treeSet = new TreeSet<>();
        Site site1 = new Site();
        site1.setBpns("testBpns");
        site1.setName("testName");


        Site site2 = new Site();
        site2.setBpns("testBpns2");
        site2.setName("testName2");

        treeSet.add(site1);
        treeSet.add(site2);
        ownPartnerEntity.setSites(treeSet);

        // when
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartnerEntity);
        SiteDto dto1 = modelMapper.map(site1, SiteDto.class);
        SiteDto dto2 = modelMapper.map(site2, SiteDto.class);


        // then
        mockMvc.perform(get("/partners/ownSites")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].bpns").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].bpns").exists());

        verify(partnerService,times(2)).getOwnPartnerEntity();
    }

    @Test
    @WithMockApiKey
    void getOwnSitesWithoutSitesTest() throws Exception {
        // given
        Partner ownPartnerEntity = new Partner();
        ownPartnerEntity.setSites(null);


        // when
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartnerEntity);

        // then
        mockMvc.perform(get("/partners/ownSites")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());

        verify(partnerService).getOwnPartnerEntity();
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
