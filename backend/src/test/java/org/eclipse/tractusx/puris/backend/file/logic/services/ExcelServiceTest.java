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
package org.eclipse.tractusx.puris.backend.file.logic.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.file.domain.model.DataImportError;
import org.eclipse.tractusx.puris.backend.file.domain.model.DataImportResult;
import org.eclipse.tractusx.puris.backend.file.logic.service.ExcelService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.logic.service.OwnProductionService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

public class ExcelServiceTest {

    @Mock
    private MaterialService materialService;
    
    @Mock
    private PartnerService partnerService;
    
    @Mock
    private OwnDemandService ownDemandService;
    
    @Mock
    private OwnProductionService ownProductionService;
    
    @Mock
    private OwnDeliveryService ownDeliveryService;
    
    @Mock
    private MaterialItemStockService materialItemStockService;
    
    @Mock
    private ProductItemStockService productItemStockService;

    @InjectMocks
    private ExcelService excelService;

    private static final Material testMaterial;
    private static final Partner testPartner;
    private static final String todaysDateFromFormula = "=TODAY()";
    private static final String tomorrowsDateFromFormula = "=TODAY()+1";
    private static final Date todaysDateFromParsing = Date.from(Instant.now());
    
    private static final String OWN_BPNS = "BPNS1234567890AB";
    private static final String OWN_BPNA = "BPNA1234567890AB";
    private static final String PARTNER_BPNS = "BPNS0987654321BA";
    private static final String PARTNER_BPNA = "BPNA0987654321BA";
    
    private static final List<String> DEMAND_HEADERS = List.of(
        "ownMaterialNumber", "partnerBpnl", "quantity", "unitOfMeasurement",
        "expectedSupplierSiteBpns", "demandSiteBpns", "demandCategoryCode",
        "day", "lastUpdatedOnDateTime"
    );
    
    private static final List<String> PRODUCTION_HEADERS = List.of(
        "ownMaterialNumber", "partnerBpnl", "quantity", "unitOfMeasurement",
        "productionSiteBpns", "estimatedCompletionTime", "customerOrderNumber",
        "customerPositionId", "supplierOrderNumber", "lastUpdatedOnDateTime"
    );
    
    private static final List<String> DELIVERY_HEADERS = List.of(
        "ownMaterialNumber", "partnerBpnl", "quantity", "unitOfMeasurement",
        "originSiteBpns", "originAddressBpna", "destinationSiteBpns", "destinationAddressBpna",
        "departureType", "departureTime", "arrivalType", "arrivalTime",
        "trackingNumber", "Incoterm", "customerOrderNumber", "customerPositionId",
        "supplierOrderNumber", "lastUpdatedOnDateTime"
    );
    
    private static final List<String> STOCK_HEADERS = List.of(
        "ownMaterialNumber", "partnerBpnl", "quantity", "unitOfMeasurement",
        "stockSiteBpns", "stockAddressBpna", "customerOrderNumber", "customerPositionId",
        "supplierOrderNumber", "isBlocked", "lastUpdatedOnDateTime", "direction"
    );

    private static final List<Object> SAMPLE_DEMAND_ROW;
    private static final List<Object> SAMPLE_PRODUCTION_ROW;
    private static final List<Object> SAMPLE_DELIVERY_ROW;
    private static final List<Object> SAMPLE_STOCK_ROW;


    static {
        testMaterial = new Material();
        testMaterial.setOwnMaterialNumber("TEST-MATERIAL-001");
        testMaterial.setMaterialFlag(true);
        testMaterial.setName("Test Material");
        
        testPartner = new Partner();
        testPartner.setUuid(UUID.randomUUID());
        testPartner.setBpnl("BPNL1234567890AB");
        testPartner.setName("Test Partner");
        testPartner.setEdcUrl("http://test.edc.url");

        SAMPLE_DEMAND_ROW = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, PARTNER_BPNS, "0001",
            todaysDateFromFormula, todaysDateFromParsing
        );

