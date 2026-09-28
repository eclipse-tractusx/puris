/*
 * Copyright (c) 2026 Volkswagen AG
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Claude Opus-5.5-pro
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.ParentData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.SingleLevelUsageAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SingleLevelUsageAsPlannedSammMapperTest {
    private static final String OWN_BPNL = "BPNL0000000001OW";
 
    private static final Partner SUPPLIER;
    private static final Material PARENT_1;
    private static final Material PARENT_2;
    private static final Material CHILD;
 
    static {
        SUPPLIER = new Partner("Test Supplier", "http://example.com", "BPNL1234567890AB", "BPNS1234567890AB", "Test Site", "BPNA1234567890AB", "Test Street", "12345", "Germany", PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);
        SUPPLIER.setUuid(UUID.randomUUID());
        PARENT_1 = Material.builder().productFlag(true).materialFlag(false).materialNumberCx("urn:uuid:parent1-cx-123").ownMaterialNumber("MAT-PARENT-001").build();
        PARENT_2 = Material.builder().productFlag(true).materialFlag(false).materialNumberCx("urn:uuid:parent2-cx-456").ownMaterialNumber("MAT-PARENT-002").build();
        CHILD = Material.builder().productFlag(false).materialFlag(true).materialNumberCx("urn:uuid:child-cx-789").ownMaterialNumber("MAT-CHILD-001").build();
    }
 
    @InjectMocks
    SingleLevelUsageAsPlannedSammMapper mapper;
 
    @Mock
    MaterialRelationService materialRelationService;
 
    @Mock
    MaterialService materialService;
 
    @Mock
    VariablesService variablesService;
 
    private MaterialRelation materialRelation1;
    private MaterialRelation materialRelation2;
    private MaterialPartnerRelation supplierMpr;
 
    @BeforeEach
    void setUp() {
        materialRelation1 = new MaterialRelation();
        materialRelation1.setUuid(UUID.randomUUID());
        materialRelation1.setParentOwnMaterialNumber(PARENT_1.getOwnMaterialNumber());
        materialRelation1.setChildOwnMaterialNumber(CHILD.getOwnMaterialNumber());
        materialRelation1.setQuantity(2.5);
        materialRelation1.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        materialRelation1.setCreatedOn(new Date());
        materialRelation1.setLastModifiedOn(new Date());
 
        materialRelation2 = new MaterialRelation();
        materialRelation2.setUuid(UUID.randomUUID());
        materialRelation2.setParentOwnMaterialNumber(PARENT_2.getOwnMaterialNumber());
        materialRelation2.setChildOwnMaterialNumber(CHILD.getOwnMaterialNumber());
        materialRelation2.setQuantity(1.0);
        materialRelation2.setMeasurementUnit(ItemUnitEnumeration.UNIT_KILOGRAM);
        materialRelation2.setCreatedOn(new Date());
        materialRelation2.setLastModifiedOn(new Date());
 
        supplierMpr = new MaterialPartnerRelation();
        supplierMpr.setMaterial(CHILD);
        supplierMpr.setPartner(SUPPLIER);
        supplierMpr.setPartnerCXNumber("urn:uuid:supplier-child-cx");
        supplierMpr.setPartnerMaterialNumber("SUPPLIER-MAT-001");
        supplierMpr.setPartnerSuppliesMaterial(true);
    }
 
    private ParentData findParentByGlobalAssetId(SingleLevelUsageAsPlanned result, String globalAssetId) {
        return result.getParentItems().stream()
                .filter(p -> Objects.equals(p.getGlobalAssetId(), globalAssetId))
                .findFirst()
                .orElse(null);
    }
 
    private void assertParentData(ParentData parentData, double expectedQuantity, String expectedUnit) {
        assertNotNull(parentData);
        assertEquals(OWN_BPNL, parentData.getBusinessPartner());
        assertEquals(expectedQuantity, parentData.getQuantity().getValue());
        assertEquals(expectedUnit, parentData.getQuantity().getUnit().getValue());
    }
 
    private void assertAspectHeader(SingleLevelUsageAsPlanned result, MaterialPartnerRelation mpr) {
        assertNotNull(result);
        assertEquals(mpr.getPartnerCXNumber(), result.getGlobalAssetId());
        assertEquals(List.of(OWN_BPNL), result.getCustomers());
    }
 
    private void setupSingleParentRelation() {
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(PARENT_1.getOwnMaterialNumber())).thenReturn(PARENT_1);
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_success() {
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(PARENT_1.getOwnMaterialNumber())).thenReturn(PARENT_1);
        when(materialService.findByOwnMaterialNumber(PARENT_2.getOwnMaterialNumber())).thenReturn(PARENT_2);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertAspectHeader(result, supplierMpr);
        assertEquals(2, result.getParentItems().size());
 
        verify(materialRelationService).findAllParents(CHILD.getOwnMaterialNumber());
 
        ParentData parent1Data = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        assertParentData(parent1Data, 2.5, "unit:piece");
 
        ParentData parent2Data = findParentByGlobalAssetId(result, PARENT_2.getMaterialNumberCx());
        assertParentData(parent2Data, 1.0, "unit:kilogram");
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_noParents_returnsNoParentItems() {
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Collections.emptyList());
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertAspectHeader(result, supplierMpr);
        assertTrue(result.getParentItems().isEmpty());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_missingParentMaterial_skipsParent() {
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(PARENT_1.getOwnMaterialNumber())).thenReturn(null);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertAspectHeader(result, supplierMpr);
        assertTrue(result.getParentItems().isEmpty());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_nonProductParent_skipsOnlyThatParent() {
        Material nonProductParent = Material.builder()
                .ownMaterialNumber(PARENT_2.getOwnMaterialNumber())
                .materialNumberCx(PARENT_2.getMaterialNumberCx())
                .productFlag(false)
                .materialFlag(true)
                .build();
 
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(PARENT_1.getOwnMaterialNumber())).thenReturn(PARENT_1);
        when(materialService.findByOwnMaterialNumber(PARENT_2.getOwnMaterialNumber())).thenReturn(nonProductParent);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertAspectHeader(result, supplierMpr);
        assertEquals(1, result.getParentItems().size());
 
        ParentData parentData = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        assertParentData(parentData, 2.5, "unit:piece");
        assertNull(findParentByGlobalAssetId(result, PARENT_2.getMaterialNumberCx()));
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_parentWithoutCxNumber_skipsParent() {
        Material parentWithoutCxNumber = Material.builder()
                .ownMaterialNumber(PARENT_1.getOwnMaterialNumber())
                .materialNumberCx(null)
                .productFlag(true)
                .build();
 
        when(variablesService.getOwnBpnl()).thenReturn(OWN_BPNL);
        when(materialRelationService.findAllParents(CHILD.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(PARENT_1.getOwnMaterialNumber())).thenReturn(parentWithoutCxNumber);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertAspectHeader(result, supplierMpr);
        assertTrue(result.getParentItems().isEmpty());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_mapsQuantityTimestampsAndValidityPeriod() {
        Date validFrom = new Date(System.currentTimeMillis() - 86400000);
        Date validTo = new Date(System.currentTimeMillis() + 86400000);
        materialRelation1.setValidFrom(validFrom);
        materialRelation1.setValidTo(validTo);
        setupSingleParentRelation();
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        ParentData parentData = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        assertParentData(parentData, 2.5, "unit:piece");
        assertEquals(materialRelation1.getCreatedOn(), parentData.getCreatedOn());
        assertEquals(materialRelation1.getLastModifiedOn(), parentData.getLastModifiedOn());
        var validityPeriod = Objects.requireNonNull(parentData.getValidityPeriod());
        assertEquals(validFrom, validityPeriod.getValidFrom());
        assertEquals(validTo, validityPeriod.getValidTo());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_openEndedValidity_keepsOnlyValidFrom() {
        Date validFrom = new Date(System.currentTimeMillis() - 86400000);
        materialRelation1.setValidFrom(validFrom);
        setupSingleParentRelation();
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        ParentData parentData = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        var validityPeriod = Objects.requireNonNull(parentData.getValidityPeriod());
        assertEquals(validFrom, validityPeriod.getValidFrom());
        assertNull(validityPeriod.getValidTo());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_noValidity_validityPeriodNull() {
        setupSingleParentRelation();
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        ParentData parentData = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        assertNotNull(parentData);
        assertNull(parentData.getValidityPeriod());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_missingTimestamps_fallBack() {
        materialRelation1.setCreatedOn(null);
        materialRelation1.setLastModifiedOn(null);
        setupSingleParentRelation();
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        ParentData parentData = findParentByGlobalAssetId(result, PARENT_1.getMaterialNumberCx());
        assertNotNull(parentData);
        assertNotNull(parentData.getCreatedOn());
        assertEquals(parentData.getCreatedOn(), parentData.getLastModifiedOn());
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_partnerDoesNotSupplyMaterial_returnsNull() {
        supplierMpr.setPartnerSuppliesMaterial(false);
        supplierMpr.setPartnerBuysMaterial(true);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertNull(result);
        verifyNoInteractions(materialRelationService, materialService);
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_missingPartnerCxNumber_returnsNull() {
        supplierMpr.setPartnerCXNumber(null);
 
        SingleLevelUsageAsPlanned result = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
 
        assertNull(result);
        verifyNoInteractions(materialRelationService, materialService);
    }
 
    @Test
    void materialPartnerRelationToSingleLevelUsageAsPlannedSamm_multipleSuppliers_oneAspectPerInboundTwin() {
        Partner supplier2 = new Partner("Second Supplier", "http://example.com", "BPNL9876543210XY", "BPNS9876543210XY", "Site 2", "BPNA9876543210XY", "Street 2", "67890", "Germany", PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);
        supplier2.setUuid(UUID.randomUUID());
        MaterialPartnerRelation supplier2Mpr = new MaterialPartnerRelation();
        supplier2Mpr.setMaterial(CHILD);
        supplier2Mpr.setPartner(supplier2);
        supplier2Mpr.setPartnerCXNumber("urn:uuid:supplier2-child-cx");
        supplier2Mpr.setPartnerMaterialNumber("SUPPLIER2-MAT-001");
        supplier2Mpr.setPartnerSuppliesMaterial(true);
 
        setupSingleParentRelation();
 
        SingleLevelUsageAsPlanned result1 = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplierMpr);
        SingleLevelUsageAsPlanned result2 = mapper.materialPartnerRelationToSingleLevelUsageAsPlannedSamm(supplier2Mpr);
 
        // each supplier's inbound twin gets its own aspect, identified by that supplier's CX number
        assertAspectHeader(result1, supplierMpr);
        assertAspectHeader(result2, supplier2Mpr);
        assertNotEquals(result1.getGlobalAssetId(), result2.getGlobalAssetId());
 
        // the parent usage is the same, independent of which supplier delivers the child
        assertEquals(result1.getParentItems(), result2.getParentItems());
        ParentData parentData = findParentByGlobalAssetId(result2, PARENT_1.getMaterialNumberCx());
        assertParentData(parentData, 2.5, "unit:piece");
    }
}
