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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.adapter;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.CriticalityEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.dataexchangerequestsamm.DataExchangeRequestSamm;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;

class DataExchangeRequestSammMapperTest {

    @Mock
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;

    @InjectMocks
    private DataExchangeRequestSammMapper dataExchangeRequestSammMapper;

    @Test
    void testOwnDataExchangeRequestToSamm() {
        UUID sourceDisruptionId = UUID.randomUUID();
        UUID relatedRequestUuid = UUID.randomUUID();
        Date desiredStart = new Date(1710000000000L);
        Date desiredEnd = new Date(1710086400000L);
        Date timestamp = new Date(1710172800000L);

        ReportedDemandAndCapacityNotification notification = ReportedDemandAndCapacityNotification.builder().sourceDisruptionId(sourceDisruptionId).build();

        ReportedDataExchangeRequest relatedRequest = ReportedDataExchangeRequest.builder().uuid(relatedRequestUuid).build();

        OwnDataExchangeRequest request = OwnDataExchangeRequest.builder()
                .notification(notification)
                .criticality(CriticalityEnumeration.HIGH)
                .desiredStartDateTime(desiredStart)
                .desiredEndDateTime(desiredEnd)
                .requestedTypes(List.of(
                        RequestedTypeEnumeration.N_TIER
                ))
                .text("Please share data")
                .timestamp(timestamp)
                .relatedDataExchangeRequest(relatedRequest)
                .build();

        DataExchangeRequestSamm samm = dataExchangeRequestSammMapper.ownDataExchangeRequestToSamm(request);

        Assertions.assertEquals(sourceDisruptionId.toString(), samm.getSourceDisruptionId());
        Assertions.assertEquals(CriticalityEnumeration.HIGH, samm.getCriticality());
        Assertions.assertEquals(desiredStart, samm.getDesiredStartDateTime());
        Assertions.assertEquals(desiredEnd, samm.getDesiredEndDateTime());
        Assertions.assertEquals(List.of(RequestedTypeEnumeration.N_TIER), samm.getRequestedTypes());
        Assertions.assertEquals("Please share data", samm.getText());
        Assertions.assertEquals(timestamp, samm.getTimestamp());
    }

    @Test
    void testOwnDataExchangeRequestToSammWithoutRelatedRequest() {
        UUID sourceDisruptionId = UUID.randomUUID();
        Date desiredStart = new Date(1710000000000L);
        Date desiredEnd = new Date(1710086400000L);
        Date timestamp = new Date(1710172800000L);

        ReportedDemandAndCapacityNotification notification = ReportedDemandAndCapacityNotification.builder().sourceDisruptionId(sourceDisruptionId).build();

        OwnDataExchangeRequest request = OwnDataExchangeRequest.builder()
                .notification(notification)
                .criticality(CriticalityEnumeration.MEDIUM)
                .desiredStartDateTime(desiredStart)
                .desiredEndDateTime(desiredEnd)
                .requestedTypes(List.of(RequestedTypeEnumeration.N_TIER))
                .text("Request without relation")
                .timestamp(timestamp)
                .relatedDataExchangeRequest(null)
                .build();

        DataExchangeRequestSamm samm = dataExchangeRequestSammMapper.ownDataExchangeRequestToSamm(request);

        Assertions.assertEquals(sourceDisruptionId.toString(), samm.getSourceDisruptionId());
    }

    @Test
    void testSammToReportedDataExchangeRequest() {
        UUID sourceDisruptionId = UUID.randomUUID();
        Date desiredStart = new Date(1710000000000L);
        Date desiredEnd = new Date(1710086400000L);
        Date timestamp = new Date(1710172800000L);
        String bpnl = "BPNL123";

        DataExchangeRequestSamm samm = DataExchangeRequestSamm.builder()
                .sourceDisruptionId(sourceDisruptionId)
                .criticality(CriticalityEnumeration.LOW)
                .desiredStartDateTime(desiredStart)
                .desiredEndDateTime(desiredEnd)
                .requestedTypes(List.of(RequestedTypeEnumeration.N_TIER))
                .text("Incoming SAMM request")
                .timestamp(timestamp)
                .build();

        ReportedDemandAndCapacityNotification notification = ReportedDemandAndCapacityNotification.builder().sourceDisruptionId(sourceDisruptionId).build();

        when(reportedDemandAndCapacityNotificationService.findByBpnlAndSourceDisruptionId(bpnl, sourceDisruptionId)).thenReturn(notification);

        ReportedDataExchangeRequest request = dataExchangeRequestSammMapper.sammToReportedDataExchangeRequest(bpnl, samm);

        Assertions.assertEquals(notification, request.getNotification());
        Assertions.assertEquals(CriticalityEnumeration.LOW, request.getCriticality());
        Assertions.assertEquals(desiredStart, request.getDesiredStartDateTime());
        Assertions.assertEquals(desiredEnd, request.getDesiredEndDateTime());
        Assertions.assertEquals(List.of(RequestedTypeEnumeration.N_TIER), request.getRequestedTypes());
        Assertions.assertEquals("Incoming SAMM request", request.getText());
        Assertions.assertEquals(timestamp, request.getTimestamp());
    }
}
