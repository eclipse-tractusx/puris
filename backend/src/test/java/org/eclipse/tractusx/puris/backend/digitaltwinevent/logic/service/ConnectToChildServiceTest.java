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
package org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.service;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.ParentData;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UniqueIdPushConnectTochildPayload;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UsageAsPlannedItem;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation.ValidityPeriod;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.ExternalMaterialRelationRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.MissingNode;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
public class ConnectToChildServiceTest {
    private static final String OWN_BPNL = "BPNL4444444444XX";

    private static final String PARTNER_BPNL = "BPNL1234567890ZZ";

    private static final String CHILD_CATENA_X_ID = "urn:uuid:5d8e2f1a-6b3c-4d7e-8f9a-1b2c3d4e5f6a";

    private static final String PARENT_CATENA_X_ID = "urn:uuid:9a3f1b2c-7d4e-4f5a-8b6c-0e1d2f3a4b5c";

    private static final String CHILD_MATERIAL_NUMBER = "MNR-7307-AU340474.002";

    private static final String PARENT_MATERIAL_NUMBER = "MNR-4177-S";

    private static final Date VALID_FROM = Date.from(Instant.parse("2026-01-01T00:00:00Z"));

    private static final Date VALID_TO = Date.from(Instant.parse("2026-12-31T00:00:00Z"));

    private static final Date CREATED_ON = Date.from(Instant.parse("2026-10-01T08:00:00Z"));

    private static final Date LAST_MODIFIED_ON = Date.from(Instant.parse("2026-10-05T08:00:00Z"));

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private PartnerService partnerService;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialPartnerRelationService mprService;

    @Mock
    private ExternalMaterialRelationRepository externalMaterialRelationRepository;

    @Mock
    private EdcAdapterService edcAdapterService;

    @Mock
    private MaterialRelationService materialRelationService;

    @InjectMocks
    private ConnectToChildService connectToChildService;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    private final Partner partner = partner(PARTNER_BPNL);

