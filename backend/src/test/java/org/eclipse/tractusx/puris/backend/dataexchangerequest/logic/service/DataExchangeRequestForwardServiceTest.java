/*
Copyright (c) 2026 Volkswagen AG

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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.LeadingRootCauseEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
 
import javax.management.openmbean.KeyAlreadyExistsException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataExchangeRequestForwardServiceTest {
 
    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String SUPPLIER_BPNS = "BPNS1111111111SI";
    final static String SUPPLIER_BPNA = "BPNA1111111111AD";

    final static String OTHER_SUPPLIER_BPNL = "BPNL2222222222LE";
    final static String OTHER_SUPPLIER_BPNS = "BPNS2222222222SI";
    final static String OTHER_SUPPLIER_BPNA = "BPNA2222222222AD";

    final static String CUSTOMER_BPNL = "BPNL4444444444XX";
    final static String CUSTOMER_BPNS = "BPNS4444444444XX";
    final static String CUSTOMER_BPNA = "BPNA4444444444ZZ";

    final static UUID SOURCE_DISRUPTION_ID = UUID.randomUUID();

    final static Partner supplierPartner = new Partner(
        "Scenario Supplier",
        "http://supplier-control-plane:9184/api/v1/dsp",
        SUPPLIER_BPNL,
        SUPPLIER_BPNS,
        "Konzernzentrale Dudelsdorf",
        SUPPLIER_BPNA,
        "Heinrich-Supplier-Straße 1",
        "77785 Dudelsdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );

    final static Partner otherSupplierPartner = new Partner(
        "Scenario Second Supplier",
        "http://second-supplier-control-plane:9284/api/v1/dsp",
        OTHER_SUPPLIER_BPNL,
        OTHER_SUPPLIER_BPNS,
        "Zweigwerk Oberdorf",
        OTHER_SUPPLIER_BPNA,
        "Zweite-Supplier-Straße 7",
        "77786 Oberdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );

    final static Partner customerPartner = new Partner(
        "Scenario Customer",
        "http://customer-control-plane:8184/api/v1/dsp",
        CUSTOMER_BPNL,
        CUSTOMER_BPNS,
        "Hauptwerk Musterhausen",
        CUSTOMER_BPNA,
        "Musterstraße 35b",
        "77777 Musterhausen",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );

    final static DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    static Date dateFromString(String str) {
        LocalDateTime ldt = LocalDateTime.from(DTF.parse(str));
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
        return new Date(Instant.from(zdt).toEpochMilli());
    }

    @Mock
    private ReportedDemandAndCapacityNotificationService reportedNotificationService;

    @Mock
    private OwnDataExchangeRequestService ownDataExchangeRequestService;

    @Mock
    private MaterialRelationService materialRelationService;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialPartnerRelationService mprService;

    @InjectMocks
    private DataExchangeRequestForwardService forwardingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void returnsEmptyWhenRelatedNotificationIdsIsNull() {
        ReportedDataExchangeRequest origin = originRequest(null, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        List<DataExchangeRequestForwardService.ForwardTarget> targets = forwardingService.resolveForwardTargets(origin);

        Assertions.assertTrue(targets.isEmpty());
        verify(reportedNotificationService, never()).findByNotificationIdIn(anyList());
    }

    @Test
    void skipsNotificationBelongingToRequestingPartner() {
        UUID loopBack = UUID.randomUUID();
        UUID genuine = UUID.randomUUID();
        List<UUID> relatedIds = List.of(loopBack, genuine);

        ReportedDataExchangeRequest origin = originRequest(relatedIds, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification loopBackNotification = reportedNotification(loopBack, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification genuineNotification = reportedNotification(genuine, supplierPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));

        when(reportedNotificationService.findByNotificationIdIn(relatedIds)).thenReturn(List.of(loopBackNotification, genuineNotification));

        List<DataExchangeRequestForwardService.ForwardTarget> targets = forwardingService.resolveForwardTargets(origin);

        Assertions.assertEquals(1, targets.size());
        Assertions.assertEquals(genuine, targets.getFirst().notification().getNotificationId());
    }

    @Test
    void skipsNotificationWithoutAffectedMaterials() {
        UUID relatedId = UUID.randomUUID();
        List<UUID> relatedIds = List.of(relatedId);

        ReportedDataExchangeRequest origin = originRequest(relatedIds, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification withoutMaterials = reportedNotification(relatedId, supplierPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"), List.of());

        when(reportedNotificationService.findByNotificationIdIn(relatedIds)).thenReturn(List.of(withoutMaterials));

        Assertions.assertTrue(forwardingService.resolveForwardTargets(origin).isEmpty());
    }

    @Test
    void targetTakesPartnerAndMaterialsFromNotification() {
        UUID relatedId = UUID.randomUUID();
        List<UUID> relatedIds = List.of(relatedId);

        ReportedDataExchangeRequest origin = originRequest(relatedIds, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification target = reportedNotification(relatedId, supplierPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));

        when(reportedNotificationService.findByNotificationIdIn(relatedIds)).thenReturn(List.of(target));

        List<DataExchangeRequestForwardService.ForwardTarget> targets = forwardingService.resolveForwardTargets(origin);

        Assertions.assertEquals(1, targets.size());
        Assertions.assertSame(supplierPartner, targets.getFirst().partner());
        Assertions.assertSame(target, targets.getFirst().notification());
        Assertions.assertEquals(target.getMaterials(), targets.getFirst().materials());
    }

    @Test
    void datesDontFitInsideTargetNotificationWindow() {
        UUID relatedId = UUID.randomUUID();
        List<UUID> relatedIds = List.of(relatedId);

        ReportedDataExchangeRequest origin = originRequest(relatedIds, customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification target = reportedNotification(relatedId, supplierPartner, dateFromString("01-03-2026 00:00:00"), dateFromString("30-06-2026 00:00:00"));

        when(reportedNotificationService.findByNotificationIdIn(relatedIds)).thenReturn(List.of(target));

        List<DataExchangeRequestForwardService.ForwardTarget> targets = forwardingService.resolveForwardTargets(origin);

        Assertions.assertEquals(1, targets.size());
        Assertions.assertEquals(dateFromString("01-03-2026 00:00:00"), targets.getFirst().start());
        Assertions.assertEquals(dateFromString("30-06-2026 00:00:00"), targets.getFirst().end());
    }

    @Test
    void datesFitInsideTargetNotificationWindow() {
        UUID relatedId = UUID.randomUUID();
        List<UUID> relatedIds = List.of(relatedId);

        ReportedDataExchangeRequest origin = originRequest(relatedIds, customerPartner, dateFromString("01-04-2026 00:00:00"), dateFromString("01-05-2026 00:00:00"));
        ReportedDemandAndCapacityNotification target = reportedNotification(relatedId, supplierPartner, dateFromString("01-03-2026 00:00:00"), dateFromString("30-06-2026 00:00:00"));

        when(reportedNotificationService.findByNotificationIdIn(relatedIds)).thenReturn(List.of(target));

        List<DataExchangeRequestForwardService.ForwardTarget> targets = forwardingService.resolveForwardTargets(origin);

        Assertions.assertEquals(1, targets.size());
        Assertions.assertEquals(dateFromString("01-04-2026 00:00:00"), targets.getFirst().start());
        Assertions.assertEquals(dateFromString("01-05-2026 00:00:00"), targets.getFirst().end());
    }
    

    @Test
    void returnsEmptyWhenMaterialHasNoChildMaterials() {
        Material parent = material("MNR-PARENT");
        MaterialPartnerRelation customerBuysParent = relation(parent, customerPartner, false, true);
        ReportedDataExchangeRequest origin = requestWithoutNotification(customerPartner, List.of(parent), dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));

        when(mprService.find(same(parent), same(customerPartner))).thenReturn(customerBuysParent);
        when(mprService.findAll()).thenReturn(List.of(customerBuysParent));
        when(materialRelationService.resolveChildOwnMaterialNumbers(eq("MNR-PARENT"), any(Date.class))).thenReturn(Set.of());

        Assertions.assertTrue(forwardingService.resolveForwardTargets(origin).isEmpty());
        verify(materialService, never()).findByOwnMaterialNumber(anyString());
    }
    
    @Test
    void createsForwardedRequests() {
        ReportedDataExchangeRequest origin = originRequest(List.of(UUID.randomUUID()), customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        ReportedDemandAndCapacityNotification targetNotification = reportedNotification(UUID.randomUUID(), supplierPartner, dateFromString("01-03-2026 00:00:00"), dateFromString("30-06-2026 00:00:00"));

        DataExchangeRequestForwardService.ForwardTarget target = new DataExchangeRequestForwardService.ForwardTarget(
            supplierPartner, targetNotification, targetNotification.getMaterials(),
            dateFromString("01-03-2026 00:00:00"), dateFromString("30-06-2026 00:00:00"));

        when(ownDataExchangeRequestService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        forwardingService.createForwardedRequests(origin, List.of(target));

        ArgumentCaptor<OwnDataExchangeRequest> argumentCaptor = ArgumentCaptor.forClass(OwnDataExchangeRequest.class);
        verify(ownDataExchangeRequestService).create(argumentCaptor.capture());
        OwnDataExchangeRequest created = argumentCaptor.getValue();

        Assertions.assertSame(origin, created.getRelatedDataExchangeRequest());
        Assertions.assertSame(targetNotification, created.getNotification());
        Assertions.assertSame(supplierPartner, created.getPartner());
        Assertions.assertEquals(origin.getSourceDisruptionId(), created.getSourceDisruptionId());
        Assertions.assertEquals(targetNotification.getLeadingRootCause(), created.getLeadingRootCause());
        Assertions.assertEquals(targetNotification.getEffect(), created.getEffect());
        Assertions.assertEquals(targetNotification.getMaterials(), created.getMaterials());
        
        Assertions.assertEquals(targetNotification.getAffectedSitesRecipient(), created.getAffectedSitesSender());
        Assertions.assertEquals(targetNotification.getAffectedSitesSender(), created.getAffectedSitesRecipient());
        Assertions.assertEquals(origin.getCriticality(), created.getCriticality());
        Assertions.assertNotEquals(origin.getText(), created.getText());
        Assertions.assertEquals(origin.getRequestedTypes(), created.getRequestedTypes());
        Assertions.assertEquals(dateFromString("01-03-2026 00:00:00"), created.getDesiredStartDateTime());
        Assertions.assertEquals(dateFromString("30-06-2026 00:00:00"), created.getDesiredEndDateTime());
    }

    @Test
    void createsForwardedRequestsWithoutNotification() {
        Material child = material("MNR-CHILD-1");
        ReportedDataExchangeRequest origin = requestWithoutNotification(customerPartner, List.of(material("MNR-PARENT")), dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        DataExchangeRequestForwardService.ForwardTarget target = new DataExchangeRequestForwardService.ForwardTarget(supplierPartner, null, List.of(child), origin.getDesiredStartDateTime(), origin.getDesiredEndDateTime());

        when(ownDataExchangeRequestService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        forwardingService.createForwardedRequests(origin, List.of(target));

        ArgumentCaptor<OwnDataExchangeRequest> argumentCaptor = ArgumentCaptor.forClass(OwnDataExchangeRequest.class);
        verify(ownDataExchangeRequestService).create(argumentCaptor.capture());
        OwnDataExchangeRequest created = argumentCaptor.getValue();

        Assertions.assertNull(created.getNotification());
        Assertions.assertSame(origin, created.getRelatedDataExchangeRequest());
        Assertions.assertSame(supplierPartner, created.getPartner());
        Assertions.assertEquals(origin.getSourceDisruptionId(), created.getSourceDisruptionId());
        Assertions.assertEquals(origin.getLeadingRootCause(), created.getLeadingRootCause());
        Assertions.assertEquals(origin.getEffect(), created.getEffect());
        Assertions.assertEquals(List.of(child), created.getMaterials());
        Assertions.assertTrue(created.getAffectedSitesSender().isEmpty());
        Assertions.assertTrue(created.getAffectedSitesRecipient().isEmpty());
        Assertions.assertEquals(origin.getDesiredStartDateTime(), created.getDesiredStartDateTime());
        Assertions.assertEquals(origin.getDesiredEndDateTime(), created.getDesiredEndDateTime());
    }

    @Test
    void skipsFailedForwardedRequests() {
        ReportedDataExchangeRequest origin = originRequest(List.of(), customerPartner, dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        List<DataExchangeRequestForwardService.ForwardTarget> targets = List.of(forwardTarget(supplierPartner), forwardTarget(otherSupplierPartner), forwardTarget(supplierPartner));

        when(ownDataExchangeRequestService.create(any()))
            .thenAnswer(inv -> inv.getArgument(0))
            .thenThrow(new KeyAlreadyExistsException("Data exchange request already exists"))
            .thenAnswer(inv -> inv.getArgument(0));

        List<OwnDataExchangeRequest> created = forwardingService.createForwardedRequests(origin, targets);

        Assertions.assertEquals(2, created.size());
        verify(ownDataExchangeRequestService, times(3)).create(any());
    }

    static Material material(String ownMaterialNumber) {
        Material material = mock(Material.class);
        when(material.getOwnMaterialNumber()).thenReturn(ownMaterialNumber);
        return material;
    }

    static MaterialPartnerRelation relation(Material material, Partner partner, boolean partnerSuppliesMaterial, boolean partnerBuysMaterial) {
        MaterialPartnerRelation relation = mock(MaterialPartnerRelation.class);
        when(relation.getMaterial()).thenReturn(material);
        when(relation.getPartner()).thenReturn(partner);
        when(relation.isPartnerSuppliesMaterial()).thenReturn(partnerSuppliesMaterial);
        when(relation.isPartnerBuysMaterial()).thenReturn(partnerBuysMaterial);
        return relation;
    }

    static ReportedDemandAndCapacityNotification reportedNotification(UUID notificationId, Partner partner, Date startOfEffect, Date expectedEndOfEffect) {
        return reportedNotification(notificationId, partner, startOfEffect, expectedEndOfEffect, List.of(material("MNR-NOTIFICATION")));
    }

    static ReportedDemandAndCapacityNotification reportedNotification(UUID notificationId, Partner partner, Date startOfEffect, Date expectedEndOfEffect, List<Material> materials) {
        return ReportedDemandAndCapacityNotification.builder()
            .uuid(UUID.randomUUID())
            .notificationId(notificationId)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .partner(partner)
            .leadingRootCause(LeadingRootCauseEnumeration.STRIKE)
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .materials(new ArrayList<>(materials))
            .affectedSitesSender(new ArrayList<>(partner.getSites()))
            .affectedSitesRecipient(new ArrayList<>())
            .startDateOfEffect(startOfEffect)
            .expectedEndDateOfEffect(expectedEndOfEffect)
            .build();
    }

    static DataExchangeRequestForwardService.ForwardTarget forwardTarget(Partner partner) {
        ReportedDemandAndCapacityNotification notification = reportedNotification(UUID.randomUUID(), partner,
            dateFromString("01-01-2026 00:00:00"), dateFromString("31-12-2026 00:00:00"));
        return new DataExchangeRequestForwardService.ForwardTarget(
            partner,
            notification,
            notification.getMaterials(),
            dateFromString("01-01-2026 00:00:00"),
            dateFromString("31-12-2026 00:00:00"));
    }

   static ReportedDataExchangeRequest originRequest(List<UUID> relatedNotificationIds, Partner requester, Date desiredStart, Date desiredEnd) {
        OwnDemandAndCapacityNotification ownNotification = OwnDemandAndCapacityNotification.builder()
            .uuid(UUID.randomUUID())
            .notificationId(UUID.randomUUID())
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .relatedNotificationIds(relatedNotificationIds)
            .partner(requester)
            .startDateOfEffect(dateFromString("01-01-2026 00:00:00"))
            .expectedEndDateOfEffect(dateFromString("31-12-2026 00:00:00"))
            .build();

        return ReportedDataExchangeRequest.builder()
            .uuid(UUID.randomUUID())
            .requestId(UUID.randomUUID().toString())
            .partner(requester)
            .notification(ownNotification)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .leadingRootCause(LeadingRootCauseEnumeration.STRIKE)
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .materials(new ArrayList<>(List.of(material("MNR-ORIGIN"))))
            .criticality(CriticalityEnumeration.HIGH)
            .desiredStartDateTime(desiredStart)
            .desiredEndDateTime(desiredEnd)
            .requestedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .text("Please provide the requested data.")
            .build();
    }

    static ReportedDataExchangeRequest requestWithoutNotification(Partner requester, List<Material> materials, Date desiredStart, Date desiredEnd) {
        return ReportedDataExchangeRequest.builder()
            .uuid(UUID.randomUUID())
            .requestId(UUID.randomUUID().toString())
            .partner(requester)
            .sourceDisruptionId(SOURCE_DISRUPTION_ID)
            .leadingRootCause(LeadingRootCauseEnumeration.STRIKE)
            .effect(EffectEnumeration.CAPACITY_REDUCTION)
            .materials(new ArrayList<>(materials))
            .criticality(CriticalityEnumeration.HIGH)
            .desiredStartDateTime(desiredStart)
            .desiredEndDateTime(desiredEnd)
            .requestedTypes(new ArrayList<>(List.of(RequestedTypeEnumeration.N_TIER)))
            .text("Please provide the requested data.")
            .build();
    }
}
