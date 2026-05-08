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

package org.eclipse.tractusx.puris.backend.delivery.logic.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Delivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.DeliveryInformation;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Location;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.OrderPositionReference;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.Position;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.TransitEvent;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.TransitLocations;
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
class DeliveryInformationSammMapperTest {

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
    DeliveryInformationSammMapper mapper;

    @Mock
    MaterialPartnerRelationService mprService;

    @Mock
    MaterialService materialService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void ownDeliveryToSamm_success() {
        Date now = new Date();
        OwnDelivery od = OwnDelivery.builder()
                .partner(PARTNER)
                .material(MATERIAL_1)
                .quantity(5.0)
                .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
                .trackingNumber("TRACK1")
                .supplierOrderNumber("S1")
                .customerOrderNumber("C1")
                .customerOrderPositionNumber("P1")
                .destinationBpns("BPNS123456789012")
                .destinationBpna("BPNA123456789012")
                .originBpns("BPNS222222222222")
                .originBpna("BPNA222222222222")
                .dateOfDeparture(now)
                .dateOfArrival(now)
                .departureType(EventTypeEnumeration.ACTUAL_DEPARTURE)
                .arrivalType(EventTypeEnumeration.ACTUAL_ARRIVAL)
                .lastUpdatedOnDateTime(now)
                .build();

        var samm = mapper.ownDeliveryToSamm(List.of(od), PARTNER, MATERIAL_1);
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
        assertNotNull(pos.getDeliveries());
        assertEquals(1, pos.getDeliveries().size());
        Delivery d = pos.getDeliveries().iterator().next();
        assertEquals("TRACK1", d.getTrackingNumber());
    }

    @Test
    void ownDeliveryToSamm_partnerMismatch_returnsNull() {
        Date now = new Date();
        OwnDelivery a = OwnDelivery.builder().partner(PARTNER_A).material(MATERIAL_2).quantity(1.0).measurementUnit(ItemUnitEnumeration.UNIT_PIECE).lastUpdatedOnDateTime(now).build();
        OwnDelivery b = OwnDelivery.builder().partner(PARTNER_B).material(MATERIAL_2).quantity(1.0).measurementUnit(ItemUnitEnumeration.UNIT_PIECE).lastUpdatedOnDateTime(now).build();

        var res = mapper.ownDeliveryToSamm(List.of(a, b), PARTNER_A, MATERIAL_2);
        assertNull(res);
    }

    @Test
    void ownDeliveryToAnonymizedSamm_success() {
        Date now = new Date();
        OwnDelivery od = OwnDelivery.builder()
                .partner(PARTNER)
                .material(MATERIAL_3)
                .quantity(2.0)
                .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
                .originBpns("BPNS111111111111")
                .destinationBpns("BPNS123456789012")
                .dateOfDeparture(now)
                .dateOfArrival(now)
                .departureType(EventTypeEnumeration.ACTUAL_DEPARTURE)
                .arrivalType(EventTypeEnumeration.ACTUAL_ARRIVAL)
                .lastUpdatedOnDateTime(now)
                .build();

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));

        var samm = mapper.ownDeliveryToAnonymizedSamm(List.of(od), PARTNER, MATERIAL_3, "SALT");
        assertNotNull(samm);
        assertTrue(samm.getMaterialGlobalAssetIdAnonymized().startsWith("enc:"));
        assertNotNull(samm.getDeliveries());
        assertEquals(1, samm.getDeliveries().size());
    }

    @Test
    void sammToReportedDeliveries_success() {
        Date dep = new Date();
        Date arr = new Date();
        ItemQuantityEntity qty = new ItemQuantityEntity(7.0, ItemUnitEnumeration.UNIT_PIECE);
        Set<TransitEvent> events = Set.of(new TransitEvent(arr, EventTypeEnumeration.ACTUAL_ARRIVAL), new TransitEvent(dep, EventTypeEnumeration.ACTUAL_DEPARTURE));
        TransitLocations locs = new TransitLocations(new Location("BPNA111111111111", "BPNS111111111111"), new Location("BPNA222222222222", "BPNS222222222222"));
        Delivery d = new Delivery(qty, arr, events, locs, "TRACKX", null);

        OrderPositionReference opr = new OrderPositionReference("S1", "C1", "P1");
        Position pos = new Position(opr, new HashSet<>(Set.of(d)));
        DeliveryInformation info = new DeliveryInformation(new HashSet<>(Set.of(pos)), MATERIAL_4.getMaterialNumberCx());

        when(materialService.findByMaterialNumberCx(MATERIAL_4.getMaterialNumberCx())).thenReturn(MATERIAL_4);

        List<ReportedDelivery> out = mapper.sammToReportedDeliveries(info, PARTNER);
        assertEquals(1, out.size());
        ReportedDelivery rd = out.get(0);
        assertEquals(7.0, rd.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, rd.getMeasurementUnit());
        assertEquals("TRACKX", rd.getTrackingNumber());
        assertEquals(EventTypeEnumeration.ACTUAL_ARRIVAL, rd.getArrivalType());
        assertEquals(EventTypeEnumeration.ACTUAL_DEPARTURE, rd.getDepartureType());
        assertEquals("BPNS111111111111", rd.getOriginBpns());
        assertEquals("BPNA111111111111", rd.getOriginBpna());
        assertEquals("BPNS222222222222", rd.getDestinationBpns());
        assertEquals("BPNA222222222222", rd.getDestinationBpna());
        assertEquals("C1", rd.getCustomerOrderNumber());
        assertEquals("P1", rd.getCustomerOrderPositionNumber());
        assertEquals("S1", rd.getSupplierOrderNumber());
    }

    @Test
    void ownDeliveryToSamm_materialFlag_usesMprService() {
        // material with materialFlag=true and productFlag=false -> should use MPR partner CX
        Material mat = MATERIAL_5;
        MaterialPartnerRelation mpr = new MaterialPartnerRelation(mat, PARTNER, "PMAT", true, false);
        mpr.setPartnerCXNumber("urn:uuid:99999999-9999-9999-9999-999999999999");

        when(mprService.find(mat, PARTNER)).thenReturn(mpr);

        Date now = new Date();
        OwnDelivery od = OwnDelivery.builder()
                .partner(PARTNER)
                .material(mat)
                .quantity(3.0)
                .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
                .lastUpdatedOnDateTime(now)
                .build();

        var samm = mapper.ownDeliveryToSamm(List.of(od), PARTNER, mat);
        assertNotNull(samm);
        assertEquals(mpr.getPartnerCXNumber(), samm.getMaterialGlobalAssetId());
    }
}
