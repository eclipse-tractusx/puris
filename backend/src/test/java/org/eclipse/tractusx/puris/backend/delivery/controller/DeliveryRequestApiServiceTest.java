/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.IncotermEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.adapter.DeliveryInformationSammMapper;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.DeliveryRequestApiService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
public class DeliveryRequestApiServiceTest {

    @InjectMocks
    private DeliveryRequestApiService deliveryRequestApiService;

    @Mock
    private PartnerService partnerService;
    @Mock
    private MaterialService materialService;
    @Mock
    private MaterialPartnerRelationService mprService;
    @Mock
    private OwnDeliveryService ownDeliveryService;
    @Mock
    private ReportedDeliveryService reportedDeliveryService;
    @Mock
    private EdcAdapterService edcAdapterService;
    @Mock
    private DeliveryInformationSammMapper sammMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MATERIAL_NUMBER_CX_CUSTOMER = UUID.randomUUID().toString();
    private static final String BPNL_CUSTOMER = "BPNL4444444444XX";
    private static final String BPNS_CUSTOMER = "BPNS4444444444XX";
    private static final String BPNA_CUSTOMER = "BPNA4444444444AA";

    private static final String MATERIAL_NUMBER_CX_SUPPLIER = UUID.randomUUID().toString();
    private static final String BPNL_SUPPLIER = "BPNL1234567890ZZ";
    private static final String BPNS_SUPPLIER = "BPNS1234567890ZZ";
    private static final String BPNA_SUPPLIER = "BPNA1234567890AA";

    private Partner CUSTOMER_PARTNER;
    private Partner SUPPLIER_PARTNER;

    private static final Material TEST_MATERIAL = new Material(
        true,
        true,
        "Own-Mnr",
        MATERIAL_NUMBER_CX_CUSTOMER,
        "Test Material",
        new Date()
    );

    private static final Material TEST_MATERIAL_SUPPLIER = new Material(
        true,
        true,
        "Own-Mnr",
        MATERIAL_NUMBER_CX_SUPPLIER,
        "Test Material",
        new Date()
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        CUSTOMER_PARTNER = new Partner(
            "Test Customer",
            "http://some-edc.com",
            BPNL_CUSTOMER,
            BPNS_CUSTOMER,
            "Site Name",
            BPNA_CUSTOMER,
            "Street 10",
            "40468 Testdorf",
            "DE"
        );
        CUSTOMER_PARTNER.setUuid(UUID.randomUUID());

        SUPPLIER_PARTNER = new Partner(
            "Test Supplier",
            "http://some-edc.com",
            BPNL_SUPPLIER,
            BPNS_SUPPLIER,
            "Site Name",
            BPNA_SUPPLIER,
            "Street 10",
            "40468 Testdorf",
            "DE"
        );
        SUPPLIER_PARTNER.setUuid(UUID.randomUUID());
    }

