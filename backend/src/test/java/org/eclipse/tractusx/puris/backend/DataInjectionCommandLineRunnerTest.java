/*
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedMaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedProductItemStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.mockito.Mockito.when;

public class DataInjectionCommandLineRunnerTest {

    @Mock
    private MaterialService materialService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private MaterialPartnerRelationService mprService;

    @Mock
    private MaterialItemStockService materialItemStockService;

    @Mock
    private ProductItemStockService productItemStockService;

    @Mock
    private ReportedMaterialItemStockService reportedMaterialItemStockService;

    @Mock
    private ReportedProductItemStockService reportedProductItemStockService;

    @Mock
    private VariablesService variablesService;

    @InjectMocks
    private ObjectMapper objectMapper;

    @InjectMocks
    DataInjectionCommandLineRunner dataInjectionCommandLineRunner;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        // make variablesService mockable
        Field field = DataInjectionCommandLineRunner.class.getDeclaredField("variablesService");
        field.setAccessible(true);
        field.set(dataInjectionCommandLineRunner, variablesService);

        // make partnerService mockable
        Field partnerField = DataInjectionCommandLineRunner.class.getDeclaredField("partnerService");
        partnerField.setAccessible(true);
        partnerField.set(dataInjectionCommandLineRunner, partnerService);
    }

    @Test
    void whenNoOwnPartnerGiven_testCreateOwnPartnerEntity_thenCreate() {

        // Set up test values
        when(variablesService.getOwnBpnl()).thenReturn("BPNL1234567890ZZ");
        when(partnerService.findByBpnl("BPNL1234567890ZZ")).thenReturn(null);
        when(variablesService.getOwnDefaultBpns()).thenReturn(null);
        when(variablesService.getOwnName()).thenReturn("Test Partner");
        when(variablesService.getEdcProtocolUrl()).thenReturn("http://test-url.de");
        when(variablesService.getOwnDefaultBpna()).thenReturn("BPNA1234567890AA");
        when(variablesService.getOwnDefaultStreetAndNumber()).thenReturn("Test Street 1");
        when(variablesService.getOwnDefaultZipCodeAndCity()).thenReturn("12345 Testcity");
        when(variablesService.getOwnDefaultCountry()).thenReturn("Testland");

        Partner createdPartner = new Partner(
            "Existing Own Partner",
            "http://test-url.de",
            "BPNL1234567890ZZ",
            "BPNA1234567890AA",
            "Test Street 1",
            "12345 Testcity",
            "Testland"
        );

        when(partnerService.create(Mockito.any(Partner.class))).thenReturn(createdPartner);

        // Call method
        dataInjectionCommandLineRunner.createOwnPartnerEntity();

        // Verify create was called
        Mockito.verify(partnerService).create(Mockito.any(Partner.class));
    }

    @Test
    void whenOwnPartnerGiven_testCreateOwnPartnerEntity_thenDontCreate() {

        Partner partnerToReturn = new Partner(
            "Existing Own Partner",
            "http://test-url.de",
            "BPNL1234567890ZZ",
            "BPNA1234567890AA",
            "Test Street 1",
            "12345 Testcity",
            "Testland"
        );
        // Set up test values
        when(variablesService.getOwnBpnl()).thenReturn("BPNL1234567890ZZ");
        when(partnerService.findByBpnl("BPNL1234567890ZZ")).thenReturn(partnerToReturn);

        // Call method
        dataInjectionCommandLineRunner.createOwnPartnerEntity();

        // Verify create was called
        Mockito.verify(partnerService, Mockito.never()).create(Mockito.any(Partner.class));
    }
}
