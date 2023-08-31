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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DataInjectionCommandLineRunner implements CommandLineRunner {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private MaterialStockService materialStockService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private VariablesService variablesService;


    @Value("${puris.demonstrator.role}")
    private String demoRole;

    private ObjectMapper objectMapper;

    private final String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
    private final String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";

    public DataInjectionCommandLineRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void run(String... args) throws Exception {
        createOwnPartnerEntity();
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
     * Generates and persists a Partner entity that holds all
     * relevant data about the owner of the running instance of
     * the PURIS application.
     */
    private void createOwnPartnerEntity() {
        Partner mySelf;
        if(variablesService.getOwnDefaultBpns()!= null && variablesService.getOwnDefaultBpns().length()!=0) {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getOwnEdcIdsUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpns(),
                variablesService.getOwnDefaultSiteName(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry());
        } else {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getOwnEdcIdsUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry()
            );
        }
        mySelf = partnerService.create(mySelf);
        log.info("Successfully created own Partner Entity: " + (partnerService.findByBpnl(mySelf.getBpnl()) != null));
        if(mySelf != null) {
            log.info(mySelf.toString());
        }
    }

    /**
     * Generates an initial set of data for a customer within the demonstration context.
     *
     * @throws JsonProcessingException
     */
    private void setupCustomerRole() throws JsonProcessingException {
        Partner supplierPartner = createAndGetSupplierPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterialForCustomer();

        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created material: %s", semiconductorMaterial));
        List<Material> materialsFound = materialService.findAllMaterials();
        log.info(String.format("Found Material: %s", materialsFound));
        log.info(String.format("UUID of supplier partner: %s", supplierPartner.getUuid()));
        supplierPartner = partnerService.findByUuid(supplierPartner.getUuid());
        log.info(String.format("Found supplier partner: %s", supplierPartner));
        supplierPartner = partnerService.findByBpns(supplierPartner.getSites().stream().findFirst().get().getBpns());
        log.info("Found supplier partner by bpns: " + (supplierPartner != null));


        MaterialPartnerRelation semiconductorPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            supplierPartner, semiconductorMatNbrSupplier, true, false);
        mprService.create(semiconductorPartnerRelation);
        semiconductorPartnerRelation = mprService.find(semiconductorMaterial, supplierPartner);
        log.info("Found Relation: " + semiconductorPartnerRelation);

        // customer + material
        Partner nonScenarioCustomer = createAndGetNonScenarioCustomer();
        Material centralControlUnitEntity = getNewCentralControlUnitMaterial();
        centralControlUnitEntity = materialService.create(centralControlUnitEntity);
        log.info(String.format("Created Product: %s", centralControlUnitEntity));

        MaterialPartnerRelation ccuPartnerRelation = new MaterialPartnerRelation(centralControlUnitEntity,
            nonScenarioCustomer, "MNR-4177-C", false, true);
        ccuPartnerRelation = mprService.create(ccuPartnerRelation);
        log.info("Found Relation: " + ccuPartnerRelation);

        log.info("All stored Relations: " + mprService.findAll());

        List<Partner> foundPartners = mprService.findAllCustomersForOwnMaterialNumber(centralControlUnitEntity.getOwnMaterialNumber());

        log.info("Customer Partner for CCU: " + foundPartners);

        List<Material> productsFound = materialService.findAllProducts();
        log.info(String.format("Found Products: %s", productsFound));

        productsFound = mprService.findAllProductsThatPartnerBuys(nonScenarioCustomer);
        log.info("Products that customer buys: " + productsFound);

        // Create Material Stock
        MaterialStock materialStockEntity = new MaterialStock(
            semiconductorMaterial,
            5,
            "BPNS4444444444XX",
            new Date()
        );
        materialStockEntity = materialStockService.create(materialStockEntity);
        log.info(String.format("Created materialStock: %s", materialStockEntity));


        // Create PartnerProductStock
        semiconductorMaterial = materialService.findByOwnMaterialNumber(semiconductorMaterial.getOwnMaterialNumber());
        PartnerProductStock partnerProductStockEntity = new PartnerProductStock(
            semiconductorMaterial,
            10,
            supplierPartner.getSites().stream().findFirst().get().getBpns(),
            new Date(),
            supplierPartner
        );
        log.info(String.format("Created partnerProductStock: %s", partnerProductStockEntity));
        partnerProductStockEntity = partnerProductStockService.create(partnerProductStockEntity);
        ProductStockSammDto productStockSammDto = productStockSammMapper.toSamm(partnerProductStockEntity);
        log.info("SAMM-DTO:\n" + objectMapper.writeValueAsString(productStockSammDto));

        log.info("Own Street and Number: " + variablesService.getOwnDefaultStreetAndNumber());
    }

    /**
     * Generates an initial set of data for a supplier within the demonstration context.
     */
    private void setupSupplierRole() {
        Partner customerPartner = createAndGetCustomerPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterialForSupplier();

        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created product: %s", semiconductorMaterial));

        MaterialPartnerRelation semiconductorPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            customerPartner, semiconductorMatNbrCustomer, false, true);
        semiconductorPartnerRelation = mprService.create(semiconductorPartnerRelation);

        log.info("Created Relation " + semiconductorPartnerRelation);

        semiconductorPartnerRelation = mprService.find(semiconductorMaterial, customerPartner);

        log.info("Found Relation " + semiconductorPartnerRelation);

        List<Material> materialsFound = materialService.findAllProducts();
        log.info(String.format("Found product: %s", materialsFound));

        List<Partner> customerPartners = mprService.findAllCustomersForOwnMaterialNumber(semiconductorMaterial.getOwnMaterialNumber());
        log.info(String.format("Found customer partners for semiconductor: %s", customerPartners));


        ProductStock productStockEntity = new ProductStock(
            semiconductorMaterial,
            20,
            "BPNS1234567890ZZ",
            new Date(),
            customerPartner
        );
        productStockEntity = productStockService.create(productStockEntity);
        log.info(String.format("Created productStock: %s", productStockEntity.toString()));

        List<ProductStock> foundProductStocks = productStockService.
            findAllByMaterialNumberCustomer(semiconductorMatNbrCustomer, customerPartner);
        log.info(String.format("Found productStocks by material number and allocated to customer " +
            "bpnl: %s", foundProductStocks));
    }


    /**
     * creates a new customer Partner entity, stores it to
     * the database and returns this entity.
     *
     * @return a reference to the newly created customer
     */
    private Partner createAndGetCustomerPartner() {
        Partner customerPartnerEntity = new Partner(
            "Scenario Customer",
            "http://sokrates-controlplane:8084/api/v1/ids",
            "BPNL4444444444XX",
            "BPNS4444444444XY",
            "Hauptwerk Musterhausen",
            "BPNA4444444444ZZ",
            "Musterstraße 35b",
            "77777 Musterhausen",
            "Germany"
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
     *
     * @return a reference to the newly created supplier
     */
    private Partner createAndGetSupplierPartner() {
        Partner supplierPartnerEntity = new Partner(
            "Scenario Supplier",
            "http://plato-controlplane:8084/api/v1/ids",
            "BPNL1234567890ZZ",
            "BPNS1234567890XY",
            "Konzernzentrale Dudelsdorf",
            "BPNA1234567890AA",
            "Heinrich-Supplier-Straße 1",
            "77785 Dudelsdorf",
            "Germany"
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
     *
     * @return a reference to the newly created non-scenario customer
     */
    private Partner createAndGetNonScenarioCustomer() {
        Partner nonScenarioCustomer = new Partner(
            "Non-Scenario Customer",
            "(None Provided!)>",
            "BPNL2222222222RR",
            "BPNS2222222222XZ",
            "Zentraleinkaufsabteilung",
            "BPNA2222222222HH",
            "54.321N, 8.7654E"
        );
        nonScenarioCustomer = partnerService.create(nonScenarioCustomer);
        log.info(String.format("Created non-scenario customer partner: %s", nonScenarioCustomer));
        nonScenarioCustomer = partnerService.findByUuid(nonScenarioCustomer.getUuid());
        log.info(String.format("Found non-scenario customer partner: %s", nonScenarioCustomer));
        return nonScenarioCustomer;
    }

    private Material getNewSemiconductorMaterialForSupplier() {
        Material material = new Material();
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        material.setProductFlag(true);
        material.setName("semiconductor");
        return material;
    }

    private Material getNewSemiconductorMaterialForCustomer() {
        Material material = new Material();
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        material.setMaterialFlag(true);
        material.setName("semiconductor");
        return material;
    }

    /**
     * creates a new central control unit Material object.
     * Note: this object is not yet stored to the database
     *
     * @return a reference to the newly created central control unit material
     */
    private Material getNewCentralControlUnitMaterial() {
        Material material = new Material();
        material.setOwnMaterialNumber("MNR-4177-S");
        material.setProductFlag(true);
        material.setName("central control unit");
        return material;
    }

    private void createRequest() throws JsonProcessingException {

        ProductStockRequest request = new ProductStockRequest();
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setRequestId(UUID.fromString("4979893e-dd6b-43db-b732-6e48b4ba35b3"));
        messageHeader.setRespondAssetId("product-stock-response-api");
        messageHeader.setContractAgreementId("some cid");
        messageHeader.setSender("BPNL1234567890ZZ");
        messageHeader.setSenderEdc("http://plato-controlplane:8084/api/v1/ids");
        messageHeader.setReceiver("BPNL4444444444XX");
        messageHeader.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeader.setCreationDate(new Date());
        request.setHeader(messageHeader);

        var productStock = request.getContent().getProductStock();
        ProductStockRequestForMaterial rfm = new ProductStockRequestForMaterial("CU-MNR",
            null, "SU-MNR");
        productStock.add(rfm);

        rfm = new ProductStockRequestForMaterial("OtherCU-MNR",
            null, "OtherSU-MNR");
        productStock.add(rfm);
        request.setState(DT_RequestStateEnum.WORKING);

        request = productStockRequestService.createRequest(request);


        String stringOutput = objectMapper.writeValueAsString(request);
        log.info("SAMPLE-Request\n" + objectMapper.readTree(stringOutput).toPrettyString());

        var deserializedRequest = objectMapper.readValue(stringOutput, ProductStockRequest.class);
        log.info(deserializedRequest.toString());


    }
}
