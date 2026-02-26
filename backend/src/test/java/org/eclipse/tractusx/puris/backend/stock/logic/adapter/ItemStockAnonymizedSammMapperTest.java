/*
 * Copyright (c) 2026 Volkswagen AG
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm.AllocatedStockAnonymized;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm.ItemStockAnonymizedSamm;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ItemStockAnonymizedSammMapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(ItemStockAnonymizedSammMapperTest.class);
    private static ItemStockSamm SAMM_FROM_CUSTOMER_PARTNER;
    final static String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
    final static String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
    final static String CX_MAT_NUMBER = UUID.randomUUID().toString();
    final static String OWN_BPNS = "BPNS4444444444SS";
    final static String OWN_BPNA = "BPNA4444444444AA";
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";

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
    private MaterialPartnerRelationService mprService;

    @Mock
    private MaterialService materialService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    private ItemStockSammMapper itemStockSammMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Order(1)
    void map_WhenSingleMaterialItemStock_ReturnsItemStockAnonymizedSamm() {
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
        mpr.setPartnerCXNumber(CX_MAT_NUMBER);

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


        // When
        when(mprService.find(semiconductorMaterial, supplierPartner)).thenReturn(mpr);

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));

        ItemStockAnonymizedSamm materialItemStockAnonymizedSamm = itemStockSammMapper.materialItemStocksToItemStockAnonymizedSamm(List.of(materialItemStock), supplierPartner, semiconductorMaterial, "SALT");

        // Then
        assertNotNull(materialItemStockAnonymizedSamm);

        assertEquals(DirectionCharacteristic.INBOUND, materialItemStockAnonymizedSamm.getDirection());
        assertTrue(materialItemStockAnonymizedSamm.getMaterialGlobalAssetIdAnonymized().startsWith("enc:"));

        
        assertNotNull(materialItemStockAnonymizedSamm.getAllocatedStocksAnonymized());
        assertEquals(1, materialItemStockAnonymizedSamm.getAllocatedStocksAnonymized().size());

        AllocatedStockAnonymized allocatedStockAnoynmized = materialItemStockAnonymizedSamm.getAllocatedStocksAnonymized().stream().toList().get(0);
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, allocatedStockAnoynmized.getQuantityOnAllocatedStock().getUnit());
        assertTrue(allocatedStockAnoynmized.getStockLocationBPNSAnonymized().startsWith("enc:"));
        assertEquals(materialItemStock.isBlocked(), allocatedStockAnoynmized.getIsBlocked());
        assertEquals(materialItemStock.getQuantity(), allocatedStockAnoynmized.getQuantityOnAllocatedStock().getValue());


        assertEquals(materialItemStock.getLastUpdatedOnDateTime(), allocatedStockAnoynmized.getLastUpdatedOnDateTime());
    }
    
}
