/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

@DataJpaTest
public class ItemStockServiceTest {

    @Mock
    private ItemStockRepository itemStockRepository;
    @Mock
    private MaterialPartnerRelationService materialPartnerRelationService;
    @Mock
    private PartnerService partnerService;
    @InjectMocks
    private ItemStockService itemStockService;
    private final String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
    private final String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";

    @Test
    void storeAndFindItemStock() {
        Partner supplierPartner = getSupplierPartner();
        ItemStock itemStock = getItemStock(supplierPartner);
        final var is = itemStock;
        when(partnerService.findByBpnl(supplierPartner.getBpnl())).thenAnswer(x -> supplierPartner);
        when(itemStockRepository.save(Mockito.any(ItemStock.class))).thenAnswer(i -> i.getArguments()[0]);
        when(itemStockRepository.findById(is.getKey())).thenAnswer(x -> Optional.of(is));
        when(materialPartnerRelationService.find(semiconductorMatNbrCustomer, supplierPartner.getUuid())).thenAnswer(x -> getMaterialPartnerRelation());

        itemStock = itemStockService.create(itemStock);
        var foundItemStock = itemStockService.findById(itemStock.getKey());
        Assertions.assertEquals(itemStock, foundItemStock);
    }

    private ItemStock getItemStock(Partner supplierPartner) {
        ItemStock.Builder builder = ItemStock.Builder.newInstance();
        var itemStock = builder
            .customerOrderId("123")
            .supplierOrderId("234")
            .customerOrderPositionId("1")
            .direction(DirectionCharacteristic.INBOUND)
            .materialNumberCustomer(semiconductorMatNbrCustomer)
            .materialNumberSupplier(semiconductorMatNbrSupplier)
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .locationBpns(supplierPartner.getSites().first().getBpns())
            .locationBpna(supplierPartner.getSites().first().getAddresses().first().getBpna())
            .partnerBpnl(supplierPartner.getBpnl())
            .quantity(5)
            .build();
        return itemStock;
    }

    private Partner getSupplierPartner() {
        Partner supplierPartnerEntity = new Partner(
            "Scenario Supplier",
            "http://supplier-control-plane:9184/api/v1/dsp",
            "BPNL1234567890ZZ",
            "BPNS1234567890ZZ",
            "Konzernzentrale Dudelsdorf",
            "BPNA1234567890AA",
            "Heinrich-Supplier-Straße 1",
            "77785 Dudelsdorf",
            "Germany"
        );
        supplierPartnerEntity.setUuid(UUID.randomUUID());
        return supplierPartnerEntity;
    }

    private MaterialPartnerRelation getMaterialPartnerRelation() {
        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setPartnerMaterialNumber(semiconductorMatNbrSupplier);
        mpr.setPartnerSuppliesMaterial(true);
        mpr.setMaterial(getMaterial());
        return mpr;
    }

    private Material getMaterial() {
        Material material = new Material();
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        material.setMaterialFlag(true);
        material.setName("Semiconductor");
        return material;
    }
}
