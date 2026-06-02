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
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ChildData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleLevelBomAsPlannedSammMapperTest {

    private static final Partner PARTNER;
    private static final Material MATERIAL_1;
    private static final Material MATERIAL_2;
    private static final Material MATERIAL_3;

    static {
        PARTNER = new Partner("Test Partner", "http://example.com", "BPNL1234567890AB", "BPNS1234567890AB", "Test Site", "BPNA1234567890AB", "Test Street", "12345", "Germany");
        PARTNER.setUuid(UUID.randomUUID());
        MATERIAL_1 = Material.builder().productFlag(true).materialFlag(false).materialNumberCx("urn:uuid:parent-cx-123").ownMaterialNumber("MAT-PARENT-001").build();
        MATERIAL_2 = Material.builder().productFlag(false).materialFlag(true).materialNumberCx("urn:uuid:child1-cx-456").ownMaterialNumber("MAT-CHILD-001").build();
        MATERIAL_3 = Material.builder().productFlag(false).materialFlag(true).materialNumberCx("urn:uuid:child2-cx-789").ownMaterialNumber("MAT-CHILD-002").build();
    }

    @InjectMocks
    SingleLevelBomAsPlannedSammMapper mapper;

    @Mock
    MaterialRelationService materialRelationService;

    @Mock
    MaterialPartnerRelationService materialPartnerRelationService;

    @Mock
    MaterialService materialService;

    private MaterialRelation materialRelation1;
    private MaterialRelation materialRelation2;
    private MaterialPartnerRelation mpr1;
    private MaterialPartnerRelation mpr2;

    @BeforeEach
    void setUp() {
        materialRelation1 = new MaterialRelation();
        materialRelation1.setUuid(UUID.randomUUID());
        materialRelation1.setParentOwnMaterialNumber(MATERIAL_1.getOwnMaterialNumber());
        materialRelation1.setChildOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber());
        materialRelation1.setQuantity(2.5);
        materialRelation1.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        materialRelation1.setCreatedOn(new Date());
        materialRelation1.setLastModifiedOn(new Date());

        materialRelation2 = new MaterialRelation();
        materialRelation2.setUuid(UUID.randomUUID());
        materialRelation2.setParentOwnMaterialNumber(MATERIAL_1.getOwnMaterialNumber());
        materialRelation2.setChildOwnMaterialNumber(MATERIAL_3.getOwnMaterialNumber());
        materialRelation2.setQuantity(1.0);
        materialRelation2.setMeasurementUnit(ItemUnitEnumeration.UNIT_KILOGRAM);
        materialRelation2.setCreatedOn(new Date());
        materialRelation2.setLastModifiedOn(new Date());

        mpr1 = new MaterialPartnerRelation();
        mpr1.setMaterial(MATERIAL_2);
        mpr1.setPartner(PARTNER);
        mpr1.setPartnerCXNumber("urn:uuid:partner-child1-cx");
        mpr1.setPartnerMaterialNumber("PARTNER-MAT-001");
        mpr1.setPartnerSuppliesMaterial(true);

        mpr2 = new MaterialPartnerRelation();
        mpr2.setMaterial(MATERIAL_3);
        mpr2.setPartner(PARTNER);
        mpr2.setPartnerCXNumber("urn:uuid:partner-child2-cx");
        mpr2.setPartnerMaterialNumber("PARTNER-MAT-002");
        mpr2.setPartnerSuppliesMaterial(true);
    }

    private ChildData findChildByCxId(SingleLevelBomAsPlanned result, String cxId) {
        return result.getChildItems().stream()
                .filter(c -> Objects.equals(c.getCatenaXId(), cxId))
                .findFirst()
                .orElse(null);
    }

    private void assertChildData(ChildData childData, String expectedBpnl, double expectedQuantity, String expectedUnit) {
        assertNotNull(childData);
        assertEquals(expectedBpnl, childData.getBusinessPartner());
        assertEquals(expectedQuantity, childData.getQuantity().getValue());
        assertEquals(expectedUnit, childData.getQuantity().getUnit().getValue());
    }

    private void setupSingleChildRelation() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(MATERIAL_2);
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_2.getOwnMaterialNumber())).thenReturn(Collections.singletonList(mpr1));
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_success() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(MATERIAL_2);
        when(materialService.findByOwnMaterialNumber(MATERIAL_3.getOwnMaterialNumber())).thenReturn(MATERIAL_3);
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_2.getOwnMaterialNumber())).thenReturn(Collections.singletonList(mpr1));
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_3.getOwnMaterialNumber())).thenReturn(Collections.singletonList(mpr2));

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertNotNull(result);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), result.getCatenaXId());
        assertEquals(2, result.getChildItems().size());

        verify(materialRelationService).findAllChildren(MATERIAL_1.getOwnMaterialNumber());

        ChildData child1Data = findChildByCxId(result, mpr1.getPartnerCXNumber());
        assertChildData(child1Data, PARTNER.getBpnl(), 2.5, "unit:piece");

        ChildData child2Data = findChildByCxId(result, mpr2.getPartnerCXNumber());
        assertChildData(child2Data, PARTNER.getBpnl(), 1.0, "unit:kilogram");
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_emptyBOM_returnsNoChildren() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Collections.emptyList());

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertNotNull(result);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_missingChildMaterial_skipsChild() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(null);

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertNotNull(result);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_noSupplierRelations_returnsNoChildren() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(MATERIAL_2);
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_2.getOwnMaterialNumber())).thenReturn(Collections.emptyList());

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertNotNull(result);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_partialSupplierRelations() {
        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(MATERIAL_2);
        when(materialService.findByOwnMaterialNumber(MATERIAL_3.getOwnMaterialNumber())).thenReturn(MATERIAL_3);
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_2.getOwnMaterialNumber())).thenReturn(Collections.singletonList(mpr1));
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_3.getOwnMaterialNumber())).thenReturn(Collections.emptyList());

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertNotNull(result);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), result.getCatenaXId());
        assertEquals(1, result.getChildItems().size());

        ChildData childData = findChildByCxId(result, mpr1.getPartnerCXNumber());
        assertChildData(childData, PARTNER.getBpnl(), 2.5, "unit:piece");
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_partnerCxNumber_doesNotAffectChildCatenaXId() {
        mpr1.setPartnerCXNumber(null);
        setupSingleChildRelation();

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertEquals(1, result.getChildItems().size());
        ChildData childData = result.getChildItems().stream().findFirst().orElse(null);
        assertNotNull(childData);
        assertNull(childData.getCatenaXId());
        assertEquals(PARTNER.getBpnl(), childData.getBusinessPartner());
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_mapsQuantityTimestampsAndValidityPeriod() {
        materialRelation1.setValidFrom(new Date(System.currentTimeMillis() - 86400000));
        materialRelation1.setValidTo(new Date(System.currentTimeMillis() + 86400000));
        setupSingleChildRelation();

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        ChildData childData = findChildByCxId(result, mpr1.getPartnerCXNumber());
        assertNotNull(childData.getQuantity());
        assertEquals(2.5, childData.getQuantity().getValue());
        assertEquals("unit:piece", childData.getQuantity().getUnit().getValue());
        assertNotNull(childData.getCreatedOn());
        assertNotNull(childData.getLastModifiedOn());
        assertInstanceOf(Date.class, childData.getCreatedOn());
        assertInstanceOf(Date.class, childData.getLastModifiedOn());
        var validityPeriod = Objects.requireNonNull(childData.getValidityPeriod());
        assertNotNull(validityPeriod.getValidFrom());
        assertNotNull(validityPeriod.getValidTo());
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_nonProduct_returnsNull() {
        Material mat = Material.builder()
                .ownMaterialNumber(MATERIAL_1.getOwnMaterialNumber())
                .materialNumberCx(MATERIAL_1.getMaterialNumberCx())
                .productFlag(false)
                .build();

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(mat);

        assertNull(result);
    }

    @Test
    void materialToSingleLevelBomAsPlannedSamm_multipleSuppliers() {
        Partner partner2 = new Partner("Second Supplier", "http://example.com", "BPNL9876543210XY", "BPNS9876543210XY", "Site 2", "BPNA9876543210XY", "Street 2", "67890", "Germany");
        partner2.setUuid(UUID.randomUUID());
        MaterialPartnerRelation mpr1b = new MaterialPartnerRelation();
        mpr1b.setMaterial(MATERIAL_2);
        mpr1b.setPartner(partner2);
        mpr1b.setPartnerCXNumber("urn:uuid:partner2-child1-cx");
        mpr1b.setPartnerMaterialNumber("PARTNER2-MAT-001");
        mpr1b.setPartnerSuppliesMaterial(true);

        when(materialRelationService.findAllChildren(MATERIAL_1.getOwnMaterialNumber())).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(MATERIAL_2.getOwnMaterialNumber())).thenReturn(MATERIAL_2);
        when(materialPartnerRelationService.findAllSupplierRelations(MATERIAL_2.getOwnMaterialNumber())).thenReturn(Arrays.asList(mpr1, mpr1b));

        SingleLevelBomAsPlanned result = mapper.materialToSingleLevelBomAsPlannedSamm(MATERIAL_1);

        assertEquals(2, result.getChildItems().size());

        ChildData child1 = findChildByCxId(result, mpr1.getPartnerCXNumber());
        assertChildData(child1, PARTNER.getBpnl(), 2.5, "unit:piece");

        ChildData child2 = findChildByCxId(result, mpr1b.getPartnerCXNumber());
        assertChildData(child2, partner2.getBpnl(), 2.5, "unit:piece");
    }

}