    @Test
    /**
     * tests that the part type information task is triggered to get the cx id if neither material is
     * neither found via material nor mpr
     * </p>
     * NOTE: This test is very theoretical as this means, that someone queries the service while we didn't even
     * create the ShellDescriptor with the HREF.
     */
    void customerGetsRequestNoCxId_testHandleDeliverySubmodelRequest_works() {
        // given
        // Delivery with incoterm with Customer responsibility
        OwnDelivery delivery = getDelivery();
        delivery.setIncoterm(IncotermEnumeration.EXW);

        // Delivery with incoterm with PARTIAL responsibility
        OwnDelivery delivery2 = getDelivery();
        delivery2.setIncoterm(IncotermEnumeration.FCA);

        // Delivery with incoterm with SUPPLIER responsibility
        OwnDelivery delivery3 = getDelivery();
        delivery3.setIncoterm(IncotermEnumeration.DAP);

        List<OwnDelivery> deliveries = Arrays.asList(delivery, delivery2, delivery3);

        // partner supplies only
        MaterialPartnerRelation mpr = getMpr(SUPPLIER_PARTNER, true, false);

        // when - Customer gets request from supplier
        when(partnerService.findByBpnl(BPNL_SUPPLIER)).thenReturn(SUPPLIER_PARTNER);
        // cx id should be on mpr as case is customer
        when(materialService.findByMaterialNumberCx(MATERIAL_NUMBER_CX_SUPPLIER)).thenReturn(null);
        // return null to trigger update of Partner CX Id, then return valid mpr after update
        when(mprService.findByPartnerAndPartnerCXNumber(SUPPLIER_PARTNER, MATERIAL_NUMBER_CX_SUPPLIER))
            .thenReturn(null)
            .thenReturn(mpr);

        // update the partnerCxNumber mocked wise - could be done differently, but is closer to workflow
        doAnswer((Answer<Void>) invocation -> {
            mpr.setPartnerCXNumber(MATERIAL_NUMBER_CX_SUPPLIER);
            return null;
        }).when(mprService).triggerPartTypeRetrievalTask(SUPPLIER_PARTNER);

        // return all deliveries defined above
        // predicate for customer case will return delivery, delivery2
        when(ownDeliveryService.findAllByFilters(
            Optional.of(mpr.getMaterial().getOwnMaterialNumber()),
            Optional.empty(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            Optional.empty()
        )).thenReturn(deliveries);

        // return mpr after update
        when(mprService.find(mpr.getMaterial(), SUPPLIER_PARTNER)).thenReturn(mpr);

        // trigger the api handling, we don't care about the answer as we don't mock the samm mapper
        deliveryRequestApiService.handleDeliverySubmodelRequest(BPNL_SUPPLIER, MATERIAL_NUMBER_CX_SUPPLIER);

        //then
        // verify that update has been triggered due to missing cx id as customer
        verify(mprService, times(1)).triggerPartTypeRetrievalTask(SUPPLIER_PARTNER);
        // we don't mock the samm mapper and do not evaluate it results as it will be null (no mock implementation).
        // verfy that samm mapper has been called correctly (our concern)
        verify(sammMapper)
            .ownDeliveryToSamm(
                eq(Arrays.asList(delivery, delivery2)),
                eq(SUPPLIER_PARTNER),
                eq(mpr.getMaterial())
            );
    }

    @Test
    void noIncotermsUniqueMprRole_testPartnerRolePredicate_returnsTrue() {
        // given
        // Delivery without incoterm
        OwnDelivery delivery = getDelivery();

        List<OwnDelivery> deliveries = Collections.singletonList(delivery);

        // partner buys only
        MaterialPartnerRelation mpr = getMpr(CUSTOMER_PARTNER, false, true);
        mpr.setPartnerCXNumber(MATERIAL_NUMBER_CX_SUPPLIER);

        // when - Supplier gets request from customer
        Predicate<OwnDelivery> partnerRoleDirectionPredicate = DeliveryRequestApiService.
            partnerRoleDirectionPredicate(true, mpr);

        List<OwnDelivery> filteredDeliveries = deliveries.stream().filter(
            partnerRoleDirectionPredicate
        ).toList();

        //then
        assertEquals(deliveries, filteredDeliveries);
    }

    @Test
    void noIncotermsNonUniqueMprRole_testPartnerRolePredicate_returnsFalse() {
        // given
        // Delivery without incoterm
        OwnDelivery delivery = getDelivery();

        List<OwnDelivery> deliveries = Collections.singletonList(delivery);

        // partner buys only
        MaterialPartnerRelation mpr = getMpr(CUSTOMER_PARTNER, true, true);
        mpr.setPartnerCXNumber(MATERIAL_NUMBER_CX_SUPPLIER);

        // when - Supplier gets request from customer
        Predicate<OwnDelivery> partnerRoleDirectionPredicate = DeliveryRequestApiService.
            partnerRoleDirectionPredicate(true, mpr);

        List<OwnDelivery> filteredDeliveries = deliveries.stream().filter(
            partnerRoleDirectionPredicate
        ).toList();

        //then
        assertEquals(0, filteredDeliveries.size());
    }

    @Test
    void setIncotermsCustomerRole_testPartnerRolePredicate_filters() {
        // given
        // Delivery with incoterm with Customer responsibility
        OwnDelivery delivery = getDelivery();
        delivery.setIncoterm(IncotermEnumeration.EXW);

        // Delivery with incoterm with PARTIAL responsibility
        OwnDelivery delivery2 = getDelivery();
        delivery2.setIncoterm(IncotermEnumeration.FCA);

        // Delivery with incoterm with SUPPLIER responsibility
        OwnDelivery delivery3 = getDelivery();
        delivery3.setIncoterm(IncotermEnumeration.DAP);

        List<OwnDelivery> deliveries = Arrays.asList(delivery, delivery2, delivery3);

        // partner buys and supplies
        MaterialPartnerRelation mpr = getMpr(CUSTOMER_PARTNER, true, true);
        mpr.setPartnerCXNumber(MATERIAL_NUMBER_CX_SUPPLIER);

        // when - Supplier gets request from customer
        Predicate<OwnDelivery> partnerRoleDirectionPredicate = DeliveryRequestApiService.
            partnerRoleDirectionPredicate(true, mpr);

        List<OwnDelivery> filteredDeliveries = deliveries.stream().filter(
            partnerRoleDirectionPredicate
        ).toList();

        //then
        assertEquals(2, filteredDeliveries.size());
        // Responsibilities SUPPLIER and PARTIAL have been found, order IS relevant
        assertEquals(Arrays.asList(delivery2, delivery3), filteredDeliveries);
    }

    @Test
    void setIncotermsSupplierRole_testPartnerRolePredicate_filters() {
        // given
        // Delivery with incoterm with Customer responsibility
        OwnDelivery delivery = getDelivery();
        delivery.setIncoterm(IncotermEnumeration.EXW);

        // Delivery with incoterm with PARTIAL responsibility
        OwnDelivery delivery2 = getDelivery();
        delivery2.setIncoterm(IncotermEnumeration.FCA);

        // Delivery with incoterm with SUPPLIER responsibility
        OwnDelivery delivery3 = getDelivery();
        delivery3.setIncoterm(IncotermEnumeration.DAP);

        List<OwnDelivery> deliveries = Arrays.asList(delivery, delivery2, delivery3);

        // partner supplies only
        MaterialPartnerRelation mpr = getMpr(CUSTOMER_PARTNER, true, true);
        mpr.setPartnerCXNumber(MATERIAL_NUMBER_CX_SUPPLIER);

        // when - Supplier gets request from customer
        Predicate<OwnDelivery> partnerRoleDirectionPredicate = DeliveryRequestApiService.
            partnerRoleDirectionPredicate(false, mpr);

        List<OwnDelivery> filteredDeliveries = deliveries.stream().filter(
            partnerRoleDirectionPredicate
        ).toList();

        //then
        assertEquals(2, filteredDeliveries.size());
        // Responsibilities PARTIAL and CUSTOMER have been found, order IS relevant
        assertEquals(Arrays.asList(delivery, delivery2), filteredDeliveries);
    }

    private OwnDelivery getDelivery() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Date tomorrowDate = Date.from(tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return OwnDelivery.builder()
            .material(TEST_MATERIAL)
            .partner(CUSTOMER_PARTNER)
            .originBpna(BPNS_SUPPLIER)
            .originBpna(BPNA_SUPPLIER)
            .departureType(EventTypeEnumeration.ESTIMATED_DEPARTURE)
            .dateOfDeparture(tomorrowDate)
            .destinationBpns(BPNS_CUSTOMER)
            .destinationBpna(BPNA_CUSTOMER)
            .arrivalType(EventTypeEnumeration.ESTIMATED_ARRIVAL)
            .dateOfArrival(new Date())
            .incoterm(null)
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(200)
            .build();
    }

    private MaterialPartnerRelation getMpr(Partner partner, boolean partnerSupplies, boolean partnerBuys) {
        return new MaterialPartnerRelation(
            TEST_MATERIAL,
            partner,
            "Partner Material Number",
            partnerSupplies,
            partnerBuys
        );
    }
}
