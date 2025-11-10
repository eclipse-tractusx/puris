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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.IncotermEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.file.domain.model.DataDocumentTypeEnumeration;
import org.eclipse.tractusx.puris.backend.file.domain.model.DataImportError;
import org.eclipse.tractusx.puris.backend.file.domain.model.DataImportResult;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.logic.service.OwnProductionService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
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
        "day",
        "lastUpdatedOnDateTime"
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
        "supplierOrderNumber",
        "lastUpdatedOnDateTime"
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
        "supplierOrderNumber",
        "lastUpdatedOnDateTime"
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
    
    public DataImportResult readExcelFile(InputStream is, String sourceName) throws IOException {
        Workbook workbook = WorkbookFactory.create(is);
        List<DataImportError> formulaErrors = evaluateWorkbook(workbook, sourceName);
        if (!formulaErrors.isEmpty()) {
            return new DataImportResult("Excel formula evaluation failed.", formulaErrors);
        }
        Sheet sheet = workbook.getSheetAt(0);
        var result = extractAndSaveData(sheet);
        workbook.close();
        return result;
    }

    private DataImportResult extractAndSaveData(Sheet sheet) {
        DataDocumentTypeEnumeration documentType = validateHeaders(sheet);
        if (documentType == null) {
            throw new IllegalArgumentException("Unsupported Excel file format: column structure does not match any supported data type (Demand, Production, Delivery, or Stock)");
        }
        
        switch(documentType) {
            case DataDocumentTypeEnumeration.DEMAND:
                return extractAndSaveDemands(sheet);
            case DataDocumentTypeEnumeration.PRODUCTION:
                return extractAndSaveProductions(sheet);
            case DataDocumentTypeEnumeration.DELIVERY:
                return extractAndSaveDeliveries(sheet);
            case DataDocumentTypeEnumeration.STOCK:
                return extractAndSaveStocks(sheet);
            default:
                throw new IllegalArgumentException("Unsupported Excel file format: column structure does not match any supported data type (Demand, Production, Delivery, or Stock)");
        }
    }

    private DataImportResult extractAndSaveDemands(Sheet sheet) {
        List<OwnDemand> demands = new ArrayList<>();
        List<DataImportError> errors = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            List<String> rowErrors = new ArrayList<>();
            Row row = rowIterator.next();
            rowIndex++;
            if (isRowEmpty(row)) {
                continue;
            }
            try {
                String ownMaterialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String expectedSupplierSiteBpns = getStringCellValue(row.getCell(4));
                String demandSiteBpns = getStringCellValue(row.getCell(5));
                String demandCategoryCodeStr = getStringCellValue(row.getCell(6));
                Date day = getDateCellValue(row.getCell(7));
                Date lastUpdatedOnDateTime = getDateCellValue(row.getCell(8));
                ItemUnitEnumeration unitEnum = null;
                try {
                    unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                } catch (Exception e) {
                    rowErrors.add("Invalid unit of measurement: " + unitOfMeasurement);
                }
                DemandCategoryEnumeration categoryEnum = null;
                try {
                    categoryEnum = DemandCategoryEnumeration.fromValue(demandCategoryCodeStr.toUpperCase());
                } catch (Exception e) {
                    rowErrors.add("Invalid demand category: " + demandCategoryCodeStr);
                }

                if (lastUpdatedOnDateTime == null) {
                    lastUpdatedOnDateTime = new Date();
                }

                Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");

                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");

                OwnDemand demand = OwnDemand.builder()
                        .material(material)
                        .partner(partner)
                        .quantity(quantity)
                        .measurementUnit(unitEnum)
                        .supplierLocationBpns(expectedSupplierSiteBpns)
                        .demandLocationBpns(demandSiteBpns)
                        .demandCategoryCode(categoryEnum)
                        .day(day)
                        .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
                        .build();
                rowErrors.addAll(ownDemandService.validateWithDetails(demand));
                if (!rowErrors.isEmpty()) {
                    errors.add(new DataImportError(rowIndex, rowErrors));
                }
                demands.add(demand);
            } catch (Exception e) {
                errors.add(new DataImportError(rowIndex, List.of(e.getMessage() + " Further validations for this row are not possible.")));
            }
        }

        if (errors.isEmpty()) {
            try {
                var conflictErrors = checkConflicts(demands);
                if (!conflictErrors.isEmpty()) {
                    return new DataImportResult("One or more conflicting rows found.", conflictErrors);
                }
            } catch(Exception e) {
                return new DataImportResult("Error while checking conflicts", List.of(new DataImportError(0, List.of(e.getMessage()))));
            }
            List<OwnDemand> addedDemands = new ArrayList<>();
            List<OwnDemand> existingDemands = ownDemandService.findAll();
            for (var newDemand : demands) {
                try {
                    var added = ownDemandService.create(newDemand);
                    if (added == null) {
                        log.error("Failed to persist demand: {}", newDemand);
                        throw new IllegalArgumentException("Invalid demand");
                    }
                    addedDemands.add(added);
                } catch (Exception e) {
                    int idx = demands.indexOf(newDemand);
                    errors.add(new DataImportError(idx + 2, List.of(e.getMessage())));
                    addedDemands.forEach(s -> ownDemandService.delete(s.getUuid()));
                    return new DataImportResult("Failed to persist demands", errors);
                }
            }
            existingDemands.forEach(d -> ownDemandService.delete(d.getUuid()));
            return new DataImportResult("Successfully imported demands", errors);
        } else {
            log.info(errors.toString());
        }

        return new DataImportResult("Failed to process Demand rows", errors);
    }

    private DataImportResult extractAndSaveProductions(Sheet sheet) {
        List<OwnProduction> productions = new ArrayList<>();
        List<DataImportError> errors = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            List<String> rowErrors = new ArrayList<>();
            Row row = rowIterator.next();
            rowIndex++;
            if (isRowEmpty(row)) {
                continue;
            }
            try {
                String ownMaterialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String productionSiteBpns = getStringCellValue(row.getCell(4));
                Date estimatedTimeOfCompletion = getDateCellValue(row.getCell(5));
                String customerOrderNumber = getStringCellValue(row.getCell(6));
                String customerPositionNumber = getStringCellValue(row.getCell(7));
                String supplierOrderNumber = getStringCellValue(row.getCell(8));
                Date lastUpdatedOnDateTime = getDateCellValue(row.getCell(9));

                ItemUnitEnumeration unitEnum = null;
                try {
                    unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                } catch (Exception e) {
                    rowErrors.add("Invalid unit of measurement: " + unitOfMeasurement);
                }

                if (lastUpdatedOnDateTime == null) {
                    lastUpdatedOnDateTime = new Date();
                }

                Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");

                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");

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
                    .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
                    .build();
                rowErrors.addAll(ownProductionService.validateWithDetails(production));
                if (!rowErrors.isEmpty()) {
                    errors.add(new DataImportError(rowIndex, rowErrors));
                }
                productions.add(production);
            } catch (Exception e) {
                errors.add(new DataImportError(rowIndex, List.of(e.getMessage() + " Further validations for this row are not possible.")));
            }
        }

        if (errors.isEmpty()) {
            try {
                var conflictErrors = checkConflicts(productions);
                if (!conflictErrors.isEmpty()) {
                    return new DataImportResult("One or more conflicting rows found.", conflictErrors);
                }
            } catch(Exception e) {
                return new DataImportResult("Error while checking conflicts", List.of(new DataImportError(0, List.of(e.getMessage()))));
            }
            List<OwnProduction> addedProductions = new ArrayList<>();
            List<OwnProduction> existingProductions = ownProductionService.findAll();
            for (var newProduction : productions) {
                try {
                    var added = ownProductionService.create(newProduction);
                    addedProductions.add(added);
                } catch (Exception e) {
                    log.error("Failed to persist production: {}", newProduction);
                    var idx = productions.indexOf(newProduction);
                    errors.add(new DataImportError(idx + 2, List.of("Failed to persist")));
                    addedProductions.forEach(p -> ownProductionService.delete(p.getUuid()));
                    return new DataImportResult("Failed to persist Productions", errors);
                }
            }
            existingProductions.forEach(p -> ownProductionService.delete(p.getUuid()));
            return new DataImportResult("Successfully imported productions", errors);
        } else {
            log.info(errors.toString());
        }
        return new DataImportResult("Failed to process Production rows", errors);
    }

    private DataImportResult extractAndSaveDeliveries(Sheet sheet) {
        List<OwnDelivery> deliveries = new ArrayList<>();
        List<DataImportError> errors = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            List<String> rowErrors = new ArrayList<>();
            Row row = rowIterator.next();
            rowIndex++;
            if (isRowEmpty(row)) {
                continue;
            }
            try {
                String ownMaterialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String originSiteBpns = getStringCellValue(row.getCell(4));
                String originAddressBpna = getStringCellValue(row.getCell(5));
                String destinationSiteBpns = getStringCellValue(row.getCell(6));
                String destinationAddressBpna = getStringCellValue(row.getCell(7));
                String departureType = getStringCellValue(row.getCell(8));
                Date departureTime = getDateCellValue(row.getCell(9));
                String arrivalType = getStringCellValue(row.getCell(10));
                Date arrivalTime = getDateCellValue(row.getCell(11));
                String trackingNumber = getStringCellValue(row.getCell(12));
                String incoterm = getStringCellValue(row.getCell(13));
                String customerOrderNumber = getStringCellValue(row.getCell(14));
                String customerPositionNumber = getStringCellValue(row.getCell(15));
                String supplierOrderNumber = getStringCellValue(row.getCell(16));
                Date lastUpdatedOnDateTime = getDateCellValue(row.getCell(17));

                ItemUnitEnumeration unitEnum = null;
                try {
                    unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                } catch (Exception e) {
                    rowErrors.add("Invalid unit of measurement: " + unitOfMeasurement);
                }

                IncotermEnumeration incotermEnum = null;
                try {
                    incotermEnum = IncotermEnumeration.valueOf(incoterm.toUpperCase());
                } catch (Exception e) {
                    rowErrors.add("Invalid incoterm: " + incoterm);
                }

                EventTypeEnumeration departureTypeEnum = null;
                try {
                    departureTypeEnum = EventTypeEnumeration.fromValue(departureType);
                } catch (Exception e) {
                    rowErrors.add("Invalid departure type: " + departureType);
                }

                EventTypeEnumeration arrivalTypeEnum = null;
                try {
                    arrivalTypeEnum = EventTypeEnumeration.fromValue(arrivalType);
                } catch (Exception e) {
                    rowErrors.add("Invalid arrival type: " + arrivalType);
                }

                if (lastUpdatedOnDateTime == null) {
                    lastUpdatedOnDateTime = new Date();
                }

                Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");

                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");

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
                    .lastUpdatedOnDateTime(lastUpdatedOnDateTime)
                    .build();
                rowErrors.addAll(ownDeliveryService.validateWithDetails(delivery));
                if (!rowErrors.isEmpty()) {
                    errors.add(new DataImportError(rowIndex, rowErrors));
                }
                deliveries.add(delivery);
            } catch (Exception e) {
                errors.add(new DataImportError(rowIndex, List.of(e.getMessage() + " Further validations for this row are not possible.")));
            }
        }

        if (errors.isEmpty()) {
            try {
                var conflictErrors = checkConflicts(deliveries);
                if (!conflictErrors.isEmpty()) {
                    return new DataImportResult("One or more conflicting rows found.", conflictErrors);
                }
            } catch(Exception e) {
                return new DataImportResult("Error while checking conflicts", List.of(new DataImportError(0, List.of(e.getMessage()))));
            }
            List<OwnDelivery> addedDeliveries = new ArrayList<>();
            List<OwnDelivery> existingDeliveries = ownDeliveryService.findAll();

            for (var newDelivery : deliveries) {
                try {
                    var added = ownDeliveryService.create(newDelivery);
                    addedDeliveries.add(added);
                } catch(Exception e) {
                    log.error("Failed to persist delivery: {}", newDelivery);
                    int idx = deliveries.indexOf(newDelivery);
                    errors.add(new DataImportError(idx + 2, List.of(e.getMessage())));
                    addedDeliveries.forEach(d -> ownDeliveryService.delete(d.getUuid()));
                    return new DataImportResult("Failed to persist Deliveries", errors);
                }
            }
            existingDeliveries.forEach(d -> ownDeliveryService.delete(d.getUuid()));
            return new DataImportResult("Successfully imported deliveries", errors);
        } else {
            log.info(errors.toString());
        }

        return new DataImportResult("Failed to process Delivery rows", errors);
    }

    private DataImportResult extractAndSaveStocks(Sheet sheet) {
        List<MaterialItemStock> materialStocks = new ArrayList<>();
        List<ProductItemStock> productStocks = new ArrayList<>();
        List<ItemStock> allStocks = new ArrayList<>();
        List<DataImportError> errors = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        int rowIndex = 0;

        if (rowIterator.hasNext()) {
            rowIterator.next();
            rowIndex++;
        }

        while (rowIterator.hasNext()) {
            List<String> rowErrors = new ArrayList<>();
            Row row = rowIterator.next();
            rowIndex++;
            if (isRowEmpty(row)) {
                continue;
            }
            try {
                String ownMaterialNumber = getStringCellValue(row.getCell(0));
                String partnerBpnl = getStringCellValue(row.getCell(1));
                double quantity = Double.parseDouble(getStringCellValue(row.getCell(2)));
                String unitOfMeasurement = getStringCellValue(row.getCell(3));
                String stockSiteBpns = getStringCellValue(row.getCell(4));
                String stockAddressBpna = getStringCellValue(row.getCell(5));
                String customerOrderNumber = getStringCellValue(row.getCell(6));
                String customerPositionNumber = getStringCellValue(row.getCell(7));
                String supplierOrderNumber = getStringCellValue(row.getCell(8));
                boolean isBlocked = row.getCell(9).getBooleanCellValue();
                Date lastUpdatedOnDateTime = getDateCellValue(row.getCell(10));
                String direction = getStringCellValue(row.getCell(11));

                ItemUnitEnumeration unitEnum = null;
                try {
                    unitEnum = ItemUnitEnumeration.fromValue(unitOfMeasurement);
                } catch (Exception e) {
                    rowErrors.add("Invalid unit of measurement: " + unitOfMeasurement);
                }

                if (lastUpdatedOnDateTime == null) {
                    lastUpdatedOnDateTime = new Date();
                }

                Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
                if (material == null) throw new IllegalArgumentException("Material not found.");

                Partner partner = partnerService.findByBpnl(partnerBpnl);
                if (partner == null) throw new IllegalArgumentException("Partner not found.");

                if ("inbound".equalsIgnoreCase(direction)) {
                    MaterialItemStock stock = MaterialItemStock.builder()
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
                    rowErrors.addAll(materialItemStockService.validateWithDetails(stock));
                    if (!rowErrors.isEmpty()) {
                        errors.add(new DataImportError(rowIndex, rowErrors));
                    }
                    materialStocks.add(stock);
                    allStocks.add(stock);
                } else if ("outbound".equalsIgnoreCase(direction)) {
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
                    rowErrors.addAll(productItemStockService.validateWithDetails(stock));
                    if (!rowErrors.isEmpty()) {
                        errors.add(new DataImportError(rowIndex, rowErrors));
                    }
                    productStocks.add(stock);
                    allStocks.add(stock);
                } else {
                    throw new IllegalArgumentException("Invalid direction: " + direction);
                }
            } catch (Exception e) {
                errors.add(new DataImportError(rowIndex, List.of(e.getMessage() + " Further validations for this row are not possible.")));
            }
        }

        if (errors.isEmpty()) {
            try {
                var conflictErrors = checkConflicts(allStocks);
                if (!conflictErrors.isEmpty()) {
                    return new DataImportResult("One or more conflicting rows found.", conflictErrors);
                }
            } catch(Exception e) {
                return new DataImportResult("Error while checking conflicts", List.of(new DataImportError(0, List.of(e.getMessage()))));
            }
            List<MaterialItemStock> addedMaterialStocks = new ArrayList<>();
            List<ProductItemStock> addedProductStocks = new ArrayList<>();
            List<MaterialItemStock> existingMaterialStocks = materialItemStockService.findAll();
            List<ProductItemStock> existingProductStocks = productItemStockService.findAll();

            for (var newStock : materialStocks) {
                try {
                    var added = materialItemStockService.create(newStock);
                    if (added == null) {
                        log.error("Failed to persist material stock: {}", newStock);
                        throw new IllegalArgumentException("Invalid material stock");
                    }
                    addedMaterialStocks.add(added);
                } catch (Exception e) {
                    int idx = allStocks.indexOf(newStock);
                    errors.add(new DataImportError(idx + 2, List.of(e.getMessage())));
                    addedMaterialStocks.forEach(s -> materialItemStockService.delete(s.getUuid()));
                    addedProductStocks.forEach(s -> productItemStockService.delete(s.getUuid()));
                    return new DataImportResult("Failed to persist stocks", errors);
                }
            }

            for (var newStock : productStocks) {
                try {

                    var added = productItemStockService.create(newStock);
                    if (added == null) {
                        log.error("Failed to persist product stock: {}", newStock);
                        throw new IllegalArgumentException("Invalid product stock");
                    }
                    addedProductStocks.add(added);
                } catch (Exception e) {
                    int idx = allStocks.indexOf(newStock);
                    errors.add(new DataImportError(idx + 2, List.of(e.getMessage())));
                    addedMaterialStocks.forEach(s -> materialItemStockService.delete(s.getUuid()));
                    addedProductStocks.forEach(s -> productItemStockService.delete(s.getUuid()));
                    return new DataImportResult("Failed to persist stocks", errors);
                }
            }

            existingMaterialStocks.forEach(s -> materialItemStockService.delete(s.getUuid()));
            existingProductStocks.forEach(s -> productItemStockService.delete(s.getUuid()));
            return new DataImportResult("Successfully imported stocks", errors);
        } else {
            log.info(errors.toString());
        }

        return new DataImportResult("Failed to process stock rows", errors);
    }

    private DataDocumentTypeEnumeration validateHeaders(Sheet sheet) {
        var headerNames = extractHeader(sheet);
        if (headerNames.containsAll(demandColumns)) {
            return DataDocumentTypeEnumeration.DEMAND;
        } else if (headerNames.containsAll(deliveryColumns)) {
            return DataDocumentTypeEnumeration.DELIVERY;
        } else if (headerNames.containsAll(productionColumns)) {
            return DataDocumentTypeEnumeration.PRODUCTION;
        } else if (headerNames.containsAll(stockColumns)) {
            return DataDocumentTypeEnumeration.STOCK;
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
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue() == null ? null : cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue()).trim();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Date getDateCellValue(Cell cell) {
        try {
            return cell == null || cell.getCellType() == CellType.BLANK ? null : cell.getCellType() == CellType.STRING
                ? Date.from(Instant.parse(cell.getStringCellValue().trim()))
                : cell.getDateCellValue();
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Checks a given List of imported rows for duplicates by comparing
     * each current object with the previous one using Object.equals. This method works
     * for all types of imported data (demand, production, delivery and stock).
     * @param <T> the type of imported data
     * @param importEntries the list of imported data rows
     * @return list of errors for each row that conflicts with one or more previous rows 
     */
    public <T> List<DataImportError> checkConflicts(List<T> importEntries) {
        List<DataImportError> errors = new ArrayList<>();
        for (int i = 0; i < importEntries.size(); i++) {
            T current = importEntries.get(i);
            List<Integer> conflictingIndexes = new ArrayList<>();

            for (int j = 0; j < i; j++) {
                if (current.equals(importEntries.get(j))) {
                    conflictingIndexes.add(j + 2);
                }
            }

            if (!conflictingIndexes.isEmpty()) {
                errors.add(new DataImportError(i + 2, List.of("The row " + (i + 2) + " conflicts with the following rows: " + conflictingIndexes.toString())));
            }
        }
        return errors;
    }

    /**
     * Checks if a given row is empty (i.e. all cells are blank or null).
     * @param row the row to check
     * @return true if the row is empty, false otherwise
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getStringCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<DataImportError> evaluateWorkbook(Workbook workbook, String fileName) {
        List<DataImportError> errors = new ArrayList<>();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        try {
            evaluator.evaluateAll();
            return errors;
        } catch (RuntimeException bulkEx) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell != null && cell.getCellType() == CellType.FORMULA) {
                            try {
                                evaluator.evaluateFormulaCell(cell);
                            } catch (RuntimeException ex) {
                                Throwable cause = ex.getCause();
                                String cellRef = new CellReference(cell).formatAsString();
                                int rowIndex = cell.getRowIndex() + 1;
                                String formula = getFormulaAsString(cell);
                                switch (cause) {
                                    case NotImplementedFunctionException nie -> {
                                        errors.add(new DataImportError(rowIndex, List.of(String.format("Unsupported function '%s' at Sheet '%s'!%s. Formula: =%s", nie.getFunctionName(), sheetName, cellRef, formula))));
                                    }
                                    case FormulaParseException fpe -> {
                                        errors.add(new DataImportError(rowIndex, List.of(String.format("Formula parse error at Sheet '%s'!%s. Formula: =%s. Reason: %s", sheetName, cellRef, formula, nullToEmpty(fpe.getMessage())))));
                                    }
                                    default -> {
                                        errors.add(new DataImportError(rowIndex, List.of(String.format("Error evaluating at Sheet '%s'!%s. Formula: =%s. Reason: %s", sheetName, cellRef, formula, nullToEmpty(ex.getMessage())))));
                                    }
                                }
                                log.error("Excel import failed for '{}' due to RuntimeException with cause: {}", fileName, cause.toString());
                            }
                        }
                    }
                }
            }

            if (!errors.isEmpty()) {
                String msg = "Excel formula evaluation failed. Likely unsupported function(s) or parse error(s) detected:\n" + errors;
                log.warn(msg, bulkEx);
                return errors;
            } else {
                String msg = "Excel formula evaluation failed, but the failing cell could not be pinpointed. ";
                log.warn(msg);
                errors.add(new DataImportError(0, List.of(msg)));
                return errors;
            }
        }
    }

    /**
     * Safely returns the formula of the given cell as a string.
     *
     * @param c the cell whose formula should be obtained
     * @return the cell's formula, or "<unavailable>" if it cannot be read
     */
    private static String getFormulaAsString(Cell c) {
        try { return c.getCellFormula(); } catch (Exception ignore) { return "<unavailable>"; }
    }

    /**
     * Returns an empty string if the given value is null.
     */
    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}
