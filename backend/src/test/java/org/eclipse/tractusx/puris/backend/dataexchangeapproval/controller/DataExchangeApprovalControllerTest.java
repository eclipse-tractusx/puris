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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.OwnDataExchangeApprovalRepository;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.RequestedTypeEnumeration;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataExchangeApprovalControllerTest {
    @Mock
    private OwnDataExchangeApprovalRepository repository;
    @InjectMocks
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void emptyApproval_testValidate_returnsFalse() {
        OwnDataExchangeApproval approval = new OwnDataExchangeApproval();
        boolean validation = ownDataExchangeApprovalService.validate(approval);
        assertEquals(false, validation);
    }

    @Test
    void emptyApproval_testCreate_throwsIllegalArgumentException() {
        OwnDataExchangeApproval approval = new OwnDataExchangeApproval();
        assertThrows(IllegalArgumentException.class,() -> ownDataExchangeApprovalService.create(approval));
    }
    
    @Test
    void testCreateValidApproval_returnsSavedEntity() {
        OwnDataExchangeApproval approval = createValidApproval();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(approval)).thenReturn(approval);
        OwnDataExchangeApproval result = ownDataExchangeApprovalService.create(approval);
        assertEquals(approval, result);
        verify(repository).save(approval);
    }

    private OwnDataExchangeApproval createValidApproval() {
        ReportedDataExchangeRequest reportedRequest = new ReportedDataExchangeRequest();
        reportedRequest.setUuid(UUID.randomUUID());
        OwnDataExchangeApproval approval = new OwnDataExchangeApproval();
        approval.setDataExchangeRequest(reportedRequest);
        approval.setApprovedTypes(List.of(RequestedTypeEnumeration.values()[0]));
        approval.setIsFinalized(true);
        return approval;
    }
    
}
