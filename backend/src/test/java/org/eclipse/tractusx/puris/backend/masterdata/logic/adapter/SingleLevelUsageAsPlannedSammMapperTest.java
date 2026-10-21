/*
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated 
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Anthropic Claude (claude-opus-5-5)
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.ExternalMaterialRelationRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.ParentData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.SingleLevelUsageAsPlanned;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SingleLevelUsageAsPlannedSammMapperTest {
    private static final String PRODUCT_CATENA_X_ID = "urn:uuid:860fb504-b884-4009-9313-c6fb6cdc776b";

    private static final String PARENT_CATENA_X_ID = "urn:uuid:9a3f1b2c-7d4e-4f5a-8b6c-0e1d2f3a4b5c";

    private static final String OTHER_PARENT_CATENA_X_ID = "urn:uuid:5d8e2f1a-6b3c-4d7e-8f9a-1b2c3d4e5f6a";

    private static final Date CREATED_ON = Date.from(Instant.parse("2026-10-01T08:00:00Z"));

    private static final Date LAST_MODIFIED_ON = Date.from(Instant.parse("2026-10-05T08:00:00Z"));

    private static final Date VALID_FROM = Date.from(Instant.parse("2026-01-01T00:00:00Z"));

    private static final Date VALID_TO = Date.from(Instant.parse("2027-12-31T00:00:00Z"));

    @InjectMocks
    private SingleLevelUsageAsPlannedSammMapper mapper;

    @Mock
    private ExternalMaterialRelationRepository externalMaterialRelationRepository;

    @Mock
    private MaterialPartnerRelationService mprService;

    private Material product;

    private Partner customer;

    private Partner otherCustomer;

    @BeforeEach
    void setUp() {
        product = new Material();
        product.setOwnMaterialNumber("MNR-8101-ID146955.001");
        product.setMaterialNumberCx(PRODUCT_CATENA_X_ID);
        product.setProductFlag(true);
        customer = partner("BPNL4444444444XX");
        otherCustomer = partner("BPNL1234567890ZZ");
    }

    @Test
    void productToSingleLevelUsageAsPlannedSamm_relation_isMappedToParentItem() {
        when(externalMaterialRelationRepository.findAllByMaterial(product)).thenReturn(List.of(
            relation(customer, PARENT_CATENA_X_ID, new ExternalMaterialRelation.ValidityPeriod(VALID_FROM, VALID_TO))));
        when(mprService.findAllCustomersForOwnMaterialNumber(product.getOwnMaterialNumber())).thenReturn(List.of(customer));

        SingleLevelUsageAsPlanned samm = mapper.productToSingleLevelUsageAsPlannedSamm(product);

        assertEquals(PRODUCT_CATENA_X_ID, samm.getCatenaXId());
        assertEquals(List.of(customer.getBpnl()), samm.getCustomers());
        assertEquals(1, samm.getParentItems().size());
        ParentData parent = findParent(samm, PARENT_CATENA_X_ID);
        assertEquals(customer.getBpnl(), parent.getBusinessPartner());
        assertEquals(2.5, parent.getQuantity().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, parent.getQuantity().getUnit());
        assertEquals(CREATED_ON, parent.getCreatedOn());
        assertEquals(LAST_MODIFIED_ON, parent.getLastModifiedOn());
        assertEquals(VALID_FROM, parent.getValidityPeriod().getValidFrom());
        assertEquals(VALID_TO, parent.getValidityPeriod().getValidTo());
    }

    @Test
    void productToSingleLevelUsageAsPlannedSamm_relationsOfTwoCustomers_listsParentItemsOfBoth() {
        when(externalMaterialRelationRepository.findAllByMaterial(product)).thenReturn(List.of(
            relation(customer, PARENT_CATENA_X_ID, null),
            relation(otherCustomer, OTHER_PARENT_CATENA_X_ID, null)));
        when(mprService.findAllCustomersForOwnMaterialNumber(product.getOwnMaterialNumber()))
            .thenReturn(List.of(customer, otherCustomer));

        SingleLevelUsageAsPlanned samm = mapper.productToSingleLevelUsageAsPlannedSamm(product);

        assertEquals(List.of(customer.getBpnl(), otherCustomer.getBpnl()), samm.getCustomers());
        assertEquals(2, samm.getParentItems().size());
        assertEquals(customer.getBpnl(), findParent(samm, PARENT_CATENA_X_ID).getBusinessPartner());
        assertEquals(otherCustomer.getBpnl(), findParent(samm, OTHER_PARENT_CATENA_X_ID).getBusinessPartner());
    }

    /**
     * validityPeriod and lastModifiedOn are optional in the SAMM.
     */
    @Test
    void productToSingleLevelUsageAsPlannedSamm_optionalFieldsMissing_areOmitted() {
        ExternalMaterialRelation relation = relation(customer, PARENT_CATENA_X_ID, null);
        relation.setLastModifiedOn(null);
        when(externalMaterialRelationRepository.findAllByMaterial(product)).thenReturn(List.of(relation));
        when(mprService.findAllCustomersForOwnMaterialNumber(product.getOwnMaterialNumber())).thenReturn(List.of(customer));

        SingleLevelUsageAsPlanned samm = mapper.productToSingleLevelUsageAsPlannedSamm(product);

        ParentData parent = findParent(samm, PARENT_CATENA_X_ID);
        assertNull(parent.getValidityPeriod());
        assertNull(parent.getLastModifiedOn());
        assertEquals(CREATED_ON, parent.getCreatedOn());
    }

    /**
     * Customers that have not sent a ConnectToChild event yet are listed as customers, but without parent items.
     */
    @Test
    void productToSingleLevelUsageAsPlannedSamm_noRelations_listsCustomersWithoutParentItems() {
        when(externalMaterialRelationRepository.findAllByMaterial(product)).thenReturn(List.of());
        when(mprService.findAllCustomersForOwnMaterialNumber(product.getOwnMaterialNumber())).thenReturn(List.of(customer));

        SingleLevelUsageAsPlanned samm = mapper.productToSingleLevelUsageAsPlannedSamm(product);

        assertEquals(PRODUCT_CATENA_X_ID, samm.getCatenaXId());
        assertTrue(samm.getParentItems().isEmpty());
        assertEquals(List.of(customer.getBpnl()), samm.getCustomers());
    }

    @Test
    void productToSingleLevelUsageAsPlannedSamm_nonProduct_returnsNull() {
        product.setProductFlag(false);

        SingleLevelUsageAsPlanned samm = mapper.productToSingleLevelUsageAsPlannedSamm(product);

        assertNull(samm);
        verifyNoInteractions(externalMaterialRelationRepository, mprService);
    }

    private ExternalMaterialRelation relation(Partner partner, String parentGlobalAssetId, ExternalMaterialRelation.ValidityPeriod validityPeriod) {
        ExternalMaterialRelation relation = new ExternalMaterialRelation();
        relation.setUuid(UUID.randomUUID());
        relation.setMaterial(product);
        relation.setPartner(partner);
        relation.setParentGlobalAssetId(parentGlobalAssetId);
        relation.setQuantity(2.5);
        relation.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        relation.setValidityPeriod(validityPeriod);
        relation.setCreatedOn(CREATED_ON);
        relation.setLastModifiedOn(LAST_MODIFIED_ON);
        return relation;
    }

    private static ParentData findParent(SingleLevelUsageAsPlanned samm, String catenaXId) {
        return samm.getParentItems().stream()
            .filter(parent -> catenaXId.equals(parent.getCatenaXId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No parent item " + catenaXId));
    }

    private static Partner partner(String bpnl) {
        Partner partner = new Partner();
        partner.setUuid(UUID.randomUUID());
        partner.setBpnl(bpnl);
        return partner;
    }
}
