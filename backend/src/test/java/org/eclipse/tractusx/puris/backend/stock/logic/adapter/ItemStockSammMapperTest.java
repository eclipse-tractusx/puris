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
package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class ItemStockSammMapperTest {

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
    void map_WhenSingleMaterialItemStock_ReturnsItemStockSamm() {
        // Given
        final String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
        final String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
        final String OWN_BPNL = "BPNL4444444444LL";
        final String OWN_BPNS = "BPNS4444444444SS";
        final String OWN_BPNA = "BPNA4444444444AA";
        final String SUPPLIER_BPNL = "BPNL1111111111LE";
        final String SUPPLIER_BPNS = "BPNS1111111111SI";
        final String SUPPLIER_BPNA = "BPNA1111111111AD";

        Partner supplierPartner = new Partner(
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

        // When - no when
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(mpr);

        // Then we could have a message as follows:
        // - MaterialItem Stock as above
        // - MaterialItem Stock as above BUT with isBlocked = false and other Quanitty
        // - MaterialItem Stock as above BUT with OrderPositionReference and other Quantity
        // These should result in two positions
        // - one WITHOUT orderPositionReference AND two allocatedStocks
        // - one WITH orderPosition AND one allocatedStocks
        ItemStockSAMM materialStockSamm = itemStockSammMapper.toItemStockSAMM(materialItemStock);

        // Then
        assertNotNull(materialStockSamm);

        assertEquals(DirectionCharacteristic.INBOUND, materialStockSamm.getDirection());
        assertEquals(CUSTOMER_MAT_NUMBER, materialStockSamm.getMaterialNumberCustomer());
        assertEquals(SUPPLIER_MAT_NUMBER, materialStockSamm.getMaterialNumberSupplier());
        assertEquals(null, materialStockSamm.getMaterialGlobalAssetId());

        // TODO: This crashes. I assume because the position is not set
        assertEquals(1, materialStockSamm.getPositions().size());

        Position position = materialStockSamm.getPositions().stream().findFirst().get();
        assertEquals(null, position.getOrderPositionReference());

        assertEquals(1, position.getAllocatedStocks().size());

        AllocatedStock allocatedStock = position.getAllocatedStocks().get(0);
        assertEquals(50, allocatedStock.getQuantityOnAllocatedStock().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, allocatedStock.getQuantityOnAllocatedStock().getUnit());
        assertEquals(OWN_BPNS, allocatedStock.getStockLocationBPNS());
        assertEquals(OWN_BPNA, allocatedStock.getStockLocationBPNA());

        assertEquals(materialItemStock.getLastUpdatedOnDateTime(), position.getLastUpdatedOnDateTime());
    }

    @Test
    void map_WhenReportedSammToProductItemStock_ReturnsMultipleReportedProductItemStock() {
        // Given
        final String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
        final String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
        final String CX_MAT_NUMBER = UUID.randomUUID().toString();
        final String OWN_BPNL = "BPNL4444444444LL";
        final String OWN_BPNS = "BPNS4444444444SS";
        final String OWN_BPNA = "BPNA4444444444AA";
        final String SUPPLIER_BPNL = "BPNL1111111111LE";
        final String SUPPLIER_BPNS = "BPNS1111111111SI";
        final String SUPPLIER_BPNA = "BPNA1111111111AD";
        final String SUPPLIER_BPNS2 = "BPNS2222222222SI";
        final String SUPPLIER_BPNA2 = "BPNA2222222222AD";

        final String OPR_REF_CUSTOMER_ORDER_ID = "C-Nbr-4711";
        final String OPR_REF_SUPPLIER_ORDER_ID = "S-Nbr-4712";
        final String OPR_REF_CUSTOMER_POS_ID = "C-Nbr-4711-Pos-1";

        ItemStockSAMM outboundProductStockSamm = new ItemStockSAMM();

        // TODO: A warning comes up. The direction is set according to the information sender. As the Supplier reports the Stock of his product, this should be OUTBOUND
        outboundProductStockSamm.setDirection(DirectionCharacteristic.OUTBOUND);
        outboundProductStockSamm.setMaterialNumberCustomer(CUSTOMER_MAT_NUMBER);
        outboundProductStockSamm.setMaterialNumberSupplier(SUPPLIER_MAT_NUMBER);
        outboundProductStockSamm.setMaterialGlobalAssetId(CX_MAT_NUMBER);

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

        outboundProductStockSamm.setPositions(Arrays.asList(anonymousPosition, positionWithOrderPositionReference));

        Partner supplierPartner = new Partner(
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

        Site site2 = new Site(SUPPLIER_BPNS2, "Site 2", SUPPLIER_BPNA2, "Test Street 2", "44174 Dortmund", "Germany");

        SortedSet<Site> adjustedSet = supplierPartner.getSites();
        adjustedSet.add(site2);

        supplierPartner.setSites(adjustedSet);

        Material semiconductorProduct = Material.builder()
            .ownMaterialNumber(CUSTOMER_MAT_NUMBER)
            .materialNumberCx(CX_MAT_NUMBER)
            .materialFlag(true)
            .productFlag(true)
            .name("Semiconductor")
            .build();

        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartner(supplierPartner);
        mpr.setMaterial(semiconductorProduct);
        mpr.setPartnerBuysMaterial(false);
        mpr.setPartnerSuppliesMaterial(true);
        mpr.setPartnerMaterialNumber(SUPPLIER_MAT_NUMBER);

        // When
        // Find material based on CX number and mpr
        when(materialService.findByMaterialNumberCx(CX_MAT_NUMBER)).thenReturn(semiconductorProduct);
        when(mprService.find(semiconductorProduct, supplierPartner)).thenReturn(mpr);

        // Then we should build 5 reported product stocks:
        // - no OrderPositionReference (OPR), blocked, 10 pieces, BPNS & BPNA
        // - no OPR, not blocked, 20 kilo, BPNS2 & BPNA2
        // - no OPR, not blocked, 20 kilo, BPNS & BPNA
        // - OPR, blocked, 10 pieces, BPNS & BPNA
        // - OPR, not blocked, 20 kilo, BPNS & BPNA
        List<ReportedProductItemStock> reportedProductItemStocks = itemStockSammMapper.sammToReportedProductItemStock(outboundProductStockSamm, supplierPartner);

        // Then
        assertEquals(5, reportedProductItemStocks.size());

        // 1. anonymous stocks
        // Check for stockBlocked
        List<ReportedProductItemStock> potentialBlockedAnonymousStock = filterReportedItemStock(
                reportedProductItemStocks,
                stockBlocked,
                anonymousPosition,
                CUSTOMER_MAT_NUMBER
            );

        assertEquals(1, potentialBlockedAnonymousStock.size());

        // Check for stockNOTBlocked
        List<ReportedProductItemStock> potentialNotBlockedAnonymousStock = filterReportedItemStock(
            reportedProductItemStocks,
            stockNotBlocked,
            anonymousPosition,
            CUSTOMER_MAT_NUMBER
        );

        assertEquals(1, potentialNotBlockedAnonymousStock.size());

        // Check for stockBlocked
        List<ReportedProductItemStock> potentialAnonymousStockOtherSite = filterReportedItemStock(
            reportedProductItemStocks,
            stockOtherSite,
            anonymousPosition,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialAnonymousStockOtherSite.size());

        // 2. opr related ones

        // Check for oprStockBlocked
        List<ReportedProductItemStock> potentialOprStockBlocked = filterReportedItemStock(
            reportedProductItemStocks,
            oprStockBlocked,
            positionWithOrderPositionReference,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialOprStockBlocked.size());

        // Check for oprStockNotBlocked
        List<ReportedProductItemStock> potentialOprStockNotBlocked = filterReportedItemStock(
            reportedProductItemStocks,
            oprStockBlocked,
            positionWithOrderPositionReference,
            CUSTOMER_MAT_NUMBER
        );
        assertEquals(1, potentialOprStockNotBlocked.size());

    }

    private List<ReportedProductItemStock> filterReportedItemStock(List<ReportedProductItemStock> reportedProductItemStocks, AllocatedStock allocatedStock, Position position, String ownMaterialNumber){

        if (position.getOrderPositionReference() ==null) {

            List<ReportedProductItemStock> potentialStocks = reportedProductItemStocks.stream()
                .filter(item -> item.isBlocked() == allocatedStock.getIsBlocked())
                .filter(item -> item.getMeasurementUnit().equals(allocatedStock.getQuantityOnAllocatedStock().getUnit()))
                .filter(item -> item.getQuantity() == allocatedStock.getQuantityOnAllocatedStock().getValue())
                .filter(item -> item.getMaterial().getOwnMaterialNumber() == ownMaterialNumber)
                .filter(item -> item.getLocationBpna().equals(allocatedStock.getStockLocationBPNA()))
                .filter(item -> item.getLocationBpns().equals(allocatedStock.getStockLocationBPNS()))
                .filter(item -> item.getLastUpdatedOnDateTime() == position.getLastUpdatedOnDateTime())
                .filter(item -> item.getCustomerOrderId() == null)
                .filter(item -> item.getSupplierOrderId() == null)
                .filter(item -> item.getCustomerOrderPositionId() == null)
                .collect(Collectors.toList());
            return potentialStocks;
        }else {
            List<ReportedProductItemStock> potentialStocks = reportedProductItemStocks.stream()
                .filter(item -> item.isBlocked() == allocatedStock.getIsBlocked())
                .filter(item -> item.getMeasurementUnit().equals(allocatedStock.getQuantityOnAllocatedStock().getUnit()))
                .filter(item -> item.getQuantity() == allocatedStock.getQuantityOnAllocatedStock().getValue())
                .filter(item -> item.getMaterial().getOwnMaterialNumber() == ownMaterialNumber)
                .filter(item -> item.getLocationBpna().equals(allocatedStock.getStockLocationBPNA()))
                .filter(item -> item.getLocationBpns().equals(allocatedStock.getStockLocationBPNS()))
                .filter(item -> item.getLastUpdatedOnDateTime() == position.getLastUpdatedOnDateTime())
                .filter(item -> item.getCustomerOrderId() == position.getOrderPositionReference().getCustomerOrderId())
                .filter(item -> item.getSupplierOrderId() == position.getOrderPositionReference().getSupplierOrderId())
                .filter(item -> item.getCustomerOrderPositionId() == position.getOrderPositionReference().getCustomerOrderPositionId())
                .collect(Collectors.toList());
            return potentialStocks;
        }
    }
}
