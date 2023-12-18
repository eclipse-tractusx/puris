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
package org.eclipse.tractusx.puris.backend.stock.masterdata.logic;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class ItemStockServiceTest {
//
//    @Mock
//    private ItemStockRepository itemStockRepository;
//    @Mock
//    private MaterialPartnerRelationService materialPartnerRelationService;
//    @Mock
//    private PartnerService partnerService;
//    @InjectMocks
//    private ItemStockService itemStockService;
//    private final String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
//    private final String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";
//
//    @Test
//    void storeAndFindItemStock() {
//        Partner supplierPartner = getSupplierPartner();
//        ItemStock itemStock = getItemStock(supplierPartner, getMaterial());
//        final var is = itemStock;
//        when(partnerService.findByBpnl(supplierPartner.getBpnl())).thenAnswer(x -> supplierPartner);
//        when(itemStockRepository.save(Mockito.any(ItemStock.class))).thenAnswer(i -> i.getArguments()[0]);
//        when(itemStockRepository.findById(is.getUuid())).thenAnswer(x -> Optional.of(is));
//        when(materialPartnerRelationService.find(semiconductorMatNbrCustomer, supplierPartner.getUuid())).thenAnswer(x -> getMaterialPartnerRelation());
//
//        itemStock = itemStockService.create(itemStock);
//        var foundItemStock = itemStockService.findById(itemStock.getUuid());
//        Assertions.assertEquals(itemStock, foundItemStock);
//    }
//
//    private ItemStock getItemStock(Partner supplierPartner, Material material) {
//        ItemStock.ItemStockBuilder builder = ItemStock.builder();
//        var itemStock = builder
//            .customerOrderId("123")
//            .supplierOrderId("234")
//            .customerOrderPositionId("1")
//            .material(material)
//            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
//            .locationBpns(supplierPartner.getSites().first().getBpns())
//            .locationBpna(supplierPartner.getSites().first().getAddresses().first().getBpna())
//            .partner(supplierPartner)
//            .quantity(5.0)
//            .build();
//        return itemStock;
//    }
//
//    private Partner getSupplierPartner() {
//        Partner supplierPartnerEntity = new Partner(
//            "Scenario Supplier",
//            "http://supplier-control-plane:9184/api/v1/dsp",
//            "BPNL1234567890ZZ",
//            "BPNS1234567890ZZ",
//            "Konzernzentrale Dudelsdorf",
//            "BPNA1234567890AA",
//            "Heinrich-Supplier-Straße 1",
//            "77785 Dudelsdorf",
//            "Germany"
//        );
//        supplierPartnerEntity.setUuid(UUID.randomUUID());
//        return supplierPartnerEntity;
//    }
//
//    private MaterialPartnerRelation getMaterialPartnerRelation() {
//        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
//        mpr.setPartnerMaterialNumber(semiconductorMatNbrSupplier);
//        mpr.setPartnerSuppliesMaterial(true);
//        mpr.setMaterial(getMaterial());
//        return mpr;
//    }
//
//    private Material getMaterial() {
//        Material material = new Material();
//        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
//        material.setMaterialFlag(true);
//        material.setName("Semiconductor");
//        return material;
//    }
}