    private final Material child = new Material(false, true, "MNR-8101-ID146955.001", CHILD_CATENA_X_ID, "Semiconductor", new Date());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(connectToChildService, "messageService", new IndustryCoreMessageService(partnerService, objectMapper));
    }

    @Test
    void newParentItem_handleConnectToChild_storesRelation() {
        givenChildSuppliedToPartner();
        when(externalMaterialRelationRepository.findByMaterialAndPartnerAndParentGlobalAssetId(child, partner, PARENT_CATENA_X_ID))
            .thenReturn(Optional.empty());

        connectToChildService.handleConnectToChild(partner, payload(item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_FROM, null))));

        ArgumentCaptor<ExternalMaterialRelation> stored = ArgumentCaptor.forClass(ExternalMaterialRelation.class);
        verify(externalMaterialRelationRepository).save(stored.capture());
        ExternalMaterialRelation relation = stored.getValue();
        assertSame(child, relation.getMaterial());
        assertSame(partner, relation.getPartner());
        assertEquals(PARENT_CATENA_X_ID, relation.getParentGlobalAssetId());
        assertEquals(2.0, relation.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, relation.getMeasurementUnit());
        assertEquals(VALID_FROM, relation.getValidityPeriod().getValidFrom());
        assertNull(relation.getValidityPeriod().getValidTo());
        assertEquals(CREATED_ON, relation.getCreatedOn());
        assertEquals(LAST_MODIFIED_ON, relation.getLastModifiedOn());
    }

    @Test
    void knownParentItem_handleConnectToChild_updatesRelation() {
        givenChildSuppliedToPartner();
        Date previousDate = Date.from(Instant.parse("2026-01-15T10:30:00Z"));
        ExternalMaterialRelation existingRelation = new ExternalMaterialRelation(UUID.randomUUID(), child, partner, PARENT_CATENA_X_ID,
            new ValidityPeriod(VALID_FROM, null), previousDate, previousDate, 1.0, ItemUnitEnumeration.UNIT_KILOGRAM);
        when(externalMaterialRelationRepository.findByMaterialAndPartnerAndParentGlobalAssetId(child, partner, PARENT_CATENA_X_ID))
            .thenReturn(Optional.of(existingRelation));

        connectToChildService.handleConnectToChild(partner, payload(item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_FROM, VALID_TO))));

        verify(externalMaterialRelationRepository, times(1)).save(any());
        verify(externalMaterialRelationRepository).save(existingRelation);
        assertEquals(previousDate, existingRelation.getCreatedOn());
        assertEquals(LAST_MODIFIED_ON, existingRelation.getLastModifiedOn());
        assertEquals(2.0, existingRelation.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, existingRelation.getMeasurementUnit());
        assertEquals(VALID_TO, existingRelation.getValidityPeriod().getValidTo());
    }

    @Test
    void unknownChildMaterial_handleConnectToChild_isRejected() {
        when(materialService.findByMaterialNumberCx(CHILD_CATENA_X_ID)).thenReturn(null);
        var payload = payload(item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_FROM, null)));

        var exception = assertThrows(NoSuchElementException.class, () -> connectToChildService.handleConnectToChild(partner, payload));

        assertEquals("Unknown child material " + CHILD_CATENA_X_ID, exception.getMessage());
        verify(externalMaterialRelationRepository, never()).save(any());
    }

    @Test
    void childMaterialNotSuppliedToSender_handleConnectToChild_isRejected() {
        when(materialService.findByMaterialNumberCx(CHILD_CATENA_X_ID)).thenReturn(child);
        when(mprService.partnerOrdersProduct(child, partner)).thenReturn(false);
        var payload = payload(item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_FROM, null)));

        var exception = assertThrows(NoSuchElementException.class, () -> connectToChildService.handleConnectToChild(partner, payload));

        assertEquals("Child material " + CHILD_CATENA_X_ID + " is not supplied to partner " + PARTNER_BPNL, exception.getMessage());
        verify(externalMaterialRelationRepository, never()).save(any());
    }

    @Test
    void parentOfOtherBusinessPartner_handleConnectToChild_isRejected() {
        givenChildSuppliedToPartner();
        var payload = payload(item(CHILD_CATENA_X_ID, parentData("BPNL0000000001AB", VALID_FROM, null)));

        var exception = assertThrows(IllegalArgumentException.class, () -> connectToChildService.handleConnectToChild(partner, payload));

        assertEquals("Partner of parent " + PARENT_CATENA_X_ID + " does not match the sender " + PARTNER_BPNL, exception.getMessage());
        verify(externalMaterialRelationRepository, never()).save(any());
    }

    @Test
    void validityPeriodEndingBeforeItStarts_handleConnectToChild_isRejected() {
        givenChildSuppliedToPartner();
        var payload = payload(item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_TO, VALID_FROM)));

        var exception = assertThrows(IllegalArgumentException.class, () -> connectToChildService.handleConnectToChild(partner, payload));

        assertEquals("validFrom of parent " + PARENT_CATENA_X_ID + " is after validTo", exception.getMessage());
        verify(externalMaterialRelationRepository, never()).save(any());
    }

    @Test
    void secondItemUnknown_handleConnectToChild_storesNothing() {
        String unknownCatenaXId = "urn:uuid:00000000-0000-4000-8000-000000000000";
        givenChildSuppliedToPartner();
        when(materialService.findByMaterialNumberCx(unknownCatenaXId)).thenReturn(null);
        var payload = payload(
            item(CHILD_CATENA_X_ID, parentData(PARTNER_BPNL, VALID_FROM, null)),
            item(unknownCatenaXId, parentData(PARTNER_BPNL, VALID_FROM, null)));

        assertThrows(NoSuchElementException.class, () -> connectToChildService.handleConnectToChild(partner, payload));

        verify(externalMaterialRelationRepository, never()).save(any());
    }

    @Test
    void supplierWithCatenaXId_sendConnectToChild_sendsMaterialRelation() throws Exception {
        MaterialRelation materialRelation = materialRelation(PARENT_MATERIAL_NUMBER);
        materialRelation.setValidFrom(VALID_FROM);
        when(mprService.findAllSupplierRelations(CHILD_MATERIAL_NUMBER)).thenReturn(List.of(supplierRelation(partner, CHILD_CATENA_X_ID)));
        when(partnerService.getOwnPartnerEntity()).thenReturn(partner(OWN_BPNL));
        when(edcAdapterService.doDigitalTwinEventPostRequest(eq(partner), any())).thenReturn(MissingNode.getInstance());

        connectToChildService.sendConnectToChild(materialRelation, product(PARENT_MATERIAL_NUMBER, PARENT_CATENA_X_ID));

        ArgumentCaptor<JsonNode> message = ArgumentCaptor.forClass(JsonNode.class);
        verify(edcAdapterService).doDigitalTwinEventPostRequest(eq(partner), message.capture());
        JsonNode header = message.getValue().get("header");
        assertEquals("IndustryCore-DigitalTwinEvent-ConnectToChild:3.0.0", header.get("context").asText());
        assertEquals(OWN_BPNL, header.get("senderBpn").asText());
        assertEquals(PARTNER_BPNL, header.get("receiverBpn").asText());
        assertDoesNotThrow(() -> Instant.parse(header.get("sentDateTime").asText()), "sentDateTime must be ISO 8601");

        UniqueIdPushConnectTochildPayload content = objectMapper.treeToValue(message.getValue().get("content"), UniqueIdPushConnectTochildPayload.class);
        assertTrue(VALIDATOR.validate(content).isEmpty());
        assertEquals(UniqueIdPushConnectTochildPayload.PART_TYPE, content.getDigitalTwinType());
        UsageAsPlannedItem item = content.getListOfItems().get(0);
        assertEquals(CHILD_CATENA_X_ID, item.getCatenaXId());
        ParentData parent = item.getParentItems().get(0);
        assertEquals(PARENT_CATENA_X_ID, parent.getCatenaXId());
        assertEquals(OWN_BPNL, parent.getBusinessPartner());
        assertEquals(5.0, parent.getQuantity().getValue());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, parent.getQuantity().getUnit());
        assertEquals(VALID_FROM, parent.getValidityPeriod().getValidFrom());
        assertNull(parent.getValidityPeriod().getValidTo());
        assertEquals(CREATED_ON, parent.getCreatedOn());
        assertEquals(LAST_MODIFIED_ON, parent.getLastModifiedOn());
        verify(mprService, never()).triggerPartTypeRetrievalTask(any());
    }

    @Test
    void sendingToOneSupplierFails_sendConnectToChild_stillInformsOtherSuppliers() {
        Partner otherSupplier = partner("BPNL0000000002CD");
        when(mprService.findAllSupplierRelations(CHILD_MATERIAL_NUMBER))
            .thenReturn(List.of(supplierRelation(partner, CHILD_CATENA_X_ID), supplierRelation(otherSupplier, CHILD_CATENA_X_ID)));
        when(partnerService.getOwnPartnerEntity()).thenReturn(partner(OWN_BPNL));
        when(edcAdapterService.doDigitalTwinEventPostRequest(eq(partner), any())).thenThrow(new IllegalStateException("EDC not reachable"));
        when(edcAdapterService.doDigitalTwinEventPostRequest(eq(otherSupplier), any())).thenReturn(MissingNode.getInstance());

        assertDoesNotThrow(() -> connectToChildService.sendConnectToChild(materialRelation(PARENT_MATERIAL_NUMBER),
            product(PARENT_MATERIAL_NUMBER, PARENT_CATENA_X_ID)));

        verify(edcAdapterService).doDigitalTwinEventPostRequest(eq(otherSupplier), any());
    }

    private void givenChildSuppliedToPartner() {
        when(materialService.findByMaterialNumberCx(CHILD_CATENA_X_ID)).thenReturn(child);
        when(mprService.partnerOrdersProduct(child, partner)).thenReturn(true);
    }

    private static Partner partner(String bpnl) {
        Partner partner = new Partner();
        partner.setUuid(UUID.randomUUID());
        partner.setBpnl(bpnl);
        return partner;
    }

    private static Material product(String ownMaterialNumber, String catenaXId) {
        return new Material(false, true, ownMaterialNumber, catenaXId, "Central Control Unit", new Date());
    }

    private static MaterialPartnerRelation supplierRelation(Partner supplier, String partnerCatenaXId) {
        Material material = new Material(true, false, CHILD_MATERIAL_NUMBER, UUID.randomUUID().toString(), "Semiconductor", new Date());
        MaterialPartnerRelation supplierRelation = new MaterialPartnerRelation(material, supplier, "MNR-8101-ID146955.001", true, false);
        supplierRelation.setPartnerCXNumber(partnerCatenaXId);
        return supplierRelation;
    }

    private static MaterialRelation materialRelation(String parentMaterialNumber) {
        return new MaterialRelation(UUID.randomUUID(), parentMaterialNumber, CHILD_MATERIAL_NUMBER, 5.0, ItemUnitEnumeration.UNIT_PIECE, null, null, CREATED_ON, LAST_MODIFIED_ON);
    }

    private static UniqueIdPushConnectTochildPayload payload(UsageAsPlannedItem... items) {
        return new UniqueIdPushConnectTochildPayload(UniqueIdPushConnectTochildPayload.PART_TYPE, null, List.of(items));
    }

    private static UsageAsPlannedItem item(String catenaXId, ParentData... parentItems) {
        return new UsageAsPlannedItem(catenaXId, List.of(parentItems));
    }

    private static ParentData parentData(String businessPartner, Date validFrom, Date validTo) {
        return new ParentData(CREATED_ON, new ItemQuantityEntity(2.0, ItemUnitEnumeration.UNIT_PIECE), LAST_MODIFIED_ON, new ValidityPeriod(validFrom, validTo), businessPartner, PARENT_CATENA_X_ID);
    }
}
