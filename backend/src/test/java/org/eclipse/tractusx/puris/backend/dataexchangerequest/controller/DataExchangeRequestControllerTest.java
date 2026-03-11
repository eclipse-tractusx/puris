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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.repository.OwnDataExchangeRequestRepository;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataExchangeRequestControllerTest {
    @Mock
    private OwnDataExchangeRequestRepository repository;
    @InjectMocks
    private OwnDataExchangeRequestService ownDataExchangeRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void emptyRequest_testValidateWithDetails_returnsValidationErrors() {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        List<String> errors = ownDataExchangeRequestService.validateWithDetails(request);
        assertTrue(errors.contains("Missing criticality."));
        assertTrue(errors.contains("Missing text."));
        assertTrue(errors.contains("Missing timestamp."));
        assertTrue(errors.contains("Missing desired start date and time."));
        assertTrue(errors.contains("Missing desired end date and time."));
        assertTrue(errors.contains("Missing reference to demand and capacity notification."));
        assertTrue(errors.contains("Missing requested types."));
    }

    @Test
    void emptyRequest_testCreate_throwsIllegalArgumentException() {
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        assertThrows(IllegalArgumentException.class,() -> ownDataExchangeRequestService.create(request));
    }

    @Test
    void testCreateValidRequest_returnsSavedEntity() {
        OwnDataExchangeRequest request = createValidRequest();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(request)).thenReturn(request);
        OwnDataExchangeRequest result = ownDataExchangeRequestService.create(request);
        assertEquals(request, result);
        verify(repository).save(request);
    }

    private OwnDataExchangeRequest createValidRequest() {
        Date now = new Date();
        Date startOfEffect = new Date(now.getTime() + 24 * 60 * 60 * 1000L);
        Date endOfEffect = new Date(now.getTime() + 10 * 24 * 60 * 60 * 1000L);
        Date desiredStart = new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000L);
        Date desiredEnd = new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000L);
        ReportedDemandAndCapacityNotification notification = new ReportedDemandAndCapacityNotification();
        notification.setUuid(UUID.randomUUID());
        notification.setStartDateOfEffect(startOfEffect);
        notification.setExpectedEndDateOfEffect(endOfEffect);
        OwnDataExchangeRequest request = new OwnDataExchangeRequest();
        request.setNotification(notification);
        request.setCriticality(CriticalityEnumeration.values()[0]);
        request.setDesiredStartDateTime(desiredStart);
        request.setDesiredEndDateTime(desiredEnd);
        request.setRequestedTypes(List.of(RequestedTypeEnumeration.values()[0]));
        request.setText("Please provide the requested data.");
        request.setTimestamp(now);
        return request;
    }

}
