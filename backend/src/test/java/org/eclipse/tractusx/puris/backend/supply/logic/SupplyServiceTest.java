/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.supply.logic;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.logic.service.OwnProductionService;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.MaterialItemStockRepository;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ProductItemStockRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnSupplierSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedCustomerSupplyRepository;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedSupplierSupplyRepository;
import org.eclipse.tractusx.puris.backend.supply.logic.service.CustomerSupplyService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.SupplierSupplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SupplyServiceTest {
    @InjectMocks
    CustomerSupplyService customerSupplyService;

    @InjectMocks
    SupplierSupplyService supplierSupplyService;

    @Mock
    MaterialService materialService;

    @Mock
    PartnerService partnerService;

    @Mock
    MaterialItemStockService materialItemStockService;

    @Mock
    ProductItemStockService productItemStockService;

    @Mock
    MaterialPartnerRelationService mprService;

    @Mock 
    MaterialItemStockRepository materialItemStockRepository;

    @Mock
    ProductItemStockRepository productItemStockRepository;

    @Mock
    OwnDeliveryService ownDeliveryService;

    @Mock
    ReportedDeliveryService reportedDeliveryService;

    @Mock
    OwnDemandService ownDemandService;

    @Mock
    OwnProductionService ownProductionService;

    @Mock
    ReportedCustomerSupplyRepository reportedCustomerSupplyRepository;

    @Mock
    ReportedSupplierSupplyRepository reportedSupplierSupplyRepository;

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
            false,
            "Own-Mnr",
            MATERIAL_NUMBER_CX_CUSTOMER,
            "Test Material",
            new Date());

    private static final Material TEST_PRODUCT = new Material(
        false,
        true,
        "Own-Mnr",
        MATERIAL_NUMBER_CX_SUPPLIER,
        "Test Product",
        new Date());

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
            "DE",
            PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
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
            "DE",
            PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
        );
        SUPPLIER_PARTNER.setUuid(UUID.randomUUID());
    }

    @Test
    void testCalculateCustomerDaysOfSupply_StandardCase() {
        List<Double> demandQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> inboundDeliveryQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> reportedInboundDeliveryQuantities = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> expectedDaysOfSupply = List.of(1.0, 1.2, 2.0, 1.0, 0.0);
        Double initialStockValue = 100.0;

        testCalculateCustomerDaysOfSupply(6, demandQuantities, inboundDeliveryQuantities, reportedInboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalculateCustomerDaysOfSupply_NoInitialStock() {
        List<Double> demandQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> inboundDeliveryQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> reportedInboundDeliveryQuantities = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> expectedDaysOfSupply = List.of(0.0, 0.0, 0.2, 0.0, 0.0);
        Double initialStockValue = 0.0;

        testCalculateCustomerDaysOfSupply(6, demandQuantities, inboundDeliveryQuantities, reportedInboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalculateCustomerDaysOfSupply_CombinedDeliveries() {
        List<Double> demandQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> inboundDeliveryQuantities = List.of(0.0, 30.0, 50.0, 0.0, 0.0, 20.0);
        List<Double> reportedInboundDeliveryQuantities = List.of(0.0, 30.0, 50.0, 0.0, 0.0, 20.0);
        List<Double> expectedDaysOfSupply = List.of(1.0, 1.2, 2.0, 1.0, 0.0);
        Double initialStockValue = 100.0;

        testCalculateCustomerDaysOfSupply(6, demandQuantities, inboundDeliveryQuantities, reportedInboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalcualteCustomerDaysOfSupply_InsufficientDays() {
        List<Double> demandQuantities = List.of(40.0);
        List<Double> inboundDeliveryQuantities = List.of(0.0);
        List<Double> reportedInboundDeliveryQuantities = List.of(0.0);
        List<Double> expectedDaysOfSupply = List.of();
        Double initialStockValue = 100.0;

        testCalculateCustomerDaysOfSupply(1, demandQuantities, inboundDeliveryQuantities, reportedInboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    void testCalculateCustomerDaysOfSupply(int numberOfDays, List<Double> demandQuantities, List<Double> inboundDeliveryQuantities, List<Double> reportedInboundDeliveryQuantities, List<Double> expectedDaysOfSupply, Double initialStockValue) {
        when(ownDemandService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            numberOfDays
        )).thenReturn(demandQuantities);

        when(ownDeliveryService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            DirectionCharacteristic.INBOUND,
            numberOfDays
        )).thenReturn(inboundDeliveryQuantities);

        when(reportedDeliveryService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            DirectionCharacteristic.INBOUND,
            numberOfDays
        )).thenReturn(reportedInboundDeliveryQuantities);

        Optional<String> partnerBpnl = Optional.of(BPNL_SUPPLIER);
        when(partnerService.findByBpnl(partnerBpnl.get())).thenReturn(SUPPLIER_PARTNER);

        when(materialItemStockService.getInitialStockQuantity(TEST_MATERIAL.getOwnMaterialNumber(), partnerBpnl, Optional.empty())).thenReturn(initialStockValue);
        when(materialService.findByOwnMaterialNumber(TEST_MATERIAL.getOwnMaterialNumber())).thenReturn(TEST_MATERIAL);

        List<OwnCustomerSupply> customerSupplies = customerSupplyService.calculateCustomerDaysOfSupply(TEST_MATERIAL.getOwnMaterialNumber(), partnerBpnl, Optional.empty(), numberOfDays);

        assertEquals(numberOfDays - 1, customerSupplies.size());
        assertEquals(expectedDaysOfSupply, customerSupplies.stream().map(supply -> supply.getDaysOfSupply()).toList());
        
    }

    @Test
    void testCalculateSupplierDaysOfSupply_StandardCase() {
        List<Double> productionQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> outboundDeliveryQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> reportedOutboundDeliveryQuantities = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> expectedDaysOfSupply = List.of(1.0, 1.2, 2.0, 1.0, 0.0);
        Double initialStockValue = 100.0;

        testCalculateSupplierDaysOfSupply(6, productionQuantities, outboundDeliveryQuantities, reportedOutboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalculateSupplierDaysOfSupply_NoInitialStock() {
        List<Double> productionQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> outboundDeliveryQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> reportedOutboundDeliveryQuantities = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> expectedDaysOfSupply = List.of(0.0, 0.0, 0.2, 0.0, 0.0);
        Double initialStockValue = 0.0;

        testCalculateSupplierDaysOfSupply(6, productionQuantities, outboundDeliveryQuantities, reportedOutboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalculateSupplierDaysOfSupply_CombinedDeliveries() {
        List<Double> productionQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> outboundDeliveryQuantities = List.of(20.0, 30.0, 25.0, 25.0, 30.0, 25.0);
        List<Double> reportedOutboundDeliveryQuantities = List.of(20.0, 30.0, 25.0, 25.0, 30.0, 25.0);
        List<Double> expectedDaysOfSupply = List.of(1.0, 1.2, 2.0, 1.0, 0.0);
        Double initialStockValue = 100.0;

        testCalculateSupplierDaysOfSupply(6, productionQuantities, outboundDeliveryQuantities, reportedOutboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    @Test
    void testCalculateSupplierDaysOfSupply_InsufficientDays() {
        List<Double> productionQuantities = List.of(40.0);
        List<Double> outboundDeliveryQuantities = List.of(0.0);
        List<Double> reportedOutboundDeliveryQuantities = List.of(0.0);
        List<Double> expectedDaysOfSupply = List.of();
        Double initialStockValue = 100.0;

        testCalculateSupplierDaysOfSupply(1, productionQuantities, outboundDeliveryQuantities, reportedOutboundDeliveryQuantities, expectedDaysOfSupply, initialStockValue);
    }

    void testCalculateSupplierDaysOfSupply(int numberOfDays, List<Double> productionQuantities, List<Double> outboundDeliveryQuantities, List<Double> reportedOutboundDeliveryQuantities, List<Double> expectedDaysOfSupply, Double initialStockValue) {

        when(ownProductionService.getQuantityForDays(
            TEST_PRODUCT.getOwnMaterialNumber(),
            Optional.of(BPNL_CUSTOMER),
            Optional.empty(),
            numberOfDays
        )).thenReturn(productionQuantities);

        when(ownDeliveryService.getQuantityForDays(
            TEST_PRODUCT.getOwnMaterialNumber(),
            Optional.of(BPNL_CUSTOMER),
            Optional.empty(),
            DirectionCharacteristic.OUTBOUND,
            numberOfDays
        )).thenReturn(outboundDeliveryQuantities);

        when(reportedDeliveryService.getQuantityForDays(
            TEST_PRODUCT.getOwnMaterialNumber(),
            Optional.of(BPNL_CUSTOMER),
            Optional.empty(),
            DirectionCharacteristic.OUTBOUND,
            numberOfDays
        )).thenReturn(reportedOutboundDeliveryQuantities);

        Optional<String> partnerBpnl = Optional.of(BPNL_CUSTOMER);
        when(partnerService.findByBpnl(partnerBpnl.get())).thenReturn(CUSTOMER_PARTNER);

        when(productItemStockService.getInitialStockQuantity(TEST_PRODUCT.getOwnMaterialNumber(), partnerBpnl, Optional.empty())).thenReturn(initialStockValue);
        when(materialService.findByOwnMaterialNumber(TEST_PRODUCT.getOwnMaterialNumber())).thenReturn(TEST_PRODUCT);

        List<OwnSupplierSupply> supplierSupplies = supplierSupplyService.calculateSupplierDaysOfSupply(TEST_PRODUCT.getOwnMaterialNumber(), partnerBpnl, Optional.empty(), numberOfDays);

        assertEquals(numberOfDays - 1, supplierSupplies.size());
        assertEquals(expectedDaysOfSupply, supplierSupplies.stream().map(supply -> supply.getDaysOfSupply()).toList());
    }
}
