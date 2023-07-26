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
package org.eclipse.tractusx.puris.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.RequestMarshallingService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DataInjectionCommandLineRunner implements CommandLineRunner {

    @Autowired
    private ModelMapper modelMapper;


    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialStockService materialStockService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private RequestService requestService;


    @Value("${puris.demonstrator.role}")
    private String demoRole;

    @Autowired
    private RequestMarshallingService requestMarshallingService;

    private ObjectMapper objectMapper;

    public DataInjectionCommandLineRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void run(String... args) throws Exception {

        log.info("Creating setup for " + demoRole.toUpperCase());
        if (demoRole.equals("supplier")) {
            setupSupplierRole();
        } else if (demoRole.equals(("customer"))) {
            setupCustomerRole();
            createRequest();
        } else {
            log.info("No role specific setup was created");
        }
    }

    /**
     * Generates an initial set of data for a customer within the demonstration context. 
     * @throws JsonProcessingException
     */
    private void setupCustomerRole() throws JsonProcessingException {
        Partner supplierPartner = createAndGetSupplierPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterial();
        semiconductorMaterial.addPartnerToSuppliedByPartners(supplierPartner);
        // adjust flags for customer role
        semiconductorMaterial.setMaterialFlag(true);
        semiconductorMaterial.setProductFlag(true);

        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created material: %s", semiconductorMaterial));
        List<Material> materialsFound = materialService.findAllMaterials();
        log.info(String.format("Found Material: %s", materialsFound));
        log.info(String.format("UUID of supplier partner: %s", supplierPartner.getUuid()));
        supplierPartner = partnerService.findByUuid(supplierPartner.getUuid());
        log.info(String.format("Found supplier partner: %s", supplierPartner));
        log.info(String.format("Relationship to material: %s", supplierPartner.getSuppliesMaterials()));

        // customer + material
        Partner nonScenarioCustomer = createAndGetNonScenarioCustomer();
        Material centralControlUnitEntity = getNewCentralControlUnitMaterial();
        centralControlUnitEntity.addPartnerToOrderedByPartners(nonScenarioCustomer);
        centralControlUnitEntity = materialService.create(centralControlUnitEntity);
        log.info(String.format("Created Product: %s", centralControlUnitEntity));
        List<Material> productsFound = materialService.findAllProducts();
        log.info(String.format("Found Products: %s", productsFound));

        centralControlUnitEntity =
                materialService.findProductByMaterialNumberCustomer(centralControlUnitEntity.getMaterialNumberCustomer());
        log.info(String.format("Found product by materialNumber customer: %s",
                centralControlUnitEntity));
        nonScenarioCustomer = partnerService.findByUuid(nonScenarioCustomer.getUuid());
        log.info(String.format("Relationship to product: %s",
                nonScenarioCustomer.getOrdersProducts()));

        centralControlUnitEntity =
                materialService.findProductByMaterialNumberCustomer(centralControlUnitEntity.getMaterialNumberCustomer());
        log.info(String.format("Found product by materialNumber customer: %s",
                centralControlUnitEntity));

        Material existingMaterial =
                materialService.findByUuid(semiconductorMaterial.getUuid());
        log.info(String.format("Found existingMaterial by uuid: %s",
                existingMaterial));

        Material existingProduct =
                materialService.findProductByMaterialNumberCustomer(centralControlUnitEntity.getMaterialNumberCustomer());
        log.info(String.format("Found existingProduct by customer number: %s",
                existingProduct));

        List<Material> existingProducts =
                materialService.findAllProducts();
        log.info(String.format("Found existingProducts by product flag true: %s",
                existingProducts));

        log.info(String.format("Relationship centralControlUnitEntity -> orderedByPartners: %s",
                centralControlUnitEntity.getOrderedByPartners().toString()));

        // Create Material Stock
        MaterialStock materialStockEntity = new MaterialStock(
                semiconductorMaterial,
                20,
                "BPNS4444444444XX",
                new Date()
        );
        materialStockEntity = materialStockService.create(materialStockEntity);
        log.info(String.format("Created materialStock: %s", materialStockEntity));
        List<MaterialStock> foundMaterialStocks =
                materialStockService.findAllByMaterialNumberCustomer(semiconductorMaterial.getMaterialNumberCustomer());
        log.info(String.format("Found materialStock: %s", foundMaterialStocks));

        // Create PartnerProductStock
        semiconductorMaterial = materialService.findByUuid(semiconductorMaterial.getUuid());
        PartnerProductStock partnerProductStockEntity = new PartnerProductStock(
                semiconductorMaterial,
                20,
                supplierPartner.getSiteBpns(),
                new Date(),
                supplierPartner
        );
        partnerProductStockEntity = partnerProductStockService.create(partnerProductStockEntity);
        log.info(String.format("Created partnerProductStock: %s", partnerProductStockEntity));
        ProductStockDto productStockDto = modelMapper.map(partnerProductStockEntity,
                ProductStockDto.class);
        ProductStockSammDto productStockSammDto = productStockSammMapper.toSamm(productStockDto);
        log.info(objectMapper.writeValueAsString(productStockSammDto));
    }
    /**
     * Generates an initial set of data for a supplier within the demonstration context. 
     */
    private void setupSupplierRole() {
        Partner customerPartner = createAndGetCustomerPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterial();
        semiconductorMaterial.addPartnerToOrderedByPartners(customerPartner);
        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created product: %s", semiconductorMaterial));

        List<Material> materialsFound = materialService.findAllProducts();
        log.info(String.format("Found product: %s", materialsFound));
        log.info(String.format("Found customer partner: %s", customerPartner));
        log.info(String.format("Relationship to material: %s", customerPartner.getOrdersProducts()));

        ProductStock productStockEntity = new ProductStock(
                semiconductorMaterial,
                20,
                "BPNS1234567890ZZ",
                new Date(),
                customerPartner
        );
        productStockEntity = productStockService.create(productStockEntity);
        log.info(String.format("Created productStock: %s", productStockEntity.toString()));
        List<ProductStock> foundProductStocks =
                productStockService
                        .findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
                                semiconductorMaterial.getMaterialNumberCustomer(),
                                customerPartner.getBpnl());
        log.info(String.format("Found productStocks by material number and allocated to customer " +
                "bpnl: %s", foundProductStocks));
    }


    /**
     * creates a new customer Partner entity, stores it to
     * the database and returns this entity. 
     * @return a reference to the newly created customer
     */
    private Partner createAndGetCustomerPartner() {
        Partner customerPartnerEntity = new Partner(
                "Scenario Customer",
                true,
                false,
                "http://sokrates-controlplane:8084/api/v1/ids",
                "BPNL4444444444XX",
                "BPNS4444444444XX"
        );
        customerPartnerEntity = partnerService.create(customerPartnerEntity);
        log.info(String.format("Created customer partner: %s", customerPartnerEntity));
        customerPartnerEntity = partnerService.findByUuid(customerPartnerEntity.getUuid());
        log.info(String.format("Found customer partner: %s", customerPartnerEntity));
        return customerPartnerEntity;
    }

    /**
     * creates a new supplier Partner entity, stores it to
     * the database and returns this entity. 
     * @return a reference to the newly created supplier
     */
    private Partner createAndGetSupplierPartner() {
        Partner supplierPartnerEntity = new Partner(
                "Scenario Supplier",
                false,
                true,
                "http://plato-controlplane:8084/api/v1/ids",
                "BPNL1234567890ZZ",
                "BPNS1234567890ZZ"
        );
        supplierPartnerEntity = partnerService.create(supplierPartnerEntity);
        log.info(String.format("Created supplier partner: %s", supplierPartnerEntity));
        supplierPartnerEntity = partnerService.findByUuid(supplierPartnerEntity.getUuid());
        log.info(String.format("Found supplier partner: %s", supplierPartnerEntity));
        return supplierPartnerEntity;
    }

    /**
     * creates a new (non-scenario) customer entity, stores
     * it to the database and returns this entity. 
     * @return a reference to the newly created non-scenario customer
     */
    private Partner createAndGetNonScenarioCustomer() {
        Partner nonScenarioCustomer = new Partner(
                "Non-Scenario Customer",
                true,
                false,
                "(None Provided!)>",
                "BPNL2222222222RR",
                "BPNL2222222222RR"
        );
        nonScenarioCustomer = partnerService.create(nonScenarioCustomer);
        log.info(String.format("Created non-scenario customer partner: %s", nonScenarioCustomer));
        nonScenarioCustomer = partnerService.findByUuid(nonScenarioCustomer.getUuid());
        log.info(String.format("Found non-scenario customer partner: %s", nonScenarioCustomer));
        return nonScenarioCustomer;
    }

    /**
     * creates a new semiconductor Material object. 
     * Note: this object is not yet stored to the database
     * @return a reference to the newly created semiconductor material
     */
    private Material getNewSemiconductorMaterial() {
        return new Material(
                false,
                true,
                "MNR-7307-AU340474.002",
                "MNR-8101-ID146955.001",
                "",
                "semiconductor"
        );
    }

    /**
     * creates a new central control unit Material object. 
     * Note: this object is not yet stored to the database 
     * @return a reference to the newly created central control unit material
     */
    private Material getNewCentralControlUnitMaterial() {
        return new Material(
                false,
                true,
                "MNR-4177-C",
                "MNR-4177-S",
                "0",
                "central control unit"
        );
    }

    private void createRequest() throws JsonProcessingException {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setRequestId(UUID.fromString("4979893e-dd6b-43db-b732-6e48b4ba35b3"));
        messageHeaderDto.setRespondAssetId("product-stock-response-api");
        messageHeaderDto.setContractAgreementId("some cid");
        messageHeaderDto.setSender("BPNL1234567890ZZ");
        messageHeaderDto.setSenderEdc("http://plato-controlplane:8084/api/v1/ids");
        messageHeaderDto.setReceiver("http://sokrates-controlplane:8084/api/v1/ids");
        messageHeaderDto.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeaderDto.setCreationDate(new Date());

        log.info(objectMapper.writeValueAsString(messageHeaderDto));

        List<ProductStockRequestForMaterialDto> messageContentDtos = new ArrayList<>();

        ProductStockRequestForMaterialDto messageContentDto = new ProductStockRequestForMaterialDto();
        messageContentDto.setMaterialNumberCatenaX("CX-MNR");
        messageContentDto.setMaterialNumberCustomer("CU-MNR");
        messageContentDto.setMaterialNumberSupplier("SU-MNR");
        messageContentDtos.add(messageContentDto);
        messageContentDto = new ProductStockRequestForMaterialDto();
        messageContentDto.setMaterialNumberCatenaX("OtherCX-MNR");
        messageContentDto.setMaterialNumberCustomer("OtherCU-MNR");
        messageContentDto.setMaterialNumberSupplier("OtherSU-MNR");
        messageContentDtos.add(messageContentDto);

        RequestDto requestDto = new RequestDto(
                DT_RequestStateEnum.RECEIPT,
                messageHeaderDto,
                messageContentDtos
        );
        Request createdRequest = requestService.createRequest(modelMapper.map(requestDto,
            Request.class));
        log.info(String.format("Created Request: %s", createdRequest));
        log.info(createdRequest.getPayload().get(0).getClass().toString());

        log.info("Testing RequestMarshallingService:");
        String transformationTest = requestMarshallingService.transformRequest(requestDto);
        log.info("transformed request to be sent:\n" + transformationTest);

        ProductStockRequestDto productStockRequestDto = requestMarshallingService.transformToProductStockRequestDto(transformationTest);
        log.info("unmarshalled the same request as productStockRequestDto: \n" + productStockRequestDto.toString());

    }
}
