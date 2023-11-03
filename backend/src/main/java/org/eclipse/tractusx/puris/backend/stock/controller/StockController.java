/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.FrontendMaterialDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.MaterialStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.PartnerProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class contains a range of REST endpoints to provide the frontend
 * the necessary data concerning stocks.
 */
@RestController
@RequestMapping("stockView")
@Slf4j
public class StockController {

    @Value("${edc.dataplane.public.port}")
    private String dataPlanePort;

    @Value("${edc.controlplane.host}")
    private String dataPlaneHost;

    @Value("${edc.applydataplaneworkaround}")
    private boolean applyDataplaneWorkaround;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private MaterialStockService materialStockService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private VariablesService variablesService;



    @CrossOrigin
    @GetMapping("materials")
    @ResponseBody
    @Operation(description = "Returns a list of all materials (excluding products)")
    public List<FrontendMaterialDto> getMaterials() {
        return materialService.findAllMaterials()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }

    @CrossOrigin
    @GetMapping("materialnumbers-mapping")
    @ResponseBody
    @Operation(description = "Returns a mapping of all material numbers, that others partners are using" +
        "for the material given in the request parameter.")
    @ApiResponses(value = {@ApiResponse(content = @Content(examples = {
        @ExampleObject(name = "Basic sample", value = "{" +
            "  \"BPNL1234567890ZZ\": \"MNR-8101-ID146955.001\"," +
            "  \"BPNL4444444444XX\": \"MNR-7307-AU340474.002\"}")
    }))})
    public Map<String, String> getMaterialNumbers(@RequestParam String ownMaterialNumber) {
        return mprService.getBPNL_To_MaterialNumberMap(ownMaterialNumber);
    }

    @CrossOrigin
    @GetMapping("products")
    @ResponseBody
    @Operation(description = "Returns a list of all products (excluding materials)")
    public List<FrontendMaterialDto> getProducts() {
        return materialService.findAllProducts()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }

    @CrossOrigin
    @GetMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all product-stocks")
    public List<ProductStockDto> getProductStocks() {
        List<ProductStockDto> allProductStocks = productStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        return allProductStocks;
    }

    @CrossOrigin
    @PostMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Creates a new product-stock")
    public ProductStockDto createProductStocks(@RequestBody ProductStockDto productStockDto) {

        ProductStock productStockToCreate = convertToEntity(productStockDto);

        productStockToCreate.setLastUpdatedOn(new Date());

        ProductStock createdProductStock = productStockService.create(productStockToCreate);
        if (createdProductStock == null){
            return null;
        }
        log.info("Created product-stock: " + createdProductStock);

        ProductStockDto productStockToReturn = convertToDto(createdProductStock);

        return productStockToReturn;
    }

    @CrossOrigin
    @PutMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing product-stock")
    public ProductStockDto updateProductStocks(@RequestBody ProductStockDto productStockDto) {
        ProductStock existingProductStock = productStockService.findByUuid(productStockDto.getUuid());
        if (existingProductStock.getUuid() == null) {
            return null;
        }

        existingProductStock.setQuantity(productStockDto.getQuantity());
        existingProductStock.setLastUpdatedOn(new Date());

        existingProductStock = productStockService.update(existingProductStock);
        log.info("Updated product-stock: " + existingProductStock);

        ProductStockDto productStockToReturn = convertToDto(existingProductStock);

        return productStockToReturn;
    }

    private ProductStockDto convertToDto(ProductStock entity) {
        ProductStockDto dto = modelMapper.map(entity, ProductStockDto.class);
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation =
            mprService.find(entity.getMaterial().getOwnMaterialNumber(), entity.getAllocatedToCustomerPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());
        return dto;
    }

    private ProductStock convertToEntity(ProductStockDto dto) {
        ProductStock productStock = modelMapper.map(dto, ProductStock.class);
        Material material = materialService.findByOwnMaterialNumber(dto.getMaterial().getMaterialNumberSupplier());
        productStock.setMaterial(material);
        return productStock;
    }

    @CrossOrigin
    @GetMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all material-stocks")
    public List<MaterialStockDto> getMaterialStocks() {
        List<MaterialStockDto> allMaterialStocks = materialStockService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        log.info(allMaterialStocks.toString());
        return allMaterialStocks;
    }

    @CrossOrigin
    @PostMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Creates a new material-stock")
    public MaterialStockDto createMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {

        MaterialStock materialStockToCreate = convertToEntity(materialStockDto);
        materialStockToCreate.setLastUpdatedOn(new Date());

        MaterialStock createdMaterialStock = materialStockService.create(materialStockToCreate);

        MaterialStockDto materialStockToReturn = convertToDto(createdMaterialStock);

        return materialStockToReturn;
    }

    @CrossOrigin
    @PutMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing material-stock")
    public MaterialStockDto updateMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {
        MaterialStock existingMaterialStock = materialStockService.findByUuid(materialStockDto.getUuid());
        if (existingMaterialStock == null || existingMaterialStock.getUuid() == null) {
            log.warn("unable to find existing stock, exiting");
            return null;
        }

        existingMaterialStock.setQuantity(materialStockDto.getQuantity());
        existingMaterialStock.setLastUpdatedOn(new Date());

        existingMaterialStock = materialStockService.update(existingMaterialStock);

        MaterialStockDto productStockToReturn = convertToDto(existingMaterialStock);

        return productStockToReturn;
    }

    private MaterialStockDto convertToDto(MaterialStock entity) {
        MaterialStockDto dto = modelMapper.map(entity, MaterialStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());
        return dto;
    }

    private MaterialStock convertToEntity(MaterialStockDto dto) {
        MaterialStock stock =  modelMapper.map(dto, MaterialStock.class);
        stock.getMaterial().setOwnMaterialNumber(dto.getMaterial().getMaterialNumberCustomer());
        return stock;
    }

