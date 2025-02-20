/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.*;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.demandandcapacitynotficationsamm.DemandAndCapacityNotificationSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapperTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class DemandAndCapacityNotificationSammMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(ItemStockSammMapperTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    final static String CUSTOMER_MAT_NUMBER = "MNR-7307-AU340474.002";
    final static String SUPPLIER_MAT_NUMBER = "MNR-8101-ID146955.001";
    final static String CX_MAT_NUMBER = "caf4a1df-6b97-46c9-8492-d137e911e611";
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";

    final static String DUMMY_MATERIAL_SUPPLIER_MNR = "supplier-mnr-123";
    final static String DUMMY_MATERIAL_CUSTOMER_MNR = "customer-mnr-456";
    final static String DUMMY_MATERIAL_CX = "dbf4a1df-6b97-46c9-8492-d137e911e722";

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

    final static DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    static Date dateFromString(String str) {
        LocalDateTime ldt = LocalDateTime.from(DTF.parse(str));
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
        return new Date(Instant.from(zdt).toEpochMilli());
    }

    @Mock
    private MaterialPartnerRelationService mprService;

    @Mock
    private MaterialService materialService;

    @Mock
    private PartnerService partnerService;

    @InjectMocks
    private DemandAndCapacityNotificationSammMapper demandAndCapacityNotificationSammMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    public void testSammCreationAsSupplier() throws Exception {
        // create Samm from Entity as a supplier
        Partner mySelf = supplierPartner;
        Partner externalPartner = customerPartner;
        Material semiconductorMaterial = new Material(false,
            true, SUPPLIER_MAT_NUMBER, CX_MAT_NUMBER, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            externalPartner,
            CUSTOMER_MAT_NUMBER,
            false,
            true);

        OwnDemandAndCapacityNotification notification = OwnDemandAndCapacityNotification.builder()
            .notificationId(UUID.randomUUID())
            .relatedNotificationId(null)
            .sourceNotificationId(null)
            .text("We are in big trouble!")
            .materials(List.of(semiconductorMaterial))
            .partner(externalPartner)
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .affectedSitesRecipient(externalPartner.getSites().stream().toList())
            .affectedSitesSender(mySelf.getSites().stream().toList())
            .startDateOfEffect(dateFromString("17-08-2024 12:00:00"))
            .expectedEndDateOfEffect(dateFromString("24-08-2024 00:00:00"))
            .leadingRootCause(LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION)
            .status(StatusEnumeration.OPEN)
            .build();


        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);

        DemandAndCapacityNotificationSamm samm = demandAndCapacityNotificationSammMapper.ownNotificationToSamm(notification);
        var jsonString = objectMapper.writeValueAsString(samm);
        var jsonNode = objectMapper.readTree(jsonString);

        LOG.info(jsonNode::toPrettyString);

        Assertions.assertEquals(jsonNode.get("leadingRootCause").asText(), LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION.getValue());
        Assertions.assertEquals(jsonNode.get("effect").asText(), EffectEnumeration.CAPACITY_REDUCTION.getValue());
        Assertions.assertTrue(jsonNode.get("startDateOfEffect").asLong() < jsonNode.get("expectedEndDateOfEffect").asLong());
        Assertions.assertEquals(jsonNode.get("status").asText(), StatusEnumeration.OPEN.getValue());

        Assertions.assertTrue(jsonNode.get("materialGlobalAssetId").isArray());
        for (var field: jsonNode.get("materialGlobalAssetId")) {
            Assertions.assertEquals(CX_MAT_NUMBER, field.asText());
        }

        Assertions.assertTrue(jsonNode.get("materialNumberSupplier").isArray());
        for (var field: jsonNode.get("materialNumberSupplier")) {
            Assertions.assertEquals(SUPPLIER_MAT_NUMBER, field.asText());
        }

        Assertions.assertTrue(jsonNode.get("materialNumberCustomer").isArray());
        for (var field: jsonNode.get("materialNumberCustomer")) {
            Assertions.assertEquals(CUSTOMER_MAT_NUMBER, field.asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {regularSammFromSupplier, sammWithoutCXIdFromSupplier, sammWithoutCXIdAndSupplierMnrFromSupplier})
    void testSammDeSerializationAsCustomer(String receivedSammString) throws Exception {
        // parse a Samm, we received from supplier as a customer

        DemandAndCapacityNotificationSamm samm = objectMapper.readValue(receivedSammString, DemandAndCapacityNotificationSamm.class);
        Partner mySelf = customerPartner;
        Partner externalPartner = supplierPartner;

        Material semiconductorMaterial = new Material(true, false,
            CUSTOMER_MAT_NUMBER, null, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial, externalPartner,
            SUPPLIER_MAT_NUMBER, true, false);

        when(materialService.findByOwnMaterialNumber(CUSTOMER_MAT_NUMBER)).thenReturn(semiconductorMaterial);
        when(mprService.findByPartnerAndPartnerCXNumber(externalPartner, CX_MAT_NUMBER)).thenReturn(materialPartnerRelation);
        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);
        when(partnerService.getOwnPartnerEntity()).thenReturn(mySelf);

        ReportedDemandAndCapacityNotification reportedDemandAndCapacityNotification =
            demandAndCapacityNotificationSammMapper.sammToReportedDemandAndCapacityNotification(samm, externalPartner);
        LOG.info(reportedDemandAndCapacityNotification::toString);

        Assertions.assertEquals(1, reportedDemandAndCapacityNotification.getMaterials().size());
        Assertions.assertEquals(semiconductorMaterial, reportedDemandAndCapacityNotification.getMaterials().getFirst());
        Assertions.assertEquals(StatusEnumeration.OPEN, reportedDemandAndCapacityNotification.getStatus());
        Assertions.assertEquals(LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION, reportedDemandAndCapacityNotification.getLeadingRootCause());
        Assertions.assertEquals(EffectEnumeration.CAPACITY_REDUCTION, reportedDemandAndCapacityNotification.getEffect());
    }

    static final String regularSammFromSupplier = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"leadingRootCause\" : \"logistics-disruption\",\n" +
        "  \"effect\" : \"capacity-reduction\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [ \"" + CX_MAT_NUMBER + "\" ],\n" +
        "  \"startDateOfEffect\" : 1723888800000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724450400000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\" ],\n" +
        "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\" ],\n" +
        "  \"notificationId\" : \"5203284f-b510-437c-9545-b9128d4be4c4\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}\n";

    static final String sammWithoutCXIdFromSupplier = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"leadingRootCause\" : \"logistics-disruption\",\n" +
        "  \"effect\" : \"capacity-reduction\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [],\n" +
        "  \"startDateOfEffect\" : 1723888800000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724450400000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\" ],\n" +
        "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\" ],\n" +
        "  \"notificationId\" : \"5203284f-b510-437c-9545-b9128d4be4c4\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}\n";

    static final String sammWithoutCXIdAndSupplierMnrFromSupplier = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"leadingRootCause\" : \"logistics-disruption\",\n" +
        "  \"effect\" : \"capacity-reduction\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [],\n" +
        "  \"startDateOfEffect\" : 1723888800000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724450400000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [],\n" +
        "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\" ],\n" +
        "  \"notificationId\" : \"5203284f-b510-437c-9545-b9128d4be4c4\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}\n";


    @Test
    public void testSammCreationAsCustomer() throws Exception {
        // create Samm from Entity as a customer
        Partner mySelf = customerPartner;
        Partner externalPartner = supplierPartner;
        Material semiconductorMaterial = new Material(true,
            false, CUSTOMER_MAT_NUMBER, null, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            externalPartner,
            SUPPLIER_MAT_NUMBER,
            true,
            false);
        materialPartnerRelation.setPartnerCXNumber(CX_MAT_NUMBER);

        OwnDemandAndCapacityNotification notification = OwnDemandAndCapacityNotification.builder()
            .notificationId(UUID.randomUUID())
            .relatedNotificationId(null)
            .sourceNotificationId(null)
            .text("We are in big trouble!")
            .materials(List.of(semiconductorMaterial))
            .partner(externalPartner)
            .effect(EffectEnumeration.DEMAND_INCREASE)
            .affectedSitesRecipient(externalPartner.getSites().stream().toList())
            .affectedSitesSender(mySelf.getSites().stream().toList())
            .startDateOfEffect(dateFromString("25-08-2024 16:00:00"))
            .expectedEndDateOfEffect(dateFromString("28-08-2024 18:00:00"))
            .leadingRootCause(LeadingRootCauseEnumeration.PRODUCTION_INCIDENT)
            .status(StatusEnumeration.OPEN)
            .build();

        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);

        DemandAndCapacityNotificationSamm samm = demandAndCapacityNotificationSammMapper.ownNotificationToSamm(notification);
        var jsonString = objectMapper.writeValueAsString(samm);
        var jsonNode = objectMapper.readTree(jsonString);

        LOG.info(jsonNode::toPrettyString);

        Assertions.assertEquals(jsonNode.get("leadingRootCause").asText(), LeadingRootCauseEnumeration.PRODUCTION_INCIDENT.getValue());
        Assertions.assertEquals(jsonNode.get("effect").asText(), EffectEnumeration.DEMAND_INCREASE.getValue());
        Assertions.assertTrue(jsonNode.get("startDateOfEffect").asLong() < jsonNode.get("expectedEndDateOfEffect").asLong());
        Assertions.assertEquals(jsonNode.get("status").asText(), StatusEnumeration.OPEN.getValue());
        Assertions.assertTrue(jsonNode.get("affectedSitesSender").toString().contains(mySelf.getSites().getFirst().getBpns()));
        Assertions.assertTrue(jsonNode.get("affectedSitesRecipient").toString().contains(externalPartner.getSites().getFirst().getBpns()));

        Assertions.assertTrue(jsonNode.get("materialNumberSupplier").isArray());
        for (var field: jsonNode.get("materialNumberSupplier")) {
            Assertions.assertEquals(SUPPLIER_MAT_NUMBER, field.asText());
        }

        Assertions.assertTrue(jsonNode.get("materialNumberCustomer").isArray());
        for (var field: jsonNode.get("materialNumberCustomer")) {
            Assertions.assertEquals(CUSTOMER_MAT_NUMBER, field.asText());
        }

        Assertions.assertTrue(jsonNode.get("materialGlobalAssetId").isArray());
        for (var field: jsonNode.get("materialGlobalAssetId")) {
            Assertions.assertEquals(CX_MAT_NUMBER, field.asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {regularSammFromCustomer, sammWithoutCXIdFromCustomer, sammWithoutCXIdAndCustomerMnrFromCustomer})
    void testSammDeSerializationAsSupplier(String receivedSammString) throws Exception {
        // parse a Samm, we received from customer as a supplier

        DemandAndCapacityNotificationSamm samm = objectMapper.readValue(receivedSammString, DemandAndCapacityNotificationSamm.class);
        Partner mySelf = supplierPartner;
        Partner externalPartner = customerPartner;

        Material semiconductorMaterial = new Material(true, false,
            SUPPLIER_MAT_NUMBER, CX_MAT_NUMBER, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial, externalPartner,
            CUSTOMER_MAT_NUMBER, false, true);

        when(materialService.findByMaterialNumberCx(CX_MAT_NUMBER)).thenReturn(semiconductorMaterial);
        when(materialService.findByOwnMaterialNumber(SUPPLIER_MAT_NUMBER)).thenReturn(semiconductorMaterial);
        when(mprService.findAllByPartnerMaterialNumber(CUSTOMER_MAT_NUMBER)).thenReturn(List.of(semiconductorMaterial));
        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);
        when(partnerService.getOwnPartnerEntity()).thenReturn(mySelf);

        ReportedDemandAndCapacityNotification reportedDemandAndCapacityNotification =
            demandAndCapacityNotificationSammMapper.sammToReportedDemandAndCapacityNotification(samm, externalPartner);
        LOG.info(reportedDemandAndCapacityNotification::toString);

        Assertions.assertEquals(1, reportedDemandAndCapacityNotification.getMaterials().size());
        Assertions.assertEquals(semiconductorMaterial, reportedDemandAndCapacityNotification.getMaterials().getFirst());
        Assertions.assertEquals(StatusEnumeration.OPEN, reportedDemandAndCapacityNotification.getStatus());
        Assertions.assertEquals(LeadingRootCauseEnumeration.PRODUCTION_INCIDENT, reportedDemandAndCapacityNotification.getLeadingRootCause());
        Assertions.assertEquals(EffectEnumeration.DEMAND_INCREASE, reportedDemandAndCapacityNotification.getEffect());
    }

    static final String regularSammFromCustomer = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"leadingRootCause\" : \"production-incident\",\n" +
        "  \"effect\" : \"demand-increase\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [ \"" + CX_MAT_NUMBER + "\" ],\n" +
        "  \"startDateOfEffect\" : 1724594400000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724860800000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\" ],\n" +
        "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\" ],\n" +
        "  \"notificationId\" : \"6a544e5e-7e30-44eb-bd14-dfc431473898\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}";

    static final String sammWithoutCXIdFromCustomer = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"leadingRootCause\" : \"production-incident\",\n" +
        "  \"effect\" : \"demand-increase\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [],\n" +
        "  \"startDateOfEffect\" : 1724594400000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724860800000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\" ],\n" +
        "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\" ],\n" +
        "  \"notificationId\" : \"6a544e5e-7e30-44eb-bd14-dfc431473898\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}";

    static final String sammWithoutCXIdAndCustomerMnrFromCustomer = "{\n" +
        "  \"affectedSitesSender\" : [ \"BPNS4444444444XX\" ],\n" +
        "  \"affectedSitesRecipient\" : [ \"BPNS1111111111SI\" ],\n" +
        "  \"leadingRootCause\" : \"production-incident\",\n" +
        "  \"effect\" : \"demand-increase\",\n" +
        "  \"text\" : \"We are in big trouble!\",\n" +
        "  \"materialGlobalAssetId\" : [],\n" +
        "  \"startDateOfEffect\" : 1724594400000,\n" +
        "  \"expectedEndDateOfEffect\" : 1724860800000,\n" +
        "  \"status\" : \"open\",\n" +
        "  \"contentChangedAt\" : null,\n" +
        "  \"sourceNotificationId\" : null,\n" +
        "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\" ],\n" +
        "  \"materialNumberCustomer\" : [],\n" +
        "  \"notificationId\" : \"6a544e5e-7e30-44eb-bd14-dfc431473898\",\n" +
        "  \"relatedNotificationId\" : null\n" +
        "}";


    @Test
    public void testSammCreationWithTwoMaterialsAsSupplier() throws Exception {
        // create Samm from Entity as a supplier
        Partner mySelf = supplierPartner;
        Partner externalPartner = customerPartner;
        Material semiconductorMaterial = new Material(false,
            true, SUPPLIER_MAT_NUMBER, CX_MAT_NUMBER, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            externalPartner,
            CUSTOMER_MAT_NUMBER,
            false,
            true);

        Material dummyMaterial = new Material(false,
            true, DUMMY_MATERIAL_SUPPLIER_MNR, DUMMY_MATERIAL_CX, "Dummy Material", new Date());

        MaterialPartnerRelation dummyMpr = new MaterialPartnerRelation(dummyMaterial,
            externalPartner,
            DUMMY_MATERIAL_CUSTOMER_MNR,
            false,
            true);

        OwnDemandAndCapacityNotification notification = OwnDemandAndCapacityNotification.builder()
            .notificationId(UUID.randomUUID())
            .relatedNotificationId(null)
            .sourceNotificationId(null)
            .text("We are in big trouble!")
            .materials(List.of(semiconductorMaterial, dummyMaterial))
            .partner(externalPartner)
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .affectedSitesRecipient(externalPartner.getSites().stream().toList())
            .affectedSitesSender(mySelf.getSites().stream().toList())
            .startDateOfEffect(dateFromString("17-08-2024 12:00:00"))
            .expectedEndDateOfEffect(dateFromString("24-08-2024 00:00:00"))
            .leadingRootCause(LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION)
            .status(StatusEnumeration.OPEN)
            .build();


        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);
        when(mprService.find(dummyMaterial, externalPartner)).thenReturn(dummyMpr);

        DemandAndCapacityNotificationSamm samm = demandAndCapacityNotificationSammMapper.ownNotificationToSamm(notification);
        var jsonString = objectMapper.writeValueAsString(samm);
        var jsonNode = objectMapper.readTree(jsonString);

        LOG.info(jsonNode::toPrettyString);

        Assertions.assertEquals(jsonNode.get("leadingRootCause").asText(), LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION.getValue());
        Assertions.assertEquals(jsonNode.get("effect").asText(), EffectEnumeration.CAPACITY_REDUCTION.getValue());
        Assertions.assertTrue(jsonNode.get("startDateOfEffect").asLong() < jsonNode.get("expectedEndDateOfEffect").asLong());
        Assertions.assertEquals(jsonNode.get("status").asText(), StatusEnumeration.OPEN.getValue());
        Assertions.assertTrue(jsonNode.get("affectedSitesSender").toString().contains(mySelf.getSites().getFirst().getBpns()));
        Assertions.assertTrue(jsonNode.get("affectedSitesRecipient").toString().contains(externalPartner.getSites().getFirst().getBpns()));

        Assertions.assertTrue(jsonNode.get("materialGlobalAssetId").isArray());
        Assertions.assertTrue(jsonNode.get("materialGlobalAssetId").toString().contains(CX_MAT_NUMBER));
        Assertions.assertTrue(jsonNode.get("materialGlobalAssetId").toString().contains(DUMMY_MATERIAL_CX));

        Assertions.assertTrue(jsonNode.get("materialNumberSupplier").isArray());
        Assertions.assertTrue(jsonNode.get("materialNumberSupplier").toString().contains(SUPPLIER_MAT_NUMBER));
        Assertions.assertTrue(jsonNode.get("materialNumberSupplier").toString().contains(DUMMY_MATERIAL_SUPPLIER_MNR));

        Assertions.assertTrue(jsonNode.get("materialNumberCustomer").isArray());
        Assertions.assertTrue(jsonNode.get("materialNumberCustomer").toString().contains(CUSTOMER_MAT_NUMBER));
        Assertions.assertTrue(jsonNode.get("materialNumberCustomer").toString().contains(DUMMY_MATERIAL_CUSTOMER_MNR));
    }

    @Test
    void testSammDeSerializationWithTwoMaterialsAsCustomer() throws Exception {
        // parse a Samm, we received from supplier as a customer

        String receivedSammString = "{\n" +
            "  \"affectedSitesSender\" : [ \"BPNS1111111111SI\" ],\n" +
            "  \"affectedSitesRecipient\" : [ \"BPNS4444444444XX\" ],\n" +
            "  \"leadingRootCause\" : \"logistics-disruption\",\n" +
            "  \"effect\" : \"capacity-reduction\",\n" +
            "  \"text\" : \"We are in big trouble!\",\n" +
            "  \"materialGlobalAssetId\" : [ \"caf4a1df-6b97-46c9-8492-d137e911e611\", \"dbf4a1df-6b97-46c9-8492-d137e911e722\" ],\n" +
            "  \"startDateOfEffect\" : 1723888800000,\n" +
            "  \"expectedEndDateOfEffect\" : 1724450400000,\n" +
            "  \"status\" : \"open\",\n" +
            "  \"contentChangedAt\" : null,\n" +
            "  \"sourceNotificationId\" : null,\n" +
            "  \"materialNumberSupplier\" : [ \"MNR-8101-ID146955.001\", \"supplier-mnr-123\" ],\n" +
            "  \"materialNumberCustomer\" : [ \"MNR-7307-AU340474.002\", \"customer-mnr-456\" ],\n" +
            "  \"notificationId\" : \"32352c4c-cb1b-4b95-99a9-e61c80462f2c\",\n" +
            "  \"relatedNotificationId\" : null\n" +
            "}\n";

        DemandAndCapacityNotificationSamm samm = objectMapper.readValue(receivedSammString, DemandAndCapacityNotificationSamm.class);
        Partner mySelf = customerPartner;
        Partner externalPartner = supplierPartner;

        Material semiconductorMaterial = new Material(false, true,
            CUSTOMER_MAT_NUMBER, null, "Semiconductor", new Date());

        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial, externalPartner,
            SUPPLIER_MAT_NUMBER, true, false);

        Material dummyMaterial = new Material(true,
            false, DUMMY_MATERIAL_CUSTOMER_MNR, null, "Dummy Material", new Date());

        MaterialPartnerRelation dummyMpr = new MaterialPartnerRelation(dummyMaterial, externalPartner,
            DUMMY_MATERIAL_CUSTOMER_MNR, true, false);

        when(materialService.findByOwnMaterialNumber(CUSTOMER_MAT_NUMBER)).thenReturn(semiconductorMaterial);
        when(materialService.findByOwnMaterialNumber(DUMMY_MATERIAL_CUSTOMER_MNR)).thenReturn(dummyMaterial);
        when(mprService.findByPartnerAndPartnerCXNumber(externalPartner, CX_MAT_NUMBER)).thenReturn(materialPartnerRelation);
        when(mprService.findByPartnerAndPartnerCXNumber(externalPartner, DUMMY_MATERIAL_CX)).thenReturn(dummyMpr);
        when(mprService.find(semiconductorMaterial, externalPartner)).thenReturn(materialPartnerRelation);
        when(mprService.find(dummyMaterial, externalPartner)).thenReturn(dummyMpr);
        when(partnerService.getOwnPartnerEntity()).thenReturn(mySelf);


        ReportedDemandAndCapacityNotification reportedDemandAndCapacityNotification =
            demandAndCapacityNotificationSammMapper.sammToReportedDemandAndCapacityNotification(samm, externalPartner);
        LOG.info(reportedDemandAndCapacityNotification::toString);

        Assertions.assertEquals(2, reportedDemandAndCapacityNotification.getMaterials().size());
        Assertions.assertTrue(reportedDemandAndCapacityNotification.getMaterials().contains(semiconductorMaterial));
        Assertions.assertTrue(reportedDemandAndCapacityNotification.getMaterials().contains(dummyMaterial));

        Assertions.assertEquals(StatusEnumeration.OPEN, reportedDemandAndCapacityNotification.getStatus());
        Assertions.assertEquals(LeadingRootCauseEnumeration.LOGISTICS_DISRUPTION, reportedDemandAndCapacityNotification.getLeadingRootCause());
        Assertions.assertEquals(EffectEnumeration.CAPACITY_REDUCTION, reportedDemandAndCapacityNotification.getEffect());
    }

}