        SAMPLE_PRODUCTION_ROW = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, todaysDateFromFormula, "ORDER-001",
            "POS-001", "SUPPLY-001", todaysDateFromParsing
        );

        SAMPLE_DELIVERY_ROW = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, PARTNER_BPNS, PARTNER_BPNA,
            "estimated-departure", todaysDateFromFormula, "estimated-arrival", tomorrowsDateFromFormula,
            "TRACK-001", "EXW", "ORDER-001", "POS-001",
            "SUPPLY-001", todaysDateFromParsing
        );

        SAMPLE_STOCK_ROW = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, "ORDER-001", "POS-001",
            "SUPPLY-001", false, todaysDateFromParsing, "INBOUND"
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReadExcelFile_Demand_MaterialNotFound_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(null);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Demand rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
    }

    @Test
    void testReadExcelFile_Demand_PartnerNotFound_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(null);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Demand rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
    }

    @Test
    void testReadExcelFile_Demand_ValidationErrors_ReturnsErrors() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFile();
        List<String> validationErrors = List.of("Validation error 1", "Validation error 2");
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownDemandService.validateWithDetails(any(OwnDemand.class)))
            .thenReturn(validationErrors);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Demand rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertEquals(2, result.getErrors().get(0).getErrors().size());

        for(int i = 0; i < validationErrors.size(); i++) {
            assertEquals(validationErrors.get(i), result.getErrors().get(0).getErrors().get(i));
        }
    }

    @Test
    void testReadExcelFile_InvalidHeaders_ThrowsError() throws IOException {
        ByteArrayInputStream inputStream = createInvalidHeaderExcelFile();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> excelService.readExcelFile(inputStream));
        assertTrue(exception.getMessage().contains("Unsupported Excel file format"));
    }

    @Test
    void testCheckConflicts_EmptyList_ReturnsEmptyList() {
        List<String> emptyList = new ArrayList<>();
        List<DataImportError> conflicts = excelService.checkConflicts(emptyList);
        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testReadExcelFile_Demand_ValidData_CallsServices() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownDemandService.validateWithDetails(any(OwnDemand.class))).thenReturn(Collections.emptyList());
        when(ownDemandService.findAll()).thenReturn(Collections.emptyList());
        when(ownDemandService.create(any(OwnDemand.class))).thenReturn(new OwnDemand());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Successfully imported demands", result.getMessage());
        assertTrue(result.getErrors().isEmpty());
        verify(materialService).findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber());
        verify(partnerService).findByBpnl(testPartner.getBpnl());
        verify(ownDemandService).validateWithDetails(any(OwnDemand.class));
        verify(ownDemandService).create(any(OwnDemand.class));
    }

    @Test
    void testReadExcelFile_Demand_ConflictingData_ReturnsErrors() throws IOException {
        ByteArrayInputStream inputStream = createConflictingDemandExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownDemandService.validateWithDetails(any(OwnDemand.class))).thenReturn(Collections.emptyList());
        when(ownDemandService.findAll()).thenReturn(Collections.emptyList());
        when(ownDemandService.create(any(OwnDemand.class))).thenReturn(new OwnDemand());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("One or more conflicting rows found.", result.getMessage());
        assertEquals(1, result.getErrors().size());
        assertEquals(3, result.getErrors().get(0).getRow());
        assertEquals("The row 3 conflicts with the following rows: [2]", result.getErrors().get(0).getErrors().get(0));
    }

    @Test
    void testReadExcelFile_Production_ValidData_CallsServices() throws IOException {
        ByteArrayInputStream inputStream = createProductionExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownProductionService.validateWithDetails(any(OwnProduction.class))).thenReturn(Collections.emptyList());
        when(ownProductionService.findAll()).thenReturn(Collections.emptyList());
        when(ownProductionService.create(any(OwnProduction.class))).thenReturn(new OwnProduction());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Successfully imported productions", result.getMessage());
        assertTrue(result.getErrors().isEmpty());
        verify(materialService).findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber());
        verify(partnerService).findByBpnl(testPartner.getBpnl());
        verify(ownProductionService).validateWithDetails(any(OwnProduction.class));
        verify(ownProductionService).create(any(OwnProduction.class));
    }

    @Test
    void testReadExcelFile_Production_ConflictingData_ReturnsErrors() throws IOException {
        ByteArrayInputStream inputStream = createConflictingProductionExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownProductionService.validateWithDetails(any(OwnProduction.class))).thenReturn(Collections.emptyList());
        when(ownProductionService.findAll()).thenReturn(Collections.emptyList());
        when(ownProductionService.create(any(OwnProduction.class))).thenReturn(new OwnProduction());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("One or more conflicting rows found.", result.getMessage());
        assertEquals(1, result.getErrors().size());
        assertEquals(3, result.getErrors().get(0).getRow());
        assertEquals("The row 3 conflicts with the following rows: [2]", result.getErrors().get(0).getErrors().get(0));
    }

    @Test
    void testReadExcelFile_Delivery_ValidData_CallsServices() throws IOException {
        ByteArrayInputStream inputStream = createDeliveryExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownDeliveryService.validateWithDetails(any(OwnDelivery.class))).thenReturn(Collections.emptyList());
        when(ownDeliveryService.findAll()).thenReturn(Collections.emptyList());
        when(ownDeliveryService.create(any(OwnDelivery.class))).thenReturn(new OwnDelivery());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Successfully imported deliveries", result.getMessage());
        assertTrue(result.getErrors().isEmpty());
        verify(materialService).findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber());
        verify(partnerService).findByBpnl(testPartner.getBpnl());
        verify(ownDeliveryService).validateWithDetails(any(OwnDelivery.class));
        verify(ownDeliveryService).create(any(OwnDelivery.class));
    }

    @Test
    void testReadExcelFile_Delivery_ConflictingData_ReturnsErrors() throws IOException {
        ByteArrayInputStream inputStream = createConflictingDeliveryExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(ownDeliveryService.validateWithDetails(any(OwnDelivery.class))).thenReturn(Collections.emptyList());
        when(ownDeliveryService.findAll()).thenReturn(Collections.emptyList());
        when(ownDeliveryService.create(any(OwnDelivery.class))).thenReturn(new OwnDelivery());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("One or more conflicting rows found.", result.getMessage());
        assertEquals(1, result.getErrors().size());
        assertEquals(3, result.getErrors().get(0).getRow());
        assertEquals("The row 3 conflicts with the following rows: [2]", result.getErrors().get(0).getErrors().get(0));
    }

    @Test
    void testReadExcelFile_Stock_ValidData_CallsServices() throws IOException {
        ByteArrayInputStream inputStream = createStockExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(materialItemStockService.validateWithDetails(any(MaterialItemStock.class))).thenReturn(Collections.emptyList());
        when(materialItemStockService.findAll()).thenReturn(Collections.emptyList());
        when(productItemStockService.findAll()).thenReturn(Collections.emptyList());
        when(materialItemStockService.create(any(MaterialItemStock.class))).thenReturn(new MaterialItemStock());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Successfully imported stocks", result.getMessage());
        assertTrue(result.getErrors().isEmpty());
        verify(materialService).findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber());
        verify(partnerService).findByBpnl(testPartner.getBpnl());
        verify(materialItemStockService).validateWithDetails(any(MaterialItemStock.class));
        verify(materialItemStockService).create(any(MaterialItemStock.class));
    }

    @Test
    void testReadExcelFile_Stock_ConflictingData_ReturnsErrors() throws IOException {
        ByteArrayInputStream inputStream = createConflictingStockExcelFile();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);
        when(materialItemStockService.validateWithDetails(any(MaterialItemStock.class))).thenReturn(Collections.emptyList());
        when(materialItemStockService.findAll()).thenReturn(Collections.emptyList());
        when(materialItemStockService.create(any(MaterialItemStock.class))).thenReturn(new MaterialItemStock());

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("One or more conflicting rows found.", result.getMessage());
        assertEquals(1, result.getErrors().size());
        assertEquals(3, result.getErrors().get(0).getRow());
        assertEquals("The row 3 conflicts with the following rows: [2]", result.getErrors().get(0).getErrors().get(0));
    }

    @Test
    void testReadExcelFile_Demand_InvalidUnitOfMeasurement_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFileWithInvalidUnit();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Demand rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("Invalid unit of measurement"));
    }

    @Test
    void testReadExcelFile_Demand_InvalidDemandCategory_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDemandExcelFileWithInvalidCategory();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Demand rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("Invalid demand category"));
    }

    @Test
    void testReadExcelFile_Delivery_InvalidIncoterm_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDeliveryExcelFileWithInvalidIncoterm();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Delivery rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("Invalid incoterm"));
    }

    @Test
    void testReadExcelFile_Delivery_InvalidDepartureType_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDeliveryExcelFileWithInvalidDepartureType();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Delivery rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).equals("Invalid departure type: invalid-departure-type"));
    }

    @Test
    void testReadExcelFile_Delivery_InvalidArrivalType_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createDeliveryExcelFileWithInvalidArrivalType();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process Delivery rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).equals("Invalid arrival type: invalid-arrival-type"));
    }

    @Test
    void testReadExcelFile_Stock_InvalidDirection_ReturnsError() throws IOException {
        ByteArrayInputStream inputStream = createStockExcelFileWithInvalidDirection();
        when(materialService.findByOwnMaterialNumber(testMaterial.getOwnMaterialNumber())).thenReturn(testMaterial);
        when(partnerService.findByBpnl(testPartner.getBpnl())).thenReturn(testPartner);

        DataImportResult result = excelService.readExcelFile(inputStream);

        assertNotNull(result);
        assertEquals("Failed to process stock rows", result.getMessage());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(2, result.getErrors().get(0).getRow());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("Invalid direction"));
    }
    
    private ByteArrayInputStream createDemandExcelFile() throws IOException {
        return createExcelFile("Demands", DEMAND_HEADERS, List.of(SAMPLE_DEMAND_ROW));
    }

    private ByteArrayInputStream createConflictingDemandExcelFile() throws IOException {
        return createExcelFile("Demands", DEMAND_HEADERS, List.of(SAMPLE_DEMAND_ROW, SAMPLE_DEMAND_ROW));
    }
    
    private ByteArrayInputStream createProductionExcelFile() throws IOException {
        return createExcelFile("Productions", PRODUCTION_HEADERS, List.of(SAMPLE_PRODUCTION_ROW));
    }

    private ByteArrayInputStream createConflictingProductionExcelFile() throws IOException {
        return createExcelFile("Productions", PRODUCTION_HEADERS, List.of(SAMPLE_PRODUCTION_ROW, SAMPLE_PRODUCTION_ROW));
    }
    
    private ByteArrayInputStream createDeliveryExcelFile() throws IOException {
        return createExcelFile("Deliveries", DELIVERY_HEADERS, List.of(SAMPLE_DELIVERY_ROW));
    }

    private ByteArrayInputStream createConflictingDeliveryExcelFile() throws IOException {
        return createExcelFile("Deliveries", DELIVERY_HEADERS, List.of(SAMPLE_DELIVERY_ROW, SAMPLE_DELIVERY_ROW));
    }
    
    private ByteArrayInputStream createStockExcelFile() throws IOException {
        return createExcelFile("Stocks", STOCK_HEADERS, List.of(SAMPLE_STOCK_ROW));
    }

    private ByteArrayInputStream createConflictingStockExcelFile() throws IOException {
        return createExcelFile("Stocks", STOCK_HEADERS, List.of(SAMPLE_STOCK_ROW, SAMPLE_STOCK_ROW));
    }

    private ByteArrayInputStream createDemandExcelFileWithInvalidUnit() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "invalid-unit",
            OWN_BPNS, PARTNER_BPNS, "0001",
            todaysDateFromFormula, todaysDateFromFormula
        );
        
        return createExcelFile("Demands", DEMAND_HEADERS, List.of(dataValues));
    }

    private ByteArrayInputStream createDemandExcelFileWithInvalidCategory() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, PARTNER_BPNS, "invalid-category",
            todaysDateFromFormula, todaysDateFromFormula
        );
        
        return createExcelFile("Demands", DEMAND_HEADERS, List.of(dataValues));
    }

    private ByteArrayInputStream createDeliveryExcelFileWithInvalidIncoterm() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, PARTNER_BPNS, PARTNER_BPNA,
            "estimated-departure", todaysDateFromFormula, "estimated-arrival", todaysDateFromFormula,
            "TRACK-001", "INVALID-INCOTERM", "ORDER-001", "POS-001",
            "SUPPLY-001", todaysDateFromFormula
        );
        
        return createExcelFile("Deliveries", DELIVERY_HEADERS, List.of(dataValues));
    }

    private ByteArrayInputStream createDeliveryExcelFileWithInvalidDepartureType() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, PARTNER_BPNS, PARTNER_BPNA,
            "invalid-departure-type", todaysDateFromFormula, "estimated-arrival", todaysDateFromFormula,
            "TRACK-001", "EXW", "ORDER-001", "POS-001",
            "SUPPLY-001", todaysDateFromFormula
        );
        
        return createExcelFile("Deliveries", DELIVERY_HEADERS, List.of(dataValues));
    }

    private ByteArrayInputStream createDeliveryExcelFileWithInvalidArrivalType() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, PARTNER_BPNS, PARTNER_BPNA,
            "estimated-departure", todaysDateFromFormula, "invalid-arrival-type", todaysDateFromFormula,
            "TRACK-001", "EXW", "ORDER-001", "POS-001",
            "SUPPLY-001", todaysDateFromFormula
        );
        
        return createExcelFile("Deliveries", DELIVERY_HEADERS, List.of(dataValues));
    }

    private ByteArrayInputStream createStockExcelFileWithInvalidDirection() throws IOException {
        List<Object> dataValues = List.of(
            testMaterial.getOwnMaterialNumber(), testPartner.getBpnl(), 100.0, "unit:piece",
            OWN_BPNS, OWN_BPNA, "ORDER-001", "POS-001",
            "SUPPLY-001", false, todaysDateFromFormula, "invalid-direction"
        );
        
        return createExcelFile("Stocks", STOCK_HEADERS, List.of(dataValues));
    }
    
    private ByteArrayInputStream createInvalidHeaderExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Invalid");
        
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("invalidHeader1");
        headerRow.createCell(1).setCellValue("invalidHeader2");
        
        return convertWorkbookToInputStream(workbook);
    }

    private ByteArrayInputStream createExcelFile(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        Sheet sheet = workbook.createSheet(sheetName);
        
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }
        
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            Row dataRow = sheet.createRow(rowIdx + 1);
            List<Object> rowData = rows.get(rowIdx);
            for (int colIdx = 0; colIdx < rowData.size(); colIdx++) {
                Cell cell = dataRow.createCell(colIdx);
                Object value = rowData.get(colIdx);
                
                if (value instanceof String) {
                    if (((String) value).startsWith("=")) {
                        cell.setCellFormula(((String) value).substring(1));
                        evaluator.evaluateFormulaCell(cell);
                    } else {
                        cell.setCellValue((String) value);
                    }
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                } else if (value instanceof Date) {
                    cell.setCellValue((Date) value);
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }
        
        return convertWorkbookToInputStream(workbook);
    }
    
    private ByteArrayInputStream convertWorkbookToInputStream(Workbook workbook) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
