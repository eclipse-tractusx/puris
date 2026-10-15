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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.controller;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataExchangeRequestControllerTest {
    final static long DAY = 24 * 60 * 60 * 1000L;

    final static String SUPPLIER_BPNL = "BPNL1111111111LE";
    final static String CUSTOMER_BPNS = "BPNS4444444444XX";
    final static String MATERIAL_NUMBER = "MNR-7307-AU340474.002";

    final static Partner supplierPartner = new Partner(
        "Scenario Supplier",
        "http://supplier-control-plane:9184/api/v1/dsp",
        SUPPLIER_BPNL,
        "BPNS1111111111SI",
        "Konzernzentrale Dudelsdorf",
        "BPNA1111111111AD",
        "Heinrich-Supplier-Straße 1",
        "77785 Dudelsdorf",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );

    final static Partner customerPartner = new Partner(
        "Scenario Customer",
        "http://customer-control-plane:8184/api/v1/dsp",
        "BPNL4444444444XX",
        CUSTOMER_BPNS,
        "Hauptwerk Musterhausen",
        "BPNA4444444444ZZ",
        "Musterstraße 35b",
        "77777 Musterhausen",
        "Germany",
        PolicyProfileVersionEnumeration.POLICY_PROFILE_2509
    );

    @Mock
    private OwnDataExchangeRequestRepository repository;
    @Mock
    private PartnerService partnerService;
    @Mock
    private MaterialPartnerRelationService mprService;
    @InjectMocks
    private OwnDataExchangeRequestService ownDataExchangeRequestService;

    private Material material;
    private MaterialPartnerRelation materialRelatedToSupplier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        material = mock(Material.class);
        when(material.getOwnMaterialNumber()).thenReturn(MATERIAL_NUMBER);
        materialRelatedToSupplier = mock(MaterialPartnerRelation.class);
    }

    @Test
    void emptyRequest_testValidate_returnsFalse() {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        boolean validation = ownDataExchangeRequestService.validate(request);
        assertEquals(false, validation);
    }

    @Test
    void emptyRequest_testValidateWithDetails_reportsMissingDisruptionFields() {
        List<String> errors = ownDataExchangeRequestService.validateWithDetails(new OwnDataExchangeRequest());
        assertTrue(errors.containsAll(List.of(
            "Missing partner.",
            "Missing sourceDisruptionId.",
            "Missing effect.",
            "At least one affected material is required.")));
    }

    @Test
    void emptyRequest_testCreate_throwsIllegalArgumentException() {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        assertThrows(IllegalArgumentException.class,() -> ownDataExchangeRequestService.create(request));
    }

    @Test
    void testCreateValidRequest_returnsSavedEntity() {
        OwnDataExchangeRequest request = createValidRequest();
        when(mprService.find(material, supplierPartner)).thenReturn(materialRelatedToSupplier);
        when(repository.save(request)).thenReturn(request);
        OwnDataExchangeRequest result = ownDataExchangeRequestService.create(request);
        assertEquals(request, result);
        verify(repository).save(request);
    }

    @Test
    void testCreateValidRequestWithoutNotification_returnsSavedEntity() {
        OwnDataExchangeRequest request = createValidRequestWithoutNotification();
        when(mprService.find(material, supplierPartner)).thenReturn(materialRelatedToSupplier);
        when(repository.save(request)).thenReturn(request);
        OwnDataExchangeRequest result = ownDataExchangeRequestService.create(request);
        assertEquals(request, result);
        assertNotNull(result.getRequestId());
        verify(repository).save(request);
    }

    @Test
    void desiredWindowOutsideNotificationWindow_testValidate_returnsFalse() {
        OwnDataExchangeRequest request = createValidRequest();
        request.setDesiredEndDateTime(new Date(request.getNotification().getExpectedEndDateOfEffect().getTime() + DAY));
        when(mprService.find(material, supplierPartner)).thenReturn(materialRelatedToSupplier);
        List<String> errors = ownDataExchangeRequestService.validateWithDetails(request);
        assertEquals(List.of("desiredEndDateTime must not be after notification expectedEndDateOfEffect."), errors);
    }

    @Test
    void requestWithoutNotification_testValidate_skipsNotificationWindow() {
        OwnDataExchangeRequest request = createValidRequestWithoutNotification();
        request.setDesiredStartDateTime(new Date(System.currentTimeMillis() - 365 * DAY));
        request.setDesiredEndDateTime(new Date(System.currentTimeMillis() + 365 * DAY));
        when(mprService.find(material, supplierPartner)).thenReturn(materialRelatedToSupplier);
        assertTrue(ownDataExchangeRequestService.validate(request));
    }

    @Test
    void materialNotRelatedToPartner_testValidate_returnsFalse() {
        OwnDataExchangeRequest request = createValidRequestWithoutNotification();
        when(mprService.find(material, supplierPartner)).thenReturn(null);
        List<String> errors = ownDataExchangeRequestService.validateWithDetails(request);
        assertEquals(List.of("Material " + MATERIAL_NUMBER + " is not related to partner " + SUPPLIER_BPNL + "."), errors);
    }
    private OwnDataExchangeRequest createValidRequest() {
        Date now = new Date();
        Date startOfEffect = new Date(now.getTime() + DAY);
        Date endOfEffect = new Date(now.getTime() + 10 * DAY);
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStartDateOfEffect(startOfEffect);
        notification.setExpectedEndDateOfEffect(endOfEffect);
        OwnDataExchangeRequest request = createValidRequestWithoutNotification();
        request.setNotification(notification);
        return request;
    }

   private OwnDataExchangeRequest createValidRequestWithoutNotification() {
        Date now = new Date();
        Date desiredStart = new Date(now.getTime() + 2 * DAY);
        Date desiredEnd = new Date(now.getTime() + 3 * DAY);
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setPartner(supplierPartner);
        request.setSourceDisruptionId(UUID.randomUUID());
        request.setEffect(EffectEnumeration.values()[0]);
        request.setMaterials(List.of(material));
        request.setCriticality(CriticalityEnumeration.values()[0]);
        request.setDesiredStartDateTime(desiredStart);
        request.setDesiredEndDateTime(desiredEnd);
        request.setRequestedTypes(List.of(RequestedTypeEnumeration.values()[0]));
        request.setText("Please provide the requested data.");
        return request;
    }
    
}
