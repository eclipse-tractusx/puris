/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.junit.jupiter.api.*;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ItemStockSammMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(ItemStockSammMapperTest.class);
    private static ItemStockSamm SAMM_FROM_CUSTOMER_PARTNER;
    final static String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
    final static String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
    final static String CX_MAT_NUMBER = UUID.randomUUID().toString();
    final static String OWN_BPNL = "BPNL4444444444LL";
    final static String OWN_BPNS = "BPNS4444444444SS";
    final static String OWN_BPNA = "BPNA4444444444AA";
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";

    final static String OPR_REF_CUSTOMER_ORDER_ID = "C-Nbr-4711";
    final static String OPR_REF_SUPPLIER_ORDER_ID = "S-Nbr-4712";
    final static String OPR_REF_CUSTOMER_POS_ID = "C-Nbr-4711-Pos-1";
    final static String SUPPLIER_BPNS2 = "BPNS2222222222SI";
    final static String SUPPLIER_BPNA2 = "BPNA2222222222AD";

    final static Partner supplierPartner = new Partner(
        "Scenario Supplier",
        "http://supplier-control-plane:9184/api/v1/dsp",
        SUPPLIER_BPNL,
        SUPPLIER_BPNS,
        "Konzernzentrale Dudelsdorf",
        SUPPLIER_BPNA,
        "Heinrich-Supplier-Straße 1",
        "77785 Dudelsdorf",
        "Germany"
    );

    final static Partner customerPartner = new Partner(
        "Scenario Customer",
        "http://customer-control-plane:8184/api/v1/dsp",
        "BPNL4444444444XX",
        "BPNS4444444444XX",
        "Hauptwerk Musterhausen",
        "BPNA4444444444ZZ",
        "Musterstraße 35b",
        "77777 Musterhausen",
        "Germany"
    );

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialPartnerRelationService mprService;

    @InjectMocks
    private ItemStockSammMapper itemStockSammMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Order(1)
    void map_WhenSingleMaterialItemStock_ReturnsItemStockSamm() {
        // Given
        Material semiconductorMaterial = Material.builder()
            .ownMaterialNumber(CUSTOMER_MAT_NUMBER)
            .materialFlag(true)
            .productFlag(false)
            .name("Semiconductor")
            .build();

        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(supplierPartner);
        mpr.setMaterial(semiconductorMaterial);
        mpr.setPartnerBuysMaterial(false);
        mpr.setPartnerSuppliesMaterial(true);
        mpr.setPartnerMaterialNumber(SUPPLIER_MAT_NUMBER);

        MaterialItemStock materialItemStock = MaterialItemStock.builder()
            .partner(supplierPartner)
            .material(semiconductorMaterial)
            .lastUpdatedOnDateTime(new Date())
            .locationBpna(OWN_BPNA)
            .locationBpns(OWN_BPNS)
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(20)
            .isBlocked(true)
            .build();

        System.out.println(materialItemStock);

        // When
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(mpr);

        // Then we could have a message as follows:
        // - MaterialItem Stock as above
        // - MaterialItem Stock as above BUT with isBlocked = false and other Quanitty
        // - MaterialItem Stock as above BUT with OrderPositionReference and other Quantity
        // These should result in two positions
        // - one WITHOUT orderPositionReference AND two allocatedStocks
        // - one WITH orderPosition AND one allocatedStocks
        ItemStockSamm materialItemStockSamm = itemStockSammMapper.materialItemStocksToItemStockSamm(List.of(materialItemStock));

        // Then
        assertNotNull(materialItemStockSamm);

        assertEquals(DirectionCharacteristic.INBOUND, materialItemStockSamm.getDirection());
        assertEquals(CUSTOMER_MAT_NUMBER, materialItemStockSamm.getMaterialNumberCustomer());
        assertEquals(SUPPLIER_MAT_NUMBER, materialItemStockSamm.getMaterialNumberSupplier());
        assertNull(materialItemStockSamm.getMaterialGlobalAssetId());

        assertEquals(1, materialItemStockSamm.getPositions().size());

        Position position = materialItemStockSamm.getPositions().stream().findFirst().get();
        assertNull(position.getOrderPositionReference());

        assertEquals(1, position.getAllocatedStocks().size());

        AllocatedStock allocatedStock = position.getAllocatedStocks().get(0);
        assertEquals(20, allocatedStock.getQuantityOnAllocatedStock().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, allocatedStock.getQuantityOnAllocatedStock().getUnit());
        assertEquals(OWN_BPNS, allocatedStock.getStockLocationBPNS());
        assertEquals(OWN_BPNA, allocatedStock.getStockLocationBPNA());

        assertEquals(materialItemStock.getLastUpdatedOnDateTime(), position.getLastUpdatedOnDateTime());
    }

    /**
     * Tests following Scenario: Supplier asks the Customer for Stocks of a given material that the customer
     * has in place and has been supplied by the Supplier earlier. The supplier receives the INBOUND ItemStockSAMM and
     * maps it to the ReportedProductItemStock.
     *
     * In technical terms, this means the following:
     * <li>Given: Create INBOUND ItemStock to represent the Stock level on Customer side</li>
     * <li>When: Use mapper to create the ReportedProductItemSoc</li>
     * <li>Then: Validate the Mappings</li>
     *
     * Note: The test brings fills the {@code SAMM_FROM_CUSTOMER_PARTNER}.
     */
    @Test
    @Order(2)
    void map_WhenReportedSammToProductItemStock_ReturnsMultipleReportedProductItemStock() {
        // If we want to map a Samm to a ProductItemStock entity, then this implies
        // that we are the supplier, who has received a Samm from his customer partner.
        // That customer partner therefore would have generated the Samm from his
        // MaterialEntityStock entities.
        // Since from the customer's point of view, the items he received from his supplier
        // are INBOUND, then we have to set the DirectionCharacteristic accordingly.

        // Given
        ItemStockSamm inboundProductStockSamm = new ItemStockSamm();

        inboundProductStockSamm.setDirection(DirectionCharacteristic.INBOUND);
        inboundProductStockSamm.setMaterialNumberCustomer(CUSTOMER_MAT_NUMBER);
        inboundProductStockSamm.setMaterialNumberSupplier(SUPPLIER_MAT_NUMBER);
        inboundProductStockSamm.setMaterialGlobalAssetId(CX_MAT_NUMBER);

        // first position
        Position anonymousPosition = new Position();
        anonymousPosition.setLastUpdatedOnDateTime(new Date());

        // with three allocatedStocks
        ItemQuantityEntity tenPieces = new ItemQuantityEntity();
        tenPieces.setUnit(ItemUnitEnumeration.UNIT_PIECE);
        tenPieces.setValue(10.0);

        AllocatedStock stockBlocked = new AllocatedStock(
            tenPieces,
            SUPPLIER_BPNS,
            true,
            SUPPLIER_BPNA
        );

        ItemQuantityEntity twentyKilo = new ItemQuantityEntity();
        twentyKilo.setUnit(ItemUnitEnumeration.UNIT_KILOGRAM);
        twentyKilo.setValue(20.0);

        AllocatedStock stockNotBlocked = new AllocatedStock(
            twentyKilo,
            SUPPLIER_BPNS,
            false,
            SUPPLIER_BPNA
        );

        AllocatedStock stockOtherSite = new AllocatedStock(
            twentyKilo,
            SUPPLIER_BPNS2,
            false,
            SUPPLIER_BPNA2
        );

        anonymousPosition.setAllocatedStocks(Arrays.asList(stockBlocked, stockNotBlocked, stockOtherSite));

        // second position WITH OrderPositionReference
        Position positionWithOrderPositionReference = new Position();
        anonymousPosition.setLastUpdatedOnDateTime(new Date());

        // with two allocatedStocks
        AllocatedStock oprStockBlocked = new AllocatedStock(
            tenPieces,
            SUPPLIER_BPNS,
            true,
            SUPPLIER_BPNA
        );

        AllocatedStock oprStockNotBlocked = new AllocatedStock(
            twentyKilo,
            SUPPLIER_BPNS,
            false,
            SUPPLIER_BPNA
        );

        positionWithOrderPositionReference.setAllocatedStocks(Arrays.asList(oprStockBlocked, oprStockNotBlocked));
        positionWithOrderPositionReference.setOrderPositionReference(new OrderPositionReference(
            OPR_REF_SUPPLIER_ORDER_ID,
            OPR_REF_CUSTOMER_ORDER_ID,
            OPR_REF_CUSTOMER_POS_ID
        ));

        inboundProductStockSamm.setPositions(Arrays.asList(anonymousPosition, positionWithOrderPositionReference));

        Site site2 = new Site(SUPPLIER_BPNS2, "Site 2", SUPPLIER_BPNA2, "Test Street 2", "44174 Dortmund", "Germany");

        SortedSet<Site> adjustedSet = customerPartner.getSites();
        adjustedSet.add(site2);

        customerPartner.setSites(adjustedSet);

        Material semiconductorProduct = Material.builder()
            .ownMaterialNumber(CUSTOMER_MAT_NUMBER)
            .materialNumberCx(CX_MAT_NUMBER)
            .materialFlag(true)
            .productFlag(true)
            .name("Semiconductor")
            .build();

        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(customerPartner);
        mpr.setMaterial(semiconductorProduct);
        mpr.setPartnerBuysMaterial(false);
        mpr.setPartnerSuppliesMaterial(true);
        mpr.setPartnerMaterialNumber(SUPPLIER_MAT_NUMBER);

        // When
        // Find material based on CX number and mpr
        when(materialService.findFromSupplierPerspective(CX_MAT_NUMBER, CUSTOMER_MAT_NUMBER, SUPPLIER_MAT_NUMBER, customerPartner)).thenReturn(semiconductorProduct);
        when(mprService.find(semiconductorProduct, customerPartner)).thenReturn(mpr);

        // Then we should build 5 reported product stocks:
        // - no OrderPositionReference (OPR), blocked, 10 pieces, BPNS & BPNA
        // - no OPR, not blocked, 20 kilo, BPNS2 & BPNA2
        // - no OPR, not blocked, 20 kilo, BPNS & BPNA
        // - OPR, blocked, 10 pieces, BPNS & BPNA
        // - OPR, not blocked, 20 kilo, BPNS & BPNA
        List<ReportedProductItemStock> reportedProductItemStocks = itemStockSammMapper.itemStockSammToReportedProductItemStock(inboundProductStockSamm, customerPartner);
        // Then
        assertEquals(5, reportedProductItemStocks.size());

        // 1. anonymous stocks
        // Check for stockBlocked
        List<? extends ItemStock> potentialBlockedAnonymousStock = filterReportedItemStock(
            reportedProductItemStocks,
            stockBlocked,
            anonymousPosition,
            CUSTOMER_MAT_NUMBER
        );

        assertEquals(1, potentialBlockedAnonymousStock.size());

        // Test that the anonymous stock has null values (not empty strings)
        ReportedProductItemStock reportedProductItemStock = (ReportedProductItemStock) potentialBlockedAnonymousStock.get(0);
        assertNull(reportedProductItemStock.getCustomerOrderId());
        assertNull(reportedProductItemStock.getSupplierOrderId());
        assertNull(reportedProductItemStock.getCustomerOrderPositionId());

        // Check for stockNOTBlocked
        List<? extends ItemStock> potentialNotBlockedAnonymousStock = filterReportedItemStock(
            reportedProductItemStocks,
            stockNotBlocked,
            anonymousPosition,
            CUSTOMER_MAT_NUMBER
        );

        assertEquals(1, potentialNotBlockedAnonymousStock.size());

        // Check for stockBlocked
        List<? extends ItemStock> potentialAnonymousStockOtherSite = filterReportedItemStock(
            reportedProductItemStocks,
            stockOtherSite,
            anonymousPosition,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialAnonymousStockOtherSite.size());

        // 2. opr related ones

        // Check for oprStockBlocked
        List<? extends ItemStock> potentialOprStockBlocked = filterReportedItemStock(
            reportedProductItemStocks,
            oprStockBlocked,
            positionWithOrderPositionReference,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialOprStockBlocked.size());

        // Test that the OPR is mapped correctly
        ReportedProductItemStock reportedOprProductItemStock = (ReportedProductItemStock) potentialOprStockBlocked.get(0);
        assertEquals(OPR_REF_CUSTOMER_ORDER_ID, reportedOprProductItemStock.getCustomerOrderId());
        assertEquals(OPR_REF_SUPPLIER_ORDER_ID, reportedOprProductItemStock.getSupplierOrderId());
        assertEquals(OPR_REF_CUSTOMER_POS_ID, reportedOprProductItemStock.getCustomerOrderPositionId());

        // Check for oprStockNotBlocked
        List<? extends ItemStock> potentialOprStockNotBlocked = filterReportedItemStock(
            reportedProductItemStocks,
            oprStockBlocked,
            positionWithOrderPositionReference,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialOprStockNotBlocked.size());

        // ATTENTION: This variable is used in marshalling tests
        // This has ben done to do the perspective Switch (supplier
        SAMM_FROM_CUSTOMER_PARTNER = inboundProductStockSamm;
    }

    /**
     * Tests following Scenario: Test checks if the SAMM can be transferred into a ReportedProductItemStock
     *
     * In technical terms, this means the following:
     * <li>Given: Use the {@code SAMM_FROM_CUSTOMER_PARTNER} created in previous test</li>
     * <li>When: Use objectMapper switch representation from SAMM json to ItemStock class</li>
     * <li>Then: Validate that mapping took place</li>
     */
    @Test
    @Order(3)
    void test_unmarshalling() {
        // Setup from the suppliers point of view
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(SUPPLIER_MAT_NUMBER);
        material.setMaterialNumberCx(CX_MAT_NUMBER);

        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(customerPartner);
        mpr.setMaterial(material);
        mpr.setPartnerBuysMaterial(true);
        mpr.setPartnerMaterialNumber(CUSTOMER_MAT_NUMBER);

        when(materialService.findFromSupplierPerspective(CX_MAT_NUMBER, CUSTOMER_MAT_NUMBER, SUPPLIER_MAT_NUMBER, customerPartner)).thenReturn(material);

        var list = itemStockSammMapper.itemStockSammToReportedProductItemStock(SAMM_FROM_CUSTOMER_PARTNER, supplierPartner);
        assertNotNull(list);
        assertEquals(5, list.size());
    }

    /**
     * Tests following Scenario: Test checks if the SAMM can be transferred into a ReportedProductItemStock via the
     * JSON serializationS
     *
     * In technical terms, this means the following:
     * <li>Given: Use the {@code SAMM_FROM_CUSTOMER_PARTNER} created in previous test</li>
     * <li>When: Use objectMapper switch representation from SAMM json to ItemStock class</li>
     * <li>Then: Validate that mapping took place</li>
     */
    @Test
    @Order(4)
    void test_deserializationFromJson() throws Exception {
        // Setup from the suppliers point of view

        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(SUPPLIER_MAT_NUMBER);
        material.setMaterialNumberCx(CX_MAT_NUMBER);

        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(customerPartner);
        mpr.setMaterial(material);
        mpr.setPartnerBuysMaterial(true);
        mpr.setPartnerMaterialNumber(CUSTOMER_MAT_NUMBER);

        when(materialService.findFromSupplierPerspective(CX_MAT_NUMBER, CUSTOMER_MAT_NUMBER, SUPPLIER_MAT_NUMBER, customerPartner)).thenReturn(material);

        ObjectMapper objectMapper = new ObjectMapper();

        String sammJsonString = objectMapper.writeValueAsString(SAMM_FROM_CUSTOMER_PARTNER);
        LOG.info(() -> {
            try {
                return objectMapper.readTree(sammJsonString).toPrettyString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        var itemStockSamm = objectMapper.readValue(sammJsonString, ItemStockSamm.class);
        assertEquals(SAMM_FROM_CUSTOMER_PARTNER, itemStockSamm);
        var list = itemStockSammMapper.itemStockSammToReportedProductItemStock(itemStockSamm, supplierPartner);
        assertEquals(5, list.size());
    }

    private List<? extends ItemStock> filterReportedItemStock(List<? extends ItemStock> reportedProductItemStocks, AllocatedStock allocatedStock, Position position, String ownMaterialNumber) {

        if (position.getOrderPositionReference() == null) {

            List<? extends ItemStock> potentialStocks = reportedProductItemStocks.stream()
                .filter(item -> item.isBlocked() == allocatedStock.getIsBlocked())
                .filter(item -> item.getMeasurementUnit().equals(allocatedStock.getQuantityOnAllocatedStock().getUnit()))
                .filter(item -> item.getQuantity() == allocatedStock.getQuantityOnAllocatedStock().getValue())
                .filter(item -> item.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .filter(item -> item.getLocationBpna().equals(allocatedStock.getStockLocationBPNA()))
                .filter(item -> item.getLocationBpns().equals(allocatedStock.getStockLocationBPNS()))
                .filter(item -> item.getLastUpdatedOnDateTime() == position.getLastUpdatedOnDateTime())
                .filter(item -> item.getCustomerOrderId() == null)
                .filter(item -> item.getSupplierOrderId() == null)
                .filter(item -> item.getCustomerOrderPositionId() == null)
                .collect(Collectors.toList());
            return potentialStocks;
        } else {
            List<? extends ItemStock> potentialStocks = reportedProductItemStocks.stream()
                .filter(item -> item.isBlocked() == allocatedStock.getIsBlocked())
                .filter(item -> item.getMeasurementUnit().equals(allocatedStock.getQuantityOnAllocatedStock().getUnit()))
                .filter(item -> item.getQuantity() == allocatedStock.getQuantityOnAllocatedStock().getValue())
                .filter(item -> item.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .filter(item -> item.getLocationBpna().equals(allocatedStock.getStockLocationBPNA()))
                .filter(item -> item.getLocationBpns().equals(allocatedStock.getStockLocationBPNS()))
                .filter(item -> item.getLastUpdatedOnDateTime() == position.getLastUpdatedOnDateTime())
                .filter(item -> item.getCustomerOrderId().equals(position.getOrderPositionReference().getCustomerOrderId()))
                .filter(item -> item.getSupplierOrderId().equals(position.getOrderPositionReference().getSupplierOrderId()))
                .filter(item -> item.getCustomerOrderPositionId().equals(position.getOrderPositionReference().getCustomerOrderPositionId()))
                .collect(Collectors.toList());
            return potentialStocks;
        }
    }
}
