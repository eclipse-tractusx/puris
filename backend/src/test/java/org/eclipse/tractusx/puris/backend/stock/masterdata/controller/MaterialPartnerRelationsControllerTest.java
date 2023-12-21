package org.eclipse.tractusx.puris.backend.stock.masterdata.controller;

import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.masterdata.controller.MaterialPartnerRelationsController;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialPartnerRelationsController.class)
@Import({ SecurityConfig.class, ApiKeyAuthenticationProvider.class })
public class MaterialPartnerRelationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private MaterialPartnerRelationService mprService;

    @Test
    @WithMockApiKey
    public void createMaterialPartnerRelationTest() throws Exception {



        /*Material material = new Material();
        material.setOwnMaterialNumber("materialNumber");
        material.setMaterialFlag(true);
        material.setMaterialNumberCx("matNumCx");
        material.setName("name");
        material.setProductFlag(true);


        Partner partner = new Partner();
        partner.setBpnl("partnerBpnl");
        partner.setEdcUrl("example.com");
        partner.setName("name2");


        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, "materialNumber", true, true);
        HashSet<MaterialPartnerRelation> set = new HashSet<>();
        set.add(newMpr);

        material.setMaterialPartnerRelations(set);
        partner.setMaterialPartnerRelations(set);

        when(materialService.findByOwnMaterialNumber("materialNumber")).thenReturn(material);
        when(partnerService.findByBpnl("partnerBpnl")).thenReturn(partner);
        when(mprService.find(material, partner)).thenReturn(null);
        when(mprService.create(any(MaterialPartnerRelation.class))).thenReturn(newMpr);

        // Perform the request
        mockMvc.perform(post("/materialpartnerrelations")
                .param("ownMaterialNumber", "materialNumber")
                .param("partnerBpnl", "partnerBpnl")
                .param("partnerMaterialNumber", "partnerMaterialNumber")
                .param("partnerSupplies", "true")
                .param("partnerBuys", "true"))
            .andExpect(status().isOk());

        // Verify the interactions
        verify(materialService).findByOwnMaterialNumber("ownMaterialNumber");
        verify(partnerService).findByBpnl("partnerBpnl");
        verify(mprService).find(material, partner);
        verify(mprService).create(any(MaterialPartnerRelation.class));*/
    }






}
