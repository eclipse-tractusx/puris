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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.*;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("stockView")
@Slf4j
public class StockController {

    @Value("${edc.dataplane.public.port}")
    private String dataPlanePort;

    @Value("${edc.controlplane.host}")
    private String dataPlaneHost;

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
    private RequestService requestService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

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
        List<ProductStock> productStocks = productStockService.findAll();
        log.info(String.format("First productStock: %s", productStocks.get(0)));
        ProductStockDto productStockDto = convertToDto(productStocks.get(0));
        log.info(String.format("First productStockDto: %s", productStockDto));

        log.info(String.format("Found %d ProductStocks", productStocks.size()));
        /*
        List<ProductStockDto> allProductStocks = productStockService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());*/

        List<ProductStockDto> result = new ArrayList<>();
        result.add(productStockDto);
        return result;
    }

    @CrossOrigin
    @PostMapping("product-stocks")
    @ResponseBody
    public ProductStockDto createProductStocks(@RequestBody ProductStockDto productStockDto) {

        ProductStock productStockToCreate = convertToEntity(productStockDto);
        productStockToCreate.setLastUpdatedOn(new Date());

        ProductStock createdProductStock = productStockService.create(productStockToCreate);

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
    public List<PartnerDto> getCustomerPartnersOrderingMaterial(@RequestParam UUID materialUuid) {
        List<PartnerDto> allCustomerPartners = partnerService.findAllCustomerPartnersForMaterialId(materialUuid).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return allCustomerPartners;
    }

    @CrossOrigin
    @GetMapping("update-partner-product-stock")
    @ResponseBody
    public List<PartnerDto> triggerPartnerProductStockUpdateForMaterial(@RequestParam String ownMaterialNumber) {

        Material materialEntity = materialService.findMaterialByMaterialNumberCustomer(ownMaterialNumber);

        List<Partner> allSupplierPartnerEntities =
                partnerService.findAllSupplierPartnersForMaterialId(materialEntity.getUuid());


        List<ProductStockRequestForMaterialDto> messageContentDtos = new ArrayList<>();

        // Message Content for all requests
        ProductStockRequestForMaterialDto materialDto = new ProductStockRequestForMaterialDto(
                materialEntity.getMaterialNumberCustomer(),
                materialEntity.getMaterialNumberCx(),
                materialEntity.getMaterialNumberSupplier()
        );
        messageContentDtos.add(materialDto);

        for (Partner supplierPartner : allSupplierPartnerEntities) {

            String [] data = edcAdapterService.getContractForRequestApi(supplierPartner.getEdcUrl());
            if(data == null) {
                log.error("failed to obtain request api from " + supplierPartner.getEdcUrl());
                continue;
            }
            String authCode = data[0];
            String cid = data[1];
            MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
            messageHeaderDto.setRequestId(UUID.randomUUID());
            messageHeaderDto.setRespondAssetId("product-stock-response-api");
            messageHeaderDto.setContractAgreementId(cid);
            messageHeaderDto.setSender(ownBpnl); 
            messageHeaderDto.setSenderEdc(ownEdcIdsUrl);
            // set receiver per partner
            messageHeaderDto.setReceiver(supplierPartner.getEdcUrl());
            messageHeaderDto.setUseCase(DT_UseCaseEnum.PURIS);
            messageHeaderDto.setCreationDate(new Date());

            ProductStockRequestDto requestDto = new ProductStockRequestDto(
                    DT_RequestStateEnum.REQUESTED,
                    messageHeaderDto,
                    messageContentDtos
            );

            
            Request request = modelMapper.map(requestDto, Request.class);
            request = requestService.createRequest(request);
            var test = requestService.findRequestByHeaderUuid(requestDto.getHeader().getRequestId());
            log.debug("Stored in Database " + (test != null) + " " + requestDto.getHeader().getRequestId());

            try {
                String requestBody = objectMapper.writeValueAsString(requestDto);
                var response = edcAdapterService.sendDataPullRequest("http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public", authCode, requestBody);
                log.debug(response.body().string());
            } catch (Exception e) {
                log.error("failed to send data pull request to " + supplierPartner.getEdcUrl(), e);
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
