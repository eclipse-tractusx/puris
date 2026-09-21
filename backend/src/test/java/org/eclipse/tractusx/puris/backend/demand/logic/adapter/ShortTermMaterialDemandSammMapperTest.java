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
package org.eclipse.tractusx.puris.backend.demand.logic.adapter;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.security.logic.AnonymizationService;
import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.anonymizeddamandsamm.DemandSeriesAnonymized;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.anonymizeddamandsamm.ShortTermMaterialDemandAnonymized;
import org.eclipse.tractusx.puris.backend.demand.logic.dto.demandsamm.Demand;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShortTermMaterialDemandSammMapperTest {
    final static String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
    final static String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
    final static String CX_MAT_NUMBER = UUID.randomUUID().toString();
    final static String OWN_BPNS = "BPNS4444444444SS";
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";
    final static String OTHER_SUPPLIER_BPNS = "BPNS2222222222SI";
 
    final static Partner supplierPartner = new Partner(
        "Scenario Supplier",
        "http://supplier-control-plane:9184/api/v1/dsp",
        SUPPLIER_BPNL,
        SUPPLIER_BPNS,
        "Konzernzentrale Dudelsdorf",
        SUPPLIER_BPNA,
        "Heinrich-Supplier-Straße 1",
        "77785 Dudelsdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );
 
    @Mock
    private MaterialPartnerRelationService mprService;
 
    @Mock
    private MaterialService materialService;
 
    @Mock
    private AnonymizationService anonymizationService;
 
    @InjectMocks
    private ShortTermMaterialDemandSammMapper demandSammMapper;
 
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
 
    @Test
    @Order(1)
    void map_WhenSingleOwnDemand_ReturnsShortTermMaterialDemandAnonymized() {
        // Given
        Material semiconductorMaterial = createSemiconductorMaterial();
        MaterialPartnerRelation mpr = createMpr(semiconductorMaterial);
 
        Date day = new Date();
        Date lastUpdated = new Date();
 
        OwnDemand ownDemand = createOwnDemand(semiconductorMaterial, 20, day, OWN_BPNS, SUPPLIER_BPNS, DemandCategoryEnumeration.DEMAND_DEFAULT, lastUpdated);
 
        // When
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(mpr);
        when(anonymizationService.anonymize(anyString(), anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));
 
        ShortTermMaterialDemandAnonymized samm = demandSammMapper.ownDemandToAnonymizedSamm(List.of(ownDemand), supplierPartner, semiconductorMaterial, "SALT");
 
        // Then
        assertNotNull(samm);
        assertTrue(samm.getMaterialGlobalAssetIdAnonymized().startsWith("enc:"));
        assertEquals("enc:" + CX_MAT_NUMBER, samm.getMaterialGlobalAssetIdAnonymized());
        assertNotNull(samm.getDemandSeries());
        assertEquals(1, samm.getDemandSeries().size());
 
        DemandSeriesAnonymized demandSeries = samm.getDemandSeries().stream().toList().get(0);
        assertEquals("enc:" + OWN_BPNS, demandSeries.getCustomerLocationBpnsAnonymized());
        assertEquals("enc:" + SUPPLIER_BPNS, demandSeries.getExpectedSupplierLocationBpnsAnonymized());
        assertEquals(lastUpdated, demandSeries.getLastUpdatedOnDateTime());
        assertNotNull(demandSeries.getDemands());
        assertEquals(1, demandSeries.getDemands().size());
 
        Demand mappedDemand = demandSeries.getDemands().stream().toList().get(0);
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, mappedDemand.getDemand().getUnit());
        assertEquals(ownDemand.getQuantity(), mappedDemand.getDemand().getValue());
        assertEquals(day, mappedDemand.getDay());
    }
 
    @Test
    @Order(2)
    void map_WhenMultipleOwnDemands_GroupsByLocationAndOmitsCategory() {
        // Given
        Material semiconductorMaterial = createSemiconductorMaterial();
        MaterialPartnerRelation mpr = createMpr(semiconductorMaterial);
 
        Date day1 = new Date(System.currentTimeMillis() - 2 * 86400000L);
        Date day2 = new Date(System.currentTimeMillis() - 86400000L);
        Date day3 = new Date();
        Date olderUpdate = new Date(System.currentTimeMillis() - 3600000L);
        Date newerUpdate = new Date();
 
        OwnDemand demand1 = createOwnDemand(semiconductorMaterial, 20, day1, OWN_BPNS, SUPPLIER_BPNS, DemandCategoryEnumeration.DEMAND_DEFAULT, olderUpdate);
        OwnDemand demand2 = createOwnDemand(semiconductorMaterial, 40, day2, OWN_BPNS, SUPPLIER_BPNS, DemandCategoryEnumeration.DEMAND_SERIES, newerUpdate);
        OwnDemand demand3 = createOwnDemand(semiconductorMaterial, 10, day3, OWN_BPNS, OTHER_SUPPLIER_BPNS, DemandCategoryEnumeration.DEMAND_DEFAULT, olderUpdate);
 
        // When
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(mpr);
        when(anonymizationService.anonymize(anyString(), anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));
 
        ShortTermMaterialDemandAnonymized samm = demandSammMapper.ownDemandToAnonymizedSamm(List.of(demand1, demand2, demand3), supplierPartner, semiconductorMaterial, "SALT");
 
        // Then
        assertNotNull(samm);
        assertEquals(2, samm.getDemandSeries().size());
 
        DemandSeriesAnonymized firstSeries = samm.getDemandSeries().stream().filter(series -> ("enc:" + SUPPLIER_BPNS).equals(series.getExpectedSupplierLocationBpnsAnonymized())).findFirst().orElseThrow();
        assertEquals("enc:" + OWN_BPNS, firstSeries.getCustomerLocationBpnsAnonymized());
        assertEquals(2, firstSeries.getDemands().size());
        assertEquals(newerUpdate, firstSeries.getLastUpdatedOnDateTime());
 
        DemandSeriesAnonymized secondSeries = samm.getDemandSeries().stream().filter(series -> ("enc:" + OTHER_SUPPLIER_BPNS).equals(series.getExpectedSupplierLocationBpnsAnonymized())).findFirst().orElseThrow();
        assertEquals("enc:" + OWN_BPNS, secondSeries.getCustomerLocationBpnsAnonymized());
        assertEquals(1, secondSeries.getDemands().size());
        assertEquals(olderUpdate, secondSeries.getLastUpdatedOnDateTime());
    }
 
    @Test
    @Order(3)
    void map_WhenNoMaterialPartnerRelationExists_ReturnsNull() {
        // Given
        Material semiconductorMaterial = createSemiconductorMaterial();
        OwnDemand ownDemand = createOwnDemand(semiconductorMaterial, 20, new Date(), OWN_BPNS, SUPPLIER_BPNS, DemandCategoryEnumeration.DEMAND_DEFAULT, new Date());
 
        // When
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(null);
 
        ShortTermMaterialDemandAnonymized samm = demandSammMapper.ownDemandToAnonymizedSamm(List.of(ownDemand), supplierPartner, semiconductorMaterial, "SALT");
 
        // Then
        assertNull(samm);
    }
 
    private Material createSemiconductorMaterial() {
        return Material.builder()
            .ownMaterialNumber(CUSTOMER_MAT_NUMBER)
            .materialFlag(true)
            .productFlag(false)
            .name("Semiconductor")
            .build();
    }
 
    private MaterialPartnerRelation createMpr(Material material) {
        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(supplierPartner);
        mpr.setMaterial(material);
        mpr.setPartnerBuysMaterial(false);
        mpr.setPartnerSuppliesMaterial(true);
        mpr.setPartnerMaterialNumber(SUPPLIER_MAT_NUMBER);
        mpr.setPartnerCXNumber(CX_MAT_NUMBER);
        return mpr;
    }
 
    private OwnDemand createOwnDemand(Material material, double quantity, Date day, String demandLocationBpns, String supplierLocationBpns, DemandCategoryEnumeration category, Date lastUpdatedOnDateTime) {
        return OwnDemand.builder()
            .partner(supplierPartner)
            .material(material)
            .quantity(quantity)
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .day(day)
            .demandLocationBpns(demandLocationBpns)
            .supplierLocationBpns(supplierLocationBpns)
            .demandCategoryCode(category)
            .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
            .build();
    }
}
