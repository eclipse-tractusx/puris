/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.*;
import org.eclipse.tractusx.puris.backend.stock.logic.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains a range of REST endpoints to provide the frontend
 * the necessary data concerning stocks.
 */
@RestController
@RequestMapping("stockView")
@Slf4j
public class StockViewController {

    @Autowired
    private ProductItemStockService productItemStockService;

    @Autowired
    private MaterialItemStockService materialItemStockService;

    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;

    @Autowired
    private ReportedProductItemStockService reportedProductItemStockService;

    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Validator validator;

    private final Pattern materialPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @GetMapping("materials")
    @ResponseBody
    @Operation(description = "Returns a list of all materials (excluding products)")
    public List<FrontendMaterialDto> getMaterials() {
        return materialService.findAllMaterials()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }

    @GetMapping("materialnumbers-mapping")
    @Operation(description = "Returns a mapping of all material numbers, that others partners are using" +
        "for the material given in the request parameter.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(examples = {
            @ExampleObject(name = "Basic sample", value = "{" +
                "  \"BPNL1234567890ZZ\": \"MNR-8101-ID146955.001\"," +
                "  \"BPNL4444444444XX\": \"MNR-7307-AU340474.002\"}")})),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")})
    public ResponseEntity<Map<String, String>> getMaterialNumbers(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(mprService.getBPNL_To_MaterialNumberMap(ownMaterialNumber));
    }

    @GetMapping("products")
    @ResponseBody
    @Operation(description = "Returns a list of all products (excluding materials)")
    public List<FrontendMaterialDto> getProducts() {
        return materialService.findAllProducts()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }

    @GetMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all product-stocks")
    public List<ProductStockDto> getProductStocks() {
        return productItemStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @PostMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Creates a new product-stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product Stock was created."),
        @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
        @ApiResponse(responseCode = "409", description = "Product Stock does already exist."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ProductStockDto createProductStocks(@RequestBody ProductStockDto productStockDto) {
        if(!validator.validate(productStockDto).isEmpty()) {
            log.warn("Rejected invalid message body");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (productStockDto.getUuid() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock misses material identification.");
        }

        if (productStockDto.getMaterial().getMaterialNumberSupplier() == null ||
            productStockDto.getMaterial().getMaterialNumberSupplier().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock misses material identification.");
        }

        ProductItemStock productStockToCreate = convertToEntity(productStockDto);

        productStockToCreate.setLastUpdatedOnDateTime(new Date());

        if (!productItemStockService.validate(productStockToCreate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock is invalid.");
        }

        List<ProductItemStock> existingProductItemStocks = productItemStockService.findByPartnerAndMaterial(
            productStockToCreate.getPartner(),
            productStockToCreate.getMaterial());

        boolean stockDoesExist = existingProductItemStocks.stream().filter(stock ->
            stock.isBlocked() == productStockToCreate.isBlocked() &&
                stock.getLocationBpns().equals(productStockToCreate.getLocationBpns()) &&
                stock.getLocationBpna().equals(productStockToCreate.getLocationBpna()) &&
                Objects.equals(stock.getCustomerOrderId(), productStockToCreate.getCustomerOrderId()) &&
                Objects.equals(stock.getCustomerOrderPositionId(), productStockToCreate.getCustomerOrderPositionId()) &&
                Objects.equals(stock.getSupplierOrderId(), productStockToCreate.getSupplierOrderId())
        ).anyMatch(stock -> true);

        if (stockDoesExist) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product Stock does already exist. Use PUT instead.");
        }

        ProductItemStock createdProductStock = productItemStockService.create(productStockToCreate);
        if (createdProductStock == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock could not be created.");
        }
        log.info("Created product-stock: " + createdProductStock);

        return convertToDto(createdProductStock);
    }

    @PutMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing productstock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product Stock was updated."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public ProductStockDto updateProductStocks(@RequestBody ProductStockDto productStockDto) {
        if(!validator.validate(productStockDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (productStockDto.getUuid() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock holds a UUID. Use POST instead.");
        }

        ProductItemStock existingProductStock = productItemStockService.findById(productStockDto.getUuid());
        if (existingProductStock == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product Stock does not exist. Use Post instead.");
        }

        existingProductStock.setQuantity(productStockDto.getQuantity());
        existingProductStock.setMeasurementUnit(productStockDto.getMeasurementUnit());
        existingProductStock.setLastUpdatedOnDateTime(new Date());

        existingProductStock = productItemStockService.update(existingProductStock);
        log.info("Updated product-stock: " + existingProductStock);

        return convertToDto(existingProductStock);
    }

    private ProductStockDto convertToDto(ProductItemStock entity) {
        ProductStockDto dto = modelMapper.map(entity, ProductStockDto.class);
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation =
            mprService.find(entity.getMaterial().getOwnMaterialNumber(), entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());

        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());

        dto.setStockLocationBpns(entity.getLocationBpns());
        dto.setStockLocationBpna(entity.getLocationBpna());

        dto.setCustomerOrderNumber(entity.getCustomerOrderId());
        dto.setCustomerOrderPositionNumber(entity.getCustomerOrderPositionId());
        dto.setSupplierOrderNumber(entity.getSupplierOrderId());

        return dto;
    }

    private ProductItemStock convertToEntity(ProductStockDto dto) {
        ProductItemStock productStock = modelMapper.map(dto, ProductItemStock.class);
        Material material = materialService.findByOwnMaterialNumber(dto.getMaterial().getMaterialNumberSupplier());
        productStock.setMaterial(material);

        PartnerDto allocationPartner = dto.getPartner();

        Partner existingPartner = partnerService.findByBpnl(allocationPartner.getBpnl());

        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                "Partner for bpnl %s could not be found",
                allocationPartner.getBpnl())
            );
        }

        productStock.setPartner(existingPartner);

        productStock.setLocationBpna(dto.getStockLocationBpna());
        productStock.setLocationBpns(dto.getStockLocationBpns());

        productStock.setCustomerOrderId(dto.getCustomerOrderNumber());
        productStock.setCustomerOrderPositionId(dto.getCustomerOrderPositionNumber());
        productStock.setSupplierOrderId(dto.getSupplierOrderNumber());

        return productStock;
    }

    @GetMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all material-stocks")
    public List<MaterialStockDto> getMaterialStocks() {
        List<MaterialStockDto> allMaterialStocks = materialItemStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        return allMaterialStocks;
    }

    @PostMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Creates a new material-stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Material Stock was created."),
        @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
        @ApiResponse(responseCode = "409", description = "Material Stock does already exist."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public MaterialStockDto createMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {
        if(!validator.validate(materialStockDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (materialStockDto.getUuid() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock holds a UUID. Use POST instead.");
        }

        if (materialStockDto.getMaterial().getMaterialNumberCustomer() == null ||
            materialStockDto.getMaterial().getMaterialNumberCustomer().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock misses material identification.");
        }

        MaterialItemStock materialStockToCreate = convertToEntity(materialStockDto);
        materialStockToCreate.setLastUpdatedOnDateTime(new Date());

        if (!materialItemStockService.validate(materialStockToCreate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock is invalid.");
        }

        List<MaterialItemStock> existingMaterialItemStocks = materialItemStockService.findByPartnerAndMaterial(materialStockToCreate.getPartner(),
            materialStockToCreate.getMaterial());

        boolean stockDoesExist = existingMaterialItemStocks.stream().filter(stock ->
            stock.isBlocked() == materialStockToCreate.isBlocked() &&
                stock.getLocationBpns().equals(materialStockToCreate.getLocationBpns()) &&
                stock.getLocationBpna().equals(materialStockToCreate.getLocationBpna()) &&
                Objects.equals(stock.getCustomerOrderId(), materialStockToCreate.getCustomerOrderId()) &&
                Objects.equals(stock.getCustomerOrderPositionId(), materialStockToCreate.getCustomerOrderPositionId()) &&
                Objects.equals(stock.getSupplierOrderId(), materialStockToCreate.getSupplierOrderId())
        ).anyMatch(stock -> true);

        if (stockDoesExist) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material Stock does already exist. Use PUT instead.");
        }

        MaterialItemStock createdMaterialStock = materialItemStockService.create(materialStockToCreate);
        if (createdMaterialStock == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock could not be created.");
        }

        return convertToDto(createdMaterialStock);
    }

    @PutMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing material-stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Material Stock was updated."),
        @ApiResponse(responseCode = "400", description = "Malformed request body."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    public MaterialStockDto updateMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {
        if(!validator.validate(materialStockDto).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (materialStockDto.getUuid() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock misses material identification.");
        }

        MaterialItemStock existingMaterialStock = materialItemStockService.findById(materialStockDto.getUuid());
        if (existingMaterialStock == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Stock misses material identification.");
        }

        existingMaterialStock.setQuantity(materialStockDto.getQuantity());
        existingMaterialStock.setMeasurementUnit(materialStockDto.getMeasurementUnit());
        existingMaterialStock.setLastUpdatedOnDateTime(new Date());

        existingMaterialStock = materialItemStockService.update(existingMaterialStock);

        return convertToDto(existingMaterialStock);
    }

    private MaterialStockDto convertToDto(MaterialItemStock entity) {
        MaterialStockDto dto = modelMapper.map(entity, MaterialStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
            entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberSupplier(materialPartnerRelation.getPartnerMaterialNumber());

        dto.setStockLocationBpns(entity.getLocationBpns());
        dto.setStockLocationBpna(entity.getLocationBpna());

        dto.setCustomerOrderNumber(entity.getCustomerOrderId());
        dto.setCustomerOrderPositionNumber(entity.getCustomerOrderPositionId());
        dto.setSupplierOrderNumber(entity.getSupplierOrderId());

        return dto;
    }

    private MaterialItemStock convertToEntity(MaterialStockDto dto) {
        MaterialItemStock materialStock = modelMapper.map(dto, MaterialItemStock.class);

        Material material = materialService.findByOwnMaterialNumber(dto.getMaterial().getMaterialNumberCustomer());
        materialStock.setMaterial(material);

        PartnerDto partnerDto = dto.getPartner();

        Partner existingPartner = partnerService.findByBpnl(partnerDto.getBpnl());

        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                "Partner for bpnl %s could not be found",
                partnerDto.getBpnl())
            );
        }
        materialStock.setPartner(existingPartner);

        materialStock.setLocationBpna(dto.getStockLocationBpna());
        materialStock.setLocationBpns(dto.getStockLocationBpns());

        materialStock.setCustomerOrderId(dto.getCustomerOrderNumber() );
        materialStock.setCustomerOrderPositionId(dto.getCustomerOrderPositionNumber());
        materialStock.setSupplierOrderId(dto.getSupplierOrderNumber());

        return materialStock;
    }

    @GetMapping("reported-material-stocks")
    @Operation(description = "Returns a list of all materials the partner (supplier) reported he has at his site." +
        " Only stocks for the given material number are returned.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<ReportedMaterialStockDto>> getSupplierMaterialStocks(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(reportedMaterialItemStockService.
            findByOwnMaterialNumber(ownMaterialNumber)
            .stream()
            .map(this::convertToDto)
            .toList());
    }

    private ReportedMaterialStockDto convertToDto(ReportedMaterialItemStock entity) {
        ReportedMaterialStockDto dto = modelMapper.map(entity, ReportedMaterialStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
            entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberSupplier(materialPartnerRelation.getPartnerMaterialNumber());

        dto.setStockLocationBpns(entity.getLocationBpns());
        dto.setStockLocationBpna(entity.getLocationBpna());

        dto.setCustomerOrderNumber(entity.getCustomerOrderId());
        dto.setCustomerOrderPositionNumber(entity.getCustomerOrderPositionId());
        dto.setSupplierOrderNumber(entity.getSupplierOrderId());

        return dto;
    }

    @GetMapping("reported-product-stocks")
    @Operation(description = "Returns a list of all products the partner (customer) reported he has at his site." +
        " Only stocks for the given material number are returned.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<ReportedProductStockDto>> getCustomerProductStocks(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(reportedProductItemStockService.
            findByOwnMaterialNumber(ownMaterialNumber)
            .stream()
            .map(this::convertToDto)
            .toList());
    }

    private ReportedProductStockDto convertToDto(ReportedProductItemStock entity) {
        ReportedProductStockDto dto = modelMapper.map(entity, ReportedProductStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
            entity.getPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());

        dto.setStockLocationBpns(entity.getLocationBpns());
        dto.setStockLocationBpna(entity.getLocationBpna());

        dto.setCustomerOrderNumber(entity.getCustomerOrderId());
        dto.setCustomerOrderPositionNumber(entity.getCustomerOrderPositionId());
        dto.setSupplierOrderNumber(entity.getSupplierOrderId());

        return dto;
    }

    @GetMapping("customer")
    @Operation(description = "Returns a list of all Partners that are ordering the given material")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> getCustomerPartnersOrderingMaterial(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(partnerService.findAllCustomerPartnersForMaterialId(ownMaterialNumber).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    @GetMapping("supplier")
    @Operation(description = "Returns a list of all Partners that are supplying the given material")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> getSupplierPartnersSupplyingMaterial(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(partnerService.findAllSupplierPartnersForMaterialId(ownMaterialNumber).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    @GetMapping("update-reported-material-stocks")
    @Operation(description = "For the given material, all known suppliers will be requested to report their" +
        "current stocks for our input material. The response body contains a list of those supplier partners that were sent a request." +
        "Please note that these requests are handled asynchronously by the partners, so there are no guarantees, if and " +
        "when the corresponding responses will be available. As soon as a response arrives, it will be available via a " +
        "call to the GET reported-material-stocks endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> triggerReportedMaterialStockUpdateForMaterialNumber(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        log.info("Found material: " + (materialEntity != null) + " " + ownMaterialNumber);
        List<Partner> allSupplierPartnerEntities = mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);

        for (Partner supplierPartner : allSupplierPartnerEntities) {
            itemStockRequestApiService.doRequestForMaterialItemStocks(supplierPartner, materialEntity);
        }

        return ResponseEntity.ok(allSupplierPartnerEntities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    @GetMapping("update-reported-product-stocks")
    @Operation(description = "For the given material, all known customers will be requested to report their" +
        "current stocks for our output material. The response body contains a list of those customer partners that were sent a request." +
        "Please note that these requests are handled asynchronously by the partners, so there are no guarantees, if and " +
        "when the corresponding responses will be available. As soon as a response arrives, it will be available via a " +
        "call to the GET reported-material-stocks endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> triggerReportedProductStockUpdateForMaterialNumber(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        log.info("Found material: " + (materialEntity != null) + " " + ownMaterialNumber);
        List<Partner> allCustomerPartnerEntities = mprService.findAllCustomersForOwnMaterialNumber(ownMaterialNumber);

        for (Partner supplierPartner : allCustomerPartnerEntities) {
            itemStockRequestApiService.doRequestForProductItemStocks(supplierPartner, materialEntity);
        }

        return ResponseEntity.ok(allCustomerPartnerEntities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    private PartnerDto convertToDto(Partner entity) {
        return modelMapper.map(entity, PartnerDto.class);
    }

}
