/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate;

import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.impl.PartnerDataUpdateBatchProcessServiceImpl;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunEntryStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.InformationEnum;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRunEntry;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunEntryRepository;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.production.logic.service.ProductionRequestApiService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.DeliveryRequestApiService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.DemandRequestApiService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.DaysOfSupplyRequestApiService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshError;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerDataUpdateBatchProcessServiceTest {

    private final String CUSTOMER_BPNL = "BPNL4444444444XX";
    private final String SUPPLIER_BPNL = "BPNL1234567890ZZ";
    private final String MATERIAL_OWN_MATERIAL_NUMBER = "MNR-7307-AU340474.002";

    @Mock
    PartnerDataUpdateBatchRunRepository runRepository;
    @Mock
    PartnerDataUpdateBatchRunEntryRepository entryRepository;
    @Mock
    MaterialPartnerRelationService mprService;
    @Mock
    ItemStockRequestApiService itemStockService;
    @Mock
    ProductionRequestApiService productionService;
    @Mock
    DemandRequestApiService demandService;
    @Mock
    DeliveryRequestApiService deliveryService;
    @Mock
    PartnerDataUpdateBatchService batchService;
    @Mock
    DaysOfSupplyRequestApiService daysOfSupplyService;

    @InjectMocks
    PartnerDataUpdateBatchProcessServiceImpl service;

    private Material material;
    private Partner customer;
    private Partner supplier;

    @BeforeEach
    void setup() {
        material = new Material();
        material.setOwnMaterialNumber(MATERIAL_OWN_MATERIAL_NUMBER);
        customer = new Partner();
        customer.setUuid(UUID.randomUUID());
        customer.setBpnl(CUSTOMER_BPNL);
        customer.setName("Partner 1");
        supplier = new Partner();
        supplier.setUuid(UUID.randomUUID());
        supplier.setBpnl(SUPPLIER_BPNL);
        supplier.setName("Partner 1");
    }

    @Test
    void executeFullBatch_callsItemStockAndDeliveryForBothRoles() {


        // given two relations: one supplier, one not
        MaterialPartnerRelation supplierMpr = new MaterialPartnerRelation(material, supplier, "supplier-part-id", true, false);
        MaterialPartnerRelation customerMpr = new MaterialPartnerRelation(material, customer, "customer-part-id", false, true);
        
        // Prepare answers
        RefreshResult successResult = mock(RefreshResult.class);
        successResult.setMessage("Success");
        successResult.setErrors(List.of());
        when(successResult.getErrors()).thenReturn(List.of());

        List<RefreshError> errors = List.of(new RefreshError(List.of("Data not found")));
        RefreshResult errorResult = mock(RefreshResult.class);
        errorResult.setMessage("Failure");
        errorResult.setErrors(errors);
        when(errorResult.getErrors()).thenReturn(errors);
        
        when(runRepository.save(any(PartnerDataUpdateBatchRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mprService.findAll()).thenReturn(List.of(supplierMpr, customerMpr));

        // partner is customer
        when(demandService.doReportedDemandRequest(any(), any())).thenReturn(successResult);
        when(itemStockService.doItemStockSubmodelReportedMaterialItemStockRequest(any(), any())).thenReturn(successResult);

        // partner is supplier
        when(productionService.doReportedProductionRequest(any(), any())).thenReturn(successResult);
        when(itemStockService.doItemStockSubmodelReportedProductItemStockRequest(any(), any())).thenReturn(errorResult);
        
        // both
        when(deliveryService.doReportedDeliveryRequest(any(), any())).thenReturn(successResult);
        when(daysOfSupplyService.doReportedDaysOfSupplyRequest(any(), any(), any())).thenReturn(successResult);

        // when
        service.executeFullBatch();

        // then
        verify(itemStockService, times(1)).doItemStockSubmodelReportedMaterialItemStockRequest(eq(supplier), eq(material));
        verify(itemStockService, times(1)).doItemStockSubmodelReportedProductItemStockRequest(eq(customer), eq(material));
        verify(daysOfSupplyService, times(1)).doReportedDaysOfSupplyRequest(eq(customer), eq(material), eq(DirectionCharacteristic.INBOUND));
        verify(daysOfSupplyService, times(1)).doReportedDaysOfSupplyRequest(eq(supplier), eq(material), eq(DirectionCharacteristic.OUTBOUND));
        verify(productionService, times(1)).doReportedProductionRequest(eq(supplier), eq(material));
        verify(demandService, times(1)).doReportedDemandRequest(eq(customer), eq(material));
        verify(deliveryService, times(1)).doReportedDeliveryRequest(eq(supplier), eq(material));
        verify(deliveryService, times(1)).doReportedDeliveryRequest(eq(customer), eq(material));
        
        verify(batchService, times(8)).createEntryRequiresNew(any());

    }

    @Test
    void executeFullBatch_setsRunStatusCompletedWithErrorsOnException() {
        // given a single supplier relation
        MaterialPartnerRelation supplierMpr = new MaterialPartnerRelation(material, supplier, "pm", true, false);
        when(mprService.findAll()).thenReturn(List.of(supplierMpr));


                // Prepare answers
        RefreshResult successResult = mock(RefreshResult.class);
        successResult.setMessage("Success");
        successResult.setErrors(List.of());
        when(successResult.getErrors()).thenReturn(List.of());

        List<RefreshError> errors = List.of(new RefreshError(List.of("Data not found")));
        RefreshResult errorResult = mock(RefreshResult.class);
        errorResult.setMessage("Failure");
        errorResult.setErrors(errors);
        when(errorResult.getErrors()).thenReturn(errors);

        when(itemStockService.doItemStockSubmodelReportedMaterialItemStockRequest(any(), any())).thenReturn(successResult);
        when(productionService.doReportedProductionRequest(any(), any())).thenThrow(new RuntimeException("boom"));
        when(deliveryService.doReportedDeliveryRequest(any(), any())).thenReturn(errorResult);
        when(daysOfSupplyService.doReportedDaysOfSupplyRequest(any(), any(), any())).thenReturn(successResult);
        when(batchService.createEntryRequiresNew(any())).thenReturn(null);

        // capture saved runs
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service.executeFullBatch();

        // then last saved run status should be COMPLETED_WITH_ERRORS
        ArgumentCaptor<PartnerDataUpdateBatchRun> captor = ArgumentCaptor.forClass(PartnerDataUpdateBatchRun.class);
        verify(runRepository, atLeastOnce()).save(captor.capture());
        PartnerDataUpdateBatchRun saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getStatus()).isEqualTo(BatchRunStatusEnum.COMPLETED_WITH_ERRORS);
        
        // check delivery error
        ArgumentCaptor<PartnerDataUpdateBatchRunEntry> entryCaptor = ArgumentCaptor.forClass(PartnerDataUpdateBatchRunEntry.class);

        // Expect three calls
        verify(batchService, atLeastOnce()).createEntryRequiresNew(entryCaptor.capture());
        List<PartnerDataUpdateBatchRunEntry> capturedEntries = entryCaptor.getAllValues();
        PartnerDataUpdateBatchRunEntry deliveryEntry = capturedEntries.stream()
            .filter(e -> e.getInformationType() == InformationEnum.DELIVERY)
            .findFirst()
            .orElseThrow();

        // Assertions for errorResult
        assertThat(deliveryEntry.getStatus()).isEqualTo(BatchRunEntryStatusEnum.ERROR);
        assertThat(deliveryEntry.getErrorMessage()).contains("Data not found");

        // Assert exception case (Production "boom")
        PartnerDataUpdateBatchRunEntry productionEntry = capturedEntries.stream()
            .filter(e -> e.getInformationType() == InformationEnum.PRODUCTION)
            .findFirst()
            .orElseThrow();

        assertThat(productionEntry.getStatus()).isEqualTo(BatchRunEntryStatusEnum.ERROR);
        assertThat(productionEntry.getErrorMessage()).contains("boom");
    }
}