    @CrossOrigin
    @GetMapping("partner-product-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all partner-product-stocks that refer to the given material number")
    public List<PartnerProductStockDto> getPartnerProductStocks(@RequestParam String ownMaterialNumber) {
        return partnerProductStockService.
            findAllByOwnMaterialNumber(ownMaterialNumber)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private PartnerProductStockDto convertToDto(PartnerProductStock entity) {
        PartnerProductStockDto dto =  modelMapper.map(entity, PartnerProductStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
                entity.getSupplierPartner().getUuid());
        dto.getMaterial().setMaterialNumberSupplier(materialPartnerRelation.getPartnerMaterialNumber());

        return dto;
    }

    @CrossOrigin
    @GetMapping("customer")
    @ResponseBody
    @Operation(description = "Returns a list of all Partners that are ordering the given material")
    public List<PartnerDto> getCustomerPartnersOrderingMaterial(@RequestParam String ownMaterialNumber) {
        List<PartnerDto> allCustomerPartners = partnerService.findAllCustomerPartnersForMaterialId(ownMaterialNumber).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return allCustomerPartners;
    }

    @CrossOrigin
    @GetMapping("update-partner-product-stock")
    @ResponseBody
    @Operation(description = "For the given material, all known suppliers will be requested to report their" +
        "current product-stocks. The response body contains a list of those supplier partners that were sent a request." +
        "Please note that these requests are handled asynchronously by the partners, so there are no guarantees, if and " +
        "when the corresponding responses will be available. As soon as a response arrives, it will be available via a " +
        "call to the GET partner-product-stocks endpoint.")
    public List<PartnerDto> triggerPartnerProductStockUpdateForMaterial(@RequestParam String ownMaterialNumber) {

        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        log.info("Found material: " + (materialEntity != null) + " " + ownMaterialNumber);

        List<Partner> allSupplierPartnerEntities = mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);

        for (Partner supplierPartner : allSupplierPartnerEntities) {

            ProductStockRequest productStockRequest = new ProductStockRequest();

            MaterialPartnerRelation materialPartnerRelation = mprService.find(materialEntity, supplierPartner);

            if (materialPartnerRelation == null) {
                log.error("Missing material-partner-relation for " + materialEntity.getOwnMaterialNumber()
                    + " and " + supplierPartner.getBpnl());
                continue;
            }

            ProductStockRequestForMaterial material = new ProductStockRequestForMaterial(
                materialEntity.getOwnMaterialNumber(),
                materialEntity.getMaterialNumberCx(),
                materialPartnerRelation.getPartnerMaterialNumber()
            );
            productStockRequest.getContent().getProductStock().add(material);

            String [] data = edcAdapterService.getContractForRequestApi(supplierPartner.getEdcUrl());
            if(data == null) {
                log.error("failed to obtain request api from " + supplierPartner.getEdcUrl());
                continue;
            }
            String authKey = data[0];
            String authCode = data[1];
            String endpoint = data[2];
            if (applyDataplaneWorkaround) {
                log.info("Applying Dataplane Address Workaround");
                endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
            }

            String cid = data[3];
            MessageHeader messageHeader = new MessageHeader();
            UUID randomUuid = UUID.randomUUID();

            // Avoid randomly choosing a UUID that was already used by this customer.
            while (productStockRequestService.findRequestByHeaderUuid(randomUuid) != null) {
                randomUuid = UUID.randomUUID();
            }
            messageHeader.setRequestId(randomUuid);
            messageHeader.setRespondAssetId(variablesService.getResponseApiAssetId());
            messageHeader.setContractAgreementId(cid);
            messageHeader.setSender(variablesService.getOwnBpnl());
            messageHeader.setSenderEdc(variablesService.getOwnEdcIdsUrl());
            // set receiver per partner
            messageHeader.setReceiver(supplierPartner.getBpnl());
            messageHeader.setUseCase(DT_UseCaseEnum.PURIS);
            messageHeader.setCreationDate(new Date());


            productStockRequest.setHeader(messageHeader);
            productStockRequest.setState(DT_RequestStateEnum.Working);
            productStockRequest = productStockRequestService.createRequest(productStockRequest);
            var test = productStockRequestService.findRequestByHeaderUuid(productStockRequest.getHeader().getRequestId());
            log.debug("Stored in Database " + (test != null) + " " + productStockRequest.getHeader().getRequestId());
            Response response = null;
            try {
                String requestBody = objectMapper.writeValueAsString(productStockRequest);
                response = edcAdapterService.sendDataPullRequest(endpoint, authKey, authCode, requestBody);
                log.debug(response.body().string());
                if(response.code() < 400) {
                    productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Requested);
                    log.debug("Sent request and received HTTP Status code " + response.code());
                    log.debug("Setting request state to " + DT_RequestStateEnum.Requested);
                    productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Requested);
                } else {
                    log.warn("Received HTTP Status Code " + response.code() + " for request " + productStockRequest.getHeader().getRequestId()
                    + " from " + productStockRequest.getHeader().getReceiver());
                    productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
                }
                
            } catch (Exception e) {
                log.error("Failed to send data pull request to " + supplierPartner.getEdcUrl(), e);
                productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
            } finally {
                try {
                    if(response != null) {
                        response.body().close();
                    }
                } catch (Exception e) {
                    log.warn("Failed to close response body");
                }
            }
        }

        List<PartnerDto> allSupplierPartners = allSupplierPartnerEntities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());


        return allSupplierPartners;
    }

    private PartnerDto convertToDto(Partner entity) {
        return modelMapper.map(entity, PartnerDto.class);
    }

}
