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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunEntryStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.BatchRunStatusEnum;
import org.eclipse.tractusx.puris.backend.batch.domain.model.InformationEnum;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRunEntry;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunEntryRepository;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunRepository;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchProcessService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.production.logic.service.ProductionRequestApiService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.DeliveryRequestApiService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.DemandRequestApiService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.DaysOfSupplyRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.eclipse.tractusx.puris.backend.common.domain.model.DirectionEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshResult;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.RefreshError;

@Service
@Slf4j
public class PartnerDataUpdateBatchProcessServiceImpl implements PartnerDataUpdateBatchProcessService {

    @Autowired
    private PartnerDataUpdateBatchRunRepository runRepository;
    @Autowired
    private PartnerDataUpdateBatchRunEntryRepository entryRepository;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private ItemStockRequestApiService itemStockService;
    @Autowired
    private ProductionRequestApiService productionService;
    @Autowired
    private DemandRequestApiService demandService;
    @Autowired
    private DeliveryRequestApiService deliveryService;
    @Autowired
    private PartnerDataUpdateBatchService batchService;
    @Autowired
    private DaysOfSupplyRequestApiService daysOfSupplyService;

    private volatile PartnerDataUpdateBatchRun currentRun;

    @Override
    @Async
    public void executeFullBatch() {
        PartnerDataUpdateBatchRun run = new PartnerDataUpdateBatchRun();
        run.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
        run.setStatus(BatchRunStatusEnum.IN_PROGRESS);
        run = runRepository.save(run);

        // remember current run for addEntry helper
        this.currentRun = run;

        boolean anyError = false;

        List<MaterialPartnerRelation> mprs = mprService.findAll();
        for (var mpr : mprs) {
            var material = mpr.getMaterial();
            var partner = mpr.getPartner();

            if (mpr.isPartnerSuppliesMaterial()) {
                // INBOUND -> supplier provides material (call material item stock, production, delivery)
                DirectionEnum dirEnum = DirectionEnum.INBOUND;
                // Item Stock (Material)
                try {
                    RefreshResult res = itemStockService.doItemStockSubmodelReportedMaterialItemStockRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.STOCK, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during item stock request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.STOCK, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }

                // Production
                try {
                    RefreshResult res = productionService.doReportedProductionRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.PRODUCTION, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during production request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.PRODUCTION, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }

                // Delivery
                try {
                    RefreshResult res = deliveryService.doReportedDeliveryRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.DELIVERY, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during delivery request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.DELIVERY, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }

                // Days of Supply
                try {
                    RefreshResult res = daysOfSupplyService.doReportedDaysOfSupplyRequest(partner, material, DirectionCharacteristic.OUTBOUND);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.DAYS_OF_SUPPLY, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during days of supply request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.DAYS_OF_SUPPLY, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }
            }

            if (mpr.isPartnerBuysMaterial()) {
                DirectionEnum dirEnum = DirectionEnum.OUTBOUND;
                // OUTBOUND -> partner buys product (call product item stock, demand, delivery)
                try {
                    RefreshResult res = itemStockService.doItemStockSubmodelReportedProductItemStockRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.STOCK, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during product item stock request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.STOCK, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }
                try {
                    RefreshResult res = demandService.doReportedDemandRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.DEMAND, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during demand request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.DEMAND, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }
                try {
                    RefreshResult res = deliveryService.doReportedDeliveryRequest(partner, material);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.DELIVERY, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during delivery request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.DELIVERY, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }

                // Days of Supply
                try {
                    RefreshResult res = daysOfSupplyService.doReportedDaysOfSupplyRequest(partner, material, DirectionCharacteristic.INBOUND);
                    String errMsg = extractErrorMessage(res);
                    boolean err = errMsg != null;
                    addEntry(material, partner, dirEnum, InformationEnum.DAYS_OF_SUPPLY, err ? BatchRunEntryStatusEnum.ERROR : BatchRunEntryStatusEnum.SUCCESS, errMsg);
                    anyError = anyError || err;
                } catch (Exception e) {
                    anyError = true;
                    log.error("Error during days of supply request for " + material.getOwnMaterialNumber(), e);
                    addEntry(material, partner, dirEnum, InformationEnum.DAYS_OF_SUPPLY, BatchRunEntryStatusEnum.ERROR, e.getMessage());
                }
            }
        }

        run.setEndTime(OffsetDateTime.now(ZoneOffset.UTC));
        run.setStatus(anyError ? BatchRunStatusEnum.COMPLETED_WITH_ERRORS : BatchRunStatusEnum.COMPLETED);
        runRepository.save(run);

        this.currentRun = null;
    }

    private void addEntry(Material material,
                          Partner partner,
                          DirectionEnum direction,
                          InformationEnum informationType,
                          BatchRunEntryStatusEnum status,
                          String error) {
        try {
            PartnerDataUpdateBatchRunEntry entry = new PartnerDataUpdateBatchRunEntry();
            entry.setBatchRun(this.currentRun);
            entry.setOwnMaterialNumber(material.getOwnMaterialNumber());
            entry.setPartnerBpnl(partner.getBpnl());
            entry.setPartnerName(partner.getName());
            entry.setDirection(direction);
            entry.setInformationType(informationType);
            entry.setStatus(status);
            entry.setErrorMessage(error);
            batchService.createEntryRequiresNew(entry);
        } catch (Exception e) {
            log.error("Failed to persist entry (addEntry) for material " + material.getOwnMaterialNumber(), e);
        }
    }

    private String extractErrorMessage(RefreshResult res) {
        if (res == null) return null;
        List<RefreshError> errors = res.getErrors();
        if (errors == null || errors.isEmpty()) return null;
        return errors.stream()
            .flatMap(err -> err.getErrors().stream())
            .collect(Collectors.joining(";"));
    }
}
