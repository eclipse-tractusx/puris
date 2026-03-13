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

package org.eclipse.tractusx.puris.backend.production.logic.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.logic.dto.anonymizedplannedproductionsamm.AllocatedPlannedProductionOutputAnonymized;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.AllocatedPlannedProductionOutput;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.OrderPositionReference;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.PlannedProductionOutput;
import org.eclipse.tractusx.puris.backend.production.logic.dto.plannedproductionsamm.Position;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannedProductionSammMapperTest {

    private static final Partner PARTNER;
    private static final Partner PARTNER_A;
    private static final Partner PARTNER_B;
    private static final Material MATERIAL_1;
    private static final Material MATERIAL_2;
    private static final Material MATERIAL_3;
    private static final Material MATERIAL_4;
    private static final Material MATERIAL_5;

    static {
        PARTNER = new Partner("name", "http://example.com", "BPNL111111111111", "BPNS111111111111", "siteName", "BPNA111111111111", "street", "zip", "country");
        PARTNER.setUuid(UUID.randomUUID());
        PARTNER_A = new Partner("p1", "http://ex", "BPNL111111111111", "BPNS111111111111", "site", "BPNA111111111111", "s", "z", "c");
        PARTNER_A.setUuid(UUID.randomUUID());
        PARTNER_B = new Partner("p2", "http://ex2", "BPNL222222222222", "BPNS222222222222", "site2", "BPNA222222222222", "s2", "z2", "c2");
        PARTNER_B.setUuid(UUID.randomUUID());

        MATERIAL_1 = Material.builder().productFlag(true).materialNumberCx("urn:uuid:11111111-1111-1111-1111-111111111111").ownMaterialNumber("OWN1").build();
        MATERIAL_2 = Material.builder().productFlag(true).materialNumberCx("urn:uuid:22222222-2222-2222-2222-222222222222").ownMaterialNumber("OWN2").build();
        MATERIAL_3 = Material.builder().productFlag(true).materialNumberCx("urn:uuid:33333333-3333-3333-3333-333333333333").ownMaterialNumber("OWN3").build();
        MATERIAL_4 = Material.builder().productFlag(true).materialNumberCx("urn:uuid:44444444-4444-4444-4444-444444444444").ownMaterialNumber("OWN4").build();
        MATERIAL_5 = Material.builder().materialFlag(true).productFlag(false).ownMaterialNumber("OWN5").build();
    }

    @InjectMocks
    PlannedProductionSammMapper mapper;

    @Mock
    MaterialPartnerRelationService mprService;

    @Mock
    MaterialPartnerRelation mpr;

    @Mock
    MaterialService materialService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void ownProductionToSamm_success() {
        Date now = new Date();
        OwnProduction op = OwnProduction.builder()
                .partner(PARTNER)
                .material(MATERIAL_1)
                .quantity(5.0)
                .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
                .supplierOrderNumber("S1")
                .customerOrderNumber("C1")
                .customerOrderPositionNumber("P1")
                .productionSiteBpns("BPNS123456789012")
                .lastUpdatedOnDateTime(now)
                .estimatedTimeOfCompletion(now)
                .build();

        var samm = mapper.ownProductionToSamm(List.of(op), PARTNER, MATERIAL_1);
        assertNotNull(samm);
        assertEquals(MATERIAL_1.getMaterialNumberCx(), samm.getMaterialGlobalAssetId());
        assertNotNull(samm.getPositions());
        assertEquals(1, samm.getPositions().size());
        Position pos = samm.getPositions().iterator().next();
        assertNotNull(pos.getOrderPositionReference());
        OrderPositionReference opr = pos.getOrderPositionReference();
        assertEquals("S1", opr.getSupplierOrderId());
        assertEquals("C1", opr.getCustomerOrderId());
        assertEquals("P1", opr.getCustomerOrderPositionId());
        assertNotNull(pos.getAllocatedPlannedProductionOutputs());
        assertEquals(1, pos.getAllocatedPlannedProductionOutputs().size());
        AllocatedPlannedProductionOutput p = pos.getAllocatedPlannedProductionOutputs().iterator().next();
        assertEquals("BPNS123456789012", p.getProductionSiteBpns());
        assertEquals(5.0, p.getPlannedProductionQuantity().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, p.getPlannedProductionQuantity().getUnit());
        assertEquals(now, p.getEstimatedTimeOfCompletion());
        assertEquals(now, p.getLastUpdatedOnDateTime());
    }

    @Test
    void ownProductionToSamm_partnerMismatch_returnsNull() {
        Date now = new Date();
        OwnProduction a = OwnProduction.builder().partner(PARTNER_A).material(MATERIAL_2).quantity(1.0).measurementUnit(ItemUnitEnumeration.UNIT_PIECE).lastUpdatedOnDateTime(now).build();
        OwnProduction b = OwnProduction.builder().partner(PARTNER_B).material(MATERIAL_2).quantity(1.0).measurementUnit(ItemUnitEnumeration.UNIT_PIECE).lastUpdatedOnDateTime(now).build();

        var res = mapper.ownProductionToSamm(List.of(a, b), PARTNER_A, MATERIAL_2);
        assertNull(res);
    }

    @Test
    void ownProductionToAnonymizedSamm_success() {
        Date now = new Date();
        OwnProduction op = OwnProduction.builder()
                .partner(PARTNER)
                .material(MATERIAL_3)
                .quantity(2.0)
                .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
                .productionSiteBpns("BPNS123456789012")
                .lastUpdatedOnDateTime(now)
                .estimatedTimeOfCompletion(now)
                .build();

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));

        var samm = mapper.ownProductionToAnonymizedSamm(List.of(op), PARTNER, MATERIAL_3, "SALT");
        assertNotNull(samm);
        assertTrue(samm.getMaterialGlobalAssetIdAnonymized().startsWith("enc:"));
        assertNotNull(samm.getAllocatedPlannedProductionOutputs());
        assertEquals(1, samm.getAllocatedPlannedProductionOutputs().size());
        AllocatedPlannedProductionOutputAnonymized p = samm.getAllocatedPlannedProductionOutputs().iterator().next();
        assertTrue(p.getProductionSiteBpnsAnonymized().startsWith("enc:"));
        assertEquals(2.0, p.getPlannedProductionQuantity().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, p.getPlannedProductionQuantity().getUnit());
        assertEquals(now, p.getEstimatedTimeOfCompletion());
        assertEquals(now, p.getLastUpdatedOnDateTime());
    }

    @Test
    void sammToReportedProductions_success() {
        Date now = new Date();
        ItemQuantityEntity qty = new ItemQuantityEntity(7.0, ItemUnitEnumeration.UNIT_PIECE);
        AllocatedPlannedProductionOutput p = new AllocatedPlannedProductionOutput(qty, "BPNS111111111111", now, now);

        OrderPositionReference opr = new OrderPositionReference("S1", "C1", "P1");
        Position pos = new Position(opr, now, new HashSet<>(Set.of(p)));
        PlannedProductionOutput prod = new PlannedProductionOutput(new HashSet<>(Set.of(pos)), MATERIAL_4.getMaterialNumberCx());

        when(mprService.findByPartnerAndPartnerCXNumber(PARTNER, MATERIAL_4.getMaterialNumberCx())).thenReturn(mpr);
        when(mpr.getMaterial()).thenReturn(MATERIAL_4); 

        List<ReportedProduction> out = mapper.sammToReportedProduction(prod, PARTNER);
        assertEquals(1, out.size());
        ReportedProduction rd = out.get(0);
        assertEquals(7.0, rd.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, rd.getMeasurementUnit());
        assertEquals("BPNS111111111111", rd.getProductionSiteBpns());
        assertEquals("C1", rd.getCustomerOrderNumber());
        assertEquals("P1", rd.getCustomerOrderPositionNumber());
        assertEquals("S1", rd.getSupplierOrderNumber());
    }
}
