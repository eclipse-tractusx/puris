/*
 * Copyright (c) 2025 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.file.logic.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.IncotermEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.logic.service.OwnProductionService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExcelService {

    private final List<String> demandColumns = List.of(
        "ownMaterialNumber",
        "partnerBpnl",
        "quantity",
        "unitOfMeasurement",
        "expectedSupplierSiteBpns",
        "demandSiteBpns",
        "demandCategoryCode",
        "day"
    );

    private final List<String> deliveryColumns = List.of(
        "ownMaterialNumber",
        "partnerBpnl",
        "quantity",
        "unitOfMeasurement",
        "originSiteBpns",
        "originAddressBpna",
        "destinationSiteBpns",
        "destinationAddressBpna",
        "departureType",
        "departureTime",
        "arrivalType",
        "arrivalTime",
        "trackingNumber",
        "Incoterm",
        "customerOrderNumber",
        "customerPositionId",
        "supplierOrderNumber"
    );

    private final List<String> productionColumns = List.of(
        "ownMaterialNumber",
        "partnerBpnl",
        "quantity",
        "unitOfMeasurement",
        "productionSiteBpns",
        "estimatedCompletionTime",
        "customerOrderNumber",
        "customerPositionId",
        "supplierOrderNumber"
    );

    private final List<String> stockColumns = List.of(
        "ownMaterialNumber",
        "partnerBpnl",
        "quantity",
        "unitOfMeasurement",
        "stockSiteBpns",
        "stockAddressBpna",
        "customerOrderNumber",
        "customerPositionId",
        "supplierOrderNumber",
        "isBlocked",
        "lastUpdatedOnDateTime",
        "direction"
    );

    @Autowired
    private MaterialService materialService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private OwnDemandService ownDemandService;
    @Autowired
    private OwnProductionService ownProductionService;
    @Autowired
    private OwnDeliveryService ownDeliveryService;
    @Autowired 
    private MaterialItemStockService materialItemStockService;
    @Autowired
    private ProductItemStockService productItemStockService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    
    public void readExcelFile(InputStream is) throws IOException {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);
        extractAndSaveData(sheet);
        workbook.close();
    }

    private void extractAndSaveData(Sheet sheet) {
        switch(validateHeaders(sheet)) {
            case "demand":
                extractAndSaveDemands(sheet);
                return;
            case "production":
                extractAndSaveProductions(sheet);
                return;
            case "delivery":
                extractAndSaveDeliveries(sheet);
                return;
            case "stock":
                extractAndSaveStocks(sheet);
                return;
            default:
                throw new Error("Invalid column structure");
        }
    }

    private void extractAndSaveDemands(Sheet sheet) {
        List<OwnDemand> demands = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowIndex++;
            try {
                String materialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String expectedSupplierSiteBpns = getStringCellValue(row.getCell(4));
                String demandSiteBpns = getStringCellValue(row.getCell(5));
                String demandCategoryCodeStr = getStringCellValue(row.getCell(6));
                Date day = row.getCell(7).getDateCellValue();

                ItemUnitEnumeration unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                DemandCategoryEnumeration categoryEnum = DemandCategoryEnumeration.fromValue(demandCategoryCodeStr.toUpperCase());

                Material material = materialService.findByOwnMaterialNumber(materialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");

                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");

                MaterialPartnerRelation mpr = mprService.find(partnerBpnl, materialNumber);
                if (!mpr.isPartnerBuysMaterial()) throw new IllegalArgumentException("Partner does not buy this material.");


                OwnDemand demand = OwnDemand.builder()
                        .material(material)
                        .partner(partner)
                        .quantity(quantity)
                        .measurementUnit(unitEnum)
                        .supplierLocationBpns(expectedSupplierSiteBpns)
                        .demandLocationBpns(demandSiteBpns)
                        .demandCategoryCode(categoryEnum)
                        .day(day)
                        .build();

                demands.add(demand);
            } catch (Exception e) {
                log.error("Failed to process demand row {}: {}", rowIndex, e.getMessage(), e);
            }
        }

        ownDemandService.findAll().forEach(d -> ownDemandService.delete(d.getUuid()));
        for (var newDemand : demands) {
            if (ownDemandService.create(newDemand) == null) {
                log.error("Failed to persist demand: {}", newDemand);
                throw new IllegalStateException("Persisting demands failed. See logs for details.");
            }
        }
    }

    private void extractAndSaveProductions(Sheet sheet) {
        List<OwnProduction> productions = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowIndex++;
            try {

                String materialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String productionSiteBpns = getStringCellValue(row.getCell(4));
                Date estimatedTimeOfCompletion = row.getCell(5).getDateCellValue();
                String customerOrderNumber = getStringCellValue(row.getCell(6));
                String customerPositionNumber = getStringCellValue(row.getCell(6));
                String supplierOrderNumber = getStringCellValue(row.getCell(6));
                
                ItemUnitEnumeration unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                
                Material material = materialService.findByOwnMaterialNumber(materialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");
                
                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");
                
                MaterialPartnerRelation mpr = mprService.find(partnerBpnl, materialNumber);
                if (!mpr.isPartnerBuysMaterial()) throw new IllegalArgumentException("Partner does not buy this material.");
                
                OwnProduction production = OwnProduction.builder()
                .material(material)
                .partner(partner)
                .quantity(quantity)
                .measurementUnit(unitEnum)
                .productionSiteBpns(productionSiteBpns)
                .estimatedTimeOfCompletion(estimatedTimeOfCompletion)
                .customerOrderNumber(customerOrderNumber)
                .customerOrderPositionNumber(customerPositionNumber)
                .supplierOrderNumber(supplierOrderNumber)
                .build();
                
                productions.add(production);
            } catch (Exception e) {
                log.error("Failed to process production row {}: {}", rowIndex, e.getMessage(), e);
            }
        }
        ownProductionService.findAll().forEach(d -> ownProductionService.delete(d.getUuid()));
        for (var newProduction : productions) {
            if (ownProductionService.create(newProduction) == null) {
                log.error("Failed to persist production: {}", newProduction);
                throw new IllegalStateException("Persisting productions failed. See logs for details.");
            }
        }
    }

    private void extractAndSaveDeliveries(Sheet sheet) {
        List<OwnDelivery> deliveries = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowIndex++;
            try {
                String materialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String originSiteBpns = getStringCellValue(row.getCell(4));
                String originAddressBpna = getStringCellValue(row.getCell(5));
                String destinationSiteBpns = getStringCellValue(row.getCell(6));
                String destinationAddressBpna = getStringCellValue(row.getCell(7));
                String departureType = getStringCellValue(row.getCell(8));
                Date departureTime = row.getCell(9).getDateCellValue();
                String arrivalType = getStringCellValue(row.getCell(10));
                Date arrivalTime = row.getCell(11).getDateCellValue();
                String trackingNumber = getStringCellValue(row.getCell(12));
                String incoterm = getStringCellValue(row.getCell(13));
                String customerOrderNumber = getStringCellValue(row.getCell(14));
                String customerPositionNumber = getStringCellValue(row.getCell(15));
                String supplierOrderNumber = getStringCellValue(row.getCell(16));
                
                ItemUnitEnumeration unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                IncotermEnumeration incotermEnum = IncotermEnumeration.valueOf(incoterm.toUpperCase());
                EventTypeEnumeration departureTypeEnum = EventTypeEnumeration.fromValue(departureType);
                EventTypeEnumeration arrivalTypeEnum = EventTypeEnumeration.fromValue(arrivalType);

                Material material = materialService.findByOwnMaterialNumber(materialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");
                
                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");
                
                MaterialPartnerRelation mpr = mprService.find(partnerBpnl, materialNumber);
                
                // Incoming delivery
                if (partner.getSites().stream().anyMatch(site -> site.getBpns().equals(originSiteBpns))) {
                    if (!mpr.isPartnerSuppliesMaterial()) throw new IllegalArgumentException("Partner does not supply this material.");
                // outgoing shipment
                } else {
                    if (!mpr.isPartnerBuysMaterial()) throw new IllegalArgumentException("Partner does not buy this material.");
                }

                OwnDelivery delivery = OwnDelivery.builder()
                        .material(material)
                        .partner(partner)
                        .quantity(quantity)
                        .measurementUnit(unitEnum)
                        .originBpns(originSiteBpns)
                        .originBpna(originAddressBpna)
                        .destinationBpns(destinationSiteBpns)
                        .destinationBpna(destinationAddressBpna)
                        .departureType(departureTypeEnum)
                        .dateOfDeparture(departureTime)
                        .arrivalType(arrivalTypeEnum)
                        .dateOfArrival(arrivalTime)
                        .trackingNumber(trackingNumber)
                        .incoterm(incotermEnum)
                        .customerOrderNumber(customerOrderNumber)
                        .customerOrderPositionNumber(customerPositionNumber)
                        .supplierOrderNumber(supplierOrderNumber)
                        .build();

                deliveries.add(delivery);
            } catch (Exception e) {
                log.error("Failed to process delivery row {}: {}", rowIndex, e.getMessage(), e);
            }
        }
        ownDeliveryService.findAll().forEach(d -> ownDeliveryService.delete(d.getUuid()));
        for (var newDelivery : deliveries) {
            if (ownDeliveryService.create(newDelivery) == null) {
                log.error("Failed to persist delivery: {}", newDelivery);
                throw new IllegalStateException("Persisting deliveries failed. See logs for details.");
            }
        }
    }

    private void extractAndSaveStocks(Sheet sheet) {
        List<MaterialItemStock> materialStocks = new ArrayList<>();
        List<ProductItemStock> productStocks = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowIndex++;
            try {
                String materialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String stockSiteBpns = getStringCellValue(row.getCell(4));
                String stockAddressBpna = getStringCellValue(row.getCell(5));
                String customerOrderNumber = getStringCellValue(row.getCell(6));
                String customerPositionNumber = getStringCellValue(row.getCell(7));
                String supplierOrderNumber = getStringCellValue(row.getCell(8));
                boolean isBlocked = row.getCell(9).getBooleanCellValue();
                Date lastUpdatedOnDateTime = row.getCell(10).getDateCellValue();
                String direction = getStringCellValue(row.getCell(11));

                ItemUnitEnumeration unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);

                Material material = materialService.findByOwnMaterialNumber(materialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");
                
                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");
                
                MaterialPartnerRelation mpr = mprService.find(partnerBpnl, materialNumber);

                if (direction.equals("inbound")) {
                    if (!mpr.isPartnerSuppliesMaterial()) throw new IllegalArgumentException("Partner does not supply this material.");
                    MaterialItemStock stock = MaterialItemStock.builder()
                        .material(material)
                        .partner(partner)
                        .quantity(quantity)
                        .measurementUnit(unitEnum)
                        .locationBpns(stockSiteBpns)
                        .locationBpna(stockAddressBpna)
                        .customerOrderId(customerOrderNumber)
                        .customerOrderPositionId(customerOrderNumber)
                        .supplierOrderId(supplierOrderNumber)
                        .isBlocked(isBlocked)
                        .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
                        .build();
                    materialStocks.add(stock);
                } else if (direction.equals("outbound")) {
                    if (!mpr.isPartnerBuysMaterial()) throw new IllegalArgumentException("Partner does not buy this material.");
                    ProductItemStock stock = ProductItemStock.builder()
                        .material(material)
                        .partner(partner)
                        .quantity(quantity)
                        .measurementUnit(unitEnum)
                        .locationBpns(stockSiteBpns)
                        .locationBpna(stockAddressBpna)
                        .customerOrderId(customerOrderNumber)
                        .customerOrderPositionId(customerPositionNumber)
                        .supplierOrderId(supplierOrderNumber)
                        .isBlocked(isBlocked)
                        .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
                        .build();
                    productStocks.add(stock);
                } else {
                    throw new Error("Invalid direction");
                }
            } catch (Exception e) {
                log.error("Failed to process stock row {}: {}", rowIndex, e.getMessage(), e);
            }

        }
        materialItemStockService.findAll().forEach(s -> materialItemStockService.delete(s.getUuid()));
        for (var newMaterialStock : materialStocks) {
            if (materialItemStockService.create(newMaterialStock) == null) {
                log.error("Failed to persist material stock: {}", newMaterialStock);
                throw new IllegalStateException("Persisting material stocks failed. See logs for details.");
            }
        }
        productItemStockService.findAll().forEach(s -> productItemStockService.delete(s.getUuid()));
        for (var newProductStock : productStocks) {
            if (productItemStockService.create(newProductStock) == null) {
                log.error("Failed to persist product stock: {}", newProductStock);
                throw new IllegalStateException("Persisting product stocks failed. See logs for details.");
            }
        }
    }

    private String validateHeaders(Sheet sheet) {
        var headerNames = extractHeader(sheet);
        if (headerNames.containsAll(demandColumns)) {
            return "demand";
        } else if (headerNames.containsAll(deliveryColumns)) {
            return "delivery";
        } else if (headerNames.containsAll(productionColumns)) {
            return "production";
        } else if (headerNames.containsAll(stockColumns)) {
            return "stock";
        } else {
            return null;
        }
    }

    private List<String> extractHeader(Sheet sheet) {
        Row headerRows = sheet.getRow(0);
        List<String> headerNames = new ArrayList<String>();
        headerRows.forEach(cell -> {
            headerNames.add(cell.getStringCellValue());
        });
        return headerNames;
    }

    private String getStringCellValue(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK ? null : cell.getCellType() == CellType.STRING
                ? cell.getStringCellValue().trim()
                : String.valueOf(cell.getNumericCellValue()).trim();
    }
}
