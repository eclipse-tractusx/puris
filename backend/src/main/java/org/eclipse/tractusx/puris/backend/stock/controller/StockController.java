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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.*;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;

    @Value("${own.bpns}")
    private String ownBpns;

    @Value("${own.bpnl}")
    private String ownBpnl;


    @CrossOrigin
    @GetMapping("materials")
    @ResponseBody
    public List<MaterialDto> getMaterials() {

        List<MaterialDto> allMaterials = materialService.findAllMaterials().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        allMaterials.stream().forEach(m -> System.out.println(m));

        return allMaterials;

    }

    private MaterialDto convertToDto(Material entity) {
        return modelMapper.map(entity, MaterialDto.class);
    }

    @CrossOrigin
    @GetMapping("products")
    @ResponseBody
    public List<MaterialDto> getProducts() {

        List<MaterialDto> allProducts = materialService.findAllProducts().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return allProducts;
    }

    @CrossOrigin
    @GetMapping("product-stocks")
    @ResponseBody
    public List<ProductStockDto> getProductStocks() {

        List<ProductStockDto> allProductStocks = productStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        return allProductStocks;
    }

    @CrossOrigin
    @PostMapping("product-stocks")
    @ResponseBody
    public ProductStockDto createProductStocks(@RequestBody ProductStockDto productStockDto) {

        ProductStock productStockToCreate = convertToEntity(productStockDto);

        productStockToCreate.setLastUpdatedOn(new Date());

        ProductStock createdProductStock = productStockService.create(productStockToCreate);
        if (createdProductStock == null){
            return null;
        }

        ProductStockDto productStockToReturn = convertToDto(createdProductStock);

        return productStockToReturn;
    }

    @CrossOrigin
    @PutMapping("product-stocks")
    @ResponseBody
    public ProductStockDto updateProductStocks(@RequestBody ProductStockDto productStockDto) {
        ProductStock existingProductStock = productStockService.findByUuid(productStockDto.getUuid());
        if (existingProductStock.getUuid() == null) {
            return null;
        }

        existingProductStock.setQuantity(productStockDto.getQuantity());
        existingProductStock.setLastUpdatedOn(new Date());

        existingProductStock = productStockService.create(existingProductStock);

        ProductStockDto productStockToReturn = convertToDto(existingProductStock);

        return productStockToReturn;
    }

    private ProductStockDto convertToDto(ProductStock entity) {
        return modelMapper.map(entity, ProductStockDto.class);
    }

    private ProductStock convertToEntity(ProductStockDto dto) {
        return modelMapper.map(dto, ProductStock.class);
    }

    @CrossOrigin
    @GetMapping("material-stocks")
    @ResponseBody
    public List<MaterialStockDto> getMaterialStocks() {
        List<MaterialStockDto> allMaterialStocks = materialStockService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return allMaterialStocks;
    }

    @CrossOrigin
    @PostMapping("material-stocks")
    @ResponseBody
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
    public MaterialStockDto updateMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {
        MaterialStock existingMaterialStock = materialStockService.findByUuid(materialStockDto.getUuid());
        if (existingMaterialStock.getUuid() == null) {
            return null;
        }

        existingMaterialStock.setQuantity(materialStockDto.getQuantity());
        existingMaterialStock.setLastUpdatedOn(new Date());

        existingMaterialStock = materialStockService.create(existingMaterialStock);

        MaterialStockDto productStockToReturn = convertToDto(existingMaterialStock);

        return productStockToReturn;
    }

    private MaterialStockDto convertToDto(MaterialStock entity) {
        return modelMapper.map(entity, MaterialStockDto.class);
    }

    private MaterialStock convertToEntity(MaterialStockDto dto) {
        return modelMapper.map(dto, MaterialStock.class);
    }

    @CrossOrigin
    @GetMapping("partner-product-stocks")
    @ResponseBody
    public List<PartnerProductStockDto> getPartnerProductStocks() {
        List<PartnerProductStockDto> allPartnerProductStocks = partnerProductStockService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return allPartnerProductStocks;
    }

    private PartnerProductStockDto convertToDto(PartnerProductStock entity) {
        return modelMapper.map(entity, PartnerProductStockDto.class);
    }

    @CrossOrigin
    @GetMapping("customer")
    @ResponseBody
    public List<PartnerDto> getCustomerPartnersOrderingMaterial(@RequestParam String ownMaterialNumber) {
        List<PartnerDto> allCustomerPartners = partnerService.findAllCustomerPartnersForMaterialId(ownMaterialNumber).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return allCustomerPartners;
    }

    @CrossOrigin
    @GetMapping("update-partner-product-stock")
    @ResponseBody
    public List<PartnerDto> triggerPartnerProductStockUpdateForMaterial(@RequestParam String ownMaterialNumber) {

        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        log.info("Found material: " + (materialEntity != null));
        log.info("All materials: " + materialService.findAllMaterials());

        List<Partner> allSupplierPartnerEntities = mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);

        for (Partner supplierPartner : allSupplierPartnerEntities) {

            ProductStockRequest productStockRequest = new ProductStockRequest();

            MaterialPartnerRelation materialPartnerRelation = mprService.find(materialEntity, supplierPartner);

            if (materialPartnerRelation == null) {
                log.error("Missing material-partner-relation for " + materialEntity.getOwnMaterialNumber() + " and " + supplierPartner.getBpnl());
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
            messageHeader.setSender(ownBpnl);
            messageHeader.setSenderEdc(ownEdcIdsUrl);
            // set receiver per partner
            messageHeader.setReceiver(supplierPartner.getBpnl());
            messageHeader.setUseCase(DT_UseCaseEnum.PURIS);
            messageHeader.setCreationDate(new Date());


            productStockRequest.setHeader(messageHeader);
            productStockRequest.setState(DT_RequestStateEnum.WORKING);
            productStockRequest = productStockRequestService.createRequest(productStockRequest);
            var test = productStockRequestService.findRequestByHeaderUuid(productStockRequest.getHeader().getRequestId());
            log.debug("Stored in Database " + (test != null) + " " + productStockRequest.getHeader().getRequestId());
            Response response = null;
            try {
                String requestBody = objectMapper.writeValueAsString(productStockRequest);
                response = edcAdapterService.sendDataPullRequest(endpoint, authKey, authCode, requestBody);
                log.debug(response.body().string());
                if(response.code() < 400) {
                    productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.REQUESTED);
                    log.debug("Sent request and received HTTP Status code " + response.code());
                    log.debug("Setting request state to " + DT_RequestStateEnum.REQUESTED);
                    productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.REQUESTED);
                } else {
                    log.warn("Receviced HTTP Status Code " + response.code() + " for request " + productStockRequest.getHeader().getRequestId()
                    + " from " + productStockRequest.getHeader().getReceiver());
                    productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.ERROR);
                }
                
            } catch (Exception e) {
                log.error("Failed to send data pull request to " + supplierPartner.getEdcUrl(), e);
                productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.ERROR);
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
